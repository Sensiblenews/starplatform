<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>시스템 관리 및 메모리 모니터링</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <style>
        body { background-color: #f0f2f5; font-family: 'Pretendard', -apple-system, sans-serif; }
        .main-content { margin-left: 260px; padding: 30px; min-height: 100vh; }
        .card { border: none; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.04); transition: transform 0.2s, box-shadow 0.2s; }
        .card:hover { transform: translateY(-3px); box-shadow: 0 6px 25px rgba(0,0,0,0.08); }
        .chart-box { background: white; border-radius: 16px; padding: 25px; box-shadow: 0 4px 20px rgba(0,0,0,0.04); }
        .status-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; margin-right: 5px; }
        .status-online { background-color: #2ecc71; animation: pulse 2s infinite; }
        @keyframes pulse {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(46, 204, 113, 0.7); }
            70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(46, 204, 113, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(46, 204, 113, 0); }
        }
        .progress { height: 16px; border-radius: 8px; background-color: #e9ecef; }
        .progress-bar { border-radius: 8px; transition: width 0.6s ease; }
        .btn-cleanup { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border: none; color: white; border-radius: 8px; font-weight: 600; padding: 10px 20px; transition: all 0.2s; }
        .btn-cleanup:hover { background: linear-gradient(135deg, #5a72df 0%, #6b4097 100%); box-shadow: 0 4px 15px rgba(118, 75, 162, 0.4); transform: translateY(-1px); }
        .btn-cleanup:active { transform: translateY(1px); }
        .metric-label { font-size: 0.85rem; color: #6c757d; font-weight: 500; }
        .metric-value { font-size: 1.25rem; font-weight: 700; color: #343a40; }
    </style>
</head>
<body>
    <!-- 공통 사이드바 include -->
    <c:set var="activeMenu" value="system_panel" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <!-- 상단 헤더 -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h2 class="fw-bold mb-1"><i class="fas fa-microchip text-primary me-2"></i>시스템 및 메모리 제어 패널</h2>
                <p class="text-muted mb-0">StarPlatform JVM 실시간 리소스 모니터링 및 성능 관리 도구</p>
            </div>
            <div class="bg-white px-3 py-2 rounded-3 shadow-sm d-flex align-items-center">
                <span class="status-dot status-online"></span>
                <span class="fw-semibold text-secondary small">모니터링 작동 중 (${pollingInterval / 1000}초 주기)</span>
            </div>
        </div>

        <div class="row g-4">
            <!-- 힙 메모리 게이지 카드 -->
            <div class="col-lg-8">
                <div class="card p-4 h-100">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold mb-0 text-dark"><i class="fas fa-memory text-info me-2"></i>JVM Heap Memory 실시간 현황</h5>
                        <span class="badge bg-light text-dark border" id="heap-ratio-badge">0MB / 0MB</span>
                    </div>

                    <div class="my-4">
                        <div class="progress mb-2">
                            <div id="heap-progress-bar" class="progress-bar bg-success" role="progressbar" style="width: 0%" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div>
                        </div>
                        <div class="d-flex justify-content-between">
                            <span class="small text-secondary" id="heap-init-val">초기: 0MB</span>
                            <span class="fw-bold text-primary" id="heap-percent-text">0% 사용 중</span>
                            <span class="small text-secondary" id="heap-max-val">최대 제한(Xmx): 0MB</span>
                        </div>
                    </div>

                    <hr class="text-muted">

                    <div class="row text-center mt-2">
                        <div class="col-4 border-end">
                            <div class="metric-label">할당된 메모리 (Committed)</div>
                            <div class="metric-value" id="heap-committed-val">0 MB</div>
                        </div>
                        <div class="col-4 border-end">
                            <div class="metric-label">사용 중인 메모리 (Used)</div>
                            <div class="metric-value text-success" id="heap-used-val">0 MB</div>
                        </div>
                        <div class="col-4">
                            <div class="metric-label">여유 공간 (Free Heap)</div>
                            <div class="metric-value text-info" id="heap-free-val">0 MB</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 메모리 청소 및 동작 제어 -->
            <div class="col-lg-4">
                <div class="card p-4 h-100 bg-white d-flex flex-column justify-content-between">
                    <div>
                        <h5 class="fw-bold mb-3 text-dark"><i class="fas fa-broom text-warning me-2"></i>메모리 최적화 도구</h5>
                        <p class="text-muted small">
                            JVM의 힙 공간을 즉각적으로 청소합니다. 가비지 컬렉터(GC)를 호출하여 
                            비즈니스 처리 후 남아있는 불필요한 객체 및 세션 찌꺼기를 해제합니다.
                        </p>
                        
                        <div class="alert alert-warning py-2 small mb-3 border-0" style="background-color: #fff9db; color: #b28607;">
                            <i class="fas fa-exclamation-triangle me-1"></i> <strong>STW 주의:</strong> 트래픽이 높은 서비스 집중 시간대에 호출하면 모든 스레드가 일시 정지(Stop-The-World)하여 미세한 응답 지연이 발생할 수 있습니다.
                        </div>
                    </div>

                    <div class="d-grid gap-2">
                        <button type="button" class="btn btn-cleanup w-100 py-3" onclick="confirmMemoryCleanup()">
                            <i class="fas fa-sync-alt fa-spin me-2" id="gc-btn-icon" style="display:none;"></i>
                            <i class="fas fa-broom me-2" id="gc-btn-normal-icon"></i>메모리 정리 실행 (GC)
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-4 mt-2">
            <!-- 쓰레드 및 구동 시간 정보 -->
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <h5 class="fw-bold mb-4 text-dark"><i class="fas fa-tasks text-success me-2"></i>JVM Active Threads</h5>
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-secondary small">현재 활성 스레드</span>
                        <span class="badge bg-primary fs-6" id="thread-count">0</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-secondary small">최대 피크 스레드 수</span>
                        <span class="fw-semibold" id="peak-thread-count">0</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="text-secondary small">시작된 총 스레드 수</span>
                        <span class="text-muted text-sm" id="total-started-threads">0</span>
                    </div>
                </div>
            </div>

            <!-- Non-Heap 메모리 세부사항 -->
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <h5 class="fw-bold mb-4 text-dark"><i class="fas fa-database text-purple me-2"></i>JVM Non-Heap Memory</h5>
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-secondary small">사용 중 (Used Metaspace)</span>
                        <span class="fw-bold text-dark" id="nonheap-used">0 MB</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-secondary small">확보된 영역 (Committed)</span>
                        <span class="fw-semibold" id="nonheap-committed">0 MB</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="text-secondary small">최대 한도 제한</span>
                        <span class="text-muted" id="nonheap-max">0 MB</span>
                    </div>
                </div>
            </div>

            <!-- 서버 하드웨어 사양 및 JVM 정보 -->
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <h5 class="fw-bold mb-4 text-dark"><i class="fas fa-info-circle text-primary me-2"></i>JVM & System Info</h5>
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="text-secondary small">JVM VM 명칭</span>
                        <span class="fw-semibold small text-end" id="jvm-name">-</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="text-secondary small">Java 버전</span>
                        <span class="fw-semibold" id="java-version">-</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="text-secondary small">서버 OS 환경</span>
                        <span class="fw-semibold small text-end" id="os-info">-</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="text-secondary small">CPU 물리 코어 수</span>
                        <span class="fw-semibold text-primary" id="cpu-cores">0 Cores</span>
                    </div>
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="text-secondary small">서버 연속 구동 시간</span>
                        <span class="badge bg-secondary" id="jvm-uptime">-</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- ===== [신규] 시스템 리소스 & 외부 연동 실시간 모니터링 ===== -->
        <div class="mt-5 mb-3">
            <h4 class="fw-bold text-dark"><i class="fas fa-heartbeat text-danger me-2"></i>시스템 리소스 & 연동 상태</h4>
            <p class="text-muted mb-0 small">CPU · 디스크 · Redis · DB · Queue 실시간 상태 (${pollingInterval / 1000}초 주기 자동 갱신)</p>
        </div>

        <div class="row g-4">
            <!-- CPU 사용률 -->
            <div class="col-lg-6">
                <div class="card p-4 h-100">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold mb-0 text-dark"><i class="fas fa-microchip text-primary me-2"></i>CPU 사용률</h5>
                        <span class="badge bg-light text-dark border" id="cpu-badge">-</span>
                    </div>
                    <div class="my-2">
                        <div class="d-flex justify-content-between mb-1">
                            <span class="small text-secondary">시스템 전체 CPU</span>
                            <span class="fw-bold small text-primary" id="cpu-system-text">- %</span>
                        </div>
                        <div class="progress mb-3">
                            <div id="cpu-system-bar" class="progress-bar bg-success" role="progressbar" style="width: 0%"></div>
                        </div>
                        <div class="d-flex justify-content-between mb-1">
                            <span class="small text-secondary">이 프로세스(JVM) CPU</span>
                            <span class="fw-semibold small text-info" id="cpu-process-text">- %</span>
                        </div>
                        <div class="progress">
                            <div id="cpu-process-bar" class="progress-bar bg-info" role="progressbar" style="width: 0%"></div>
                        </div>
                    </div>
                    <div class="mt-2 text-end">
                        <span class="small text-muted">System Load Average: <span id="cpu-loadavg" class="fw-semibold">-</span></span>
                    </div>
                </div>
            </div>

            <!-- Disk 사용량 -->
            <div class="col-lg-6">
                <div class="card p-4 h-100">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold mb-0 text-dark"><i class="fas fa-hdd text-warning me-2"></i>디스크 사용량</h5>
                        <span class="badge bg-light text-dark border" id="disk-badge">-</span>
                    </div>
                    <div class="my-2">
                        <div class="progress mb-2">
                            <div id="disk-bar" class="progress-bar bg-success" role="progressbar" style="width: 0%"></div>
                        </div>
                        <div class="text-end">
                            <span class="fw-bold text-primary" id="disk-percent-text">0% 사용 중</span>
                        </div>
                    </div>
                    <hr class="text-muted">
                    <div class="row text-center mt-1">
                        <div class="col-4 border-end">
                            <div class="metric-label">전체 용량</div>
                            <div class="metric-value" id="disk-total-val">0 GB</div>
                        </div>
                        <div class="col-4 border-end">
                            <div class="metric-label">사용 중</div>
                            <div class="metric-value text-success" id="disk-used-val">0 GB</div>
                        </div>
                        <div class="col-4">
                            <div class="metric-label">여유 공간</div>
                            <div class="metric-value text-info" id="disk-free-val">0 GB</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-4 mt-1">
            <!-- Redis 상태 -->
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <h5 class="fw-bold mb-3 text-dark"><i class="fas fa-bolt text-danger me-2"></i>Redis</h5>
                    <div class="d-flex align-items-center mb-2">
                        <span class="status-dot" id="redis-dot" style="background-color:#adb5bd;"></span>
                        <span class="fw-bold fs-5" id="redis-status">확인 중...</span>
                    </div>
                    <p class="text-muted small mb-2" id="redis-msg">-</p>
                    <div class="small text-secondary" id="redis-detail" style="display:none;">
                        메모리: <span class="fw-semibold" id="redis-memory">-</span>
                        (peak <span id="redis-peak">-</span>)<br>
                        클라이언트: <span class="fw-semibold" id="redis-clients">-</span> ·
                        키: <span class="fw-semibold" id="redis-keys">-</span><br>
                        축출된 키: <span class="fw-semibold" id="redis-evicted">-</span>
                    </div>
                </div>
            </div>

            <!-- DB 상태 -->
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <h5 class="fw-bold mb-3 text-dark"><i class="fas fa-database text-primary me-2"></i>Database</h5>
                    <div class="d-flex align-items-center mb-2">
                        <span class="status-dot" id="db-dot" style="background-color:#adb5bd;"></span>
                        <span class="fw-bold fs-5" id="db-status">확인 중...</span>
                    </div>
                    <p class="text-muted small mb-1" id="db-msg">-</p>
                    <div class="small text-secondary" id="db-pool" style="display:none;">
                        활성 커넥션: <span class="fw-semibold" id="db-active">-</span> /
                        유휴: <span class="fw-semibold" id="db-idle">-</span>
                        <span id="db-max-wrap" style="display:none;"> / 최대: <span class="fw-semibold" id="db-max">-</span></span>
                    </div>
                </div>
            </div>

            <!-- Queue 상태 -->
            <div class="col-md-4">
                <div class="card p-4 h-100">
                    <h5 class="fw-bold mb-3 text-dark"><i class="fas fa-stream text-secondary me-2"></i>Queue</h5>
                    <div class="d-flex align-items-center mb-2">
                        <span class="status-dot" style="background-color:#adb5bd;"></span>
                        <span class="fw-bold fs-5 text-muted" id="queue-status">N/A</span>
                    </div>
                    <p class="text-muted small mb-0" id="queue-msg">메시지 큐 미도입</p>
                </div>
            </div>
        </div>

        <!-- ===== [신규] 비용 지표 (구간값) ===== -->
        <div class="mt-5 mb-3">
            <h4 class="fw-bold text-dark"><i class="fas fa-coins text-warning me-2"></i>비용 지표 (구간값)</h4>
            <p class="text-muted mb-0 small">
                아웃바운드 트래픽 · 요청량 · 디스크 증가 추세. 5분 간격으로 수집한 델타값이며,
                누적 표시는 <span class="fw-semibold">수집 시작 이후</span> 기준이다 (WAS 재시작 시 초기화).
            </p>
        </div>

        <div class="alert alert-secondary py-2 px-3 small" id="collector-warning" style="display:none;">
            <i class="fas fa-circle-info me-1"></i>
            <span id="collector-warning-text">구간 지표 수집기가 아직 표본을 모으지 못했습니다. 첫 값은 수집 시작 후 5~10분 뒤에 나타납니다.</span>
        </div>

        <div class="row g-4">
            <!-- 아웃바운드 트래픽 -->
            <div class="col-lg-8">
                <div class="card p-4 h-100">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold mb-0 text-dark"><i class="fas fa-arrow-up-from-bracket text-danger me-2"></i>아웃바운드 트래픽</h5>
                        <span class="badge bg-light text-dark border" id="traffic-source">-</span>
                    </div>
                    <div class="row text-center">
                        <div class="col-3 border-end">
                            <div class="metric-label">최근 5분</div>
                            <div class="metric-value" id="traffic-5m">- MB</div>
                        </div>
                        <div class="col-3 border-end">
                            <div class="metric-label">최근 1시간</div>
                            <div class="metric-value text-primary" id="traffic-1h">- GB</div>
                        </div>
                        <div class="col-3 border-end">
                            <div class="metric-label">오늘 누적</div>
                            <div class="metric-value text-success" id="traffic-today">- GB</div>
                        </div>
                        <div class="col-3">
                            <div class="metric-label">월 환산 예상</div>
                            <div class="metric-value text-danger" id="traffic-monthly">- GB</div>
                        </div>
                    </div>
                    <hr class="text-muted">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <span class="small text-secondary">최근 24시간 추이 (5분 단위 송신 MB)</span>
                        <span class="small text-muted" id="traffic-spark-max">-</span>
                    </div>
                    <svg id="traffic-spark" viewBox="0 0 600 80" preserveAspectRatio="none"
                         style="width:100%;height:80px;background:#f8f9fa;border-radius:6px;">
                        <polyline id="traffic-spark-line" fill="none" stroke="#dc3545" stroke-width="2" points=""></polyline>
                    </svg>
                    <div class="mt-3 small text-secondary">
                        앱이 낸 응답 바이트: <span class="fw-semibold" id="traffic-app-1h">-</span>
                        (NIC 송신 대비 <span class="fw-semibold" id="traffic-app-share">-</span>)
                        <br>
                        <span class="text-muted">
                            NIC 값은 정적 파일까지 포함한 실제 송신량이다. 두 값의 차이가 크면 WAS를 거치지 않는 트래픽이 크다는 뜻이다.
                        </span>
                    </div>
                </div>
            </div>

            <!-- 디스크 증가 추세 -->
            <div class="col-lg-4">
                <div class="card p-4 h-100">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold mb-0 text-dark"><i class="fas fa-chart-line text-warning me-2"></i>디스크 증가 추세</h5>
                        <span class="badge bg-light text-dark border" id="disk-trend-window">-</span>
                    </div>
                    <div class="text-center my-3">
                        <div class="metric-label">시간당 증가량</div>
                        <div class="fw-bold fs-3" id="disk-growth">-</div>
                    </div>
                    <hr class="text-muted">
                    <div class="text-center">
                        <div class="metric-label">이 속도면 디스크가 가득 차기까지</div>
                        <div class="fw-bold fs-4" id="disk-days-left">-</div>
                    </div>
                    <p class="text-muted small mb-0 mt-3">
                        사용률 %만으로는 임계치를 넘은 뒤에야 알게 된다. 증가 속도를 봐야 미리 정리할 수 있다.
                    </p>
                </div>
            </div>
        </div>

        <div class="row g-4 mt-1">
            <!-- 요청량 -->
            <div class="col-lg-5">
                <div class="card p-4 h-100">
                    <h5 class="fw-bold mb-3 text-dark"><i class="fas fa-wave-square text-info me-2"></i>API 요청량</h5>
                    <div class="row text-center">
                        <div class="col-4 border-end">
                            <div class="metric-label">최근 5분</div>
                            <div class="metric-value" id="req-5m">-</div>
                        </div>
                        <div class="col-4 border-end">
                            <div class="metric-label">분당 평균</div>
                            <div class="metric-value text-primary" id="req-per-min">-</div>
                        </div>
                        <div class="col-4">
                            <div class="metric-label">최근 1시간</div>
                            <div class="metric-value text-info" id="req-1h">-</div>
                        </div>
                    </div>
                    <hr class="text-muted">
                    <div class="small text-secondary mb-2">상태코드 분포 (수집 시작 이후 누적)</div>
                    <div id="req-status-badges">
                        <span class="badge bg-success me-1">2xx <span id="req-2xx">0</span></span>
                        <span class="badge bg-secondary me-1">3xx <span id="req-3xx">0</span></span>
                        <span class="badge bg-warning text-dark me-1">4xx <span id="req-4xx">0</span></span>
                        <span class="badge bg-danger me-1">5xx <span id="req-5xx">0</span></span>
                    </div>
                    <div class="small text-muted mt-3 mb-0">
                        전체 누적 요청: <span class="fw-semibold" id="req-total">-</span>
                    </div>
                </div>
            </div>

            <!-- 응답 바이트 상위 경로 -->
            <div class="col-lg-7">
                <div class="card p-4 h-100">
                    <h5 class="fw-bold mb-3 text-dark"><i class="fas fa-list-ol text-danger me-2"></i>응답 바이트 상위 경로</h5>
                    <div class="table-responsive" style="max-height:280px;overflow-y:auto;">
                        <table class="table table-sm align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th style="width:60%;">경로</th>
                                    <th class="text-end">요청 수</th>
                                    <th class="text-end">응답 MB</th>
                                </tr>
                            </thead>
                            <tbody id="top-paths-body">
                                <tr><td colspan="3" class="text-muted text-center py-4">수집 대기 중</td></tr>
                            </tbody>
                        </table>
                    </div>
                    <p class="text-muted small mb-0 mt-2">
                        절감 모드에서 무엇부터 끌지 정하는 근거. 숫자 ID는 {id}로 묶어 집계한다.
                    </p>
                </div>
            </div>
        </div>

        <div class="row g-4 mt-1">
            <!-- 디렉터리 용량 -->
            <div class="col-lg-6">
                <div class="card p-4 h-100">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold mb-0 text-dark"><i class="fas fa-folder-tree text-secondary me-2"></i>디렉터리 용량</h5>
                        <span class="badge bg-light text-dark border" id="dir-scanned-at">-</span>
                    </div>
                    <div id="dir-list">
                        <p class="text-muted small mb-0">
                            감시 대상 디렉터리가 설정되지 않았습니다.
                            globals.properties의 monitor.dir.upload / monitor.dir.log / monitor.dir.temp 에 경로를 넣으면 집계합니다.
                        </p>
                    </div>
                    <p class="text-muted small mb-0 mt-3">
                        30분 간격으로만 계산해 캐시한다. 폴링마다 재계산하면 스캔 자체가 디스크 부하가 되기 때문이다.
                    </p>
                </div>
            </div>

            <!-- 런타임 상세 -->
            <div class="col-lg-6">
                <div class="row g-4 h-100">
                    <div class="col-12">
                        <div class="card p-4">
                            <h5 class="fw-bold mb-3 text-dark"><i class="fas fa-recycle text-success me-2"></i>가비지 컬렉션</h5>
                            <div class="row text-center">
                                <div class="col-4 border-end">
                                    <div class="metric-label">총 실행 횟수</div>
                                    <div class="metric-value" id="gc-count">-</div>
                                </div>
                                <div class="col-4 border-end">
                                    <div class="metric-label">총 소요 시간</div>
                                    <div class="metric-value text-primary" id="gc-time">-</div>
                                </div>
                                <div class="col-4">
                                    <div class="metric-label">가동시간 대비</div>
                                    <div class="metric-value text-danger" id="gc-percent">-</div>
                                </div>
                            </div>
                            <div class="small text-secondary mt-3" id="gc-collectors">-</div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card p-4 h-100">
                            <h5 class="fw-bold mb-3 text-dark"><i class="fas fa-server text-primary me-2"></i>톰캣</h5>
                            <div class="small text-secondary mb-1">처리 스레드</div>
                            <div class="progress mb-2">
                                <div id="tomcat-bar" class="progress-bar bg-success" role="progressbar" style="width:0%"></div>
                            </div>
                            <div class="d-flex justify-content-between small">
                                <span id="tomcat-threads">-</span>
                                <span class="fw-semibold" id="tomcat-percent">- %</span>
                            </div>
                            <hr class="text-muted">
                            <div class="d-flex justify-content-between">
                                <span class="small text-secondary">활성 세션</span>
                                <span class="fw-bold" id="tomcat-sessions">-</span>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card p-4 h-100">
                            <h5 class="fw-bold mb-3 text-dark"><i class="fas fa-file-lines text-info me-2"></i>파일 디스크립터</h5>
                            <div class="progress mb-2">
                                <div id="fd-bar" class="progress-bar bg-success" role="progressbar" style="width:0%"></div>
                            </div>
                            <div class="d-flex justify-content-between small">
                                <span id="fd-count">-</span>
                                <span class="fw-semibold" id="fd-percent">- %</span>
                            </div>
                            <p class="text-muted small mb-0 mt-3">
                                꾸준히 늘기만 하면 커넥션·스트림 누수 신호다.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 메모리 청소 확인용 모달 -->
    <div class="modal fade" id="cleanupConfirmModal" data-bs-backdrop="static" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <div class="modal-header bg-dark text-white border-0 py-3">
                    <h5 class="modal-title fw-bold"><i class="fas fa-broom text-warning me-2"></i>메모리 정리 실행 확인</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="text-center mb-4">
                        <i class="fas fa-exclamation-triangle text-warning fa-3x"></i>
                    </div>
                    <p class="mb-2"><strong>가비지 컬렉터(System.gc())</strong>를 호출하시겠습니까?</p>
                    <p class="text-muted small mb-0">
                        * GC 작업이 진행되는 동안 JVM 애플리케이션의 모든 처리가 순간적으로 정지(Stop-The-World)되어, 
                        현재 동시 접속 중인 일반 사용자의 모바일 앱 응답이 미세하게 지연될 수 있습니다.
                    </p>
                </div>
                <div class="modal-footer border-0 bg-light p-3">
                    <button type="button" class="btn btn-outline-secondary px-3" data-bs-dismiss="modal">취소</button>
                    <button type="button" class="btn btn-danger px-4" onclick="executeMemoryCleanup()">정리 실행</button>
                </div>
            </div>
        </div>
    </div>

    <!-- 메모리 정리 결과 완료 안내 모달 -->
    <div class="modal fade" id="cleanupResultModal" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <div class="modal-header bg-success text-white border-0 py-3">
                    <h5 class="modal-title fw-bold"><i class="fas fa-check-circle me-2"></i>최적화 완료</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4 text-center">
                    <i class="fas fa-leaf text-success fa-4x mb-3"></i>
                    <h5 class="fw-bold mb-2">메모리가 성공적으로 확보되었습니다!</h5>
                    <p id="cleanup-result-text" class="text-secondary mb-0"></p>
                </div>
                <div class="modal-footer border-0 justify-content-center pb-4">
                    <button type="button" class="btn btn-success px-5" data-bs-dismiss="modal">확인</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        // 모니터링 상태 폴링 스크립트
        let pollingInterval = null;

        // global.properties 에서 주입된 임계치 (색상 분기 기준)
        const CPU_WARN = ${cpuWarn}, CPU_DANGER = ${cpuDanger};
        const DISK_WARN = ${diskWarn}, DISK_DANGER = ${diskDanger};

        // 진행바 + 퍼센트 텍스트를 임계치에 따라 색상과 함께 갱신
        function setBar(barId, textId, percent, warn, danger, baseClass) {
            const bar = document.getElementById(barId);
            if (!bar) return;
            const p = (typeof percent === 'number' && percent >= 0) ? percent : 0;
            bar.style.width = p + "%";
            bar.setAttribute('aria-valuenow', p);
            bar.className = "progress-bar";
            if (p >= danger)      bar.classList.add("bg-danger");
            else if (p >= warn)   bar.classList.add("bg-warning");
            else                  bar.classList.add(baseClass || "bg-success");
            if (textId) document.getElementById(textId).innerText = p + " %";
        }

        // UP/DOWN 상태 점 + 텍스트 + 메시지 갱신
        function setServiceStatus(dotId, statusId, msgId, status, msg) {
            const isUp = (status === 'UP');
            const dot = document.getElementById(dotId);
            const statusEl = document.getElementById(statusId);
            dot.style.backgroundColor = isUp ? '#2ecc71' : '#e74c3c';
            dot.style.boxShadow = isUp ? '0 0 8px rgba(46,204,113,0.6)' : '0 0 8px rgba(231,76,60,0.5)';
            statusEl.innerText = isUp ? '정상 (UP)' : '중단 (DOWN)';
            statusEl.className = "fw-bold fs-5 " + (isUp ? 'text-success' : 'text-danger');
            document.getElementById(msgId).innerText = msg || (isUp ? '정상' : '연결되지 않음');
        }

        document.addEventListener("DOMContentLoaded", function() {
            // 즉시 데이터 1회 가져오기
            updateSystemStatus();
            // 폴링 주기(ms)는 global.properties(monitor.polling.interval)에서 주입
            pollingInterval = setInterval(updateSystemStatus, ${pollingInterval});

            // 이력은 5분 간격 표본이라 5초 폴링과 같이 돌릴 이유가 없다. 별도 주기로 갱신
            updateTrafficHistory();
            setInterval(updateTrafficHistory, 300000);
        });

        function updateSystemStatus() {
            fetch('/super/system/status.json')
            .then(response => response.json())
            .then(data => {
                if(data.status === 'success') {
                    // 1. 힙 메모리 세팅
                    const used = data.heapUsed;
                    const max = data.heapMax;
                    const committed = data.heapCommitted;
                    const init = data.heapInit;
                    const percent = data.heapPercent;
                    const free = committed - used; // 할당된 범위 중 남은 크기

                    document.getElementById('heap-ratio-badge').innerText = used + "MB / " + max + "MB";
                    document.getElementById('heap-percent-text').innerText = percent + "% 사용 중";
                    document.getElementById('heap-init-val').innerText = "초기: " + init + "MB";
                    document.getElementById('heap-max-val').innerText = "최대 제한(Xmx): " + max + "MB";

                    const progressBar = document.getElementById('heap-progress-bar');
                    progressBar.style.width = percent + "%";
                    progressBar.setAttribute('aria-valuenow', percent);

                    // 사용량에 따른 프로그레스바 색상 변경
                    progressBar.className = "progress-bar";
                    if(percent >= 80) {
                        progressBar.classList.add("bg-danger");
                    } else if (percent >= 60) {
                        progressBar.classList.add("bg-warning");
                    } else {
                        progressBar.classList.add("bg-success");
                    }

                    document.getElementById('heap-committed-val').innerText = committed.toLocaleString() + " MB";
                    document.getElementById('heap-used-val').innerText = used.toLocaleString() + " MB";
                    document.getElementById('heap-free-val').innerText = (free > 0 ? free : 0).toLocaleString() + " MB";

                    // 2. 스레드 세팅
                    document.getElementById('thread-count').innerText = data.threadCount.toLocaleString();
                    document.getElementById('peak-thread-count').innerText = data.peakThreadCount.toLocaleString();
                    document.getElementById('total-started-threads').innerText = data.totalStartedThreadCount.toLocaleString();

                    // 3. Non-Heap 세팅
                    document.getElementById('nonheap-used').innerText = data.nonHeapUsed.toLocaleString() + " MB";
                    document.getElementById('nonheap-committed').innerText = data.nonHeapCommitted.toLocaleString() + " MB";
                    document.getElementById('nonheap-max').innerText = (data.nonHeapMax > 0 ? data.nonHeapMax.toLocaleString() + " MB" : "제한 없음");

                    // 4. 시스템/JVM 세팅
                    document.getElementById('jvm-name').innerText = data.jvmName;
                    document.getElementById('java-version').innerText = "JDK " + data.jvmVersion;
                    document.getElementById('os-info').innerText = data.osName + " (" + data.osArch + ")";
                    document.getElementById('cpu-cores').innerText = data.availableProcessors + " Cores";
                    document.getElementById('jvm-uptime').innerText = data.jvmUptime;

                    // 5. CPU 사용률 세팅
                    if (data.cpuAvailable) {
                        setBar('cpu-system-bar', 'cpu-system-text', data.cpuSystemPercent, CPU_WARN, CPU_DANGER);
                        setBar('cpu-process-bar', 'cpu-process-text', data.cpuProcessPercent, CPU_WARN, CPU_DANGER, 'bg-info');
                        document.getElementById('cpu-badge').innerText = data.cpuSystemPercent + "% (System)";
                    } else {
                        document.getElementById('cpu-badge').innerText = "정밀 측정 미지원";
                        document.getElementById('cpu-system-text').innerText = "N/A";
                        document.getElementById('cpu-process-text').innerText = "N/A";
                    }
                    const load = (typeof data.systemLoadAverage === 'number' && data.systemLoadAverage >= 0)
                        ? data.systemLoadAverage.toFixed(2) : "N/A";
                    document.getElementById('cpu-loadavg').innerText = load;

                    // 6. Disk 사용량 세팅
                    if (data.diskAvailable) {
                        setBar('disk-bar', null, data.diskPercent, DISK_WARN, DISK_DANGER);
                        document.getElementById('disk-percent-text').innerText = data.diskPercent + "% 사용 중";
                        document.getElementById('disk-badge').innerText = data.diskPercent + "%";
                        document.getElementById('disk-total-val').innerText = data.diskTotalGb.toLocaleString() + " GB";
                        document.getElementById('disk-used-val').innerText = data.diskUsedGb.toLocaleString() + " GB";
                        document.getElementById('disk-free-val').innerText = data.diskFreeGb.toLocaleString() + " GB";
                    } else {
                        document.getElementById('disk-badge').innerText = "조회 불가";
                    }

                    // 7. Redis 상태 세팅
                    setServiceStatus('redis-dot', 'redis-status', 'redis-msg', data.redisStatus, data.redisMsg);

                    // 8. DB 상태 세팅
                    setServiceStatus('db-dot', 'db-status', 'db-msg', data.dbStatus, data.dbMsg);
                    // DBCP 풀 통계 (도달 가능한 경우에만 표시)
                    if (typeof data.dbActive === 'number' && data.dbActive >= 0) {
                        document.getElementById('db-pool').style.display = 'block';
                        document.getElementById('db-active').innerText = data.dbActive;
                        document.getElementById('db-idle').innerText = data.dbIdle;
                    }

                    // 9. Queue 상태 세팅 (N/A 자리표시자)
                    if (data.queueMsg) {
                        document.getElementById('queue-status').innerText = data.queueStatus || "N/A";
                        document.getElementById('queue-msg').innerText = data.queueMsg;
                    }

                    // 10. Redis 상세 (INFO 조회에 성공한 경우에만)
                    if (typeof data.redisUsedMemoryMb === 'number' && data.redisUsedMemoryMb >= 0) {
                        document.getElementById('redis-detail').style.display = 'block';
                        setText('redis-memory', data.redisUsedMemoryMb.toLocaleString() + " MB");
                        setText('redis-peak', (data.redisPeakMemoryMb >= 0 ? data.redisPeakMemoryMb.toLocaleString() + " MB" : "-"));
                        setText('redis-clients', data.redisClients);
                        setText('redis-evicted', data.redisEvictedKeys);
                        setText('redis-keys', (typeof data.redisKeyCount === 'number' && data.redisKeyCount >= 0)
                            ? data.redisKeyCount.toLocaleString() : "-");
                    }
                    // DB 최대 커넥션
                    if (typeof data.dbMaxActive === 'number' && data.dbMaxActive >= 0) {
                        document.getElementById('db-max-wrap').style.display = 'inline';
                        setText('db-max', data.dbMaxActive);
                    }

                    // 11. 비용 지표 / 런타임 상세
                    renderCostMetrics(data);
                    renderRuntimeDetail(data);
                }
            })
            .catch(error => {
                console.error("실시간 상태 조회 도중 오류 발생:", error);
            });
        }

        // 텍스트 세팅 (요소가 없으면 무시)
        function setText(id, value) {
            const el = document.getElementById(id);
            if (el) el.innerText = (value === null || value === undefined) ? "-" : value;
        }

        // 숫자 표기 (수집 전이면 "-")
        function fmtNum(v, suffix, digits) {
            if (typeof v !== 'number' || v < 0) return "-";
            const d = (typeof digits === 'number') ? digits : 2;
            return v.toFixed(d) + (suffix || "");
        }

        // 비용 지표(트래픽 · 요청량 · 디스크 추세 · 디렉터리) 렌더
        function renderCostMetrics(data) {
            // 수집기 미탑재 또는 표본 부족 안내
            const warnBox = document.getElementById('collector-warning');
            const sampleCount = (typeof data.sampleCount === 'number') ? data.sampleCount : 0;
            if (data.collectorAvailable === false) {
                warnBox.style.display = 'block';
                setText('collector-warning-text', '구간 지표 수집기가 로딩되지 않았습니다. 서버 로그를 확인해 주세요.');
            } else if (sampleCount < 2) {
                warnBox.style.display = 'block';
                setText('collector-warning-text',
                    '구간 지표 수집기가 아직 표본을 모으지 못했습니다 (현재 ' + sampleCount + '개). 첫 값은 수집 시작 후 5~10분 뒤에 나타납니다.');
            } else {
                warnBox.style.display = 'none';
            }

            // 트래픽
            setText('traffic-source', data.trafficAvailable ? (data.trafficSource || "NIC") : "NIC 조회 불가");
            setText('traffic-5m', fmtNum(data.last5mOutMb, " MB"));
            setText('traffic-1h', fmtNum(data.lastHourOutGb, " GB"));
            setText('traffic-today', fmtNum(data.todayOutGb, " GB"));
            setText('traffic-monthly', fmtNum(data.projectedMonthlyOutGb, " GB"));
            setText('traffic-app-1h', fmtNum(data.lastHourAppMb, " MB"));
            setText('traffic-app-share',
                (typeof data.appSharePercent === 'number' && data.appSharePercent >= 0) ? data.appSharePercent + " %" : "-");

            // 디스크 추세
            if (data.trendAvailable) {
                const g = data.growthGbPerHour;
                setText('disk-growth', (Math.abs(g) >= 1) ? fmtNum(g, " GB/h") : fmtNum(data.growthMbPerHour, " MB/h"));
                setText('disk-trend-window', "관측 " + fmtNum(data.windowHours, "시간", 1));
                const days = data.daysUntilFull;
                setText('disk-days-left', (typeof days === 'number' && days >= 0) ? ("약 " + days.toLocaleString() + "일") : "증가 없음");
            } else {
                setText('disk-growth', "-");
                setText('disk-trend-window', "표본 부족");
                setText('disk-days-left', "-");
            }

            // 요청량
            setText('req-5m', (typeof data.requestsLast5m === 'number') ? data.requestsLast5m.toLocaleString() : "-");
            setText('req-per-min', fmtNum(data.requestsPerMinute, "", 1));
            setText('req-1h', (typeof data.requestsLastHour === 'number') ? data.requestsLastHour.toLocaleString() : "-");
            setText('req-total', (typeof data.requestsTotal === 'number') ? data.requestsTotal.toLocaleString() : "-");
            const sc = data.statusCounts || {};
            setText('req-2xx', (sc['2xx'] || 0).toLocaleString());
            setText('req-3xx', (sc['3xx'] || 0).toLocaleString());
            setText('req-4xx', (sc['4xx'] || 0).toLocaleString());
            setText('req-5xx', (sc['5xx'] || 0).toLocaleString());

            renderTopPaths(data.topPaths);
            renderDirectories(data);
        }

        // 응답 바이트 상위 경로 테이블 (경로 문자열은 textContent로만 넣는다)
        function renderTopPaths(rows) {
            const body = document.getElementById('top-paths-body');
            if (!body) return;
            body.innerHTML = "";
            if (!rows || rows.length === 0) {
                const tr = document.createElement('tr');
                const td = document.createElement('td');
                td.colSpan = 3;
                td.className = "text-muted text-center py-4";
                td.textContent = "수집 대기 중";
                tr.appendChild(td);
                body.appendChild(tr);
                return;
            }
            rows.forEach(function(r) {
                const tr = document.createElement('tr');

                const tdPath = document.createElement('td');
                tdPath.className = "text-truncate";
                tdPath.style.maxWidth = "0";
                tdPath.title = r.path;
                tdPath.textContent = r.path;

                const tdCount = document.createElement('td');
                tdCount.className = "text-end";
                tdCount.textContent = (r.count || 0).toLocaleString();

                const tdMb = document.createElement('td');
                tdMb.className = "text-end fw-semibold";
                tdMb.textContent = (r.mb || 0).toFixed(2);

                tr.appendChild(tdPath);
                tr.appendChild(tdCount);
                tr.appendChild(tdMb);
                body.appendChild(tr);
            });
        }

        // 디렉터리 용량 목록
        function renderDirectories(data) {
            const box = document.getElementById('dir-list');
            if (!box) return;
            const dirs = data.directories;
            if (!data.configured || !dirs || dirs.length === 0) {
                return; // 초기 안내 문구 유지
            }
            box.innerHTML = "";
            dirs.forEach(function(d) {
                const wrap = document.createElement('div');
                wrap.className = "d-flex justify-content-between align-items-start py-2 border-bottom";

                const left = document.createElement('div');
                const label = document.createElement('div');
                label.className = "fw-semibold";
                label.textContent = d.label;
                const path = document.createElement('div');
                path.className = "small text-muted text-truncate";
                path.style.maxWidth = "320px";
                path.title = d.path;
                path.textContent = d.path;
                left.appendChild(label);
                left.appendChild(path);

                const right = document.createElement('div');
                right.className = "text-end";
                const size = document.createElement('div');
                size.className = "fw-bold";
                size.textContent = d.exists ? ((d.gb >= 1) ? d.gb.toFixed(2) + " GB" : d.mb.toFixed(1) + " MB") : "경로 없음";
                const meta = document.createElement('div');
                meta.className = "small text-muted";
                meta.textContent = d.exists
                    ? ((d.fileCount || 0).toLocaleString() + "개" + (d.truncated ? " (상한 도달, 실제는 더 큼)" : ""))
                    : "-";
                right.appendChild(size);
                right.appendChild(meta);

                wrap.appendChild(left);
                wrap.appendChild(right);
                box.appendChild(wrap);
            });
            if (data.scannedAt) {
                const d = new Date(data.scannedAt);
                setText('dir-scanned-at', d.getHours() + "시 " + ("0" + d.getMinutes()).slice(-2) + "분 기준");
            }
        }

        // GC · 톰캣 · 파일 디스크립터
        function renderRuntimeDetail(data) {
            if (data.gcAvailable) {
                setText('gc-count', (data.gcTotalCount || 0).toLocaleString() + " 회");
                setText('gc-time', ((data.gcTotalTimeMs || 0) / 1000).toFixed(1) + " 초");
                setText('gc-percent', (data.gcTimePercent || 0) + " %");
                const names = (data.gcCollectors || []).map(function(g) {
                    return g.name + " " + (g.count || 0) + "회 / " + ((g.timeMs || 0) / 1000).toFixed(1) + "초";
                });
                setText('gc-collectors', names.join(" · "));
            }

            if (data.tomcatAvailable) {
                setBar('tomcat-bar', null, data.tomcatThreadPercent, 60, 80);
                setText('tomcat-threads', data.tomcatThreadsBusy + " / " + data.tomcatThreadsMax);
                setText('tomcat-percent', data.tomcatThreadPercent + " %");
            } else {
                setText('tomcat-threads', "JMX 조회 불가");
                setText('tomcat-percent', "-");
            }
            setText('tomcat-sessions',
                (typeof data.tomcatActiveSessions === 'number' && data.tomcatActiveSessions >= 0)
                    ? data.tomcatActiveSessions.toLocaleString() : "-");

            if (data.fdAvailable) {
                setBar('fd-bar', null, data.fdPercent, 60, 80);
                setText('fd-count', data.fdOpen.toLocaleString() + " / " + data.fdMax.toLocaleString());
                setText('fd-percent', data.fdPercent + " %");
            } else {
                setText('fd-count', "미지원 환경");
                setText('fd-percent', "-");
            }
        }

        // 트래픽 이력 스파크라인 (외부 차트 라이브러리 없이 SVG polyline으로 그린다)
        function updateTrafficHistory() {
            fetch('/super/system/history.json')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.status !== 'success' || !data.available) return;
                const samples = data.samples || [];
                const line = document.getElementById('traffic-spark-line');
                if (!line) return;
                if (samples.length < 2) {
                    line.setAttribute('points', "");
                    setText('traffic-spark-max', "표본 " + samples.length + "개");
                    return;
                }
                let max = 0;
                samples.forEach(function(s) { if (s.outMb > max) max = s.outMb; });
                if (max <= 0) max = 1;

                const w = 600, h = 80, pad = 4;
                const step = (samples.length > 1) ? (w / (samples.length - 1)) : w;
                const pts = samples.map(function(s, i) {
                    const x = (i * step).toFixed(1);
                    const y = (h - pad - ((s.outMb / max) * (h - pad * 2))).toFixed(1);
                    return x + "," + y;
                });
                line.setAttribute('points', pts.join(" "));
                setText('traffic-spark-max', "최대 " + max.toFixed(1) + " MB / 5분 · 표본 " + samples.length + "개");
            })
            .catch(function(err) {
                console.error("지표 이력 조회 도중 오류 발생:", err);
            });
        }

        // 메모리 청소 모달 노출
        function confirmMemoryCleanup() {
            const confirmModal = new bootstrap.Modal(document.getElementById('cleanupConfirmModal'));
            confirmModal.show();
        }

        // 메모리 청소(GC) 실행 호출
        function executeMemoryCleanup() {
            // 확인 모달 닫기
            const confirmModalEl = document.getElementById('cleanupConfirmModal');
            const confirmModal = bootstrap.Modal.getInstance(confirmModalEl);
            if(confirmModal) confirmModal.hide();

            // GC 버튼 로더 아이콘 노출
            document.getElementById('gc-btn-normal-icon').style.display = 'none';
            document.getElementById('gc-btn-icon').style.display = 'inline-block';

            fetch('/super/system/clean.do', { method: 'POST' })
            .then(response => response.json())
            .then(data => {
                // GC 버튼 상태 원복
                document.getElementById('gc-btn-icon').style.display = 'none';
                document.getElementById('gc-btn-normal-icon').style.display = 'inline-block';

                if(data.status === 'success') {
                    // 청소 결과 모달 텍스트 세팅 및 노출
                    document.getElementById('cleanup-result-text').innerText = data.msg;
                    new bootstrap.Modal(document.getElementById('cleanupResultModal')).show();
                    // 즉각 상태 새로고침
                    updateSystemStatus();
                } else {
                    alert("메모리 정리 실패: " + data.msg);
                }
            })
            .catch(error => {
                document.getElementById('gc-btn-icon').style.display = 'none';
                document.getElementById('gc-btn-normal-icon').style.display = 'inline-block';
                console.error("메모리 정리 도중 통신 에러 발생:", error);
                alert("메모리 정리 실패: 네트워크 통신 오류");
            });
        }
    </script>
</body>
</html>
