import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActionSheetController, AlertController, IonContent, IonicModule, ModalController } from '@ionic/angular';
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

// 롱프레스 시작 지점 (스크롤과 구분하려고 이동 거리를 잰다)
interface PressPoint {
  x: number;
  y: number;
}

// 신고 사유. 값은 서버 화이트리스트(DmService.REPORT_REASONS)와 1:1로 맞춘다
const REPORT_REASONS: { label: string; value: string }[] = [
  { label: 'Harassment or abuse', value: 'ABUSE' },
  { label: 'Sexual content', value: 'SEXUAL' },
  { label: 'Spam or advertising', value: 'SPAM' },
  { label: 'Scam or phishing', value: 'FRAUD' },
  { label: 'Threats or violence', value: 'THREAT' },
  { label: 'Something else', value: 'OTHER' },
];

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
  // 롱프레스 판정 시간. 짧으면 스크롤 중에 메뉴가 뜨고, 길면 눌러도 반응이 없다고 느낀다
  private static readonly LONG_PRESS_MS = 600;
  // 이만큼 움직이면 스크롤로 보고 롱프레스를 취소한다
  private static readonly MOVE_CANCEL_PX = 10;
  // 롱프레스로 시트를 연 뒤, touchend가 만들어내는 click이 뒤에서 뷰어를 여는 것을 막는 시간
  private static readonly TAP_SUPPRESS_MS = 500;

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
  // 롱프레스 상태. 메시지는 msgId로만 들고 다닌다 — mergeMessages가 새 객체를 돌려주면
  // 붙잡아 둔 DmMessage 참조는 화면에 그려진 목록과 조용히 어긋난다
  private pressTimer: any = null;
  private pressStart: PressPoint = { x: 0, y: 0 };
  private suppressTapUntil = 0;

  constructor(
    private modalCtrl: ModalController,
    private dm: DmService,
    private helper: HelperService,
    private photo: PhotoService,
    private video: VideoService,
    private actionSheetCtrl: ActionSheetController,
    private alertCtrl: AlertController,
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
    this.onPressEnd();
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

  // ===== 신고 (2-28차) =====
  //
  // 롱프레스는 터치 타이머로 직접 만든다. Ionic GestureController를 쓰면 롱프레스
  // 프리미티브가 없어 타이머는 똑같이 짜야 하는데, 제스처가 구체 엘리먼트에 묶이는 탓에
  // 메시지 행마다 생성·해제를 관리해야 한다. 폴링이 3초마다 목록 객체를 갈아끼우므로
  // 그 관리 비용이 상시로 발생하고, ion-content 스크롤 제스처와의 우선순위도 따로 맞춰야 한다.
  //
  // 핸들러는 말풍선이 아니라 행 래퍼에 붙인다. 터치 이벤트가 위로 올라오므로
  // 사진·영상의 기존 (click)="openViewer(...)"는 그대로 살아 있다.
  // touchstart에서 preventDefault·stopPropagation을 부르면 스크롤이 죽으므로 절대 부르지 않는다.

  onPressStart(msg: DmMessage, event: TouchEvent) {
    this.onPressEnd();
    if (this.isMine(msg)) return;

    const touch = event && event.touches && event.touches[0];
    this.pressStart = { x: touch ? touch.clientX : 0, y: touch ? touch.clientY : 0 };

    const msgId = msg.msgId;
    this.pressTimer = setTimeout(() => {
      this.pressTimer = null;
      this.openReportSheet(msgId);
    }, DmChatComponent.LONG_PRESS_MS);
  }

  onPressMove(event: TouchEvent) {
    if (!this.pressTimer) return;
    const touch = event && event.touches && event.touches[0];
    if (!touch) return;
    const moved = DmChatComponent.movedTooFar(
      this.pressStart,
      { x: touch.clientX, y: touch.clientY },
      DmChatComponent.MOVE_CANCEL_PX,
    );
    if (moved) this.onPressEnd();
  }

  onPressEnd() {
    if (this.pressTimer) {
      clearTimeout(this.pressTimer);
      this.pressTimer = null;
    }
  }

  /** 데스크톱 우클릭과 iOS 자체 콜아웃 경로도 같은 메뉴로 받는다 */
  onContextMenu(msg: DmMessage, event: Event) {
    if (event) event.preventDefault();
    this.onPressEnd();
    if (this.isMine(msg)) return;
    this.openReportSheet(msg.msgId);
  }

  private async openReportSheet(msgId: number) {
    this.suppressTapUntil = Date.now() + DmChatComponent.TAP_SUPPRESS_MS;

    const sheet = await this.actionSheetCtrl.create({
      header: 'Message options',
      buttons: [
        {
          text: 'Report',
          role: 'destructive',
          icon: 'flag-outline',
          handler: () => { this.askReportReason(msgId); },
        },
        { text: 'Cancel', role: 'cancel' },
      ],
    });
    await sheet.present();
  }

  private async askReportReason(msgId: number) {
    const alert = await this.alertCtrl.create({
      header: 'Report message',
      message: 'Please select a reason. Our team keeps a copy of this message for review.',
      inputs: REPORT_REASONS.map((item, index) => ({
        name: 'reason',
        type: 'radio' as const,
        label: item.label,
        value: item.value,
        checked: index === 0,
      })),
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        // 라디오 입력은 선택값 문자열이 그대로 인자로 들어온다 (객체가 아니다)
        { text: 'Report', handler: (reason: string) => { this.submitReport(msgId, reason); } },
      ],
    });
    await alert.present();
  }

  private submitReport(msgId: number, reason: string) {
    if (!reason) return;
    this.dm.report(msgId, reason).subscribe({
      next: (res: any) => {
        if (res && res.result === 'OK') {
          this.helper.toast('Report submitted. Our team will review it.', 'middle');
        } else {
          this.helper.toast(res && res.msg ? res.msg : 'Could not submit the report.', 'middle');
        }
      },
      error: () => {
        this.helper.toast('Could not submit the report.', 'middle');
      },
    });
  }

  /** 내가 보낸 메시지는 신고할 수 없다 */
  static canReport(msg: DmMessage, myId: string): boolean {
    if (!msg || !myId) return false;
    return msg.senderId !== myId;
  }

  /** 손가락이 임계값을 넘게 움직였는지 (스크롤과 롱프레스를 가른다) */
  static movedTooFar(start: PressPoint, current: PressPoint, threshold: number): boolean {
    const dx = current.x - start.x;
    const dy = current.y - start.y;
    return Math.sqrt(dx * dx + dy * dy) > threshold;
  }

  // ===== 보기 =====

  openViewer(url: string, type: 'IMAGE' | 'VIDEO', poster: string = '') {
    // 롱프레스로 시트를 연 직후 touchend가 만들어내는 click은 무시한다 (뒤에서 뷰어가 열린다)
    if (Date.now() < this.suppressTapUntil) return;
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
