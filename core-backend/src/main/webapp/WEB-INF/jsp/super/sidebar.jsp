<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<style>
    /* 공통 사이드바 전용 스타일 */
    .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; top: 0; left: 0; z-index: 1000; }
    .nav-link { color: rgba(255,255,255,0.7); padding: 12px 20px; font-size: 1.1rem; text-decoration: none; display: block; }
    .nav-link:hover, .nav-link.active { color: #fff; background: rgba(255,255,255,0.1); }
    .chat-bubble { max-width: 75%; padding: 10px 14px; border-radius: 15px; margin-bottom: 10px; font-size: 14px; }
    .chat-user { background: #e9ecef; color: #333; align-self: flex-start; border-bottom-left-radius: 2px; }
    .chat-admin { background: #3880ff; color: #fff; align-self: flex-end; border-bottom-right-radius: 2px; }
    .chat-row { display: flex; flex-direction: column; }
    .chat-time { font-size: 11px; color: #999; margin-top: 2px; }
</style>

<div class="sidebar d-flex flex-column p-3">
    <h3 class="text-center mb-5 mt-3 fw-bold">🚀 Super Admin</h3>
    <ul class="nav flex-column">
        <li class="nav-item mb-2">
            <a href="/super/dashboard.do" class="nav-link <c:if test="${activeMenu eq 'dashboard'}">active</c:if>"><i class="fas fa-chart-pie me-2"></i> 대시보드</a>
        </li>
        <li class="nav-item mb-2">
            <a href="/super/star/list.do" class="nav-link <c:if test="${activeMenu eq 'star_list'}">active</c:if>"><i class="fas fa-users-cog me-2"></i> 스타 관리</a>
        </li>
        <li class="nav-item mb-2">
            <a href="/super/star/create.do" class="nav-link <c:if test="${activeMenu eq 'star_create'}">active</c:if>"><i class="fas fa-user-plus me-2"></i> 스타 등록</a>
        </li>
        
        <c:if test="${sessionScope.SUPER_USER_SESSION.PRS_AUTH eq 'SM'}">
            <!-- 🌟 최고 권한 전용: 스타 일괄 등록 메뉴 추가 -->
            <li class="nav-item mb-2">
                <a href="/super/star/bulk-create.do" class="nav-link <c:if test="${activeMenu eq 'star_bulk'}">active</c:if>"><i class="fas fa-file-import me-2"></i> 스타 일괄 등록</a>
            </li>
            <li class="nav-item mb-2">
                <a href="#" class="nav-link text-info" onclick="openThreadsModal()">
                    <i class="fas fa-envelope me-2"></i> 정산/1:1 문의
                    <span id="globalMsgBadge" class="badge bg-danger ms-2" style="display:none;">0</span>
                </a>
            </li>
            <li class="nav-item mb-2">
                <a href="/super/local/create.do" class="nav-link text-warning <c:if test="${activeMenu eq 'local_create'}">active</c:if>"><i class="fas fa-user-shield me-2"></i> 지역 관리자 생성</a>
            </li>
            <li class="nav-item mb-2">
                <a href="/super/report/list.do" class="nav-link <c:if test="${activeMenu eq 'report'}">active</c:if>" style="color: #ff8a65;">
                    <i class="fas fa-flag me-2"></i> 신고 관리
                </a>
            </li>
        </c:if>
        
        <li class="nav-item mt-auto">
            <a href="/super/logout.do" class="nav-link text-danger"><i class="fas fa-sign-out-alt me-2"></i> 로그아웃</a>
        </li>
    </ul>
</div>

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
    }

    function checkUnreadMessages() {
        fetch('/super/message/unreadCount.do', { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            const badge = document.getElementById('globalMsgBadge');
            if (badge) {
                if(data.count > 0) {
                    badge.innerText = data.count;
                    badge.style.display = 'inline-block';
                } else {
                    badge.style.display = 'none';
                }
            }
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
