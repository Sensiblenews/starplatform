<%--
  [2-31차] 웹 영상 자동재생 — 화면에 보일 때만 음소거 재생, 벗어나면 정지.

  대상: <video data-sp-video data-src="원본 mp4" poster="썸네일" muted playsinline loop preload="none">
  - src 는 처음 화면에 들어올 때 넣는다. 보지 않은 영상은 내려받지 않는다.
  - 화면의 50% 이상 보이는 영상 중 가장 많이 보이는 1개만 재생한다 (한 화면 1개 규칙).
  - controls 가 있는 영상(글 상세)을 사용자가 직접 멈추면 기억해 두고 다시 자동재생하지 않는다.
    사용자가 다시 재생하면 해제. 카드 영상은 링크라 사용자가 멈출 수 없어 해당 없음.
  - 탭을 떠나면(visibilitychange) 모두 정지, 돌아오면 다시 판정.
  - play() 가 거절되면(저전력 모드 등) 포스터 상태로 둔다. 상세는 네이티브 controls 로 직접 재생 가능.
  - 재생 실패(브라우저가 못 여는 형식 등):
    - data-sp-overlay 영상(카드)은 지워 아래 썸네일 img 만 남긴다.
    - data-sp-keep 영상(상세 갤러리)은 페이지 쪽 error 처리에 맡긴다.
    - 그 밖의 영상은 포스터 img 로 바꾼다.
  - IntersectionObserver 미지원·데이터 절약·모션 줄이기 설정이면 자동재생하지 않는다.
  - 동적으로 붙은 카드는 window.spVideoScan(root) 로 등록한다 (홈 Load more).

  include 조각은 Jasper 가 기본 인코딩으로 읽으므로 출력되는 스타일·스크립트에는 ASCII 만 쓴다.
--%>
<style>
  .sp-vid-overlay { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; display: block; opacity: 0; transition: opacity .2s; background: transparent; pointer-events: none; }
  .sp-vid-overlay.is-playing { opacity: 1; }
  .sp-vid-badge { position: absolute; left: 8px; bottom: 8px; width: 26px; height: 26px; border-radius: 50%; background: rgba(15, 23, 42, 0.65); color: #ffffff; display: inline-flex; align-items: center; justify-content: center; pointer-events: none; }
  .sp-vid-badge svg { width: 12px; height: 12px; margin-left: 2px; }
  .sp-vid-badge-sm { left: 4px; bottom: 4px; width: 20px; height: 20px; }
  .sp-vid-badge-sm svg { width: 9px; height: 9px; }
</style>
<script>
  (function () {
    if (window.spVideoScan) return;
    var vids = [];
    var conn = navigator.connection;
    var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var canAuto = 'IntersectionObserver' in window && !(conn && conn.saveData) && !reduce;
    var io = canAuto ? new IntersectionObserver(function (entries) {
      entries.forEach(function (e) { e.target.__spRatio = e.isIntersecting ? e.intersectionRatio : 0; });
      pick();
    }, { threshold: [0, 0.25, 0.5, 0.75, 1] }) : null;

    function load(v) {
      var src = v.getAttribute('data-src');
      if (src && !v.getAttribute('src')) v.src = src;
    }
    function autoPause(v) {
      if (!v.paused) { v.__spAutoPause = true; v.pause(); }
    }
    function autoPlay(v) {
      if (!v.paused) return;
      load(v);
      v.muted = true;
      v.__spAutoPlay = true;
      var p = v.play();
      if (p && p.catch) p.catch(function () { v.__spAutoPlay = false; });
    }
    <%-- 50% 이상 보이는 영상 중 가장 많이 보이는 1개만 재생. 사용자가 직접 재생한 영상이 보이면 그것을 우선한다 --%>
    function pick() {
      if (!io) return;
      var best = null;
      if (!document.hidden) {
        vids.forEach(function (v) {
          var r = v.__spRatio || 0;
          if (r < 0.5 || v.__spUserPaused) return;
          if (!best || (v.__spUserPlayed && !best.__spUserPlayed) || (r > best.__spRatio && !best.__spUserPlayed)) best = v;
        });
      }
      vids.forEach(function (v) { if (v !== best) autoPause(v); });
      if (best) autoPlay(best);
    }
    function unregister(v) {
      var i = vids.indexOf(v);
      if (i >= 0) vids.splice(i, 1);
      if (io) io.unobserve(v);
    }
    function onError(v) {
      <%-- 갤러리 영상은 칸마다 src 를 바꿔 다시 쓰므로 등록을 유지한다 --%>
      if (v.hasAttribute('data-sp-keep')) return;
      unregister(v);
      if (!v.hasAttribute('data-sp-overlay') && v.poster) {
        var img = document.createElement('img');
        img.src = v.poster;
        img.alt = v.getAttribute('aria-label') || '';
        v.parentNode.insertBefore(img, v);
      }
      if (v.parentNode) v.parentNode.removeChild(v);
    }
    function register(v) {
      if (v.__spReg) return;
      v.__spReg = true;
      vids.push(v);
      v.addEventListener('playing', function () { v.classList.add('is-playing'); });
      v.addEventListener('play', function () {
        if (v.__spAutoPlay) { v.__spAutoPlay = false; return; }
        <%-- 사용자가 직접 재생: 정지 기억 해제, 다른 영상은 멈춘다 --%>
        v.__spUserPaused = false;
        v.__spUserPlayed = true;
        load(v);
        vids.forEach(function (o) { if (o !== v) { o.__spUserPlayed = false; autoPause(o); } });
      });
      v.addEventListener('pause', function () {
        if (v.__spAutoPause) { v.__spAutoPause = false; return; }
        <%-- 컨트롤로 직접 멈춘 경우만 기억한다. 브라우저가 화면 밖·탭 이탈로 멈춘 것은 사용자 정지가 아니다 --%>
        if (v.controls && !document.hidden && (v.__spRatio || 0) > 0) {
          v.__spUserPaused = true;
          v.__spUserPlayed = false;
        }
      });
      v.addEventListener('error', function () { onError(v); });
      if (io) io.observe(v);
    }

    window.spVideoScan = function (root) {
      (root || document).querySelectorAll('video[data-sp-video]').forEach(register);
    };
    <%-- 갤러리에서 영상 칸을 다시 보일 때 정지 기억을 새 영상 기준으로 초기화한다 --%>
    window.spVideoReset = function (v) {
      v.__spUserPaused = false;
      v.__spUserPlayed = false;
      v.__spRatio = 0;
      if (io) { io.unobserve(v); io.observe(v); }
    };
    document.addEventListener('visibilitychange', pick);
    window.spVideoScan(document);
  })();
</script>
