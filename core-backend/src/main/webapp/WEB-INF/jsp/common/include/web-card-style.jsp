<%--
  포스트 카드 스타일. 홈 목록(home.jsp)과 마케팅 허브의 최근 포스트(posts.jsp)가 같은
  카드를 쓴다. 한쪽만 고치면 두 화면이 어긋나므로 여기 한 곳에만 둔다.
  마크업도 include/web-post-cards.jsp 로 공유한다.

  [2-29차 후속] 화이트 모드: 흰 카드 + 옅은 테두리.
--%>
<style>
  .post-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(290px, 1fr)); gap: 16px; margin-top: 30px; text-align: left; }
  .post-card { display: flex; gap: 12px; align-items: flex-start; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 14px; text-decoration: none; color: inherit; }
  .post-card:hover { background: #f8fafc; border-color: #cbd5e1; }
  .post-thumb { width: 64px; height: 64px; border-radius: 8px; object-fit: cover; flex-shrink: 0; background: #f1f5f9; }
  .post-info { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
  .post-head { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
  .post-author { font-size: 14px; font-weight: 600; color: #0f172a; }
  /* 직군 뱃지: 작성자가 어떤 유형의 스타인지 카드에서 바로 드러낸다 */
  .post-badge { font-size: 10px; font-weight: 700; letter-spacing: 0.4px; text-transform: uppercase; color: #1d4ed8; background: #dbeafe; border-radius: 999px; padding: 2px 8px; }
  .post-snippet { font-size: 13px; color: #475569; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; word-break: break-word; }
  /* 메타 줄: 본문이 짧은 글이 대부분이라 날짜·사진 수·반응 수로 읽을 거리를 채운다 */
  .post-meta { font-size: 11px; color: #64748b; display: flex; flex-wrap: wrap; gap: 4px 8px; }
  .post-meta span { white-space: nowrap; }
  .post-meta span + span::before { content: "\00b7"; margin-right: 6px; color: #94a3b8; }
</style>
