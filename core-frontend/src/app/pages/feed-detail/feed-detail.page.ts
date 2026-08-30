import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpService } from '../../services/http.service';
import { environment } from 'src/environments/environment';
import { DomSanitizer, SafeHtml, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-feed-detail',
  templateUrl: './feed-detail.page.html',
  styleUrls: ['./feed-detail.page.scss'],
})
export class FeedDetailPage implements OnInit, OnDestroy {
  conId: string;
  contentInfo: any = {};
  mediaList: any[] = [];
  
  starName: string = 'Unknown';
  starProfileImg: string = 'assets/img/defaultImg/avatar.svg';

  // [신규] 안전하게 변환된 유튜브 URL을 담을 변수
  safeYoutubeUrl: SafeResourceUrl | null = null;
  safeContentHtml: SafeHtml | null = null;

  // 검수 대기 글을 타인이 딥링크로 열면 서버가 FAIL을 준다(2-27차). 빈 화면 대신 안내를 띄운다
  notFound = false;

  private paramSub: any;

  constructor(
    private route: ActivatedRoute,
    private http: HttpService,
    private sanitizer: DomSanitizer
  ) {}

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
  }

  loadFeedDetail() {
    // 작성자 본인이면 검수 대기 이미지 접근 토큰을 받기 위해 starToken을 함께 보낸다(2-26차)
    this.http.post(`/api/super/feed/${this.conId}`, {
      starToken: localStorage.getItem('starToken') || ''
    }).subscribe((res: any) => {
      if (res.result !== 'OK') {
        this.notFound = true;
        return;
      }
      this.notFound = false;
      this.contentInfo = res.content;
      this.safeContentHtml = this.sanitizer.bypassSecurityTrustHtml(
        this.linkifyContent(this.contentInfo?.CON_BODY || '')
      );

      if (this.contentInfo.YOUTUBE_URL) {
         const embedUrl = this.convertToYoutubeEmbedUrl(this.contentInfo.YOUTUBE_URL);
         this.safeYoutubeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
      }

      // [핵심] 받아온 미디어 리스트를 순회하며, 비디오일 경우 초기 음소거 상태(true)를 넣어줍니다.
      // 검수 대기 글은 서버가 미디어 주소를 주지 않는다. 작성자 본인에게만
      // 단기 접근 토큰이 내려오므로 그것으로 원본을 그린다(2-26차)
      const pendingToken = res.pendingImageToken || null;
      const rawMedias = res.medias || [];
      this.mediaList = rawMedias.map((media: any) => {
          if (media.MEDIA_TYPE === 'VIDEO') {
              media.isMuted = true; // 기본 음소거 상태 추가
          }

          const isPending = media.MDR_STATUS === 'PENDING';
          media.isPendingOwn = isPending && media.MEDIA_TYPE === 'PHOTO' && !!pendingToken;
          media.displayUrl = media.MEDIA_URL
            || (media.isPendingOwn
                ? `${environment.apiBaseURL}/api/super/media/pending?t=${encodeURIComponent(pendingToken)}`
                : null);
          media.isUnderReview = isPending && !media.displayUrl;
          return media;
      });


      if (res.content.PRS_NAME) this.starName = res.content.PRS_NAME;
      if (res.content.STORED_FILE_NM) this.starProfileImg = res.content.STORED_FILE_NM;
    });
  }

  toggleSound(media: any, event: Event) {
    event.stopPropagation(); // 버블링 방지
    media.isMuted = !media.isMuted;
  }
  
  handleImgError(ev: any) {
    ev.target.src = 'assets/img/defaultImg/avatar.svg';
  }

  private linkifyContent(text: string): string {
    const escaped = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');

    const withLinks = escaped.replace(
      /(https?:\/\/[^\s<]+)/g,
      '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>'
    );

    return withLinks.replace(/\n/g, '<br />');
  }

  // 🖼️ [신규] 이미지 목록만 필터링 (1순위)
  getPhotoList(): any[] {
    return this.mediaList.filter(m => m.MEDIA_TYPE === 'PHOTO');
  }

  // 🎥 [신규] 동영상 목록만 필터링 (2순위).
  // 검수 대기 글의 동영상은 주소가 가려져 재생할 수 없으므로 목록에서 뺀다
  getVideoList(): any[] {
    return this.mediaList.filter(m => m.MEDIA_TYPE === 'VIDEO' && m.MEDIA_URL);
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