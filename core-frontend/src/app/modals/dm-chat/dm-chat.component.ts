import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonContent, IonicModule, ModalController } from '@ionic/angular';
import { DmMessage, DmService } from 'src/app/services/dm.service';
import { HelperService } from 'src/app/services/helper.service';
import { PhotoService } from 'src/app/services/photo.service';
import { VideoService } from 'src/app/services/video.service';

// 첨부 대기 상태 (전송 전 미리보기)
interface PendingAttachment {
  kind: 'IMAGE' | 'VIDEO';
  base64: string;   // 순수 base64
  mime: string;
  previewUrl: string;
}

/** 채팅 모달 열기. 닫힐 때까지 기다린 뒤 미읽음을 갱신한다 */
export async function openDmChat(modalCtrl: ModalController, dm: DmService,
  props: { peerId: string; peerName?: string; peerImage?: string | null }): Promise<void> {
  const modal = await modalCtrl.create({
    component: DmChatComponent,
    componentProps: props,
  });
  await modal.present();
  await modal.onDidDismiss();
  dm.refreshUnread();
}

/**
 * 1:1 채팅 화면 (2-29차) — 레거시 지구로또 chat-write-modal의 흐름을 옮겼다.
 * 텍스트·사진·영상 전송, 읽음 표시, 발송 5분·읽음 1분 뒤 자동 삭제 안내.
 * 열려 있는 동안 3초 폴링으로 새 메시지를 받고 읽음 처리한다 (스타↔어드민 문의 모달과 같은 방식).
 */
@Component({
  selector: 'app-dm-chat',
  standalone: true,
  imports: [CommonModule, IonicModule, FormsModule],
  templateUrl: './dm-chat.component.html',
  styleUrls: ['./dm-chat.component.scss'],
})
export class DmChatComponent implements OnInit, OnDestroy {
  private static readonly POLL_MS = 3000;
  private static readonly VIDEO_MAX_BYTES = 30 * 1024 * 1024;
  private static readonly IMAGE_MAX_BYTES = 10 * 1024 * 1024;

  @Input() peerId: string;
  @Input() peerName = '';
  @Input() peerImage: string | null = null;
  @ViewChild('content', { static: false }) content: IonContent;

  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  myId = '';
  messages: DmMessage[] = [];
  draft = '';
  pending: PendingAttachment | null = null;
  sending = false;
  loaded = false;
  // 전체 보기 (사진은 원본, 영상은 소리·컨트롤 포함 재생)
  viewerUrl = '';
  viewerType: 'IMAGE' | 'VIDEO' | '' = '';
  // 전체 보기 영상이 로드되기 전에 보일 포스터 (서버가 뽑은 첫 프레임 썸네일)
  viewerPoster = '';

  private pollId: any = null;
  private lastCount = -1;

  constructor(
    private modalCtrl: ModalController,
    private dm: DmService,
    private helper: HelperService,
    private photo: PhotoService,
    private video: VideoService,
  ) { }

  ngOnInit() {
    this.myId = this.dm.myId;
    this.load(true);
    this.pollId = setInterval(() => this.load(false), DmChatComponent.POLL_MS);
  }

  ngOnDestroy() {
    if (this.pollId) {
      clearInterval(this.pollId);
      this.pollId = null;
    }
  }

  dismiss() {
    this.modalCtrl.dismiss();
  }

  isMine(msg: DmMessage): boolean {
    return msg.senderId === this.myId;
  }

  url(relative: string | undefined): string {
    return this.dm.fileUrl(relative);
  }

  trackByMsg(_: number, msg: DmMessage): number {
    return msg.msgId;
  }

  /** 대화 조회 + 상대 메시지 읽음 처리. 개수가 바뀐 경우에만 맨 아래로 스크롤 */
  private load(first: boolean) {
    this.dm.messages(this.peerId).subscribe({
      next: (res: any) => {
        if (!res || res.result !== 'OK') {
          if (first) this.helper.toast(res && res.msg ? res.msg : 'Could not load messages.', 'middle');
          this.loaded = true;
          return;
        }
        if (res.peer) {
          this.peerName = res.peer.name || this.peerName;
          this.peerImage = res.peer.image || this.peerImage;
        }
        const list: DmMessage[] = DmChatComponent.mergeMessages(this.messages, res.messages || []);
        const hasUnreadFromPeer = list.some(m => !this.isMine(m) && !m.readDate);
        this.messages = list;
        this.loaded = true;
        if (list.length !== this.lastCount) {
          this.lastCount = list.length;
          this.scrollToBottom();
        }
        if (hasUnreadFromPeer) {
          this.dm.markRead(this.peerId).subscribe({ next: () => { }, error: () => { } });
        }
      },
      error: () => {
        this.loaded = true;
        if (first) this.helper.toast('Could not load messages.', 'middle');
      }
    });
  }

  /**
   * 폴링 응답을 기존 목록과 합친다.
   * 서버는 응답마다 첨부 URL에 새 토큰을 붙이므로 그대로 바꿔 끼우면 <video>·<img> src가 3초마다 바뀌어
   * 영상이 처음으로 되돌아가고 화면이 깜빡였다(실기기 확인). 첨부 URL은 처음 받은 값을 유지하고,
   * 읽음 상태까지 같으면 객체도 그대로 재사용해 DOM을 건드리지 않는다. (토큰 10분 > 메시지 수명 5분)
   */
  static mergeMessages(prev: DmMessage[], next: DmMessage[]): DmMessage[] {
    const prevById = new Map<number, DmMessage>();
    for (const m of prev) prevById.set(m.msgId, m);
    return next.map(m => {
      const old = prevById.get(m.msgId);
      if (!old) return m;
      if (old.readDate === m.readDate) return old;
      return { ...m, fileUrl: old.fileUrl || m.fileUrl, thumbUrl: old.thumbUrl || m.thumbUrl };
    });
  }

  private scrollToBottom() {
    setTimeout(() => {
      if (this.content) this.content.scrollToBottom(200);
    }, 100);
  }

  // ===== 전송 =====

  async send() {
    if (this.sending) return;
    if (this.pending) {
      await this.sendPending();
      return;
    }
    const text = this.draft.trim();
    if (!text) return;

    this.sending = true;
    this.dm.sendText(this.peerId, text).subscribe({
      next: (res: any) => {
        this.sending = false;
        if (res && res.result === 'OK') {
          this.draft = '';
          this.load(false);
        } else {
          this.helper.toast(res && res.msg ? res.msg : 'Could not send the message.', 'middle');
        }
      },
      error: () => {
        this.sending = false;
        this.helper.toast('Could not send the message.', 'middle');
      }
    });
  }

  private async sendPending() {
    const p = this.pending;
    if (!p) return;
    this.sending = true;
    this.helper.toast('Uploading...', 'middle');
    this.dm.sendFile(this.peerId, p.kind, p.base64, p.mime).subscribe({
      next: (res: any) => {
        this.sending = false;
        if (res && res.result === 'OK') {
          this.pending = null;
          this.load(false);
        } else {
          this.helper.toast(res && res.msg ? res.msg : 'Upload failed.', 'middle');
        }
      },
      error: () => {
        this.sending = false;
        this.helper.toast('Upload failed.', 'middle');
      }
    });
  }

  // ===== 첨부 =====

  async attachPhoto() {
    try {
      const photo = await this.photo.getPhotos();
      if (!photo || !photo.base64String) return;
      const base64 = photo.base64String;
      if (DmChatComponent.byteLength(base64) > DmChatComponent.IMAGE_MAX_BYTES) {
        this.helper.toast('Image is too large. Please choose a file under 10MB.', 'middle');
        return;
      }
      const format = (photo.format || 'jpeg').toLowerCase();
      const mime = `image/${format === 'jpg' ? 'jpeg' : format}`;
      this.pending = { kind: 'IMAGE', base64, mime, previewUrl: `data:${mime};base64,${base64}` };
    } catch (e) {
      this.helper.toast('Could not attach the photo.', 'middle');
    }
  }

  async attachVideo() {
    try {
      const { base64String, format } = await this.video.getVideos();
      if (!base64String) return;
      if (DmChatComponent.byteLength(base64String) > DmChatComponent.VIDEO_MAX_BYTES) {
        this.helper.toast('Video is too large. Please choose a file under 30MB.', 'middle');
        return;
      }
      const mime = `video/${(format || 'mp4').toLowerCase()}`;
      this.pending = { kind: 'VIDEO', base64: base64String, mime, previewUrl: `data:${mime};base64,${base64String}` };
    } catch (e) {
      // VideoService는 길이(60초)·크기 초과도 같은 에러로 던진다
      this.helper.toast('Could not attach the video. Please choose a video under 60 seconds and 30MB.', 'middle');
    }
  }

  removePending() {
    this.pending = null;
  }

  /** base64 문자열 길이 → 원본 바이트 수 (패딩 보정) */
  static byteLength(base64: string): number {
    const clean = base64.replace(/^data:[^,]+,/, '');
    const padding = clean.endsWith('==') ? 2 : clean.endsWith('=') ? 1 : 0;
    return Math.floor(clean.length * 3 / 4) - padding;
  }

  // ===== 보기 =====

  openViewer(url: string, type: 'IMAGE' | 'VIDEO', poster: string = '') {
    this.viewerUrl = url;
    this.viewerType = type;
    this.viewerPoster = poster;
  }

  closeViewer() {
    this.viewerUrl = '';
    this.viewerType = '';
    this.viewerPoster = '';
  }

  // 미리보기 영상 무음 보장. 일부 웹뷰는 [muted] 바인딩 전에 재생을 시작하므로 메타데이터 로드 시점에 한 번 더 잠근다
  muteVideo(event: Event) {
    const el = event && (event.target as HTMLVideoElement);
    if (el) {
      el.muted = true;
      el.volume = 0;
    }
  }

  // 전체 보기 안의 영상을 탭해도 닫히지 않게 (컨트롤 조작용)
  stopClose(event: Event) {
    event.stopPropagation();
  }

  handleImageError(event: any) {
    if (event && event.target) event.target.src = this.defaultAvatar;
  }
}
