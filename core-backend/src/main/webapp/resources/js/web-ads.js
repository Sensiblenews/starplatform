/*
 * 공개 웹(witch-hunting.com) AdSense 광고 단위 (2-34차 3-1).
 *
 * 각 페이지의 광고 자리(.ad-slot[data-ad-slot], 크기만 잡은 점선 상자)에 <ins class="adsbygoogle"> 를 넣는다.
 * - ENABLED 가 false 면 아무것도 하지 않는다 (AdSense 승인 전 — ca93e86 에서 광고 코드를 뺀 이유와 같다).
 *   승인 메일을 받으면 ENABLED 를 true 로 바꿔 배포한다. 페이지 JSP 는 건드리지 않는다
 * - 광고 단위 ID 는 위치별로 UNITS 에서 고른다. 지금 계정에 단위가 하나(3314400335)뿐이라 전부 같은 값이다.
 *   AdSense 콘솔에서 위치별 단위를 만들면 여기만 바꾸면 된다 (위치별 성과를 따로 볼 수 있다)
 * - 광고 게이트 (ca93e86 이전 규칙 복원): 고유 콘텐츠([data-ad-content]) 높이 합이 600px 미만이면 광고를 넣지 않는다.
 *   표시가 없는 페이지(홈·목록)는 게이트를 통과한다. 4초 안에 채워지지 않으면 자리를 접는다(빈 상자 방지)
 * - 화면에 숨겨진 자리(PC 전용·모바일 전용)는 건너뛴다 — 폭 0 에 광고를 요청하면 AdSense 오류가 난다
 * - 카카오 애드핏은 코드 수령 후 같은 자리 구조에 네트워크 분기로 추가한다 (아직 없음)
 */
(function () {
  'use strict';

  var ENABLED = false;
  var CLIENT = 'ca-pub-9109251900558498';
  var DEFAULT_UNIT = '3314400335';
  var UNITS = {
    'hero-leaderboard': DEFAULT_UNIT,
    'mobile-top': DEFAULT_UNIT,
    'mobile-infeed': DEFAULT_UNIT,
    'list-leaderboard': DEFAULT_UNIT,
    'mobile-bottom': DEFAULT_UNIT,
    'sidebar-halfpage': DEFAULT_UNIT,
    'star-a': DEFAULT_UNIT,
    'star-infeed': DEFAULT_UNIT,
    'star-b': DEFAULT_UNIT,
    'star-mobile-c': DEFAULT_UNIT,
    'post-d': DEFAULT_UNIT,
    'post-e': DEFAULT_UNIT,
    'post-mobile-b': DEFAULT_UNIT,
    'post-mobile-c': DEFAULT_UNIT
  };
  var MIN_CONTENT_PX = 600;
  var NO_FILL_COLLAPSE_MS = 4000;

  function contentHeight() {
    var marked = document.querySelectorAll('[data-ad-content]');
    if (!marked.length) return Infinity;
    var sum = 0;
    for (var i = 0; i < marked.length; i++) sum += marked[i].offsetHeight;
    return sum;
  }

  function collapse(slot) {
    slot.style.display = 'none';
  }

  function fill(slot) {
    var key = slot.getAttribute('data-ad-slot');
    var unit = UNITS[key];
    if (!unit) return;
    var ins = document.createElement('ins');
    ins.className = 'adsbygoogle';
    // 자리 상자 크기 그대로 고정 크기 광고를 요청한다 (규격은 2-32차 자리 설계를 따른다)
    ins.style.display = 'inline-block';
    ins.style.width = slot.clientWidth + 'px';
    ins.style.height = slot.clientHeight + 'px';
    ins.setAttribute('data-ad-client', CLIENT);
    ins.setAttribute('data-ad-slot', unit);
    slot.classList.add('ad-live');
    slot.removeAttribute('aria-hidden');
    slot.appendChild(ins);
    try {
      (window.adsbygoogle = window.adsbygoogle || []).push({});
    } catch (e) {
      collapse(slot);
      return;
    }
    setTimeout(function () {
      if (ins.getAttribute('data-ad-status') !== 'filled') collapse(slot);
    }, NO_FILL_COLLAPSE_MS);
  }

  function run() {
    if (!ENABLED) return;
    var slots = document.querySelectorAll('.ad-slot[data-ad-slot]');
    if (!slots.length) return;
    if (contentHeight() < MIN_CONTENT_PX) {
      for (var i = 0; i < slots.length; i++) collapse(slots[i]);
      return;
    }
    for (var j = 0; j < slots.length; j++) {
      // 숨겨진 자리(display:none 조상)는 offsetParent 가 null — 고정 위치(sticky 제외) 요소가 아니므로 이 판정으로 충분하다
      if (slots[j].offsetParent === null || slots[j].clientWidth === 0) continue;
      fill(slots[j]);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', run);
  } else {
    run();
  }
})();
