<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<style>
    /* 공통 사이드바 전용 스타일 */
    /* 메뉴가 화면 높이를 넘으면 사이드바 안에서만 스크롤된다.
       overflow가 없으면 fixed 요소라 넘친 하단 메뉴에 접근할 방법이 없다. */
    .sidebar {
        width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff;
        top: 0; left: 0; z-index: 1000;
        overflow-y: auto; overflow-x: hidden;
        scrollbar-width: thin; scrollbar-color: rgba(255,255,255,0.25) transparent;
    }
    /* 어두운 배경에 맞춘 얇은 스크롤바 (webkit 계열) */
    .sidebar::-webkit-scrollbar { width: 6px; }
    .sidebar::-webkit-scrollbar-track { background: transparent; }
    .sidebar::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 3px; }
    .sidebar::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.35); }

    .sidebar-title { font-size: 1.15rem; }
    .nav-link { color: rgba(255,255,255,0.7); padding: 9px 14px; font-size: 0.95rem; text-decoration: none; display: block; border-radius: 6px; }
    .nav-link:hover, .nav-link.active { color: #fff; background: rgba(255,255,255,0.1); }
    /* 아이콘 폭을 고정해 라벨 시작선을 맞춘다 */
    .nav-link i { font-size: 0.85rem; width: 18px; text-align: center; }

    /* ===== 1단계 아코디언 메뉴 (2026-10-12) =====
       카테고리 머리글을 누르면 그 아래 항목이 접히고 펼쳐진다. 현재 화면이 속한 카테고리는 항상 펼친다.
       펼침 상태는 localStorage 에 기억한다(카테고리 id 별). */
    .nav-group { margin-bottom: 4px; }
    .nav-group-toggle {
        display: flex; align-items: center; gap: 8px;
        padding: 9px 14px; border-radius: 6px; cursor: pointer; user-select: none;
        color: rgba(255,255,255,0.85); font-size: 0.9rem; font-weight: 700; letter-spacing: 0.2px;
        text-decoration: none;
    }
    .nav-group-toggle:hover { color: #fff; background: rgba(255,255,255,0.08); }
    .nav-group-toggle i.group-icon { width: 18px; text-align: center; font-size: 0.9rem; }
    .nav-group-toggle .group-caret {
        margin-left: auto; width: auto; font-size: 0.7rem; color: rgba(255,255,255,0.45);
        transition: transform 0.2s ease;
    }
    .nav-group-toggle[aria-expanded="true"] .group-caret { transform: rotate(180deg); }
    .nav-group-toggle .badge { font-size: 0.65rem; }
    /* 하위 항목: 왼쪽 안내선 + 들여쓰기 */
    .nav-sub { list-style: none; padding-left: 0; margin: 2px 0 4px 14px; border-left: 1px solid rgba(255,255,255,0.12); }
    .nav-sub .nav-link { padding: 7px 10px 7px 14px; font-size: 0.9rem; border-radius: 0 6px 6px 0; }
    .nav-sub .nav-link.active { box-shadow: inset 3px 0 0 #4fc3f7; }
    .nav-divider { border-top: 1px solid rgba(255,255,255,0.1); margin: 8px 6px; }
    /* 좁은 화면(기존 페이지들의 가로 메뉴 배치)에서는 항상 펼친다 — 접힌 메뉴에 손이 안 닿는 일 방지 */
    @media (max-width: 768px) {
        .nav-sub.collapse { display: block !important; }
        .nav-group-toggle .group-caret { display: none; }
        .nav-sub { border-left: none; margin-left: 0; }
    }
    .chat-bubble { max-width: 75%; padding: 10px 14px; border-radius: 15px; margin-bottom: 10px; font-size: 14px; }
    .chat-user { background: #e9ecef; color: #333; align-self: flex-start; border-bottom-left-radius: 2px; }
    .chat-admin { background: #3880ff; color: #fff; align-self: flex-end; border-bottom-right-radius: 2px; }
    .chat-row { display: flex; flex-direction: column; }
    .chat-time { font-size: 11px; color: #999; margin-top: 2px; }
</style>

<div class="sidebar d-flex flex-column p-3">
    <h3 class="sidebar-title text-center mb-4 mt-2 fw-bold">🚀 Super Admin</h3>
    <c:set var="isSM" value="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}" />
    <ul class="nav flex-column">
        <li class="nav-item mb-1">
            <a href="/super/dashboard.do" class="nav-link <c:if test="${activeMenu eq 'dashboard'}">active</c:if>"><i class="fas fa-chart-pie me-2"></i> 대시보드</a>
        </li>

        <!-- ① 스타 — 계정·분류·소개 (SM 전체 / LC 자국) -->
        <li class="nav-group" data-group="star">
            <a class="nav-group-toggle" data-bs-toggle="collapse" href="#navGroupStar" role="button" aria-expanded="false" aria-controls="navGroupStar">
                <i class="fas fa-star group-icon" style="color: #ffd166;"></i> 스타
                <i class="fas fa-chevron-down group-caret"></i>
            </a>
            <ul class="nav-sub collapse" id="navGroupStar">
                <li><a href="/super/star/list.do" class="nav-link <c:if test="${activeMenu eq 'star_list'}">active</c:if>"><i class="fas fa-users-cog me-2"></i> 스타 관리</a></li>
                <li><a href="/super/star/create.do" class="nav-link <c:if test="${activeMenu eq 'star_create'}">active</c:if>"><i class="fas fa-user-plus me-2"></i> 스타 등록</a></li>
                <c:if test="${isSM}">
                    <!-- 🌟 최고 권한 전용: 스타 일괄 등록 -->
                    <li><a href="/super/star/bulk-create.do" class="nav-link <c:if test="${activeMenu eq 'star_bulk'}">active</c:if>"><i class="fas fa-file-import me-2"></i> 스타 일괄 등록</a></li>
                </c:if>
                <!-- 🌟 VS 배틀필드: 스타 직군 분류 -->
                <li><a href="/super/star/category.do" class="nav-link <c:if test="${activeMenu eq 'star_category'}">active</c:if>"><i class="fas fa-tags me-2"></i> 스타 직군 분류</a></li>
                <!-- 🌟 웹 랜딩 About 섹션용 스타 소개문 -->
                <li><a href="/super/star/bio.do" class="nav-link <c:if test="${activeMenu eq 'star_bio'}">active</c:if>"><i class="fas fa-id-card me-2"></i> 스타 소개(Bio) 관리</a></li>
            </ul>
        </li>

        <!-- 인사이트 — 지표별 스타 순위와 스타별 그래프 (2-34차, SM 전체 / LC 자국) -->
        <li class="nav-group" data-group="insight">
            <a class="nav-group-toggle" data-bs-toggle="collapse" href="#navGroupInsight" role="button" aria-expanded="false" aria-controls="navGroupInsight">
                <i class="fas fa-chart-line group-icon" style="color: #90caf9;"></i> 인사이트
                <i class="fas fa-chevron-down group-caret"></i>
            </a>
            <ul class="nav-sub collapse" id="navGroupInsight">
                <li><a href="/super/insight/list.do?metric=RANK" class="nav-link <c:if test="${activeMenu eq 'insight_rank'}">active</c:if>"><i class="fas fa-trophy me-2"></i> 랭킹</a></li>
                <li><a href="/super/insight/list.do?metric=VISIT" class="nav-link <c:if test="${activeMenu eq 'insight_visit'}">active</c:if>"><i class="fas fa-eye me-2"></i> 방문자수</a></li>
                <li><a href="/super/insight/list.do?metric=FOLLOW" class="nav-link <c:if test="${activeMenu eq 'insight_follow'}">active</c:if>"><i class="fas fa-star me-2"></i> 즐겨찾기</a></li>
                <li><a href="/super/insight/list.do?metric=LIKE" class="nav-link <c:if test="${activeMenu eq 'insight_like'}">active</c:if>"><i class="fas fa-heart me-2"></i> 좋아요</a></li>
                <li><a href="/super/insight/list.do?metric=POST" class="nav-link <c:if test="${activeMenu eq 'insight_post'}">active</c:if>"><i class="fas fa-file-alt me-2"></i> 게시물</a></li>
            </ul>
        </li>

        <c:if test="${isSM}">
            <!-- ② 로비 편성 — 앱 로비에 전 국가 공통으로 노출되는 요소 (SM 전용) -->
            <li class="nav-group" data-group="lobby">
                <a class="nav-group-toggle" data-bs-toggle="collapse" href="#navGroupLobby" role="button" aria-expanded="false" aria-controls="navGroupLobby">
                    <i class="fas fa-mobile-screen group-icon" style="color: #f48fb1;"></i> 로비 편성
                    <i class="fas fa-chevron-down group-caret"></i>
                </a>
                <ul class="nav-sub collapse" id="navGroupLobby">
                    <li><a href="/super/latest-contents/list.do" class="nav-link <c:if test="${activeMenu eq 'latest_contents'}">active</c:if>"><i class="fas fa-film me-2"></i> Star Shorts 숏폼</a></li>
                    <li><a href="/super/vs/list.do" class="nav-link <c:if test="${activeMenu eq 'vs_list'}">active</c:if>"><i class="fas fa-bolt me-2"></i> VS 배틀필드</a></li>
                    <li><a href="/super/live-news/list.do" class="nav-link <c:if test="${activeMenu eq 'live_news'}">active</c:if>"><i class="fas fa-broadcast-tower me-2"></i> LIVE NEWS</a></li>
                    <%-- 2-34차: 같은 편성 화면을 구역별로 연다 (세로형 = 앱 "Latest Content", 가로형 = 앱 "Hot Content") --%>
                    <li><a href="/super/latest-contents/list.do?section=LATEST" class="nav-link <c:if test="${activeMenu eq 'latest_feed'}">active</c:if>"><i class="fas fa-grip-lines me-2"></i> 세로형 자동재생</a></li>
                    <li><a href="/super/latest-contents/list.do?section=HOT" class="nav-link <c:if test="${activeMenu eq 'hot_content'}">active</c:if>"><i class="fas fa-grip-lines-vertical me-2"></i> 가로형 미니숏폼</a></li>
                </ul>
            </li>

            <!-- ③ 검수·신고 — 콘텐츠 사전 검수와 신고 처리 (SM 전용) -->
            <li class="nav-group" data-group="moderation">
                <a class="nav-group-toggle" data-bs-toggle="collapse" href="#navGroupModeration" role="button" aria-expanded="false" aria-controls="navGroupModeration">
                    <i class="fas fa-shield-alt group-icon" style="color: #ff8a65;"></i> 검수 · 신고
                    <i class="fas fa-chevron-down group-caret"></i>
                </a>
                <ul class="nav-sub collapse" id="navGroupModeration">
                    <li><a href="/super/moderation/list.do" class="nav-link <c:if test="${activeMenu eq 'moderation'}">active</c:if>"><i class="fas fa-photo-video me-2"></i> 미디어 검수</a></li>
                    <li><a href="/super/report/list.do" class="nav-link <c:if test="${activeMenu eq 'report'}">active</c:if>"><i class="fas fa-flag me-2"></i> 신고 관리</a></li>
                    <li><a href="/super/dm-report/list.do" class="nav-link <c:if test="${activeMenu eq 'dm_report'}">active</c:if>"><i class="fas fa-comment-slash me-2"></i> 채팅 신고</a></li>
                </ul>
            </li>

            <!-- ④ 문의 — 스타 1:1 문의와 공개 웹 문의함 (SM 전용). 머리글 배지 = 미읽음 합계 -->
            <li class="nav-group" data-group="inquiry">
                <a class="nav-group-toggle" data-bs-toggle="collapse" href="#navGroupInquiry" role="button" aria-expanded="false" aria-controls="navGroupInquiry">
                    <i class="fas fa-envelope group-icon" style="color: #80cbc4;"></i> 문의
                    <span id="groupBadgeInquiry" class="badge bg-danger ms-1" style="display:none;">0</span>
                    <i class="fas fa-chevron-down group-caret"></i>
                </a>
                <ul class="nav-sub collapse" id="navGroupInquiry">
                    <li>
                        <a href="#" class="nav-link" onclick="openThreadsModal(); return false;">
                            <i class="fas fa-comments me-2"></i> 정산/1:1 문의
                            <span id="globalMsgBadge" class="badge bg-danger ms-2" style="display:none;">0</span>
                        </a>
                    </li>
                    <!-- 🌟 공개 웹 문의함 (2-29차) — witch-hunting.com/contact 접수분 -->
                    <li><a href="/super/contact/list.do" class="nav-link <c:if test="${activeMenu eq 'web_contact'}">active</c:if>"><i class="fas fa-envelope-open-text me-2"></i> 웹 문의함</a></li>
                </ul>
            </li>
        </c:if>

        <!-- ⑤ 시스템 · 보안 — 이중인증은 SM·LC 공통, 나머지는 SM 전용. 머리글 배지 = 열린 경보 수 -->
        <li class="nav-group" data-group="system">
            <a class="nav-group-toggle" data-bs-toggle="collapse" href="#navGroupSystem" role="button" aria-expanded="false" aria-controls="navGroupSystem">
                <i class="fas fa-server group-icon" style="color: #4fc3f7;"></i> 시스템 · 보안
                <span id="groupBadgeSystem" class="badge bg-danger ms-1" style="display:none;">0</span>
                <i class="fas fa-chevron-down group-caret"></i>
            </a>
            <ul class="nav-sub collapse" id="navGroupSystem">
                <!-- 보안 · 이중인증(OTP): 내 등록 상태 (SM·LC 공통) -->
                <li><a href="/super/mfa/manage.do" class="nav-link <c:if test="${activeMenu eq 'mfa_manage'}">active</c:if>"><i class="fas fa-shield-halved me-2"></i> 이중인증(OTP)</a></li>
                <c:if test="${isSM}">
                    <!-- 서버 위험 경보 센터 — 열린 경보 수를 배지로 표시 (60초 폴링) -->
                    <li>
                        <a href="/super/alert/center.do" class="nav-link <c:if test="${activeMenu eq 'alert_center'}">active</c:if>">
                            <i class="fas fa-bell me-2"></i> 경보 센터
                            <span id="alertBadge" class="badge bg-danger ms-2" style="display:none;">0</span>
                        </a>
                    </li>
                    <li><a href="/super/system/panel.do" class="nav-link <c:if test="${activeMenu eq 'system_panel'}">active</c:if>"><i class="fas fa-gauge-high me-2"></i> 시스템 관리</a></li>
                    <!-- 보안 감사 로그 (로그인·OTP·경보 설정 변경) -->
                    <li><a href="/super/mfa/audit.do" class="nav-link <c:if test="${activeMenu eq 'audit_log'}">active</c:if>"><i class="fas fa-clipboard-list me-2"></i> 감사 로그</a></li>
                    <li><a href="/super/local/create.do" class="nav-link <c:if test="${activeMenu eq 'local_create'}">active</c:if>"><i class="fas fa-user-shield me-2"></i> 지역 관리자 생성</a></li>
                </c:if>
            </ul>
        </li>

        <c:if test="${isSM}">
            <li class="nav-divider"></li>
            <!-- 🌟 약관/개인정보처리방침 수정 (좌측 탭 맨 하단 배치 — 클라이언트 요청이라 그룹에 넣지 않는다) -->
            <li class="nav-item mb-1">
                <a href="/super/policy/edit.do" class="nav-link <c:if test="${activeMenu eq 'policy'}">active</c:if>" style="color: #a5d6a7;">
                    <i class="fas fa-file-contract me-2"></i> 약관 수정
                </a>
            </li>
        </c:if>

        <li class="nav-item mt-auto">
            <a href="/super/logout.do" class="nav-link text-danger"><i class="fas fa-sign-out-alt me-2"></i> 로그아웃</a>
        </li>
    </ul>
</div>

<!-- 아코디언 펼침 상태: 현재 화면이 속한 그룹은 항상 펼치고, 나머지는 사용자가 마지막에 둔 상태를 따른다 -->
<script>
    (function () {
        var STORAGE_KEY = 'superAdminNavOpen';
        var opened = {};
        try { opened = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') || {}; } catch (e) { opened = {}; }

        var groups = document.querySelectorAll('.sidebar .nav-group');
        var hasActive = false;
        groups.forEach(function (group) {
            var id = group.getAttribute('data-group');
            var toggle = group.querySelector('.nav-group-toggle');
            var panel = group.querySelector('.nav-sub');
            var containsActive = !!group.querySelector('.nav-link.active');
            if (containsActive) hasActive = true;
            // 처음 방문(저장값 없음)은 펼친 상태로 시작해 메뉴가 어디 있는지 보이게 한다
            var open = containsActive || (id in opened ? !!opened[id] : true);
            if (open) {
                panel.classList.add('show');
                toggle.setAttribute('aria-expanded', 'true');
            }
            panel.addEventListener('shown.bs.collapse', function () { opened[id] = true; save(); });
            panel.addEventListener('hidden.bs.collapse', function () { opened[id] = false; save(); });
        });

        function save() {
            try { localStorage.setItem(STORAGE_KEY, JSON.stringify(opened)); } catch (e) { /* 사생활 모드 등 — 무시 */ }
        }
    })();
</script>

<!-- 정산 및 1:1 문의용 모달 레이아웃 -->
<div class="modal fade" id="threadsModal" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header bg-dark text-white">
        <h5 class="modal-title">📬 정산 및 1:1 문의 목록</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body p-0">
        <div class="list-group list-group-flush" id="threadListContainer">
        </div>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="chatModal" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header bg-info text-white">
        <h5 class="modal-title" id="chatModalTitle">대화창</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body" id="chatContainer" style="height: 400px; overflow-y: auto; background: #f8f9fa;">
      </div>
      <div class="modal-footer p-2">
        <div class="input-group">
          <input type="text" id="chatInput" class="form-control" placeholder="답변을 입력하세요..." onkeypress="if(event.keyCode==13) sendAdminMessage()">
          <button class="btn btn-primary" onclick="sendAdminMessage()">전송</button>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- 1:1 문의 처리 자바스크립트 로직 (전체 어드민 공통 제공) -->
<script>
    let currentChatPageId = '';
    let chatPollingTimer = null;

    // 1. [뱃지] 10초마다 안 읽은 메시지 개수 체크 (SM 권한일 때만 실행)
    if ("${sessionScope.SUPER_USER_SESSION.PRS_AUTH}" === "SM") {
        // 즉시 한 번 호출하고 10초 주기로 폴링
        setTimeout(checkUnreadMessages, 1000);
        setInterval(checkUnreadMessages, 10000);
        // 서버 경보: 배지 + 위험 이상이면 모든 어드민 화면 상단에 띠 표시 (60초 주기, 메모리 상태라 가볍다)
        setTimeout(checkOpenAlerts, 1500);
        setInterval(checkOpenAlerts, 60000);
    }

    function checkOpenAlerts() {
        fetch('/super/alert/count.json', { credentials: 'same-origin' })
        .then(res => res.json())
        .then(data => {
            const badge = document.getElementById('alertBadge');
            const groupBadge = document.getElementById('groupBadgeSystem');
            [badge, groupBadge].forEach(function (el) {
                if (!el) return;
                if (data.open > 0) { el.innerText = data.open; el.style.display = 'inline-block'; }
                else { el.style.display = 'none'; }
            });
            let bar = document.getElementById('globalAlertBar');
            const serious = data.open > 0 && (data.maxSeverity === 'DANGER' || data.maxSeverity === 'CRITICAL');
            if (serious) {
                if (!bar) {
                    bar = document.createElement('div');
                    bar.id = 'globalAlertBar';
                    bar.style.cssText = 'position:fixed;top:0;left:260px;right:0;z-index:1050;padding:8px 16px;font-size:14px;color:#fff;text-align:center;';
                    document.body.appendChild(bar);
                }
                bar.style.background = data.maxSeverity === 'CRITICAL' ? '#e74c3c' : '#e67e22';
                bar.innerHTML = '서버 경보 ' + data.open + '건 (최고 등급: ' + data.maxSeverityLabel + ') — <a href="/super/alert/center.do" style="color:#fff;text-decoration:underline;">경보 센터로 이동</a>';
            } else if (bar) {
                bar.remove();
            }
        }).catch(err => console.error('Error fetching alert count:', err));
    }

    function checkUnreadMessages() {
        fetch('/super/message/unreadCount.do', { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            const badge = document.getElementById('globalMsgBadge');
            const groupBadge = document.getElementById('groupBadgeInquiry');
            [badge, groupBadge].forEach(function (el) {
                if (!el) return;
                if (data.count > 0) {
                    el.innerText = data.count;
                    el.style.display = 'inline-block';
                } else {
                    el.style.display = 'none';
                }
            });
        }).catch(err => console.error('Error fetching unread count:', err));
    }

    // 2. [목록] 채팅방 목록 모달 열기
    function openThreadsModal() {
        fetch('/super/message/threads.do', { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            let html = '';
            data.list.forEach(t => {
                const badgeStr = t.UNREAD_COUNT > 0 ? `<span class="badge bg-danger rounded-pill">\${t.UNREAD_COUNT}</span>` : '';
                html += `
                <a href="#" class="list-group-item list-group-item-action d-flex justify-content-between align-items-start" onclick="openChatModal('\${t.PAGE_ID}', '\${t.PRS_NAME}')">
                    <div class="ms-2 me-auto">
                        <div class="fw-bold">\${t.PRS_NAME} <span class="text-muted" style="font-size:12px;">(\${t.PAGE_ID})</span></div>
                        <span class="text-muted" style="font-size: 13px;">\${t.LAST_MSG || '사진/동영상'}</span>
                    </div>
                    \${badgeStr}
                </a>`;
            });
            document.getElementById('threadListContainer').innerHTML = html || '<div class="p-4 text-center text-muted">메시지가 없습니다.</div>';
            new bootstrap.Modal(document.getElementById('threadsModal')).show();
        }).catch(err => console.error('Error fetching threads:', err));
    }

    // 3. [상세 채팅] 특정 스타와의 채팅창 열기
    function openChatModal(pageId, name) {
        currentChatPageId = pageId;
        document.getElementById('chatModalTitle').innerText = name + "님과의 대화";
        
        const threadsModalEl = document.getElementById('threadsModal');
        const threadsModalInstance = bootstrap.Modal.getInstance(threadsModalEl);
        if (threadsModalInstance) {
            threadsModalInstance.hide();
        }
        
        new bootstrap.Modal(document.getElementById('chatModal')).show();
        
        loadChatMessages();
        // 모달이 열려있는 동안 3초마다 실시간 폴링
        if(chatPollingTimer) clearInterval(chatPollingTimer);
        chatPollingTimer = setInterval(loadChatMessages, 3000);
    }

    // 모달 닫힐 때 폴링 중지 및 뱃지 즉시 갱신
    document.getElementById('chatModal').addEventListener('hidden.bs.modal', function () {
        if(chatPollingTimer) clearInterval(chatPollingTimer);
        checkUnreadMessages();
    });

    // 4. 메시지 리스트 가져오기
    function loadChatMessages() {
        fetch('/api/super/message/list', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pageId: currentChatPageId, readerType: 'ADMIN' })
        })
        .then(res => res.json())
        .then(data => {
            let html = '';
            data.list.forEach(msg => {
                const isMe = msg.SENDER_TYPE === 'ADMIN';
                const bubbleClass = isMe ? 'chat-admin' : 'chat-user';
                const alignClass = isMe ? 'align-items-end' : 'align-items-start';
                
                const d = new Date(msg.CREATED_AT);
                const timeStr = `\${d.getMonth()+1}.\${d.getDate()} \${d.getHours()}:\${String(d.getMinutes()).padStart(2,'0')}`;
                
                html += `
                <div class="chat-row \${alignClass} mb-3">
                    <div class="chat-bubble \${bubbleClass}">\${msg.MESSAGE}</div>
                    <div class="chat-time">\${timeStr}</div>
                </div>`;
            });
            const container = document.getElementById('chatContainer');
            const isScrolledToBottom = container.scrollHeight - container.clientHeight <= container.scrollTop + 50;
            
            container.innerHTML = html;
            
            if(isScrolledToBottom) container.scrollTop = container.scrollHeight;
        }).catch(err => console.error('Error loading chat messages:', err));
    }

    // 5. 메시지 전송
    function sendAdminMessage() {
        const input = document.getElementById('chatInput');
        const msg = input.value.trim();
        if(!msg) return;

        fetch('/api/super/message/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pageId: currentChatPageId, senderType: 'ADMIN', message: msg })
        })
        .then(() => {
            input.value = '';
            loadChatMessages(); // 전송 후 즉시 리로드
            setTimeout(() => {
                const c = document.getElementById('chatContainer');
                c.scrollTop = c.scrollHeight;
            }, 100);
        }).catch(err => console.error('Error sending message:', err));
    }
</script>
