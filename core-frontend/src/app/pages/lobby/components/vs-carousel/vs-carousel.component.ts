import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';

// VS 카드 한쪽(좌/우) 스타 정보 — GET /api/super/lobby/vs-cards 응답 형식
export interface VsCardSide {
  id: string;
  name: string;
  image: string;
  starCategory?: string;
  score: number;
  // 표시용 지표 3종 — 카드 종류와 무관하게 누적 기준 (G 점수 구성요소)
  viewCount?: number;
  likeCnt?: number;
  followerCnt?: number;
}

// VS 카드 1장. right가 null이면 도전자 대기 상태
export interface VsCard {
  vsId: number;
  type: 'GLOBAL' | 'DAILY' | 'CUSTOM';
  category: string;
  isPinned?: boolean;
  title?: string;
  left: VsCardSide;
  right: VsCardSide | null;
}

@Component({
  selector: 'app-vs-carousel',
  templateUrl: './vs-carousel.component.html',
  styleUrls: ['./vs-carousel.component.scss'],
})
export class VsCarouselComponent implements OnInit, OnDestroy {

  // 5초 자동 순환 (요청서 확정값)
  private static readonly ROTATE_MS = 3500; // 클라이언트 요청으로 5초 → 3.5초
  // 스와이프로 인정할 최소 가로 이동량(px)
  private static readonly SWIPE_THRESHOLD = 40;
  // 크로스페이드 길이. vs-carousel.component.scss의 .vs-layer transition과 반드시 일치시킨다
  private static readonly TRANSITION_MS = 300;

  private _cards: VsCard[] = [];

  @Input()
  set cards(value: VsCard[]) {
    this._cards = value || [];
    // 폴링 갱신으로 카드 수가 줄어 인덱스가 범위를 벗어나면 처음으로 복귀
    if (this.currentIndex >= this._cards.length) {
      this.currentIndex = 0;
    }

    // 3초 폴링 갱신은 화면을 교체하지 않는다. 현재 보이는 레이어가 든 참조만
    // 최신 객체로 바꿔 점수 텍스트를 갱신한다. 이미지 URL이 그대로면 Angular가
    // src 속성을 건드리지 않으므로 이미지가 다시 로딩되지 않는다.
    this.layers[this.activeLayer] = this._cards[this.currentIndex] || null;
    this.stageNext();
  }
  get cards(): VsCard[] {
    return this._cards;
  }

  // 중앙(VS 영역) 클릭 — 로비가 해당 카테고리 TOP100 탭으로 전환한다
  @Output() centerSelect = new EventEmitter<VsCard>();

  currentIndex = 0;

  // 겹쳐 놓는 카드 2장(더블 버퍼). 전환 시 DOM을 파괴하지 않고 opacity만 바꾼다.
  // 카드를 매번 재생성하면 아바타가 다시 디코딩되고 등장 애니메이션이 재실행돼
  // 3.5초마다 깜빡이는 것처럼 보인다(2-26차).
  layers: (VsCard | null)[] = [null, null];
  activeLayer = 0;

  // 아바타 숨쉬기 모션 on/off. 로비가 화면을 떠날 때 자동 순환과 함께 멈춘다
  isMotionActive = true;

  private rotateIntervalId: any = null;
  private touchStartX = 0;

  // 전환 중에는 뒤 레이어를 건드리지 않는다 (사라지는 카드의 내용이 바뀌어 보인다)
  private isTransitioning = false;
  private rafId: any = null;
  private stageTimeoutId: any = null;
  // 이미 프리로드한 URL. 3초 폴링마다 같은 이미지를 다시 요청하지 않기 위해 기억한다
  private preloadedUrls = new Set<string>();

  private readonly categoryLabels: { [key: string]: string } = {
    GLOBAL: '🌐 All',
    STAR: '⭐ Star',
    CELEB: '👤 Celeb',
    BRAND: '🏢 Brand',
    ORG: '🏛 Org',
    UNIV: '🎓 Univ',
    CITY: '🌆 City',
    MEDIA: '📰 Media'
  };

  constructor(private router: Router) { }

  ngOnInit() {
    this.startAutoPlay();
  }

  ngOnDestroy() {
    this.stopAutoPlay();
    this.cancelPending();
  }

  // 로비 페이지가 ionViewDidEnter/ionViewWillLeave에서 호출해 백그라운드 낭비를 막는다
  startAutoPlay() {
    this.stopAutoPlay();
    this.isMotionActive = true;
    this.rotateIntervalId = setInterval(() => this.next(), VsCarouselComponent.ROTATE_MS);
  }

  stopAutoPlay() {
    this.isMotionActive = false;
    if (this.rotateIntervalId) {
      clearInterval(this.rotateIntervalId);
      this.rotateIntervalId = null;
    }
  }

  next() {
    if (this._cards.length < 2) return;
    this.transitionTo((this.currentIndex + 1) % this._cards.length);
  }

  prev() {
    if (this._cards.length < 2) return;
    this.transitionTo((this.currentIndex - 1 + this._cards.length) % this._cards.length);
  }

  /**
   * 특정 카드로 이동 (로비 LIVE 티커 탭, 2-29차). 카드가 목록에 없으면 false.
   * 자동 순환 타이머를 다시 시작해 방금 보여준 카드가 한 주기를 온전히 머물게 한다.
   */
  jumpTo(vsId: number): boolean {
    const index = this._cards.findIndex(c => c && c.vsId === vsId);
    if (index < 0) return false;
    if (index !== this.currentIndex) {
      this.transitionTo(index);
    }
    if (this.rotateIntervalId) this.startAutoPlay();
    return true;
  }

  // 레이어는 배열을 제자리에서 바꾸므로 인덱스로 추적한다.
  // 기본(참조) 추적을 쓰면 카드가 바뀔 때마다 뷰가 재생성돼 크로스페이드가 무의미해진다.
  trackByLayerIndex(index: number): number {
    return index;
  }

  // ===== 전환 =====
  private transitionTo(index: number) {
    const target = this._cards[index];
    if (!target) return;

    // 이전 전환이 아직 진행 중이면 취소하고 요청받은 카드로 바로 넘어간다
    this.cancelPending();

    const back = this.activeLayer === 0 ? 1 : 0;
    // 정상 흐름에서는 stageNext()가 이미 뒤 레이어에 올려두었다.
    // 스와이프처럼 예상 밖 인덱스면 지금 채운다.
    if (this.layers[back] !== target) {
      this.layers[back] = target;
      this.preloadCard(target);
    }
    this.currentIndex = index;

    this.isTransitioning = true;
    // 뒤 레이어가 opacity 0 상태로 한 프레임 그려진 뒤에 전환을 시작해야
    // 브라우저가 transition을 건너뛰고 즉시 교체해버리지 않는다
    this.rafId = requestAnimationFrame(() => {
      this.rafId = null;
      this.activeLayer = back;

      this.stageTimeoutId = setTimeout(() => {
        this.stageTimeoutId = null;
        this.isTransitioning = false;
        this.stageNext();
      }, VsCarouselComponent.TRANSITION_MS);
    });
  }

  // 다음 카드를 뒤 레이어에 미리 올려둔다. opacity 0이라 보이지 않지만
  // DOM에는 있으므로 브라우저가 이미지를 미리 받아 디코딩해 둔다.
  private stageNext() {
    if (this.isTransitioning) return;
    if (this._cards.length === 0) {
      this.layers[this.activeLayer === 0 ? 1 : 0] = null;
      return;
    }

    const back = this.activeLayer === 0 ? 1 : 0;
    const nextCard = this._cards[(this.currentIndex + 1) % this._cards.length] || null;
    this.layers[back] = nextCard;
    this.preloadCard(nextCard);
  }

  private cancelPending() {
    if (this.rafId !== null) {
      cancelAnimationFrame(this.rafId);
      this.rafId = null;
    }
    if (this.stageTimeoutId !== null) {
      clearTimeout(this.stageTimeoutId);
      this.stageTimeoutId = null;
    }
    this.isTransitioning = false;
  }

  private preloadCard(card: VsCard | null) {
    if (!card) return;
    this.preloadImage(card.left && card.left.image);
    this.preloadImage(card.right && card.right.image);
  }

  private preloadImage(url: string | null | undefined) {
    if (!url || this.preloadedUrls.has(url)) return;
    this.preloadedUrls.add(url);

    const img = new Image();
    img.setAttribute('decoding', 'async');
    img.src = url;
  }

  // 좌우 비율 게이지: 0 나눗셈 방지 + 10~90% 보정 (한쪽이 압도해도 레이아웃 유지)
  getLeftRatio(card: VsCard): number {
    if (!card || !card.right) return 100;
    const leftScore = (card.left && card.left.score) || 0;
    const rightScore = card.right.score || 0;
    const total = leftScore + rightScore;
    if (total === 0) return 50;
    return Math.max(10, Math.min(90, (leftScore / total) * 100));
  }

  getScoreGap(card: VsCard): number {
    if (!card || !card.left || !card.right) return 0;
    return Math.abs((card.left.score || 0) - (card.right.score || 0));
  }

  getTypeLabel(card: VsCard): string {
    if (!card) return '';
    if (card.type === 'CUSTOM') return card.title || 'Special Match';
    const cat = this.categoryLabels[card.category] || card.category;
    return card.type === 'DAILY' ? `${cat} · Daily King` : `${cat} · Global Ranking`;
  }

  formatScore(value: number): string {
    const n = value || 0;
    if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M';
    if (n >= 1000) return (n / 1000).toFixed(1) + 'K';
    return String(n);
  }

  // 점수 단위: DAILY는 오늘 노출 수(views), GLOBAL·CUSTOM은 종합점수(pts)
  getScoreUnit(card: VsCard): string {
    return card && card.type === 'DAILY' ? 'views' : 'pts';
  }

  // ===== 클릭 동선 =====
  goToProfile(side: VsCardSide | null, event: Event) {
    event.stopPropagation();
    if (!side || !side.id) return;
    this.router.navigate(['/star', side.id]);
  }

  onCenterClick(card: VsCard) {
    this.centerSelect.emit(card);
  }

  // ===== 수동 스와이프 (넘긴 뒤 자동 순환 타이머 재시작) =====
  onTouchStart(event: TouchEvent) {
    this.touchStartX = event.touches[0].clientX;
  }

  onTouchEnd(event: TouchEvent) {
    const delta = event.changedTouches[0].clientX - this.touchStartX;
    if (Math.abs(delta) < VsCarouselComponent.SWIPE_THRESHOLD) return;
    if (delta < 0) {
      this.next();
    } else {
      this.prev();
    }
    if (this.rotateIntervalId) {
      this.startAutoPlay();
    }
  }

  handleImageError(event: any) {
    event.target.src = 'assets/img/defaultImg/avatar.svg';
  }

  trackByVsId(index: number, card: VsCard): string {
    return `${card.vsId}-${card.type}-${card.category}`;
  }
}
