package com.sensible.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 요청 수 / 응답 바이트 누적 카운터.
 *
 * 서블릿 필터(MetricsCounterFilter)가 매 요청마다 record()를 호출하고,
 * 수집기(SystemMetricsCollector)가 주기적으로 snapshot()을 떠서 델타를 계산한다.
 *
 * 필터는 스프링 빈이 아니므로(web.xml 등록) 정적 저장소로 둔다.
 * 값은 누적만 하고 리셋하지 않는다. 구간 값은 스냅샷 간 차이로 구한다.
 */
public final class RequestMetrics {

    /** 경로 키 상한. 초과분은 OTHER_KEY 하나로 합산해 메모리 무한 증가를 막는다. */
    private static final int MAX_PATH_KEYS = 300;

    /** 경로 키 상한을 넘은 요청이 합산되는 키 */
    public static final String OTHER_KEY = "(other)";

    /** 경로에서 식별자로 판단할 최소 길이 (UUID·해시 등) */
    private static final int ID_LIKE_MIN_LENGTH = 16;

    /** 경로 키로 유지할 최대 세그먼트 수 */
    private static final int MAX_SEGMENTS = 4;

    /** 상태코드 구간: 0=1xx, 1=2xx, 2=3xx, 3=4xx, 4=5xx, 5=기타 */
    private static final int STATUS_BUCKETS = 6;

    private static final AtomicLong totalRequests = new AtomicLong();
    private static final AtomicLong totalBytes = new AtomicLong();
    private static final AtomicLong[] statusCounts = new AtomicLong[STATUS_BUCKETS];
    private static final ConcurrentHashMap<String, PathStat> pathStats =
            new ConcurrentHashMap<String, PathStat>();

    static {
        for (int i = 0; i < STATUS_BUCKETS; i++) {
            statusCounts[i] = new AtomicLong();
        }
    }

    private RequestMetrics() {
    }

    /**
     * 요청 1건을 기록한다. 필터에서 호출하므로 예외를 밖으로 던지지 않는다.
     *
     * @param path   정규화된 경로 (normalizePath 결과)
     * @param status HTTP 상태코드
     * @param bytes  응답 바이트 수 (측정 불가 시 0)
     */
    public static void record(String path, int status, long bytes) {
        try {
            totalRequests.incrementAndGet();
            if (bytes > 0) {
                totalBytes.addAndGet(bytes);
            }
            statusCounts[statusBucket(status)].incrementAndGet();

            String key = (path == null || path.isEmpty()) ? OTHER_KEY : path;
            PathStat stat = pathStats.get(key);
            if (stat == null) {
                // 신규 키인데 상한을 넘었으면 (other)로 몰아넣는다
                if (pathStats.size() >= MAX_PATH_KEYS) {
                    key = OTHER_KEY;
                    stat = pathStats.get(key);
                }
                if (stat == null) {
                    PathStat created = new PathStat();
                    PathStat prev = pathStats.putIfAbsent(key, created);
                    stat = (prev != null) ? prev : created;
                }
            }
            stat.count.incrementAndGet();
            if (bytes > 0) {
                stat.bytes.addAndGet(bytes);
            }
        } catch (Throwable ignore) {
            // 통계 수집 실패가 요청 처리에 영향을 주면 안 된다
        }
    }

    /** 상태코드를 구간 인덱스로 변환 */
    static int statusBucket(int status) {
        int group = status / 100;
        if (group >= 1 && group <= 5) {
            return group - 1;
        }
        return STATUS_BUCKETS - 1;
    }

    /**
     * 경로를 통계 키로 정규화한다.
     * - 숫자만으로 된 세그먼트, 길이가 긴 식별자 세그먼트는 {id}로 치환
     * - 앞에서 MAX_SEGMENTS개까지만 유지 (키 폭발 방지)
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String work = path;
        int q = work.indexOf('?');
        if (q >= 0) {
            work = work.substring(0, q);
        }
        int semi = work.indexOf(';'); // jsessionid 등 path parameter 제거
        if (semi >= 0) {
            work = work.substring(0, semi);
        }
        String[] segments = work.split("/");
        StringBuilder sb = new StringBuilder();
        int kept = 0;
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i];
            if (seg.isEmpty()) {
                continue;
            }
            if (kept >= MAX_SEGMENTS) {
                sb.append("/*");
                break;
            }
            sb.append('/').append(isIdLike(seg) ? "{id}" : seg);
            kept++;
        }
        return (sb.length() == 0) ? "/" : sb.toString();
    }

    /** 숫자 ID / UUID / 해시처럼 보이는 세그먼트인지 판정 */
    private static boolean isIdLike(String seg) {
        boolean allDigit = true;
        for (int i = 0; i < seg.length(); i++) {
            if (!Character.isDigit(seg.charAt(i))) {
                allDigit = false;
                break;
            }
        }
        if (allDigit) {
            return true;
        }
        if (seg.length() < ID_LIKE_MIN_LENGTH) {
            return false;
        }
        // 길이가 긴 세그먼트 중 영숫자·하이픈으로만 이뤄진 것은 식별자로 본다
        boolean hasDigit = false;
        for (int i = 0; i < seg.length(); i++) {
            char c = seg.charAt(i);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetter(c) && c != '-' && c != '_') {
                return false;
            }
        }
        return hasDigit;
    }

    /** 현재 누적값의 스냅샷 */
    public static Snapshot snapshot() {
        Snapshot s = new Snapshot();
        s.requests = totalRequests.get();
        s.bytes = totalBytes.get();
        s.status = new long[STATUS_BUCKETS];
        for (int i = 0; i < STATUS_BUCKETS; i++) {
            s.status[i] = statusCounts[i].get();
        }
        s.paths = new HashMap<String, long[]>();
        for (Map.Entry<String, PathStat> e : pathStats.entrySet()) {
            PathStat v = e.getValue();
            s.paths.put(e.getKey(), new long[] { v.count.get(), v.bytes.get() });
        }
        return s;
    }

    /** 경로별 누적값 */
    private static final class PathStat {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong bytes = new AtomicLong();
    }

    /** 특정 시점의 누적값 묶음 */
    public static final class Snapshot {
        public long requests;
        public long bytes;
        /** 상태코드 구간별 누적 요청 수 */
        public long[] status;
        /** 경로 → {요청 수, 응답 바이트} */
        public Map<String, long[]> paths;
    }
}
