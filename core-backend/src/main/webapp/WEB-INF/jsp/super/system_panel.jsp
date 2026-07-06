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
                <span class="fw-semibold text-secondary small">모니터링 작동 중 (5초 주기)</span>
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

        document.addEventListener("DOMContentLoaded", function() {
            // 즉시 데이터 1회 가져오기
            updateSystemStatus();
            // 5초 간격 폴링 시작
            pollingInterval = setInterval(updateSystemStatus, 5000);
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
                }
            })
            .catch(error => {
                console.error("실시간 상태 조회 도중 오류 발생:", error);
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
