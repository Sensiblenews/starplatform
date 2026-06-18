<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>신고 관리 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        body { background-color: #f0f2f5; }
        .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; }
        .main-content { margin-left: 260px; padding: 30px; }
        .table-card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
        .text-truncate-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    </style>
</head>
<body>

    <div class="sidebar d-flex flex-column p-3">
        <h3 class="text-center mb-5 mt-3 fw-bold">🚀 Super Admin</h3>
        <ul class="nav flex-column">
            <li class="nav-item mb-2"><a href="/witch/super/dashboard.do" class="nav-link text-white-50"><i class="fas fa-chart-pie me-2"></i> 대시보드</a></li>
            <li class="nav-item mb-2"><a href="/witch/super/star/list.do" class="nav-link text-white-50"><i class="fas fa-users-cog me-2"></i> 스타 관리</a></li>
            <li class="nav-item mb-2"><a href="/witch/super/star/create.do" class="nav-link text-white-50"><i class="fas fa-user-plus me-2"></i> 스타 등록</a></li>
            <li class="nav-item mb-2"><a href="/witch/super/local/create.do" class="nav-link text-warning"><i class="fas fa-user-shield me-2"></i> 지역 관리자 생성</a></li>
            <li class="nav-item mb-2"><a href="/witch/super/report/list.do" class="nav-link active" style="color: #ff8a65;"><i class="fas fa-flag me-2"></i> 신고 관리</a></li>
            <li class="nav-item mt-auto"><a href="/witch/super/logout.do" class="nav-link text-danger"><i class="fas fa-sign-out-alt me-2"></i> 로그아웃</a></li>
        </ul>
    </div>

    <div class="main-content">
        <h2 class="fw-bold mb-4 text-danger"><i class="fas fa-exclamation-triangle me-2"></i>유저 신고 내역 (Global)</h2>

        <div class="table-card">
            <table class="table table-hover align-middle" style="table-layout: fixed;">
                <thead class="table-light">
                    <tr>
                        <th width="10%">신고일시</th>
                        <th width="12%">신고자 기기</th>
                        <th width="12%">신고 사유</th>
                        <th width="50%">원문 내용 (피의자 닉네임)</th>
                        
                        <th width="16%" class="text-center">즉각 조치</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="r" items="${reportList}">
                        <tr>
                            <td class="small text-muted">${r.REPORT_DATE}</td>
                            <td class="small text-secondary text-truncate" title="${r.REPORTER_DEVICE_ID}">${r.REPORTER_DEVICE_ID}</td>
                            <td><span class="badge bg-warning text-dark">${r.REASON}</span></td>
                            
                            <td>
                                <div class="text-truncate-2 small fw-bold text-dark mb-1">
                                    ${r.REPORTED_CONTENT != null ? r.REPORTED_CONTENT : '<span class="text-muted">(이미 삭제된 댓글입니다)</span>'}
                                </div>
                                <div class="small text-primary">
                                    <i class="fas fa-user"></i> ${r.REPORTED_NICKNAME != null ? r.REPORTED_NICKNAME : '알수없음'} 
                                    <span class="text-muted" style="font-size: 10px;">(ID: ${r.REPORTED_DEVICE_ID})</span>
                                </div>
                            </td>

                            <td class="text-center">
                                <div class="btn-group btn-group-sm">
                                    <c:if test="${r.COMMENT_STATUS eq 'NORMAL'}">
                                        <button class="btn btn-outline-dark" onclick="takeAction('BLIND', '${r.TARGET_ID}')">댓글 삭제</button>
                                    </c:if>
                                    <button class="btn btn-danger fw-bold" onclick="takeAction('GLOBAL_BLOCK', '', '${r.REPORTED_DEVICE_ID}')">영구 차단</button>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    
                    <c:if test="${empty reportList}">
                        <tr><td colspan="6" class="text-center py-5 text-muted">들어온 신고 내역이 없습니다.</td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>

<script>
function takeAction(actionType, cmtId, deviceId) {
    var confirmMsg = "";
    var payload = { action: actionType };

    if (actionType === 'BLIND') {
        confirmMsg = "이 댓글을 모든 유저에게서 즉시 숨김(블라인드) 처리하시겠습니까?";
        payload.cmtId = cmtId;
    } else if (actionType === 'GLOBAL_BLOCK') {
        if(!deviceId) {
            alert("차단할 기기 ID 정보가 없습니다."); return;
        }
        confirmMsg = "⚠️ 해당 유저(기기)를 앱 전체에서 영구 차단하시겠습니까?\n차단 즉시 앱 내 어느 곳에서도 댓글을 작성할 수 없습니다.";
        payload.deviceId = deviceId;
    }

    if(!confirm(confirmMsg)) return;

    $.post('/witch/super/report/action.do', payload, function(res) {
        if(res.status === 'success') {
            alert(res.msg);
            location.reload(); // 즉시 새로고침하여 상태 반영
        } else {
            alert("처리 실패: " + res.msg);
        }
    });
}
</script>
</body>
</html>