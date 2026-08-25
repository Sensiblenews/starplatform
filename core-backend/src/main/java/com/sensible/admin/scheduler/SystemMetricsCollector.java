package com.sensible.admin.scheduler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sensible.common.util.RequestMetrics;

/**
 * 비용 관련 지표를 주기적으로 모아 두는 수집기.
 *
 * 시스템 패널의 기존 status.json은 전부 순간값이라 "지금 몇 %"만 알 수 있다.
 * 과금은 "시간당 몇 GB 나갔나"로 결정되므로 델타(구간값)가 필요하고,
 * 델타는 이전 스냅샷이 있어야 계산된다. 그 이전 스냅샷을 여기서 보관한다.
 *
 * 보관은 JVM 메모리 링버퍼(기본 24시간치)만 쓴다. DB 테이블·Redis 적재는 하지 않으므로
 * WAS 재시작 시 이력은 사라진다. 누적 표시는 "수집 시작 이후" 기준이다.
 *
 * 무거운 지표(디렉터리 용량)는 5초 폴링에서 분리해 별도 주기로만 계산하고 캐시한다.
 */
@Component
public class SystemMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(SystemMetricsCollector.class);

    /** 링버퍼 기본 크기. 5분 간격 × 288 = 24시간 */
    private static final int DEFAULT_HISTORY_SIZE = 288;

    /** 수집 주기(분). @Scheduled cron과 맞춰야 한다 */
    private static final int COLLECT_INTERVAL_MINUTES = 5;

    private static final long HOUR_MS = 3600000L;

    /** 디렉터리 1회 스캔 시 방문할 최대 파일 수 */
    private static final int DEFAULT_DIR_SCAN_MAX_FILES = 200000;

    /** 디렉터리 1회 스캔 시간 상한(ms) */
    private static final long DEFAULT_DIR_SCAN_BUDGET_MS = 20000L;

    private static final long MB = 1024L * 1024L;
    private static final long GB = 1024L * 1024L * 1024L;

    @Resource(name = "config")
    private Properties config;

    /** 5분 간격 표본 링버퍼 */
    private final Deque<Sample> history = new ArrayDeque<Sample>();

    /** 직전 수집 시점의 누적값 */
    private Sample previous;

    /** 수집 시작 시각 (누적 표시 기준점) */
    private volatile long collectStartedAt;

    /** 당일 누적 (자정 리셋) */
    private long todayTxBytes;
    private long todayRxBytes;
    private long todayRequests;
    private long todayAppBytes;

    /** 디렉터리 용량 캐시 (별도 주기로만 갱신) */
    private volatile Map<String, DirStat> directoryCache = Collections.emptyMap();
    private volatile long directoryScannedAt;

    // ==========================================
    // 스케줄
    // ==========================================

    /** 5분 간격 지표 수집 */
    @Scheduled(cron = "0 0/5 * * * *")
    public void collect() {
        try {
            collectOnce(System.currentTimeMillis());
        } catch (Throwable t) {
            logger.warn("[Metrics] 지표 수집 실패: {}", t.toString());
        }
    }

    /** 디렉터리 용량 스캔 (무거운 작업이라 30분 간격) */
    @Scheduled(cron = "0 3/30 * * * *")
    public void scanDirectories() {
        try {
            Map<String, String> targets = configuredDirectories();
            Map<String, DirStat> scanned = new LinkedHashMap<String, DirStat>();
            for (Map.Entry<String, String> e : targets.entrySet()) {
                scanned.put(e.getKey(), scanDirectory(new File(e.getValue()),
                        DEFAULT_DIR_SCAN_MAX_FILES, DEFAULT_DIR_SCAN_BUDGET_MS));
            }
            directoryCache = scanned;
            directoryScannedAt = System.currentTimeMillis();
        } catch (Throwable t) {
            logger.warn("[Metrics] 디렉터리 스캔 실패: {}", t.toString());
        }
    }

    /** 당일 누적 리셋 */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDaily() {
        synchronized (history) {
            todayTxBytes = 0;
            todayRxBytes = 0;
            todayRequests = 0;
            todayAppBytes = 0;
        }
    }

    // ==========================================
    // 수집 본체
    // ==========================================

    /** 표본 1개를 뜨고 링버퍼에 넣는다. 테스트에서 시각을 넘겨 호출할 수 있도록 분리 */
    void collectOnce(long now) {
        Sample s = new Sample();
        s.time = now;

        long[] net = readNetworkBytes();
        s.netRxTotal = net[0];
        s.netTxTotal = net[1];
        s.netAvailable = (net[0] >= 0 && net[1] >= 0);

        RequestMetrics.Snapshot snap = RequestMetrics.snapshot();
        s.requestTotal = snap.requests;
        s.appBytesTotal = snap.bytes;

        long[] disk = readDiskBytes();
        s.diskTotal = disk[0];
        s.diskUsed = disk[1];
        s.diskFree = disk[2];

        s.cpuPercent = readCpuPercent();
        s.heapPercent = readHeapPercent();

        synchronized (history) {
            if (collectStartedAt == 0) {
                collectStartedAt = now;
            }
            if (previous != null) {
                s.netRxDelta = positiveDelta(s.netRxTotal, previous.netRxTotal, s.netAvailable);
                s.netTxDelta = positiveDelta(s.netTxTotal, previous.netTxTotal, s.netAvailable);
                s.requestDelta = positiveDelta(s.requestTotal, previous.requestTotal, true);
                s.appBytesDelta = positiveDelta(s.appBytesTotal, previous.appBytesTotal, true);
                s.diskUsedDelta = s.diskUsed - previous.diskUsed;

                todayRxBytes += s.netRxDelta;
                todayTxBytes += s.netTxDelta;
                todayRequests += s.requestDelta;
                todayAppBytes += s.appBytesDelta;
            }
            previous = s;
            history.addLast(s);
            while (history.size() > historySize()) {
                history.removeFirst();
            }
        }
    }

    /**
     * 누적 카운터의 증가분. 카운터가 되감기면(재부팅·인터페이스 리셋) 0으로 본다.
     */
    static long positiveDelta(long current, long prev, boolean available) {
        if (!available || current < 0 || prev < 0 || current < prev) {
            return 0L;
        }
        return current - prev;
    }

    // ==========================================
    // 지표 읽기
    // ==========================================

    /**
     * NIC 누적 바이트를 읽는다. 반환 {rx, tx}, 실패 시 {-1, -1}.
     * 정적 파일까지 포함한 실제 아웃바운드 총량이라 과금 근사치로는 이 값이 가장 정확하다.
     * Linux 전용이므로 macOS 로컬 개발에서는 -1이 나오고 화면에서 숨겨진다.
     */
    long[] readNetworkBytes() {
        BufferedReader reader = null;
        try {
            File procFile = new File("/proc/net/dev");
            if (!procFile.exists()) {
                return new long[] { -1L, -1L };
            }
            List<String> lines = new ArrayList<String>();
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(procFile), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return parseProcNetDev(lines, networkInterfaceFilter());
        } catch (Throwable t) {
            return new long[] { -1L, -1L };
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * /proc/net/dev 본문을 파싱해 {rx, tx} 누적 바이트를 돌려준다.
     *
     * 형식: "  eth0: rxBytes rxPackets ... txBytes txPackets ..."
     * 콜론 뒤 0번째가 수신 바이트, 8번째가 송신 바이트다.
     *
     * @param filter "auto"면 lo를 제외한 전체 인터페이스 합산, 그 외에는 해당 인터페이스만
     */
    static long[] parseProcNetDev(List<String> lines, String filter) {
        long rx = 0;
        long tx = 0;
        boolean matched = false;
        boolean auto = (filter == null || filter.trim().isEmpty() || "auto".equalsIgnoreCase(filter.trim()));

        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            int colon = raw.indexOf(':');
            if (colon < 0) {
                continue; // 헤더 2줄
            }
            String name = raw.substring(0, colon).trim();
            if (name.isEmpty()) {
                continue;
            }
            if (auto) {
                if ("lo".equals(name)) {
                    continue; // 루프백은 외부 트래픽이 아니다
                }
            } else if (!name.equals(filter.trim())) {
                continue;
            }

            String[] cols = raw.substring(colon + 1).trim().split("\\s+");
            if (cols.length < 9) {
                continue;
            }
            try {
                rx += Long.parseLong(cols[0]);
                tx += Long.parseLong(cols[8]);
                matched = true;
            } catch (NumberFormatException ignore) {
                // 깨진 줄은 건너뛴다
            }
        }
        return matched ? new long[] { rx, tx } : new long[] { -1L, -1L };
    }

    /** 루트 파일시스템 용량. 반환 {total, used, free}, 실패 시 {-1,-1,-1} */
    private long[] readDiskBytes() {
        try {
            String path = getConfig("monitor.disk.path", "/");
            File disk = new File(path);
            long total = disk.getTotalSpace();
            long usable = disk.getUsableSpace();
            if (total <= 0) {
                return new long[] { -1L, -1L, -1L };
            }
            return new long[] { total, total - usable, usable };
        } catch (Throwable t) {
            return new long[] { -1L, -1L, -1L };
        }
    }

    private int readCpuPercent() {
        try {
            java.lang.management.OperatingSystemMXBean os =
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean) {
                double load = ((com.sun.management.OperatingSystemMXBean) os).getSystemCpuLoad();
                if (load >= 0) {
                    return (int) Math.round(load * 100);
                }
            }
        } catch (Throwable ignore) {
            // 미지원 환경
        }
        return -1;
    }

    private int readHeapPercent() {
        try {
            java.lang.management.MemoryUsage heap =
                    java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            if (heap.getMax() > 0) {
                return (int) ((double) heap.getUsed() / heap.getMax() * 100);
            }
        } catch (Throwable ignore) {
            // 무시
        }
        return -1;
    }

    // ==========================================
    // 디렉터리 스캔
    // ==========================================

    /** 감시할 디렉터리 목록 (설정에 값이 없으면 대상에서 제외) */
    private Map<String, String> configuredDirectories() {
        Map<String, String> targets = new LinkedHashMap<String, String>();
        putIfConfigured(targets, "upload", "monitor.dir.upload");
        putIfConfigured(targets, "log", "monitor.dir.log");
        putIfConfigured(targets, "temp", "monitor.dir.temp");
        return targets;
    }

    private void putIfConfigured(Map<String, String> targets, String label, String key) {
        String v = getConfig(key, "");
        if (v != null && !v.trim().isEmpty()) {
            targets.put(label, v.trim());
        }
    }

    /**
     * 디렉터리 용량을 잰다. 파일 수·시간 상한을 두고, 상한에 걸리면 truncated로 표시한다.
     * 상한이 없으면 파일이 많은 업로드 디렉터리에서 스캔 자체가 디스크 부하가 된다.
     */
    static DirStat scanDirectory(File root, int maxFiles, long budgetMs) {
        DirStat stat = new DirStat();
        stat.path = (root != null) ? root.getAbsolutePath() : "";
        if (root == null || !root.exists() || !root.isDirectory()) {
            stat.exists = false;
            return stat;
        }
        stat.exists = true;

        long deadline = System.currentTimeMillis() + budgetMs;
        Deque<File> stack = new ArrayDeque<File>();
        stack.push(root);

        while (!stack.isEmpty()) {
            if (stat.fileCount >= maxFiles || System.currentTimeMillis() > deadline) {
                stat.truncated = true;
                break;
            }
            File dir = stack.pop();
            File[] children = dir.listFiles();
            if (children == null) {
                continue; // 권한 없음 등
            }
            for (File child : children) {
                if (child.isDirectory()) {
                    stack.push(child);
                } else {
                    stat.fileCount++;
                    stat.bytes += child.length();
                    if (stat.fileCount >= maxFiles) {
                        stat.truncated = true;
                        break;
                    }
                }
            }
        }
        return stat;
    }

    // ==========================================
    // 조회 (컨트롤러용)
    // ==========================================

    /** 트래픽 요약 */
    public Map<String, Object> getTrafficSummary() {
        Map<String, Object> m = new HashMap<String, Object>();
        List<Sample> snap = snapshotHistory();
        Sample last = snap.isEmpty() ? null : snap.get(snap.size() - 1);

        boolean available = (last != null && last.netAvailable);
        m.put("trafficAvailable", available);
        m.put("trafficSource", available ? "/proc/net/dev" : "미지원 환경");
        m.put("sampleCount", snap.size());
        m.put("collectStartedAt", collectStartedAt);

        synchronized (history) {
            m.put("todayOutGb", round2((double) todayTxBytes / GB));
            m.put("todayInGb", round2((double) todayRxBytes / GB));
            m.put("todayAppOutGb", round2((double) todayAppBytes / GB));
        }

        m.put("last5mOutMb", round2((double) ((last != null) ? last.netTxDelta : 0L) / MB));
        m.put("lastHourOutGb", round2((double) sumWindow(snap, HOUR_MS, Field.TX) / GB));
        m.put("last24hOutGb", round2((double) sumWindow(snap, 24 * HOUR_MS, Field.TX) / GB));

        // 앱이 낸 응답 바이트 / NIC 송신 바이트 = 앱 기여 비중
        long nicHour = sumWindow(snap, HOUR_MS, Field.TX);
        long appHour = sumWindow(snap, HOUR_MS, Field.APP_BYTES);
        m.put("lastHourAppMb", round2((double) appHour / MB));
        m.put("appSharePercent", (nicHour > 0) ? (int) Math.round((double) appHour / nicHour * 100) : -1);

        // 지금 속도로 한 달을 채웠을 때의 예상 아웃바운드
        double perHourGb = (double) nicHour / GB;
        m.put("projectedMonthlyOutGb", round2(perHourGb * 24 * 30));
        return m;
    }

    /** 요청량 요약 */
    public Map<String, Object> getRequestSummary() {
        Map<String, Object> m = new HashMap<String, Object>();
        List<Sample> snap = snapshotHistory();

        long last5m = snap.isEmpty() ? 0 : snap.get(snap.size() - 1).requestDelta;
        long lastHour = sumWindow(snap, HOUR_MS, Field.REQUESTS);
        m.put("requestsLast5m", last5m);
        m.put("requestsLastHour", lastHour);
        // 분당 평균은 마지막 두 표본의 실제 간격으로 나눈다 (수집이 밀린 구간을 그대로 반영)
        m.put("requestsPerMinute", round2((double) last5m / lastIntervalMinutes(snap)));
        synchronized (history) {
            m.put("requestsToday", todayRequests);
        }

        RequestMetrics.Snapshot cur = RequestMetrics.snapshot();
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        String[] labels = { "1xx", "2xx", "3xx", "4xx", "5xx", "etc" };
        for (int i = 0; i < labels.length && i < cur.status.length; i++) {
            status.put(labels[i], cur.status[i]);
        }
        m.put("statusCounts", status);
        m.put("requestsTotal", cur.requests);

        // 응답 바이트가 큰 경로 상위 10개 — GUARD 단계에서 무엇부터 끌지 정하는 근거
        List<Map<String, Object>> top = new ArrayList<Map<String, Object>>();
        List<Map.Entry<String, long[]>> entries =
                new ArrayList<Map.Entry<String, long[]>>(cur.paths.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, long[]>>() {
            @Override
            public int compare(Map.Entry<String, long[]> a, Map.Entry<String, long[]> b) {
                int byBytes = Long.compare(b.getValue()[1], a.getValue()[1]);
                return (byBytes != 0) ? byBytes : Long.compare(b.getValue()[0], a.getValue()[0]);
            }
        });
        for (int i = 0; i < entries.size() && i < 10; i++) {
            Map.Entry<String, long[]> e = entries.get(i);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("path", e.getKey());
            row.put("count", e.getValue()[0]);
            row.put("mb", round2((double) e.getValue()[1] / MB));
            top.add(row);
        }
        m.put("topPaths", top);
        return m;
    }

    /** 디스크 증가 추세 */
    public Map<String, Object> getDiskTrend() {
        Map<String, Object> m = new HashMap<String, Object>();
        List<Sample> snap = snapshotHistory();
        if (snap.size() < 2) {
            m.put("trendAvailable", false);
            m.put("growthGbPerHour", 0.0);
            m.put("daysUntilFull", -1);
            return m;
        }
        Sample first = snap.get(0);
        Sample last = snap.get(snap.size() - 1);
        long elapsedMs = last.time - first.time;
        if (elapsedMs <= 0 || last.diskUsed < 0 || first.diskUsed < 0) {
            m.put("trendAvailable", false);
            m.put("growthGbPerHour", 0.0);
            m.put("daysUntilFull", -1);
            return m;
        }
        double hours = elapsedMs / 3600000.0;
        double growthGbPerHour = ((double) (last.diskUsed - first.diskUsed) / GB) / hours;

        m.put("trendAvailable", true);
        m.put("windowHours", round2(hours));
        m.put("growthGbPerHour", round2(growthGbPerHour));
        m.put("growthMbPerHour", round2(growthGbPerHour * 1024));

        // 증가 중일 때만 소진 예상일을 낸다. 감소·정체 중이면 -1
        if (growthGbPerHour > 0.0001 && last.diskFree > 0) {
            double hoursLeft = ((double) last.diskFree / GB) / growthGbPerHour;
            m.put("daysUntilFull", (int) Math.floor(hoursLeft / 24));
        } else {
            m.put("daysUntilFull", -1);
        }
        return m;
    }

    /** 디렉터리별 용량 (캐시값) */
    public Map<String, Object> getDirectorySizes() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        Map<String, DirStat> cache = directoryCache;
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, DirStat> e : cache.entrySet()) {
            DirStat s = e.getValue();
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("label", e.getKey());
            row.put("path", s.path);
            row.put("exists", s.exists);
            row.put("gb", round2((double) s.bytes / GB));
            row.put("mb", round2((double) s.bytes / MB));
            row.put("fileCount", s.fileCount);
            row.put("truncated", s.truncated);
            rows.add(row);
        }
        m.put("directories", rows);
        m.put("scannedAt", directoryScannedAt);
        m.put("configured", !cache.isEmpty());
        return m;
    }

    /** 차트용 이력 */
    public List<Map<String, Object>> getHistory() {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Sample s : snapshotHistory()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("t", s.time);
            row.put("outMb", round2((double) s.netTxDelta / MB));
            row.put("inMb", round2((double) s.netRxDelta / MB));
            row.put("appMb", round2((double) s.appBytesDelta / MB));
            row.put("req", s.requestDelta);
            row.put("diskUsedGb", (s.diskUsed >= 0) ? round2((double) s.diskUsed / GB) : -1);
            row.put("cpu", s.cpuPercent);
            row.put("heap", s.heapPercent);
            out.add(row);
        }
        return out;
    }

    // ==========================================
    // 내부 헬퍼
    // ==========================================

    private List<Sample> snapshotHistory() {
        synchronized (history) {
            return new ArrayList<Sample>(history);
        }
    }

    /** 합산할 필드 지정 */
    enum Field { TX, APP_BYTES, REQUESTS }

    /**
     * 최근 windowMs 구간의 델타 합.
     *
     * 표본 개수가 아니라 시각 기준으로 자른다. 스프링 기본 스케줄러는 스레드 풀이 1개라
     * 다른 스케줄 작업과 겹치면 수집이 밀릴 수 있고, 그때 "표본 12개 = 1시간" 가정이 깨진다.
     */
    static long sumWindow(List<Sample> samples, long windowMs, Field field) {
        if (samples.isEmpty()) {
            return 0L;
        }
        long until = samples.get(samples.size() - 1).time;
        long from = until - windowMs;
        long sum = 0;
        for (int i = samples.size() - 1; i >= 0; i--) {
            Sample s = samples.get(i);
            if (s.time <= from) {
                break;
            }
            switch (field) {
                case TX:
                    sum += s.netTxDelta;
                    break;
                case APP_BYTES:
                    sum += s.appBytesDelta;
                    break;
                default:
                    sum += s.requestDelta;
                    break;
            }
        }
        return sum;
    }

    /** 마지막 두 표본 사이의 실제 간격(분). 표본이 부족하면 설정 주기로 대체 */
    static double lastIntervalMinutes(List<Sample> samples) {
        if (samples.size() >= 2) {
            long gap = samples.get(samples.size() - 1).time - samples.get(samples.size() - 2).time;
            if (gap > 0) {
                return gap / 60000.0;
            }
        }
        return COLLECT_INTERVAL_MINUTES;
    }

    private int historySize() {
        return getConfigInt("monitor.history.size", DEFAULT_HISTORY_SIZE);
    }

    private String networkInterfaceFilter() {
        return getConfig("monitor.net.interface", "auto");
    }

    private String getConfig(String key, String defaultValue) {
        try {
            if (config == null) {
                return defaultValue;
            }
            String v = config.getProperty(key);
            return (v == null || v.trim().isEmpty()) ? defaultValue : v.trim();
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    private int getConfigInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getConfig(key, String.valueOf(defaultValue)));
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    static double round2(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0;
        }
        return Math.round(v * 100.0) / 100.0;
    }

    /** 5분 간격 표본 1개 */
    static class Sample {
        long time;
        boolean netAvailable;
        long netRxTotal;
        long netTxTotal;
        long netRxDelta;
        long netTxDelta;
        long requestTotal;
        long requestDelta;
        long appBytesTotal;
        long appBytesDelta;
        long diskTotal;
        long diskUsed;
        long diskFree;
        long diskUsedDelta;
        int cpuPercent;
        int heapPercent;
    }

    /** 디렉터리 스캔 결과 */
    static class DirStat {
        String path = "";
        boolean exists;
        long bytes;
        int fileCount;
        /** 파일 수·시간 상한에 걸려 중단된 경우 true (표시값이 실제보다 작다) */
        boolean truncated;
    }
}
