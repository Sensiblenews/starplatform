import { Directive, ElementRef, NgZone, OnDestroy, OnInit } from '@angular/core';

/** 최대 확대 배율 */
const MAX_SCALE = 4;
/** 손을 뗀 뒤 원래 자리로 돌아가는 시간(ms) */
const RETURN_MS = 220;

/**
 * 피드 사진 제자리 핀치 확대 (2-35차 — 클라이언트 요청: 스타페이지에서도 상세처럼 손가락으로 사진 확대).
 *
 * 인스타그램처럼 두 손가락으로 벌리면 그 자리에서 커지고, 손을 떼면 원래 크기로 돌아간다.
 * 한 손가락 탭은 건드리지 않으므로 카드 탭(게시물 상세로 이동)은 그대로 동작한다.
 *
 * 사진은 overflow:hidden 인 1:1 카드 안에 잘려 있어 그 자리에서 키우면 카드 밖으로 나가지 못한다.
 * 그래서 확대하는 동안에는 같은 위치·크기의 사본을 화면 맨 위(position:fixed)에 띄워 키우고,
 * 원본은 잠시 숨긴다. 끝나면 사본을 원래 자리로 되돌린 뒤 지운다.
 *
 * 사용: <img appPinchZoom ...>
 */
@Directive({
  selector: 'img[appPinchZoom]',
  standalone: true,
})
export class PinchZoomDirective implements OnInit, OnDestroy {
  private startDist = 0;
  private startMid = { x: 0, y: 0 };
  private rect: DOMRect | null = null;
  private overlay: HTMLDivElement | null = null;
  private clone: HTMLImageElement | null = null;
  /** 핀치 직후 따라오는 click(카드 → 상세 이동)을 한 번 막는다 */
  private suppressClickUntil = 0;

  constructor(private el: ElementRef<HTMLImageElement>, private ngZone: NgZone) { }

  ngOnInit(): void {
    const img = this.el.nativeElement;
    // touchmove 에서 preventDefault(스크롤 막기)를 해야 하므로 passive:false. 변경 감지는 필요 없다
    this.ngZone.runOutsideAngular(() => {
      img.addEventListener('touchstart', this.onStart, { passive: false });
      img.addEventListener('touchmove', this.onMove, { passive: false });
      img.addEventListener('touchend', this.onEnd);
      img.addEventListener('touchcancel', this.onEnd);
      img.addEventListener('click', this.onClick, true);
    });
  }

  ngOnDestroy(): void {
    const img = this.el.nativeElement;
    img.removeEventListener('touchstart', this.onStart);
    img.removeEventListener('touchmove', this.onMove);
    img.removeEventListener('touchend', this.onEnd);
    img.removeEventListener('touchcancel', this.onEnd);
    img.removeEventListener('click', this.onClick, true);
    this.removeOverlay();
  }

  private onStart = (ev: TouchEvent) => {
    if (ev.touches.length !== 2 || this.overlay) return;
    const img = this.el.nativeElement;
    if (!img.complete || !img.naturalWidth) return; // 아직 안 불러온 사진은 건너뛴다
    ev.preventDefault();

    this.startDist = distance(ev.touches[0], ev.touches[1]);
    this.startMid = midpoint(ev.touches[0], ev.touches[1]);
    this.rect = img.getBoundingClientRect();
    this.createOverlay(img, this.rect);
  };

  private onMove = (ev: TouchEvent) => {
    if (!this.overlay || !this.clone || !this.rect) return;
    ev.preventDefault();
    if (ev.touches.length < 2) return;

    const dist = distance(ev.touches[0], ev.touches[1]);
    const mid = midpoint(ev.touches[0], ev.touches[1]);
    const scale = Math.min(MAX_SCALE, Math.max(1, dist / (this.startDist || 1)));

    // 손가락 사이 중심을 기준으로 키우고, 중심이 움직인 만큼 따라간다
    const originX = this.startMid.x - this.rect.left;
    const originY = this.startMid.y - this.rect.top;
    const dx = mid.x - this.startMid.x;
    const dy = mid.y - this.startMid.y;
    this.clone.style.transformOrigin = `${originX}px ${originY}px`;
    this.clone.style.transform = `translate(${dx}px, ${dy}px) scale(${scale})`;
    // 커질수록 뒤를 어둡게 (최대 0.6)
    this.overlay.style.backgroundColor = `rgba(0, 0, 0, ${Math.min(0.6, (scale - 1) * 0.4)})`;
  };

  private onEnd = (ev: TouchEvent) => {
    if (!this.overlay || !this.clone) return;
    // 두 손가락 중 하나만 떼도 끝낸다 (인스타그램과 같음)
    if (ev.touches.length >= 2) return;
    this.suppressClickUntil = Date.now() + 500;

    const clone = this.clone;
    const overlay = this.overlay;
    clone.style.transition = `transform ${RETURN_MS}ms ease-out`;
    overlay.style.transition = `background-color ${RETURN_MS}ms ease-out`;
    clone.style.transform = 'translate(0px, 0px) scale(1)';
    overlay.style.backgroundColor = 'rgba(0, 0, 0, 0)';
    setTimeout(() => this.removeOverlay(), RETURN_MS);
  };

  private onClick = (ev: MouseEvent) => {
    if (Date.now() < this.suppressClickUntil) {
      ev.stopPropagation();
      ev.preventDefault();
    }
  };

  private createOverlay(img: HTMLImageElement, rect: DOMRect): void {
    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;z-index:99999;pointer-events:none;background-color:rgba(0,0,0,0);';

    const clone = document.createElement('img');
    clone.src = img.currentSrc || img.src;
    const fit = getComputedStyle(img).objectFit || 'cover';
    clone.style.cssText = [
      'position:absolute',
      `left:${rect.left}px`,
      `top:${rect.top}px`,
      `width:${rect.width}px`,
      `height:${rect.height}px`,
      `object-fit:${fit}`,
      'will-change:transform',
      'transform:translate(0px, 0px) scale(1)',
    ].join(';');

    overlay.appendChild(clone);
    document.body.appendChild(overlay);
    img.style.visibility = 'hidden';
    this.overlay = overlay;
    this.clone = clone;
  }

  private removeOverlay(): void {
    if (this.overlay && this.overlay.parentNode) this.overlay.parentNode.removeChild(this.overlay);
    this.overlay = null;
    this.clone = null;
    this.rect = null;
    this.el.nativeElement.style.visibility = '';
  }
}

function distance(a: Touch, b: Touch): number {
  return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
}

function midpoint(a: Touch, b: Touch): { x: number; y: number } {
  return { x: (a.clientX + b.clientX) / 2, y: (a.clientY + b.clientY) / 2 };
}
