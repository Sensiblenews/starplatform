import { Component, ElementRef, OnInit, OnDestroy, QueryList, ViewChildren } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AlertController, ModalController, NavController, PopoverController } from '@ionic/angular';
import { Share } from '@capacitor/share';
import { HttpService } from '../../services/http.service';
import { environment } from 'src/environments/environment';
import { DomSanitizer, SafeHtml, SafeResourceUrl } from '@angular/platform-browser';
import { AdMobService } from '../../services/ad-mob.service';
import { FeedMenuComponent } from './feed-menu.component';
import { DeepLinkService } from '../../services/deep-link.service';
import { WriteModalService } from '../../services/write-modal.service';
import { handleLinkClick, linkifyText } from '../../pipes/linkify';
import { PhotoViewerComponent } from '../../components/photo-viewer/photo-viewer.component';
import { MediaAudioService } from '../../services/media-audio.service';

@Component({
  selector: 'app-feed-detail',
  templateUrl: './feed-detail.page.html',
  styleUrls: ['./feed-detail.page.scss'],
})
export class FeedDetailPage implements OnInit, OnDestroy {
  conId: string;
  contentInfo: any = {};
  mediaList: any[] = [];
  
  // 응답 전 빈 값 — 예전 기본값 'Unknown' 이 로딩 중 잠깐 보였다(2-33차 버그). 작성자 행은 isLoaded 뒤에만 그린다
  starName: string = '';
  starProfileImg: string = 'assets/img/defaultImg/avatar.svg';
  isLoaded = false;

  // [신규] 안전하게 변환된 유튜브 URL을 담을 변수
  safeYoutubeUrl: SafeResourceUrl | null = null;
  safeContentHtml: SafeHtml | null = null;

  // 검수 대기 글을 타인이 딥링크로 열면 서버가 FAIL을 준다(2-27차). 빈 화면 대신 안내를 띄운다
  notFound = false;

  private paramSub: any;

  constructor(
    private route: ActivatedRoute,
    private http: HttpService,
    private sanitizer: DomSanitizer,
    private admob: AdMobService,
    private navCtrl: NavController,
    private popoverCtrl: PopoverController,
    private alertCtrl: AlertController,
    private router: Router,
    private deepLink: DeepLinkService,
    private writeModalService: WriteModalService,
    private modalCtrl: ModalController,
    private audio: MediaAudioService
  ) {}

  // ===== 동영상 재생 (2-35차) =====
  // 클라이언트 요청: 오른쪽 아래 소리 아이콘 없이 처음부터 소리와 함께 재생.
  // 로비 숏폼과 같은 MediaAudioService 로 화면에 보이는 영상 하나만 소리 켜고 재생하고, 벗어나면 멈춘다
  @ViewChildren('detailVideo') videoEls: QueryList<ElementRef<HTMLVideoElement>>;
  private videoObserver: IntersectionObserver | null = null;
  private videoSub: any;
  private viewActive = false;

  ngAfterViewInit() {
    this.videoSub = this.videoEls.changes.subscribe(() => this.observeVideos());
  }

  ionViewDidEnter() {
    this.viewActive = true;
    this.observeVideos();
  }

  private observeVideos() {
    if (this.videoObserver) this.videoObserver.disconnect();
    this.videoObserver = null;
    if (!this.viewActive || !this.videoEls || this.videoEls.length === 0) return;
    this.videoObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        const video = entry.target as HTMLVideoElement;
        if (entry.isIntersecting) this.audio.play(video);
        else this.audio.pause(video);
      });
    }, { threshold: 0.6 });
    this.videoEls.forEach(el => this.videoObserver!.observe(el.nativeElement));
  }

  private stopVideos() {
    if (this.videoObserver) this.videoObserver.disconnect();
    this.videoObserver = null;
    if (this.videoEls) this.videoEls.forEach(el => this.audio.pause(el.nativeElement));
  }

  /** 로그인한 스타 본인 글인지 (2-35차 — 수정·삭제 메뉴 노출). 실제 권한은 서버가 다시 확인한다 */
  get isOwnPost(): boolean {
    const myStarId = localStorage.getItem('starId') || '';
    return localStorage.getItem('isStar') === 'true' && !!myStarId
      && this.contentInfo?.FEED_TYPE === 'STAR' && String(this.contentInfo?.PRS_ID) === myStarId;
  }

  /** 본문 링크 탭 — 앱 안/인앱 브라우저로 연다 (2-35차) */
  onBodyClick(event: Event) {
    handleLinkClick(event, this.deepLink, this.router);
  }

  /** 사진 탭 → 전체 화면 뷰어 (2-35차 — 두 손가락 확대·축소) */
  async openPhotoViewer(index: number) {
    const urls = this.getPhotoList().map(m => m.displayUrl).filter(u => !!u);
    if (!urls.length) return;
    const modal = await this.modalCtrl.create({
      component: PhotoViewerComponent,
      componentProps: { urls, startIndex: Math.min(index, urls.length - 1) },
      cssClass: 'photo-viewer-modal'
    });
    await modal.present();
  }

  // 헤더 로고 탭 → 로비 첫 화면 (스타 페이지 goBack 과 동일한 동작, 2026-10-23 8번)
  goLobby() {
    this.navCtrl.navigateBack('/lobby', { state: { lobbyScrollTop: true } });
  }

  // 헤더 점점점 메뉴 (2-33차 — 아이콘만 있고 동작이 없던 버그 수정)
  async presentMenu(ev: Event) {
    const popover = await this.popoverCtrl.create({
      component: FeedMenuComponent,
      componentProps: { canEdit: this.isOwnPost },
      event: ev,
      translucent: false,
      showBackdrop: true,
      dismissOnSelect: true,
      cssClass: 'star-menu-popover'
    });
    await popover.present();
    const { data } = await popover.onDidDismiss();
    if (data && data.action) this.handleMenuAction(data.action);
  }

  handleMenuAction(action: string) {
    switch (action) {
      case 'search':
        this.navCtrl.navigateBack('/lobby');
        break;
      case 'refresh':
        this.loadFeedDetail();
        break;
      case 'share':
        this.shareContent();
        break;
      case 'report':
        this.openReportDialog();
        break;
      case 'edit':
        this.editPost();
        break;
      case 'delete':
        this.deletePost();
        break;
    }
  }

  // 수정 (2-35차) — 스타 페이지 연필 아이콘과 같은 작성판을 수정 모드로 연다. 영상은 멈춰 둔다
  editPost() {
    const starId = localStorage.getItem('starId') || '';
    this.pauseVideos();
    this.writeModalService.openEditModal(starId, this.conId, () => this.loadFeedDetail(), {
      // 저장 없이 닫아도 보이는 영상을 다시 튼다 (저장하면 다시 그려지며 자동으로 걸린다)
      onDismiss: () => this.observeVideos()
    });
  }

  // 삭제 (2-35차) — 스타 페이지 휴지통과 같은 API. 지우면 이전 화면으로 돌아간다
  async deletePost() {
    const alert = await this.alertCtrl.create({
      header: 'Delete Post',
      message: 'Are you sure you want to delete this post? This cannot be undone.',
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Delete',
          role: 'destructive',
          handler: () => {
            const payload = {
              starId: localStorage.getItem('starId') || '',
              starToken: localStorage.getItem('starToken') || '',
              conId: this.conId
            };
            this.http.post('/api/super/star/feed/delete', payload).subscribe((res: any) => {
              if (res && res.result === 'OK') {
                this.navCtrl.back();
              } else {
                this.showNotice((res && res.msg) || 'Failed to delete.');
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }

  private async showNotice(msg: string) {
    const alert = await this.alertCtrl.create({ header: 'Notice', message: msg, buttons: ['OK'] });
    await alert.present();
  }

  private pauseVideos() {
    this.stopVideos();
  }

  // 공유 — 스타 페이지의 글 공유와 같은 공개 주소(/post/{id})와 집계 API 를 쓴다
  async shareContent() {
    const link = `https://witch-hunting.com/post/${this.conId}`;
    try {
      await Share.share({ title: 'Star Platform', url: link });
      this.http.post('/api/super/share/add', { targetType: 'FEED', targetId: this.conId }).subscribe(() => { });
    } catch (e) {
      // 사용자가 공유 시트를 닫은 경우 — 조용히 무시
    }
  }

  // 신고 — 스타 페이지와 같은 메일 접수 방식, 대상에 글 번호를 함께 적는다
  async openReportDialog() {
    const alert = await this.alertCtrl.create({
      header: '🚨 Report Post',
      message: 'Please provide details for the report. Our team will review it shortly.',
      inputs: [{ name: 'reportContent', type: 'textarea', placeholder: 'e.g., Inappropriate photos, abusive language, etc.' }],
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Submit via Email',
          handler: (data) => {
            if (!data.reportContent || data.reportContent.trim() === '') return false;
            this.sendReportEmail(data.reportContent);
            return true;
          }
        }
      ]
    });
    await alert.present();
  }

  private sendReportEmail(content: string) {
    const email = 'witchhunting777@gmail.com';
    const subject = encodeURIComponent(`[StarPlatform Report] Report regarding post ${this.conId}`);
    const bodyText = `Please submit your report below.\n\n- Target Post: ${this.conId} (by ${this.starName || '-'})\n- Report Details:\n${content}\n\n------------------------\n* Please do not modify the form above for faster processing.`;
    window.location.href = `mailto:${email}?subject=${subject}&body=${encodeURIComponent(bodyText)}`;
  }

  /**
   * 뒤로 나갈 때 전면 광고를 시도한다.
   *
   * 이 화면에서 앞으로 나가는 경로가 없어 ionViewWillLeave 는 곧 "뒤로가기"와 같다.
   * 헤더의 ion-back-button, 스와이프 백, 안드로이드 하드웨어 백을 한 번에 덮으므로
   * 버튼에 (click)을 다는 것보다 빠짐이 없다.
   *
   * 여기에 호출을 넣는 이유: 기존 전면 광고 호출 4곳 중 상세 화면 담당은
   * detail-page(tutorial/:tutorialId, contents/:contentId)뿐이었는데, 로비·스타페이지에서
   * 글을 열면 실제로는 이 feed-detail 로 온다. 가장 왕래가 잦은 화면에 트리거가 없었다.
   *
   * 노출 여부는 AdMobService 의 빈도 정책이 판단한다 — 여기서는 조건을 따지지 않는다.
   * 웹·봇은 showInterstitial() 첫 줄의 isNativePlatform() 검사에서 걸러진다.
   */
  ionViewWillLeave() {
    this.viewActive = false;
    this.stopVideos();
    this.admob.showInterstitial('피드 상세 뒤로가기');
  }

  ngOnInit() {
    this.paramSub = this.route.paramMap.subscribe(params => {
      this.conId = params.get('conId');
      this.loadFeedDetail();
    });
  }

  ngOnDestroy() {
    if (this.paramSub) {
      this.paramSub.unsubscribe();
    }
    if (this.videoSub) this.videoSub.unsubscribe();
    this.stopVideos();
  }

  loadFeedDetail() {
    // 작성자 본인이면 검수 대기 이미지 접근 토큰을 받기 위해 starToken을 함께 보낸다(2-26차)
    this.http.post(`/api/super/feed/${this.conId}`, {
      starToken: localStorage.getItem('starToken') || ''
    }).subscribe((res: any) => {
      if (res.result !== 'OK') {
        this.notFound = true;
        this.isLoaded = true;
        return;
      }
      this.notFound = false;
      this.isLoaded = true;
      this.contentInfo = res.content;
      this.safeContentHtml = this.sanitizer.bypassSecurityTrustHtml(
        linkifyText(this.contentInfo?.CON_BODY || '')
      );

      // 수정으로 유튜브 주소를 지웠을 수 있다 — 새로고침 때 이전 값이 남지 않게 매번 다시 정한다
      this.safeYoutubeUrl = null;
      if (this.contentInfo.YOUTUBE_URL) {
         const embedUrl = this.convertToYoutubeEmbedUrl(this.contentInfo.YOUTUBE_URL);
         this.safeYoutubeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
      }

      // [핵심] 받아온 미디어 리스트를 순회하며, 비디오일 경우 초기 음소거 상태(true)를 넣어줍니다.
      // 검수 대기 글은 서버가 미디어 주소를 주지 않는다. 작성자 본인에게만
      // 단기 접근 토큰이 내려오므로 그것으로 원본을 그린다(2-26차)
      const pendingToken = res.pendingImageToken || null;
      const rawMedias = res.medias || [];
      const tokenUrl = pendingToken
        ? `${environment.apiBaseURL}/api/super/media/pending?t=${encodeURIComponent(pendingToken)}`
        : null;
      // 토큰 주소는 글당 하나이고 서버가 "사진이 있으면 사진, 없으면 영상 썸네일"을 돌려준다.
      // 사진이 같이 붙은 글에서는 영상 포스터로 쓰지 않는다 (사진이 포스터로 잘못 보이지 않게)
      const hasPhoto = rawMedias.some((m: any) => m.MEDIA_TYPE === 'PHOTO');
      this.mediaList = rawMedias.map((media: any) => {
          const isPending = media.MDR_STATUS === 'PENDING';

          if (media.MEDIA_TYPE === 'VIDEO') {
              media.isMuted = true; // 기본 음소거 상태 추가
              // 검수 대기 영상도 작성자 본인에게는 토큰 주소로 재생한다 (2-35차 — 2-32차엔 썸네일만이라 오류로 보였다).
              // 승인 전에는 서버가 MEDIA_URL·THUMB_URL 을 비우므로 토큰 주소가 썸네일이 된다
              media.isPendingOwn = isPending && !!tokenUrl;
              media.displayUrl = media.MEDIA_URL || (media.isPendingOwn ? `${tokenUrl}&kind=video` : null);
              media.posterUrl = media.THUMB_URL || (media.isPendingOwn && !hasPhoto ? tokenUrl : null);
              media.isUnderReview = isPending && !media.displayUrl;
              return media;
          }

          media.isPendingOwn = isPending && !!tokenUrl;
          media.displayUrl = media.MEDIA_URL || (media.isPendingOwn ? tokenUrl : null);
          media.isUnderReview = isPending && !media.displayUrl;
          return media;
      });


      if (res.content.PRS_NAME) this.starName = res.content.PRS_NAME;
      if (res.content.STORED_FILE_NM) this.starProfileImg = res.content.STORED_FILE_NM;
    });
  }

  handleImgError(ev: any) {
    ev.target.src = 'assets/img/defaultImg/avatar.svg';
  }

  // 🖼️ [신규] 이미지 목록만 필터링 (1순위)
  getPhotoList(): any[] {
    return this.mediaList.filter(m => m.MEDIA_TYPE === 'PHOTO');
  }

  // 🎥 [신규] 동영상 목록만 필터링 (2순위).
  // 검수 대기 글의 동영상은 주소가 가려지지만 목록에서 빼지 않는다 — 자리에 "Under review" 를 그린다(2-32차)
  getVideoList(): any[] {
    return this.mediaList.filter(m => m.MEDIA_TYPE === 'VIDEO' && (m.MEDIA_URL || m.MDR_STATUS === 'PENDING'));
  }

  // 🎬 YouTube URL을 embed URL로 변환
  convertToYoutubeEmbedUrl(youtubeUrl: string): string {
    if (!youtubeUrl) return '';

    // 다양한 YouTube URL 형식에서 VIDEO_ID 추출
    const patterns = [
      /(?:youtube\.com\/(?:watch\?v=|embed\/|v\/|shorts\/)|youtu\.be\/)([^&\n?#]+)/,
      /youtube\.com\/watch\?v=([^&]+)/,
      /m\.youtube\.com\/watch\?v=([^&]+)/,
    ];

    let videoId = '';
    for (const pattern of patterns) {
      const match = youtubeUrl.match(pattern);
      if (match && match[1]) {
        videoId = match[1];
        break;
      }
    }

    if (videoId) {
      // Use proxy page to avoid iOS YouTube 153 errors in Capacitor webview.
      return `https://witch-hunting.com/youtube.jsp?v=${videoId}`;
    }

    return '';
  }
}