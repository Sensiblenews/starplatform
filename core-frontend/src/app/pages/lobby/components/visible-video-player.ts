import { pickActiveIndex, pickActiveIndexes } from '../latest-contents-logic';
import { MediaAudioService } from 'src/app/services/media-audio.service';

/**
 * 카드 여러 장 중 화면에 가장 많이 보이는 영상 카드 하나만 재생하고 나머지는 멈춘다 (2-34차 세로형·가로형 공용).
 * 스타 페이지 피드와 같은 IntersectionObserver 방식. ion-content 안의 스크롤도 뷰포트 기준 교차로 잡힌다.
 *
 * - 카드 요소 안의 첫 <video> 를 제어한다. 사진 카드는 isVideo 로 건너뛴다
 * - start() 전·stop() 뒤에는 아무것도 재생하지 않는다 (로비 이탈·백그라운드)
 * - 재생·소리는 MediaAudioService 가 맡는다 — 기본 소리 켬 (2-34차 1-3)
 * - maxActive 가 2 이상이면(가로형, 2-34 후속) 절반 이상 보이는 영상 카드를 화면 순서대로 그만큼 함께 재생한다.
 *   소리는 첫 번째 카드만, 나머지는 음소거
 */
export class VisibleVideoPlayer {
  /** 지금 재생 중인 카드 번호들 (첫 번째가 소리 담당). 없으면 빈 배열 */
  activeIndexes: number[] = [];

  private observer: IntersectionObserver | null = null;
  private cards: HTMLElement[] = [];
  private ratios: number[] = [];
  private running = false;

  constructor(
    private readonly audio: MediaAudioService,
    private readonly isVideo: (index: number) => boolean,
    private readonly onActiveChange?: (indexes: number[]) => void,
    private readonly maxActive = 1,
  ) { }

  /** 카드 목록이 바뀔 때마다 부른다 (ngAfterViewInit·목록 갱신 뒤) */
  setCards(cards: HTMLElement[]): void {
    this.disconnect();
    this.cards = cards || [];
    this.ratios = this.cards.map(() => 0);
    if (typeof IntersectionObserver === 'undefined' || this.cards.length === 0) return;
    this.observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        const index = this.cards.indexOf(entry.target as HTMLElement);
        if (index >= 0) this.ratios[index] = entry.isIntersecting ? entry.intersectionRatio : 0;
      });
      this.apply();
    }, { threshold: [0, 0.25, 0.5, 0.75, 1] });
    this.cards.forEach(card => this.observer!.observe(card));
  }

  start(): void {
    this.running = true;
    this.apply();
  }

  stop(): void {
    this.running = false;
    this.apply();
  }

  destroy(): void {
    this.running = false;
    this.apply();
    this.disconnect();
    this.cards = [];
  }

  private disconnect(): void {
    if (this.observer) {
      this.observer.disconnect();
      this.observer = null;
    }
  }

  private apply(): void {
    const isVideo = this.cards.map((_, i) => this.isVideo(i));
    let next: number[] = [];
    if (this.running) {
      if (this.maxActive > 1) {
        next = pickActiveIndexes(this.ratios, isVideo, this.maxActive);
      } else {
        const one = pickActiveIndex(this.ratios, isVideo);
        next = one >= 0 ? [one] : [];
      }
    }
    this.cards.forEach((card, i) => {
      const video = card.querySelector('video');
      if (!video) return;
      const slot = next.indexOf(i);
      if (slot === 0) {
        // 다른 섹션이 재생을 가져갔으면(isCurrent 아님) 화면에 가장 많이 보이게 된 지금 다시 가져온다
        if (video.paused || !this.audio.isCurrent(video)) {
          this.audio.play(video); // 거부되면 포스터가 남는다
        }
      } else if (slot > 0) {
        this.audio.playMuted(video);
      } else {
        this.audio.pause(video);
      }
    });
    if (next.join(',') !== this.activeIndexes.join(',')) {
      this.activeIndexes = next;
      if (this.onActiveChange) this.onActiveChange(next);
    }
  }
}
