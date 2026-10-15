import { Component, Input, OnInit } from '@angular/core';
import { ModalController, AlertController, ToastController } from '@ionic/angular';
import { HttpService } from 'src/app/services/http.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { HttpEventType } from '@angular/common/http';
import { FeedUploadService } from 'src/app/services/feed-upload.service';

@Component({
  selector: 'app-admin-write-modal',
  templateUrl: './admin-write-modal.component.html',
  styleUrls: [`./admin-write-modal.component.scss`]
})
export class AdminWriteModalComponent implements OnInit {
  @Input() adminLevel: string; 
  
  // 🌟 신규: 스타 여부 및 스타 ID 파라미터 추가
  @Input() isStar: boolean = false; 
  @Input() starId: string = '';

  content: string = '';
  selectedFile: File | null = null;
  imagePreview: string | ArrayBuffer | null = null;
  
  // 🎥 동영상 관련 필드
  selectedVideo: File | null = null;
  videoPreview: string | ArrayBuffer | null = null;
  videoThumbnail: string | null = null; // 동영상 썸네일 (첫 프레임)
  /** 동영상 첨부 상한(MB). 안내 문구와 검사가 같은 값을 쓴다 (2-33차: 5 → 15, 2-34차: 15 → 95 — 서버 압축 도입) */
  static readonly MAX_VIDEO_SIZE_MB = FeedUploadService.MAX_VIDEO_SIZE_MB;
  readonly maxVideoSizeMb = AdminWriteModalComponent.MAX_VIDEO_SIZE_MB;
  youtubeUrl: string = '';
  youtubeEmbedUrl: SafeResourceUrl | null = null;
  showYoutubeInput: boolean = false;

  // 🔄 로딩 상태
  isSubmitting: boolean = false;
  /** 동영상 전송 진행률(0~100). 동영상 업로드 중에만 숫자 (2-34차) */
  uploadProgress: number | null = null;
  /** 동영상 전송 실패 사유 — 모달을 닫지 않고 보여 주고 Post 로 다시 시도한다 */
  uploadError: string | null = null;

  constructor(
    private modalCtrl: ModalController,
    private alertCtrl: AlertController,
    private http: HttpService,
    private sanitizer: DomSanitizer,
    private toastCtrl: ToastController,
    private feedUpload: FeedUploadService
  ) {}

  /** 모달이 닫힌 뒤에도 결과를 알리기 위한 토스트 */
  private async showToast(message: string, color: string) {
    const toast = await this.toastCtrl.create({
      message: message,
      duration: 3500,
      position: 'bottom',
      color: color
    });
    await toast.present();
  }

  ngOnInit() {
    console.log(`[Write Modal] isStar: ${this.isStar} / Admin Level: ${this.adminLevel}`);
  }



  close() {
    this.modalCtrl.dismiss();
  }

  toggleYoutubeInput() {
    this.showYoutubeInput = !this.showYoutubeInput;
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreview = reader.result;
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage() {
    this.selectedFile = null;
    this.imagePreview = null;
  }

  // 🎥 동영상 파일 선택 핸들러
  onVideoFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // 용량 제한 체크 (2-33차 15MB → 2-34차 95MB. 서버가 재생용으로 압축한다)
      const maxSizeInMB = AdminWriteModalComponent.MAX_VIDEO_SIZE_MB;
      const maxSizeInBytes = maxSizeInMB * 1024 * 1024;
      
      if (file.size > maxSizeInBytes) {
        this.showError(`Video size must be less than ${maxSizeInMB}MB. Current size: ${(file.size / 1024 / 1024).toFixed(2)}MB`);
        return;
      }
      
      // 비디오 파일 형식 검증
      const videoFormats = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime', 'video/x-msvideo', 'video/3gpp', 'video/x-m4v'];
      if (!videoFormats.includes(file.type)) {
        this.showError('Please select a valid video file (mp4, webm, ogg, mov, avi)');
        return;
      }
      
      this.selectedVideo = file;
      this.uploadError = null;
      // 2-34차: 큰 영상을 data URL 로 읽지 않는다 (95MB → 127MB 문자열이 WebView 메모리를 잡아먹는다).
      // 미리보기는 첫 프레임 썸네일만 쓰고, 전송은 파일 그대로 multipart 로 한다
      this.videoPreview = file.name;
      this.extractVideoThumbnail(file);
    }
  }

  // 동영상의 첫 프레임을 썸네일로 추출
  private extractVideoThumbnail(file: File): void {
    const video = document.createElement('video');
    const objectUrl = URL.createObjectURL(file);
    video.src = objectUrl;
    video.onloadedmetadata = () => {
      video.currentTime = 0;
    };
    video.onseeked = () => {
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(video, 0, 0);
        canvas.toBlob((blob) => {
          if (blob) {
            const thumbReader = new FileReader();
            thumbReader.onload = () => {
              this.videoThumbnail = thumbReader.result as string;
            };
            thumbReader.readAsDataURL(blob);
          }
          // 메모리 정리
          URL.revokeObjectURL(objectUrl);
        });
      }
    };
  }

  // 🌐 YouTube URL 처리
  onYoutubeUrlChange(url: string) {
    this.youtubeUrl = url.trim();
    
    if (this.youtubeUrl) {
      this.youtubeEmbedUrl = this.extractYoutubeEmbedUrl(this.youtubeUrl);
      if (!this.youtubeEmbedUrl) {
        this.showError('Invalid YouTube URL. Please check and try again.');
      } else {
        this.showYoutubeInput = false;
      }
    } else {
      this.youtubeEmbedUrl = null;
    }
  }

  // YouTube URL을 embed URL로 변환
  private extractYoutubeEmbedUrl(url: string): SafeResourceUrl | null {
    const match = url.match(
      /(?:youtube\.com\/(?:watch\?v=|embed\/|v\/|shorts\/)|youtu\.be\/)([\w\-]{11})/
    );
    
    if (!match || !match[1]) return null;
    
    const videoId = match[1];
    const embedUrl = `https://www.youtube.com/embed/${videoId}`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
  }

  // 동영상 파일 제거
  removeVideo() {
    this.selectedVideo = null;
    this.videoPreview = null;
    this.videoThumbnail = null;
  }

  // YouTube URL 제거
  removeYoutube() {
    this.youtubeUrl = '';
    this.youtubeEmbedUrl = null;
    this.showYoutubeInput = false;
  }

  async submitPost() {
    if (!this.content.trim()) {
      const alert = await this.alertCtrl.create({ header: 'Notice', message: 'Please enter content.', buttons: ['OK'] });
      return alert.present();
    }

    // 🎥 스타 글 + 동영상은 파일 업로드 경로 (2-34차)
    if (this.isStar && this.selectedVideo) {
      return this.submitVideoPost();
    }

    this.isSubmitting = true;

    // 🌟 스타인지 관리자인지에 따라 페이로드와 URL을 분기 처리
    const payload = this.isStar ? {
      prsId: this.starId,
      starPw: localStorage.getItem('starPw') || '', // 스타 본인 증명용
      feedText: this.content.trim(),
      starToken: localStorage.getItem('starToken') || '', // 스타 인증 토큰
      imageBase64: this.imagePreview || null,
      // 동영상은 submitVideoPost(멀티파트)로 보낸다. 여기는 사진·유튜브·본문만
      videoBase64: null,
      youtubeUrl: this.youtubeUrl || null
    } : {
      adminId: localStorage.getItem('adminId') || '',
      adminPw: localStorage.getItem('adminPw') || '',
      adminLevel: this.adminLevel,
      content: this.content.trim(),
      imageBase64: this.imagePreview || null,
      // 관리자 피드 API 는 동영상을 받지 않는다 (서버가 무시하던 값 — 큰 base64 를 보내지 않는다)
      videoBase64: null,
      youtubeUrl: this.youtubeUrl || null
    };

    const targetUrl = this.isStar ? '/api/super/star/feed/add' : '/api/super/admin/feed/add';

    // 1. 백그라운드 업로드 시작 (컴포넌트가 사라져도 브라우저가 네트워크 작업을 진행함).
    //    ToastController는 앱 전역 서비스라 모달이 닫힌 뒤에도 결과를 띄울 수 있다.
    //    검수 안내 토스트는 클라이언트 요청으로 표시하지 않는다. 실패 안내만 남긴다.
    this.http.post(targetUrl, payload).subscribe({
      next: (res: any) => {
        console.log('[Background Upload] Success:', res);
        if (res.result !== 'OK') {
          console.error('[Background Upload] Server error:', res.msg);
          // 이미지 검증에서 거부되면 글이 저장되지 않는다. 조용히 사라지면 안 된다(2-26차)
          this.showToast(res.msg || 'Upload failed. Please try again.', 'danger');
        }
      },
      error: (err) => {
        console.error('[Background Upload] Request error:', err);
        this.showToast('Upload failed. Please check your connection.', 'danger');
      }
    });

    // 2. 사용자에게는 0.5초(500ms) 동안만 로딩 오버레이를 보여준 뒤 즉시 모달 닫기
    setTimeout(() => {
      this.isSubmitting = false;
      this.modalCtrl.dismiss({ success: true });
    }, 500);
  }

  /**
   * 동영상 글 (2-34차): 파일 그대로 전송하며 진행률을 보여 준다. 전송이 끝나면 서버가 작업 ID 를 주고
   * 압축·썸네일·게시물 생성은 백그라운드에서 한다. 모달은 전송 완료 시 닫고, 결과는 FeedUploadService 가 토스트로 알린다.
   * 전송이 실패하면 모달을 닫지 않고 사유를 보여 준다 — Post 를 다시 누르면 재시도.
   */
  private submitVideoPost() {
    const form = new FormData();
    form.append('video', this.selectedVideo as File, (this.selectedVideo as File).name);
    form.append('prsId', this.starId);
    form.append('starToken', localStorage.getItem('starToken') || '');
    form.append('feedText', this.content.trim());
    if (this.youtubeUrl) form.append('youtubeUrl', this.youtubeUrl);
    if (this.imagePreview) form.append('imageBase64', String(this.imagePreview));

    this.isSubmitting = true;
    this.uploadError = null;
    this.uploadProgress = 0;

    this.feedUpload.uploadStarVideo(form).subscribe({
      next: event => {
        const progress = FeedUploadService.progressOf(event);
        if (progress !== null) {
          this.uploadProgress = progress;
          return;
        }
        if (event.type !== HttpEventType.Response) return;
        const res: any = event.body;
        if (res && res.result === 'OK' && res.jobId) {
          this.feedUpload.watchJob(res.jobId);
          this.isSubmitting = false;
          this.uploadProgress = null;
          this.modalCtrl.dismiss({ success: true, processing: true });
        } else {
          this.failVideoUpload((res && res.msg) || 'Upload failed. Please try again.');
        }
      },
      error: err => {
        // 413 = 프록시 본문 한도 초과 (상한 검사를 통과했는데도 나면 네트워크 쪽 제한)
        const msg = err && err.status === 413
          ? `The video is too large. Please choose a video under ${this.maxVideoSizeMb}MB.`
          : 'Upload failed. Please check your connection and try again.';
        this.failVideoUpload(msg);
      },
    });
  }

  private failVideoUpload(message: string) {
    this.isSubmitting = false;
    this.uploadProgress = null;
    this.uploadError = message;
  }

  async showError(msg: string) {
    const alert = await this.alertCtrl.create({ header: 'Error', message: msg, buttons: ['OK'] });
    await alert.present();
  }
}