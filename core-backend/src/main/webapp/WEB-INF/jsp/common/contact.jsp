<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
  문의하기 (/contact).

  검증은 전부 서버(ContactService)가 한다. 아래 required·type 속성은 사용자 편의일 뿐
  방어선이 아니다 — 브라우저를 거치지 않는 제출을 막지 못한다.
--%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <title>Contact StarPlatform - Support, Reports, and Partnerships</title>
  <meta name="description" content="Send a message to the StarPlatform team: general support, account and star page questions, content reports, copyright claims, advertising, and partnership enquiries." />
  <meta name="robots" content="index, follow" />
  <meta name="google-adsense-account" content="ca-pub-9109251900558498" />

  <meta property="og:title" content="Contact StarPlatform" />
  <meta property="og:description" content="Support, reports, copyright claims, advertising, and partnership enquiries." />
  <meta property="og:image" content="https://witch-hunting.com/resources/img/icon.png" />
  <meta property="og:type" content="website" />
  <meta property="og:url" content="${canonicalUrl}" />
  <meta name="twitter:card" content="summary" />

  <link rel="canonical" href="${canonicalUrl}" />

  <script type="application/ld+json">
  {"@context":"https://schema.org","@type":"ContactPage","name":"Contact StarPlatform","url":"https://witch-hunting.com/contact","isPartOf":{"@type":"WebSite","name":"StarPlatform","url":"https://witch-hunting.com/"}}
  </script>

  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap" rel="stylesheet">
  <%@ include file="/WEB-INF/jsp/common/include/web-base-style.jsp"%>
  <%@ include file="/WEB-INF/jsp/common/include/web-chrome-style.jsp"%>
  <style>
    .field { margin-bottom: 18px; }
    .field label { display: block; font-size: 14px; font-weight: 600; color: #e2e8f0; margin-bottom: 6px; }
    .field .hint { font-size: 13px; color: #64748b; margin-top: 6px; }
    .field input[type="text"], .field input[type="email"], .field select, .field textarea {
      width: 100%; padding: 11px 12px; border-radius: 8px; border: 1px solid #334155;
      background: #0b1220; color: #e2e8f0; font-size: 15px; font-family: inherit;
    }
    .field textarea { min-height: 180px; resize: vertical; line-height: 1.6; }
    .field-error input, .field-error select, .field-error textarea { border-color: #f87171; }
    .checkline { display: flex; align-items: flex-start; gap: 10px; }
    .checkline input { margin-top: 4px; flex-shrink: 0; }
    .checkline label { font-weight: 400; color: #94a3b8; font-size: 14px; }
    .notice { border-radius: 10px; padding: 14px 16px; margin-bottom: 22px; font-size: 14px; }
    .notice-error { background: #3f1d1d; border: 1px solid #7f1d1d; color: #fecaca; }
    .notice-ok { background: #0f2e1d; border: 1px solid #14532d; color: #bbf7d0; }
    /* 허니팟: 봇만 채우도록 화면에서 완전히 뺀다. display:none 대신 화면 밖으로 밀어
       두는 편이, 숨김 필드를 걸러내는 봇에게도 평범한 입력칸으로 보인다 */
    .hp { position: absolute; left: -9999px; width: 1px; height: 1px; overflow: hidden; }
  </style>
</head>
<body>

<%@ include file="/WEB-INF/jsp/common/include/web-nav.jsp"%>

<main class="page-wrap prose">
  <h1 class="page-title">Contact us</h1>
  <p class="page-lead">Tell us what you need and we will reply by email. Choose the enquiry type that fits so your message reaches the right team.</p>

  <c:choose>
    <c:when test="${submitted}">
      <div class="notice notice-ok" role="status">
        <strong>Your message has been received.</strong><br>
        We read enquiries in the order they arrive and reply to the email address you gave us. Reports of illegal content and rights infringement are handled first.
      </div>
      <div class="panel">
        <h3>While you wait</h3>
        <p>Many questions are already answered in our <a href="${pageContext.request.contextPath}/faq">FAQ</a>, and <a href="${pageContext.request.contextPath}/about">About StarPlatform</a> explains how the service works.</p>
      </div>
    </c:when>

    <c:otherwise>
      <c:if test="${not empty errorMsg}">
        <div class="notice notice-error" role="alert"><c:out value="${errorMsg}"/></div>
      </c:if>

      <form method="post" action="${pageContext.request.contextPath}/contact" novalidate>
        <%-- 폼을 그린 시각. 너무 빨리 제출되면 사람이 쓴 것으로 보지 않는다 --%>
        <input type="hidden" name="formTime" value="${formTime}">

        <%-- 허니팟. 사람은 보지 못하는 칸이라 채워져 있으면 봇이다 --%>
        <div class="hp" aria-hidden="true">
          <label for="website">Website</label>
          <input type="text" id="website" name="website" tabindex="-1" autocomplete="off">
        </div>

        <div class="field ${errorField eq 'contactType' ? 'field-error' : ''}">
          <label for="contactType">Enquiry type</label>
          <select id="contactType" name="contactType" required>
            <c:forEach var="t" items="${contactTypes}">
              <option value="${t.code}" ${selectedType eq t.code ? 'selected' : ''}><c:out value="${t.label}"/></option>
            </c:forEach>
          </select>
        </div>

        <div class="field ${errorField eq 'senderName' ? 'field-error' : ''}">
          <label for="senderName">Your name</label>
          <input type="text" id="senderName" name="senderName" maxlength="100" required value="<c:out value='${inputName}'/>">
        </div>

        <div class="field ${errorField eq 'senderEmail' ? 'field-error' : ''}">
          <label for="senderEmail">Email address</label>
          <input type="email" id="senderEmail" name="senderEmail" maxlength="255" required value="<c:out value='${inputEmail}'/>">
          <p class="hint">We reply to this address. Never send us passwords — we do not need them.</p>
        </div>

        <div class="field ${errorField eq 'subject' ? 'field-error' : ''}">
          <label for="subject">Subject</label>
          <input type="text" id="subject" name="subject" maxlength="200" required value="<c:out value='${inputSubject}'/>">
        </div>

        <div class="field ${errorField eq 'message' ? 'field-error' : ''}">
          <label for="message">Message</label>
          <textarea id="message" name="message" maxlength="5000" required><c:out value="${inputMessage}"/></textarea>
          <p class="hint">If you are reporting content, include the page or post address so we can find it.</p>
        </div>

        <div class="field ${errorField eq 'privacyAgree' ? 'field-error' : ''}">
          <div class="checkline">
            <input type="checkbox" id="privacyAgree" name="privacyAgree" value="Y" ${inputAgree eq 'Y' ? 'checked' : ''} required>
            <label for="privacyAgree">I agree that my name, email address, and message will be stored and used to handle this enquiry, as described in the <a href="${pageContext.request.contextPath}/privacy">privacy policy</a>.</label>
          </div>
        </div>

        <button type="submit" class="btn btn-primary">Send message</button>
      </form>
    </c:otherwise>
  </c:choose>

  <h2>Other ways to reach us</h2>
  <p>Reports about content inside the app can also be sent from the app itself: posts and direct messages each carry a report action, and members can be blocked from your conversation list.</p>
  <p>For anything about your own personal data, choose the privacy enquiry type above. Our <a href="${pageContext.request.contextPath}/privacy">privacy policy</a> describes what we collect and how long we keep it.</p>

  <h2>What to include</h2>
  <ul>
    <li><strong>Content reports</strong> — the address of the page or post, and what the problem is.</li>
    <li><strong>Copyright claims</strong> — the address of the material, a description of the work you hold rights to, and how to reach you.</li>
    <li><strong>Account questions</strong> — the star page name or account address involved.</li>
    <li><strong>Partnership proposals</strong> — who you represent and what you have in mind.</li>
  </ul>
</main>

<%@ include file="/WEB-INF/jsp/common/include/web-footer.jsp"%>
</body>
</html>
