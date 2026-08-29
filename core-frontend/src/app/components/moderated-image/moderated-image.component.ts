import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { IonicModule } from '@ionic/angular';
import { environment } from 'src/environments/environment';

/**
 * 검수 상태를 반영해 이미지를 그리는 공통 컴포넌트 (2-26차).
 *
 * 규칙은 세 가지다.
 *   승인됨          → 이미지를 그대로 보여준다
 *   검수 대기 + 본인 → 서버가 내려준 단기 토큰으로 원본을 불러온다
 *   검수 대기 + 타인 → "검토 중" 자리를 보여준다 (깨진 이미지를 노출하지 않는다)
 *
 * 회원 콘텐츠 목록과 스타 페이지 등 여러 곳에서 같은 판단을 하므로 한 곳에 모았다.
 */
@Component({
  standalone: true,
  imports: [CommonModule, IonicModule],
  selector: 'app-moderated-image',
  templateUrl: './moderated-image.component.html',
  styleUrls: ['./moderated-image.component.scss'],
})
export class ModeratedImageComponent {

  /** 승인된 이미지 주소. 승인 전이면 서버가 null로 내려준다 */
  @Input() src: string | null = null;

  /** 서버 검수 상태 (PENDING / APPROVED / REJECTED / HIDDEN) */
  @Input() status: string | null = null;

  /** 작성자 본인일 때만 서버가 내려주는 단기 접근 토큰 */
  @Input() pendingToken: string | null = null;

  /** 이미지를 못 불러왔을 때 대신 쓸 주소 */
  @Input() fallback = '';

  /** css class를 그대로 넘겨 기존 목록의 모양을 유지한다 */
  @Input() imgClass = '';

  @Input() alt = '';

  get isUnderReview(): boolean {
    return this.status === 'PENDING' || this.status === 'REJECTED' || this.status === 'HIDDEN';
  }

  /** 실제로 그릴 주소. 없으면 "검토 중" 자리를 그린다 */
  get resolvedSrc(): string | null {
    if (!this.isUnderReview) {
      return this.src || this.fallback || null;
    }
    // 작성자 본인에게만 토큰이 내려온다. 서버가 매 요청마다 상태를 다시 확인한다
    if (this.status === 'PENDING' && this.pendingToken) {
      return `${environment.apiBaseURL}/api/super/media/pending?t=${encodeURIComponent(this.pendingToken)}`;
    }
    return null;
  }

  handleError(event: any) {
    if (this.fallback && event.target.src !== this.fallback) {
      event.target.src = this.fallback;
    }
  }
}
