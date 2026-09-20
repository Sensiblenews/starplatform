<%--
  포스트 카드 목록 마크업. 반복 대상은 ${postCards} 로 고정한다.
  호출하는 쪽에서 이름이 다르면 <c:set var="postCards" value="..."/> 로 맞춰 넣을 것.
  스타일은 include/web-card-style.jsp 에 있다.
--%>
<div class="post-grid">
  <c:forEach var="p" items="${postCards}">
    <a class="post-card" href="${pageContext.request.contextPath}/post/${p.conId}">
      <c:if test="${not empty p.image}">
        <img class="post-thumb" src="${p.image}" alt="${p.alt}" loading="lazy" onerror="this.style.display='none'">
      </c:if>
      <span class="post-info">
        <span class="post-head">
          <span class="post-author">${p.author}</span>
          <c:if test="${not empty p.category}"><span class="post-badge">${p.category}</span></c:if>
        </span>
        <c:if test="${not empty p.snippet}"><span class="post-snippet">${p.snippet}</span></c:if>
        <span class="post-meta">
          <%-- 항목 사이 가운뎃점은 CSS로 넣는다. 마크업에 박으면 앞 항목이 비었을 때 점이 홀로 남는다 --%>
          <c:if test="${not empty p.date}"><span>${p.date}</span></c:if>
          <c:if test="${p.mediaCnt gt 1}"><span><c:out value="${p.mediaCnt}"/> photos</span></c:if>
          <%-- 반응이 0인 글이 대부분이라 0은 감춘다 — 표시해봐야 정보가 아니라 잡음이다 --%>
          <c:if test="${p.likeCnt gt 0}"><span><c:out value="${p.likeCnt}"/> likes</span></c:if>
          <c:if test="${p.commentCnt gt 0}"><span><c:out value="${p.commentCnt}"/> comments</span></c:if>
          <c:if test="${p.followerCnt gt 0}"><span><c:out value="${p.followerCnt}"/> followers</span></c:if>
        </span>
      </span>
    </a>
  </c:forEach>
</div>
