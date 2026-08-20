import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

// 화면 유형별 스켈레톤. 실제 콘텐츠와 높이를 맞춰 로딩 해제 시 레이아웃이 튀지 않게 한다.
export type SkeletonVariant = 'card' | 'list' | 'ranking' | 'vs' | 'page';

@Component({
  standalone: true,
  imports: [CommonModule],
  selector: 'app-skeleton',
  templateUrl: './skeleton.component.html',
  styleUrls: ['./skeleton.component.scss'],
})
export class SkeletonComponent {
  // 표시할 스켈레톤 종류
  @Input() variant: SkeletonVariant = 'list';

  // 반복 개수 (list, ranking, card에서 사용)
  @Input() count = 5;

  // 가로 스크롤 영역에 놓이는 카드인지 여부
  @Input() horizontal = false;

  get rows(): number[] {
    return Array.from({ length: this.count }, (_, i) => i);
  }
}
