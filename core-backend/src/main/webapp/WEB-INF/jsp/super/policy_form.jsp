<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>약관 수정 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        body { background-color: #f0f2f5; font-family: 'Pretendard', sans-serif; }
        .main-content { margin-left: 260px; padding: 30px; }
        .form-card { background: white; border-radius: 16px; padding: 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); max-width: 1000px; margin: 0 auto 24px; }
        .form-label { font-weight: bold; color: #495057; }
        /* 장문 법률 문서 편집용: HTML 원문을 그대로 다룬다.
           WYSIWYG(SmartEditor2)은 재직렬화 과정에서 장문 2개국어 HTML을 훼손할 위험이 있어 쓰지 않는다. */
        .policy-body { font-family: 'D2Coding', 'Consolas', monospace; font-size: 13px; line-height: 1.5; }
        #previewFrame { width: 100%; height: 70vh; border: 0; }
    </style>
</head>
<body>

    <!-- 공통 사이드바 include -->
    <c:set var="activeMenu" value="policy" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2 class="fw-bold">📄 약관 수정</h2>
        </div>
        <div class="alert alert-info mx-auto" style="max-width: 1000px;">
            <i class="fas fa-info-circle"></i>
            여기서 저장한 내용은 앱의 약관 화면과 웹(<code>/terms</code>, <code>/privacy</code>)에 즉시 반영됩니다.
            본문은 HTML 원문이며, 저장 전 <b>미리보기</b>로 렌더링 결과를 확인하세요.
        </div>

        <div id="policyContainer">
            <div class="text-center text-muted py-5" id="loadingMsg">불러오는 중...</div>
        </div>
    </div>

    <!-- 미리보기 모달 -->
    <div class="modal fade" id="previewModal" tabindex="-1">
        <div class="modal-dialog modal-xl modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header bg-dark text-white">
                    <h5 class="modal-title" id="previewTitle">미리보기</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-0">
                    <iframe id="previewFrame" sandbox=""></iframe>
                </div>
            </div>
        </div>
    </div>

<script>
    // 약관 목록 로드 후 카드 렌더링.
    // 본문에 HTML/따옴표가 포함되므로 문자열 템플릿 조립 대신 DOM API + value 할당으로 넣는다.
    function loadPolicies() {
        fetch('/super/policy/list.do', { method: 'POST' })
            .then(function (res) { return res.json(); })
            .then(function (data) {
                var container = document.getElementById('policyContainer');
                container.innerHTML = '';
                if (data.status !== 'success' || !data.list || data.list.length === 0) {
                    container.innerHTML = '<div class="alert alert-warning mx-auto" style="max-width:1000px;">'
                        + '등록된 약관(CON_TYPE=7)이 없습니다. 앱 설정 콘텐츠에 약관을 먼저 등록해 주세요.</div>';
                    return;
                }
                data.list.forEach(function (row) { container.appendChild(buildCard(row)); });
            })
            .catch(function (err) {
                console.error(err);
                document.getElementById('loadingMsg').innerText = '목록 조회 중 오류가 발생했습니다.';
            });
    }

    function buildCard(row) {
        var card = document.createElement('div');
        card.className = 'form-card';

        var header = document.createElement('div');
        header.className = 'd-flex justify-content-between align-items-center mb-3 border-bottom pb-2';
        var h5 = document.createElement('h5');
        h5.className = 'mb-0 fw-bold';
        h5.textContent = 'CON_ID ' + row.CON_ID;
        var updated = document.createElement('span');
        updated.className = 'text-muted';
        updated.style.fontSize = '13px';
        updated.textContent = '최종 수정: ' + (row.CON_UDATE || '-');
        header.appendChild(h5);
        header.appendChild(updated);

        var titleLabel = document.createElement('label');
        titleLabel.className = 'form-label';
        titleLabel.textContent = '제목';
        var titleInput = document.createElement('input');
        titleInput.type = 'text';
        titleInput.className = 'form-control mb-3';
        titleInput.value = row.CON_TITLE || '';

        var bodyLabel = document.createElement('label');
        bodyLabel.className = 'form-label';
        bodyLabel.textContent = '본문 (HTML)';
        var bodyArea = document.createElement('textarea');
        bodyArea.className = 'form-control policy-body mb-3';
        bodyArea.rows = 22;
        bodyArea.value = row.CON_BODY || '';

        var btnRow = document.createElement('div');
        btnRow.className = 'd-flex justify-content-end gap-2';

        var previewBtn = document.createElement('button');
        previewBtn.type = 'button';
        previewBtn.className = 'btn btn-outline-secondary';
        previewBtn.innerHTML = '<i class="fas fa-eye"></i> 미리보기';
        previewBtn.onclick = function () { openPreview(titleInput.value, bodyArea.value); };

        var saveBtn = document.createElement('button');
        saveBtn.type = 'button';
        saveBtn.className = 'btn btn-primary px-4 fw-bold';
        saveBtn.innerHTML = '<i class="fas fa-save"></i> 저장';
        saveBtn.onclick = function () { savePolicy(row.CON_ID, titleInput.value, bodyArea.value, saveBtn); };

        btnRow.appendChild(previewBtn);
        btnRow.appendChild(saveBtn);

        card.appendChild(header);
        card.appendChild(titleLabel);
        card.appendChild(titleInput);
        card.appendChild(bodyLabel);
        card.appendChild(bodyArea);
        card.appendChild(btnRow);
        return card;
    }

    function openPreview(title, body) {
        document.getElementById('previewTitle').innerText = title || '미리보기';
        // sandbox iframe + srcdoc: 미리보기 중 스크립트 실행을 차단한 채 렌더링만 확인
        document.getElementById('previewFrame').srcdoc =
            '<!DOCTYPE html><html><head><meta charset="UTF-8">'
            + '<style>body{font-family:sans-serif;padding:24px;line-height:1.6;}</style>'
            + '</head><body>' + body + '</body></html>';
        new bootstrap.Modal(document.getElementById('previewModal')).show();
    }

    function savePolicy(conId, title, body, btn) {
        if (!body || !body.trim()) {
            alert('본문이 비어 있습니다.');
            return;
        }
        if (!confirm('저장하시겠습니까? 앱과 웹에 즉시 반영됩니다.')) return;

        btn.disabled = true;
        fetch('/super/policy/save.do', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ CON_ID: conId, CON_TITLE: title, CON_BODY: body })
        })
            .then(function (res) { return res.json(); })
            .then(function (data) {
                btn.disabled = false;
                if (data.status === 'success') {
                    alert('저장되었습니다.');
                    loadPolicies();
                } else {
                    alert('저장 실패: ' + (data.msg || '알 수 없는 오류'));
                }
            })
            .catch(function (err) {
                btn.disabled = false;
                console.error(err);
                alert('서버 통신 중 오류가 발생했습니다.');
            });
    }

    loadPolicies();
</script>

</body>
</html>
