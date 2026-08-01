<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${policyTitle} | Star Platform</title>
    <style>
        body { font-family: 'Pretendard', -apple-system, sans-serif; line-height: 1.7; color: #333; padding: 40px 20px; max-width: 900px; margin: 0 auto; background-color: #f4f7f9; }
        .container { background: white; padding: 50px; border-radius: 16px; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
        h1 { font-size: 2rem; border-bottom: 3px solid #3880ff; padding-bottom: 15px; color: #1a1a1a; margin-bottom: 30px; }
        h2 { margin-top: 40px; color: #2c3e50; font-size: 1.3rem; border-left: 5px solid #3880ff; padding-left: 15px; }
        p, li { margin-bottom: 12px; font-size: 1rem; color: #4f5b66; }
        ul { padding-left: 20px; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        table th, table td { border: 1px solid #ddd; padding: 12px; text-align: left; font-size: 0.95rem; }
        table th { background-color: #f8f9fa; }
        .footer-info { margin-top: 60px; padding-top: 20px; border-top: 1px solid #eee; font-size: 0.9rem; color: #888; }
    </style>
</head>
<body>
    <div class="container">
        <h1>${policyTitle}</h1>

        <%-- 본문은 SM 관리자만 편집 가능한 WH_CONTENT(CON_TYPE=7) 원문 HTML을 신뢰하고 그대로 출력한다.
             앱(/app/policyDetail)도 동일 원문을 렌더링한다. --%>
        <c:out value="${policyBody}" escapeXml="false"/>

        <c:if test="${not empty policyUpdated}">
            <div class="footer-info">Last updated: ${policyUpdated}</div>
        </c:if>
    </div>
</body>
</html>
