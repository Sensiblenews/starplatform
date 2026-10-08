<%--
  포스트 카드 스타일. 홈 목록(home.jsp)과 마케팅 허브의 최근 포스트(posts.jsp)가 같은
  카드를 쓴다. 한쪽만 고치면 두 화면이 어긋나므로 여기 한 곳에만 둔다.
  마크업도 include/web-post-cards.jsp 로 공유한다.

  [2-29차 후속] 클라이언트 PC 시안: 흰 카드 + 옅은 테두리, 64px 썸네일, 작성자 옆 파란 STAR 뱃지,
  아래 줄에 날짜 · 하트 수. 카드 사이 간격 14px, 세 열.
--%>
<style>
  .post-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(290px, 100%), 1fr)); gap: 14px; margin-top: 24px; text-align: left; }
  .post-card { display: flex; gap: 14px; align-items: center; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; text-decoration: none; color: inherit; min-height: 100px; }
  .post-card:hover { border-color: #93c5fd; box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06); }
  .post-thumb { width: 72px; height: 72px; border-radius: 8px; object-fit: cover; flex-shrink: 0; background: #f1f5f9; display: block; }
  .post-thumb-video { position: relative; overflow: hidden; }
  .post-thumb-video img { width: 100%; height: 100%; object-fit: cover; display: block; }
  .post-info { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
  .post-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
  .post-author { font-size: 15px; font-weight: 700; color: #0f172a; }
  /* 직군 뱃지: 작성자가 어떤 유형의 스타인지 카드에서 바로 드러낸다 */
  .post-badge { display: inline-flex; align-items: center; gap: 3px; font-size: 10px; font-weight: 700; letter-spacing: 0.4px; text-transform: uppercase; color: #1d4ed8; background: #dbeafe; border-radius: 999px; padding: 2px 8px; }
  .post-badge svg { width: 10px; height: 10px; }
  .post-snippet { font-size: 13px; color: #334155; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; word-break: break-word; }
  /* 메타 줄: 날짜 · 반응 수. 아이콘은 글자색과 같은 회색 */
  .post-meta { font-size: 12px; color: #64748b; display: flex; flex-wrap: wrap; align-items: center; gap: 4px 14px; margin-top: 2px; }
  .post-meta span { white-space: nowrap; }
  .post-stat { display: inline-flex; align-items: center; gap: 4px; }
  .post-stat svg { width: 14px; height: 14px; }
</style>
