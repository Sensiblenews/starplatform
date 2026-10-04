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
    - data-sp-overlay 영상(카드)은 지워 아래 썸네일 img 만 남긴다. 콘솔에 경고를 남긴다.
    - data-sp-keep 영상(상세 갤러리)은 페이지 쪽 error 처리에 맡긴다.
    - 그 밖의 영상은 포스터 img 로 바꾼다.
  - IntersectionObserver 미지원·데이터 절약·모션 줄이기 설정이면 자동재생하지 않는다.
  - 동적으로 붙은 카드는 window.spVideoScan(root) 로 등록한다 (홈·스타 페이지 Load more).

  [2-31차 후속] 클릭 전체화면 뷰어 — 자동재생 로직은 그대로 두고 뷰어만 덧붙인다.
  - 카드(data-sp-overlay)의 미디어 상자를 누르면 글 페이지로 가지 않고 뷰어가 뜬다. 글 제목·본문은 그대로 링크.
  - 상세 페이지의 영상은 controls 와 겹치므로 영상 위 펼침 버튼([data-sp-expand])으로 연다.
  - 뷰어는 fixed 레이어(100vw x 100dvh, 검정 배경)이고 같은 원본 주소를 쓴다. 사용자 제스처가 있으니 소리를 켜고
    바로 재생하며, 거절되면 음소거로 재시도한다.
  - history.pushState 로 뒤로가기 = 닫기. X·Esc·배경 탭도 닫는다. 닫으면 카드 자동재생 판정(pick)을 다시 돈다.
  - 카드 영상이 재생 실패로 치워졌으면(같은 파일이라 뷰어도 못 연다) 상자 클릭은 원래대로 글 페이지로 간다.

  include 조각은 Jasper 가 기본 인코딩으로 읽으므로 출력되는 스타일·스크립트에는 ASCII 만 쓴다.
--%>
<style>
  .sp-vid-overlay { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; display: block; opacity: 0; transition: opacity .2s; background: transparent; pointer-events: none; }
  .sp-vid-overlay.is-playing { opacity: 1; }
  .sp-vid-badge { position: absolute; left: 8px; bottom: 8px; width: 26px; height: 26px; border-radius: 50%; background: rgba(15, 23, 42, 0.65); color: #ffffff; display: inline-flex; align-items: center; justify-content: center; pointer-events: none; }
  .sp-vid-badge svg { width: 12px; height: 12px; margin-left: 2px; }
  .sp-vid-badge-sm { left: 4px; bottom: 4px; width: 20px; height: 20px; }
  .sp-vid-badge-sm svg { width: 9px; height: 9px; }
  .sp-vid-open { cursor: pointer; }
  .sp-vid-expand { position: absolute; top: 10px; right: 10px; width: 36px; height: 36px; border: 0; border-radius: 50%; background: rgba(15, 23, 42, 0.65); color: #ffffff; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; z-index: 2; padding: 0; }
  .sp-vid-expand svg { width: 18px; height: 18px; }
  video[hidden] ~ .sp-vid-expand { display: none; }
  .sp-viewer { position: fixed; inset: 0; width: 100vw; height: 100vh; height: 100dvh; z-index: 99999; background: #000000; display: flex; align-items: center; justify-content: center; }
  .sp-viewer[hidden] { display: none; }
  .sp-viewer video { width: 100%; height: 100%; object-fit: contain; background: #000000; outline: 0; }
  .sp-viewer-close { position: absolute; top: 12px; top: calc(12px + env(safe-area-inset-top)); right: 12px; width: 44px; height: 44px; border: 0; border-radius: 50%; background: rgba(255, 255, 255, 0.18); color: #ffffff; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; z-index: 1; padding: 0; }
  .sp-viewer-close svg { width: 20px; height: 20px; }
  .sp-viewer-msg { position: absolute; left: 16px; right: 16px; bottom: 28px; text-align: center; color: #ffffff; font-size: 14px; }
  .sp-viewer-msg[hidden] { display: none; }
  html.sp-viewer-open, html.sp-viewer-open body { overflow: hidden; }
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
      if (!document.hidden && !viewerOpen) {
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
      var err = v.error;
      if (window.console && console.warn) {
        console.warn('[sp-video] cannot play', v.currentSrc || v.getAttribute('data-src'), err ? 'code=' + err.code : '');
      }
      <%-- 갤러리 영상은 칸마다 src 를 바꿔 다시 쓰므로 등록을 유지한다 --%>
      if (v.hasAttribute('data-sp-keep')) return;
      unregister(v);
      var box = v.parentNode;
      if (box && box.__spOpen) {
        <%-- 같은 파일이라 뷰어도 못 연다. 상자 클릭은 원래 링크로 돌려보낸다 --%>
        box.__spDisabled = true;
        box.classList.remove('sp-vid-open');
      }
      if (box) {
        var ex = box.querySelector('[data-sp-expand]');
        if (ex) ex.hidden = true;
      }
      if (!v.hasAttribute('data-sp-overlay') && v.poster) {
        var img = document.createElement('img');
        img.src = v.poster;
        img.alt = v.getAttribute('aria-label') || '';
        v.parentNode.insertBefore(img, v);
      }
      if (v.parentNode) v.parentNode.removeChild(v);
    }
    function cardLabel(v) {
      var a = v.closest ? v.closest('a') : null;
      var t = a && a.querySelector('.post-snippet, .m-post-text, .st-post-text');
      return t ? t.textContent.replace(/\s+/g, ' ').trim() : '';
    }
    function bindOverlayClick(v) {
      var box = v.parentNode;
      if (!box || box.__spOpen) return;
      box.__spOpen = true;
      box.classList.add('sp-vid-open');
      box.addEventListener('click', function (e) {
        if (box.__spDisabled) return;
        e.preventDefault();
        e.stopPropagation();
        openViewer(v.getAttribute('data-src'), v.getAttribute('poster'), cardLabel(v));
      });
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
      if (v.hasAttribute('data-sp-overlay')) bindOverlayClick(v);
      if (io) io.observe(v);
    }
    function bindExpand(btn) {
      if (btn.__spReg) return;
      btn.__spReg = true;
      btn.addEventListener('click', function (e) {
        e.preventDefault();
        e.stopPropagation();
        var vid = btn.parentNode ? btn.parentNode.querySelector('video') : null;
        if (!vid) return;
        var src = vid.currentSrc || vid.getAttribute('src') || vid.getAttribute('data-src');
        openViewer(src, vid.getAttribute('poster'), vid.getAttribute('aria-label') || '');
      });
    }

    <%-- 전체화면 뷰어 --%>
    var viewerOpen = false;
    var viewer = null;
    var viewerVideo = null;
    var viewerMsg = null;
    var pushed = false;
    var CLOSE_SVG = '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" d="M6 6l12 12M18 6L6 18"/></svg>';
    function ensureViewer() {
      if (viewer) return viewer;
      viewer = document.createElement('div');
      viewer.className = 'sp-viewer';
      viewer.hidden = true;
      viewer.setAttribute('role', 'dialog');
      viewer.setAttribute('aria-modal', 'true');
      viewer.setAttribute('aria-label', 'Video');
      viewer.innerHTML = '<video controls playsinline preload="auto"></video>'
        + '<button type="button" class="sp-viewer-close" aria-label="Close">' + CLOSE_SVG + '</button>'
        + '<p class="sp-viewer-msg" hidden>This video cannot be played in this browser.</p>';
      viewerVideo = viewer.querySelector('video');
      viewerMsg = viewer.querySelector('.sp-viewer-msg');
      viewer.querySelector('.sp-viewer-close').addEventListener('click', requestClose);
      viewer.addEventListener('click', function (e) { if (e.target === viewer) requestClose(); });
      viewerVideo.addEventListener('error', function () {
        if (!viewerOpen) return;
        if (window.console && console.warn) console.warn('[sp-video] viewer cannot play', viewerVideo.currentSrc);
        viewerMsg.hidden = false;
      });
      document.body.appendChild(viewer);
      return viewer;
    }
    function openViewer(src, poster, label) {
      if (!src) return;
      ensureViewer();
      viewerOpen = true;
      vids.forEach(autoPause);
      viewerMsg.hidden = true;
      viewerVideo.poster = poster || '';
      viewerVideo.setAttribute('aria-label', label || 'Video');
      viewerVideo.src = src;
      viewer.hidden = false;
      document.documentElement.classList.add('sp-viewer-open');
      <%-- 사용자가 눌렀으니 소리를 켜고 시작. 그래도 거절되면 음소거로 재시도 --%>
      viewerVideo.muted = false;
      var p = viewerVideo.play();
      if (p && p.catch) {
        p.catch(function () {
          viewerVideo.muted = true;
          var q = viewerVideo.play();
          if (q && q.catch) q.catch(function () { });
        });
      }
      try {
        history.pushState({ spViewer: true }, '');
        pushed = true;
      } catch (e) {
        pushed = false;
      }
    }
    function closeViewer() {
      if (!viewerOpen) return;
      viewerOpen = false;
      viewerVideo.pause();
      viewerVideo.removeAttribute('src');
      viewerVideo.load();
      viewer.hidden = true;
      document.documentElement.classList.remove('sp-viewer-open');
      pick();
    }
    function requestClose() {
      if (pushed) { pushed = false; history.back(); return; }
      closeViewer();
    }
    window.addEventListener('popstate', function () {
      if (viewerOpen) { pushed = false; closeViewer(); }
    });
    document.addEventListener('keydown', function (e) {
      if (viewerOpen && (e.key === 'Escape' || e.key === 'Esc')) requestClose();
    });
    <%-- 다른 페이지로 갈 때(bfcache 복귀 포함) 뷰어가 열린 채 남지 않게 --%>
    window.addEventListener('pageshow', function () { if (viewerOpen) closeViewer(); });

    window.spVideoScan = function (root) {
      (root || document).querySelectorAll('video[data-sp-video]').forEach(register);
      (root || document).querySelectorAll('[data-sp-expand]').forEach(bindExpand);
    };
    window.spVideoOpen = openViewer;
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
