import { Component, Input, OnInit } from '@angular/core';
import { ModalController, AlertController } from '@ionic/angular';
import { HttpService } from 'src/app/services/http.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

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
  youtubeUrl: string = '';
  youtubeEmbedUrl: SafeResourceUrl | null = null;
  showYoutubeInput: boolean = false;

  // 🔄 로딩 상태
  isSubmitting: boolean = false;

  constructor(
    private modalCtrl: ModalController,
    private alertCtrl: AlertController,
    private http: HttpService,
    private sanitizer: DomSanitizer
  ) {}

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
      // 5MB 제한 체크
      const maxSizeInMB = 5;
      const maxSizeInBytes = maxSizeInMB * 1024 * 1024;
      
      if (file.size > maxSizeInBytes) {
        this.showError(`Video size must be less than ${maxSizeInMB}MB. Current size: ${(file.size / 1024 / 1024).toFixed(2)}MB`);
        return;
      }
      
      // 비디오 파일 형식 검증
      const videoFormats = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime', 'video/x-msvideo'];
      if (!videoFormats.includes(file.type)) {
        this.showError('Please select a valid video file (mp4, webm, ogg, mov, avi)');
        return;
      }
      
      this.selectedVideo = file;
      
      const reader = new FileReader();
      reader.onload = () => {
        this.videoPreview = reader.result;
        // 동영상 썸네일 추출
        this.extractVideoThumbnail(file);
      };
      reader.readAsDataURL(file);
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

    this.isSubmitting = true;

    // 🌟 스타인지 관리자인지에 따라 페이로드와 URL을 분기 처리
    const payload = this.isStar ? {
      prsId: this.starId,
      starPw: localStorage.getItem('starPw') || '', // 스타 본인 증명용
      feedText: this.content.trim(),
      starToken: localStorage.getItem('starToken') || '', // 스타 인증 토큰
      imageBase64: this.imagePreview || null,
      // 🎥 동영상과 YouTube URL을 각각 전송 가능
      videoBase64: this.videoPreview || null,
      youtubeUrl: this.youtubeUrl || null
    } : {
      adminId: localStorage.getItem('adminId') || '',
      adminPw: localStorage.getItem('adminPw') || '',
      adminLevel: this.adminLevel,
      content: this.content.trim(),
      imageBase64: this.imagePreview || null,
      // 🎥 동영상과 YouTube URL을 각각 전송 가능
      videoBase64: this.videoPreview || null,
      youtubeUrl: this.youtubeUrl || null
    };

    const targetUrl = this.isStar ? '/api/super/star/feed/add' : '/api/super/admin/feed/add';

    this.http.post(targetUrl, payload).subscribe({
      next: async (res: any) => {
        this.isSubmitting = false;
        if (res.result === 'OK') {
          this.modalCtrl.dismiss({ success: true });
        } else {
          this.showError(res.msg || 'Failed to post.');
        }
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error(err);
        this.showError('Server communication error occurred.');
      }
    });
  }

  async showError(msg: string) {
    const alert = await this.alertCtrl.create({ header: 'Error', message: msg, buttons: ['OK'] });
    await alert.present();
  }
}