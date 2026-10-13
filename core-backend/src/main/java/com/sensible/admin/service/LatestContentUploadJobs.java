package com.sensible.admin.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;

/**
 * 로비 Latest Contents 어드민 직접 업로드의 백그라운드 작업 관리 (2-33차 후속).
 *
 * 영상 변환(ffmpeg)이 업로드 요청 안에서 동기로 돌면 큰 파일은 응답이 프록시 타임아웃을 넘긴다.
 * 그러면 브라우저는 "서버 오류"를 띄우지만 서버는 변환을 끝내고 편성까지 마치므로,
 * 화면은 실패인데 실제로는 올라가 있는 상태가 된다. 그래서 파일 저장까지만 요청 안에서 하고
 * 변환부터는 여기서 받아 백그라운드로 돌린다. 화면은 작업 ID로 결과를 폴링한다.
 *
 * - 변환은 CPU 를 많이 쓰므로 한 번에 하나만 돈다 (단일 스레드, 나머지는 대기).
 * - 상태는 JVM 메모리에만 둔다. WAS 재시작 시 진행 중이던 작업과 결과는 사라진다.
 *   화면은 "알 수 없는 작업" 응답을 받으면 목록을 새로고침해 편성 여부를 확인하게 안내한다.
 * - 끝난 작업은 {@link #RETENTION_MS} 가 지나면 지운다.
 */
@Service("latestContentUploadJobs")
public class LatestContentUploadJobs {

    /** 끝난 작업을 메모리에 남겨 두는 시간. 화면이 결과를 가져갈 여유만 주면 된다 */
    public static final long RETENTION_MS = 60L * 60 * 1000;

    public enum State { QUEUED, RUNNING, DONE, FAILED }

    /** 작업 1건. 상태 필드는 작업 스레드가 쓰고 요청 스레드가 읽으므로 volatile */
    public static final class Job {
        public final String id;
        public final String fileName;
        public final long createdAt;
        public volatile State state = State.QUEUED;
        public volatile String message;
        public volatile int lcId;
        public volatile long finishedAt;
        private final CountDownLatch done = new CountDownLatch(1);

        Job(String id, String fileName, long createdAt) {
            this.id = id;
            this.fileName = fileName;
            this.createdAt = createdAt;
        }

        public boolean isFinished() {
            return state == State.DONE || state == State.FAILED;
        }

        /** 테스트·동기 호출용. 끝날 때까지 최대 timeoutMs 기다린다 */
        public boolean await(long timeoutMs) throws InterruptedException {
            return done.await(timeoutMs, TimeUnit.MILLISECONDS);
        }
    }

    private final ConcurrentHashMap<String, Job> jobs = new ConcurrentHashMap<String, Job>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "latest-content-upload");
            // 톰캣 종료를 막지 않도록 데몬으로 둔다. 진행 중 작업은 재시작 시 사라진다 (클래스 주석 참조)
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * 작업 등록. work 는 편성 ID(LC_ID)를 돌려주거나 예외를 던진다.
     * 예외 메시지가 그대로 화면의 실패 사유가 되므로 사용자가 읽을 수 있는 문장이어야 한다.
     */
    public Job submit(String fileName, final Callable<Integer> work) {
        evictFinished(System.currentTimeMillis());
        final Job job = new Job(UUID.randomUUID().toString().replace("-", ""), fileName, System.currentTimeMillis());
        jobs.put(job.id, job);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                job.state = State.RUNNING;
                try {
                    Integer lcId = work.call();
                    job.lcId = lcId == null ? 0 : lcId.intValue();
                    job.state = State.DONE;
                } catch (Throwable t) {
                    job.message = t.getMessage() == null ? t.toString() : t.getMessage();
                    job.state = State.FAILED;
                    System.out.println("[LATEST] upload job " + job.id + " failed: " + job.message);
                } finally {
                    job.finishedAt = System.currentTimeMillis();
                    job.done.countDown();
                }
            }
        });
        return job;
    }

    /** 없으면 null (모르는 ID 이거나 보관 기간이 지났거나 WAS 가 재시작됨) */
    public Job get(String jobId) {
        if (jobId == null) {
            return null;
        }
        return jobs.get(jobId.trim());
    }

    /** 화면 폴링 응답. 모르는 작업은 state=UNKNOWN 으로 알린다 */
    public Map<String, Object> status(String jobId, long now) {
        Map<String, Object> m = new HashMap<String, Object>();
        Job job = get(jobId);
        if (job == null) {
            m.put("state", "UNKNOWN");
            return m;
        }
        m.put("state", job.state.name());
        m.put("fileName", job.fileName);
        m.put("elapsedSec", Math.max(0L, ((job.isFinished() ? job.finishedAt : now) - job.createdAt) / 1000));
        if (job.state == State.DONE) {
            m.put("lcId", job.lcId);
        }
        if (job.state == State.FAILED) {
            m.put("msg", job.message);
        }
        return m;
    }

    /** 보관 기간이 지난 완료 작업을 지운다. 진행 중·대기 중 작업은 남긴다 */
    int evictFinished(long now) {
        int removed = 0;
        Iterator<Map.Entry<String, Job>> it = jobs.entrySet().iterator();
        while (it.hasNext()) {
            Job job = it.next().getValue();
            if (job.isFinished() && now - job.finishedAt > RETENTION_MS) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    int size() {
        return jobs.size();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
