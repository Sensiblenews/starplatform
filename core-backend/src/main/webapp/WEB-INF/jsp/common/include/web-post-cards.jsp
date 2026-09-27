<%--
  포스트 카드 목록 마크업. 반복 대상은 ${postCards} 로 고정한다.
  호출하는 쪽에서 이름이 다르면 <c:set var="postCards" value="..."/> 로 맞춰 넣을 것.
  스타일은 include/web-card-style.jsp 에 있다.

  [2-29차 후속] 클라이언트 PC 시안 카드: 왼쪽 썸네일, 작성자 + STAR 뱃지, 글 제목, 날짜 · 반응 수.
  시안의 눈(조회수) 지표는 글 단위 조회수가 DB 에 없어(조회는 스타 페이지 단위 WH_AD_LOG) 넣지 않고,
  실제 있는 좋아요·댓글 수를 아이콘과 함께 보인다. 아이콘은 인라인 SVG(ASCII).
--%>
<div class="post-grid">
  <c:forEach var="p" items="${postCards}">
    <a class="post-card" href="${pageContext.request.contextPath}/post/${p.conId}">
      <c:choose>
        <c:when test="${not empty p.image}"><img class="post-thumb" src="${p.image}" alt="${p.alt}" loading="lazy" onerror="this.style.visibility='hidden'"></c:when>
        <c:otherwise><span class="post-thumb post-thumb-empty" aria-hidden="true"></span></c:otherwise>
      </c:choose>
      <span class="post-info">
        <span class="post-head">
          <span class="post-author">${p.author}</span>
          <c:if test="${not empty p.category}"><span class="post-badge"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 2.5l2.9 6.2 6.8.8-5 4.6 1.3 6.7L12 17.4l-6 3.4 1.3-6.7-5-4.6 6.8-.8z"/></svg>${p.category}</span></c:if>
        </span>
        <c:if test="${not empty p.snippet}"><span class="post-snippet">${p.snippet}</span></c:if>
        <span class="post-meta">
          <c:if test="${not empty p.date}"><span class="post-date">${p.date}</span></c:if>
          <span class="post-stat"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" d="M12 20.5s-7.5-4.6-9.2-9.3C1.6 8 3.6 4.5 7 4.5c2 0 3.6 1.1 5 3 1.4-1.9 3-3 5-3 3.4 0 5.4 3.5 4.2 6.7-1.7 4.7-9.2 9.3-9.2 9.3z"/></svg><c:out value="${empty p.likeCnt ? 0 : p.likeCnt}"/></span>
          <c:if test="${p.commentCnt gt 0}"><span class="post-stat"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" d="M4 5h16v11H9l-5 4z"/></svg><c:out value="${p.commentCnt}"/></span></c:if>
          <c:if test="${p.mediaCnt gt 1}"><span class="post-stat"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2" fill="none" stroke="currentColor" stroke-width="2"/><path fill="none" stroke="currentColor" stroke-width="2" d="M3 16l5-5 4 4 3-3 6 5"/></svg><c:out value="${p.mediaCnt}"/></span></c:if>
        </span>
      </span>
    </a>
  </c:forEach>
</div>
