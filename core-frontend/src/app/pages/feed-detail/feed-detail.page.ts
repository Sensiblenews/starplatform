import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpService } from '../../services/http.service';
import { DomSanitizer, SafeHtml, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-feed-detail',
  templateUrl: './feed-detail.page.html',
  styleUrls: ['./feed-detail.page.scss'],
})
export class FeedDetailPage implements OnInit {
  conId: string;
  contentInfo: any = {};
  mediaList: any[] = [];
  
  starName: string = 'Unknown';
  starProfileImg: string = 'assets/img/defaultImg/avatar.svg';

  // [신규] 안전하게 변환된 유튜브 URL을 담을 변수
  safeYoutubeUrl: SafeResourceUrl | null = null;
  safeContentHtml: SafeHtml | null = null;

  constructor(
    private route: ActivatedRoute,
    private http: HttpService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    this.conId = this.route.snapshot.paramMap.get('conId');
    this.loadFeedDetail();
  }

  loadFeedDetail() {
    this.http.post(`/api/super/feed/${this.conId}`, {}).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.contentInfo = res.content;
        this.safeContentHtml = this.sanitizer.bypassSecurityTrustHtml(
          this.linkifyContent(this.contentInfo?.CON_BODY || '')
        );

        if (this.contentInfo.YOUTUBE_URL) {
           const embedUrl = this.convertToYoutubeEmbedUrl(this.contentInfo.YOUTUBE_URL);
           this.safeYoutubeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
        }
        
        // [핵심] 받아온 미디어 리스트를 순회하며, 비디오일 경우 초기 음소거 상태(true)를 넣어줍니다.
        const rawMedias = res.medias || [];
        this.mediaList = rawMedias.map((media: any) => {
            if (media.MEDIA_TYPE === 'VIDEO') {
                media.isMuted = true; // 기본 음소거 상태 추가
            }
            return media;
        });
        
        if (res.content.PRS_NAME) this.starName = res.content.PRS_NAME;
        if (res.content.STORED_FILE_NM) this.starProfileImg = res.content.STORED_FILE_NM;
      }
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

  // 🎥 [신규] 동영상 목록만 필터링 (2순위)
  getVideoList(): any[] {
    return this.mediaList.filter(m => m.MEDIA_TYPE === 'VIDEO');
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