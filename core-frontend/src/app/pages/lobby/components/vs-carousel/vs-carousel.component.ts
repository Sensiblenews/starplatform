import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';

// VS 카드 한쪽(좌/우) 스타 정보 — GET /api/super/lobby/vs-cards 응답 형식
export interface VsCardSide {
  id: string;
  name: string;
  image: string;
  starCategory?: string;
  score: number;
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
  private static readonly ROTATE_MS = 5000;
  // 스와이프로 인정할 최소 가로 이동량(px)
  private static readonly SWIPE_THRESHOLD = 40;

  private _cards: VsCard[] = [];

  @Input()
  set cards(value: VsCard[]) {
    this._cards = value || [];
    // 폴링 갱신으로 카드 수가 줄어 인덱스가 범위를 벗어나면 처음으로 복귀
    if (this.currentIndex >= this._cards.length) {
      this.currentIndex = 0;
    }
  }
  get cards(): VsCard[] {
    return this._cards;
  }

  // 중앙(VS 영역) 클릭 — 로비가 해당 카테고리 TOP100 탭으로 전환한다
  @Output() centerSelect = new EventEmitter<VsCard>();

  currentIndex = 0;

  private rotateIntervalId: any = null;
  private touchStartX = 0;

  private readonly categoryLabels: { [key: string]: string } = {
    GLOBAL: '🌐 All',
    STAR: '⭐ Star',
    CELEB: '👤 Celeb',
    BRAND: '🏢 Brand',
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
  }

  // 로비 페이지가 ionViewDidEnter/ionViewWillLeave에서 호출해 백그라운드 낭비를 막는다
  startAutoPlay() {
    this.stopAutoPlay();
    this.rotateIntervalId = setInterval(() => this.next(), VsCarouselComponent.ROTATE_MS);
  }

  stopAutoPlay() {
    if (this.rotateIntervalId) {
      clearInterval(this.rotateIntervalId);
      this.rotateIntervalId = null;
    }
  }

  next() {
    if (this.cards.length === 0) return;
    this.currentIndex = (this.currentIndex + 1) % this.cards.length;
  }

  prev() {
    if (this.cards.length === 0) return;
    this.currentIndex = (this.currentIndex - 1 + this.cards.length) % this.cards.length;
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
