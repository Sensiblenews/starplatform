<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>보안 감사 로그 - Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <style>
        body { background-color: #f0f2f5; font-family: 'Pretendard', sans-serif; }
        .main-content { margin-left: 260px; padding: 30px; }
        .table-card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
        .ua { max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        @media (max-width: 768px) { .main-content { margin-left: 0; padding: 15px; } }
    </style>
</head>
<body>
    <c:set var="activeMenu" value="audit_log" scope="request" />
    <jsp:include page="/WEB-INF/jsp/super/sidebar.jsp" />

    <div class="main-content">
        <div class="mb-4">
            <h4 class="fw-bold mb-1"><i class="fas fa-clipboard-list text-primary me-2"></i>보안 감사 로그</h4>
            <div class="text-secondary small">관리자 로그인·OTP 등록/검증/재설정·경보 설정 변경 이력입니다. 비밀키·OTP 번호·복구 코드·비밀번호는 기록하지 않습니다.</div>
        </div>

        <c:if test="${storageError}">
            <div class="alert alert-warning">감사 로그 테이블을 읽을 수 없습니다. <code>core-backend/db/admin-mfa-audit.sql</code> 적용 여부를 확인하세요.</div>
        </c:if>

        <div class="table-card">
            <form class="row g-2 align-items-end mb-3" method="get" action="/super/mfa/audit.do">
                <div class="col-sm-3">
                    <label class="form-label small text-secondary mb-1">관리자 ID (주체 또는 대상)</label>
                    <input type="text" name="prsId" class="form-control form-control-sm" value="<c:out value='${prsId}'/>">
                </div>
                <div class="col-sm-3">
                    <label class="form-label small text-secondary mb-1">이벤트</label>
                    <select name="eventType" class="form-select form-select-sm">
                        <option value="">전체</option>
                        <%-- 목록은 컨트롤러가 넘긴다 (Tomcat 7 의 EL 2.2 는 리스트 리터럴을 지원하지 않는다) --%>
                        <c:forEach var="t" items="${eventTypes}">
                            <option value="${t}" ${t eq eventType ? 'selected' : ''}>${t}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-sm-auto">
                    <button class="btn btn-primary btn-sm">조회</button>
                    <a class="btn btn-outline-secondary btn-sm" href="/super/mfa/audit.do">초기화</a>
                </div>
                <div class="col text-end small text-secondary">전체 <c:out value="${totalCount}"/>건</div>
            </form>

            <div class="table-responsive">
                <table class="table table-sm table-hover align-middle">
                    <thead class="table-light">
                        <tr><th>시각</th><th>관리자</th><th>이벤트</th><th>결과</th><th>사유</th><th>대상</th><th>IP</th><th>설명</th><th>UA</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="l" items="${logs}">
                            <tr>
                                <td class="small text-secondary text-nowrap"><c:out value="${l.CREATED_DATE}"/></td>
                                <td class="fw-semibold small"><c:out value="${l.PRS_ID}"/></td>
                                <td class="small"><code><c:out value="${l.EVENT_TYPE}"/></code></td>
                                <td><c:choose><c:when test="${l.RESULT eq 'S'}"><span class="badge bg-success">성공</span></c:when><c:otherwise><span class="badge bg-danger">실패</span></c:otherwise></c:choose></td>
                                <td class="small text-secondary"><c:out value="${l.REASON_CODE}"/></td>
                                <td class="small"><c:out value="${l.TARGET_ID}"/></td>
                                <td class="small text-secondary"><c:out value="${l.CLIENT_IP}"/></td>
                                <td class="small"><c:out value="${l.DETAIL}"/></td>
                                <td class="small text-secondary ua" title="<c:out value='${l.USER_AGENT}'/>"><c:out value="${l.USER_AGENT}"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty logs}"><tr><td colspan="9" class="text-center text-secondary py-5">기록이 없습니다.</td></tr></c:if>
                    </tbody>
                </table>
            </div>

            <c:if test="${totalPages gt 1}">
                <nav class="mt-3"><ul class="pagination pagination-sm justify-content-center mb-0">
                    <c:set var="from" value="${currentPage - 5 lt 1 ? 1 : currentPage - 5}"/>
                    <c:set var="to" value="${from + 9 gt totalPages ? totalPages : from + 9}"/>
                    <c:forEach var="p" begin="${from}" end="${to}">
                        <li class="page-item ${p eq currentPage ? 'active' : ''}">
                            <a class="page-link" href="/super/mfa/audit.do?page=${p}&prsId=<c:out value='${prsId}'/>&eventType=<c:out value='${eventType}'/>">${p}</a>
                        </li>
                    </c:forEach>
                </ul></nav>
            </c:if>
        </div>
    </div>
</body>
</html>
