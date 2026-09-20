<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>웹 문의함 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <style>
        body { background-color: #f0f2f5; }
        .sidebar { width: 260px; height: 100vh; background: #212529; position: fixed; color: #fff; }
        .main-content { margin-left: 260px; padding: 30px; }
        .nav-link { color: rgba(255,255,255,0.7); padding: 12px 20px; }
        .nav-link:hover, .nav-link.active { color: #fff; background: rgba(255,255,255,0.1); }
        .table-card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
        /* 본문은 길어서 목록에서는 줄여 보여주고, 펼침 행에서 원문을 본다 */
        .msg-preview { max-width: 420px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .msg-full { white-space: pre-wrap; word-break: break-word; background: #f8f9fa; border-radius: 8px; padding: 14px; }

        @media (max-width: 768px) {
            .sidebar { width: 100%; height: auto; position: relative; padding-bottom: 10px; }
            .sidebar .nav { flex-direction: row !important; flex-wrap: wrap; justify-content: center; gap: 10px; }
            .sidebar .nav-item { margin-bottom: 0 !important; }
            .main-content { margin-left: 0; padding: 15px; }
        }
    </style>
</head>
<body>
    <!-- 공통 사이드바 include -->
    <c:set var="activeMenu" value="web_contact" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="mb-4">
            <h4 class="fw-bold mb-1"><i class="fas fa-envelope-open-text text-primary me-2"></i>웹 문의함</h4>
            <div class="text-secondary small">
                witch-hunting.com/contact 로 들어온 문의입니다. 접수 시 관리자 메일로도 발송되며,
                메일이 유실돼도 이 목록에는 남습니다. 회신은 문의자 이메일로 직접 보내세요.
            </div>
        </div>

        <div class="table-card">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <div class="fw-bold">전체 <c:out value="${totalCount}"/>건</div>
                <div class="text-secondary small">미처리 건이 위로 옵니다</div>
            </div>

            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead class="table-light">
                        <tr>
                            <th style="width: 90px;">상태</th>
                            <th style="width: 150px;">유형</th>
                            <th>제목 / 내용</th>
                            <th style="width: 200px;">보낸 사람</th>
                            <th style="width: 150px;">접수 일시</th>
                            <th style="width: 110px;">처리</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${contactList}">
                            <tr>
                                <td>
                                    <c:choose>
                                        <c:when test="${row.STATUS eq 'DONE'}"><span class="badge bg-secondary">완료</span></c:when>
                                        <c:otherwise><span class="badge bg-danger">미처리</span></c:otherwise>
                                    </c:choose>
                                    <%-- 메일이 안 나간 건은 여기서만 확인할 수 있으므로 표시해 둔다 --%>
                                    <c:if test="${row.MAIL_SENT_YN ne 'Y'}">
                                        <span class="badge bg-warning text-dark mt-1" title="관리자 알림 메일 발송 실패">메일X</span>
                                    </c:if>
                                </td>
                                <td class="small"><c:out value="${row.CONTACT_TYPE}"/></td>
                                <td>
                                    <div class="fw-semibold"><c:out value="${row.SUBJECT}"/></div>
                                    <div class="text-secondary small msg-preview"><c:out value="${row.MESSAGE_BODY}"/></div>
                                    <a class="small" data-bs-toggle="collapse" href="#msg${row.CONTACT_ID}" role="button">전문 보기</a>
                                    <div class="collapse mt-2" id="msg${row.CONTACT_ID}">
                                        <div class="msg-full small"><c:out value="${row.MESSAGE_BODY}"/></div>
                                    </div>
                                </td>
                                <td class="small">
                                    <div><c:out value="${row.SENDER_NAME}"/></div>
                                    <a href="mailto:<c:out value='${row.SENDER_EMAIL}'/>"><c:out value="${row.SENDER_EMAIL}"/></a>
                                </td>
                                <td class="small text-secondary"><c:out value="${row.CREATED_DATE}"/></td>
                                <td>
                                    <c:if test="${row.STATUS ne 'DONE'}">
                                        <button type="button" class="btn btn-sm btn-outline-primary"
                                                onclick="markDone('<c:out value="${row.CONTACT_ID}"/>')">완료</button>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>

                        <c:if test="${empty contactList}">
                            <tr><td colspan="6" class="text-center text-secondary py-5">접수된 문의가 없습니다.</td></tr>
                        </c:if>
                    </tbody>
                </table>
            </div>

            <c:if test="${totalPages gt 1}">
                <nav class="mt-3">
                    <ul class="pagination justify-content-center mb-0">
                        <c:forEach var="p" begin="1" end="${totalPages}">
                            <li class="page-item ${p eq currentPage ? 'active' : ''}">
                                <a class="page-link" href="/super/contact/list.do?page=${p}">${p}</a>
                            </li>
                        </c:forEach>
                    </ul>
                </nav>
            </c:if>
        </div>
    </div>

    <script>
        function markDone(contactId) {
            if (!confirm('처리 완료로 표시할까요?')) return;

            $.post('/super/contact/done.do', { contactId: contactId }, function (res) {
                if (res && res.status === 'success') {
                    location.reload();
                } else {
                    alert((res && res.msg) ? res.msg : '처리에 실패했습니다.');
                }
            }).fail(function () {
                alert('처리에 실패했습니다.');
            });
        }
    </script>
</body>
</html>
