import { Component, Input, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { HttpService } from '../../../services/http.service';

@Component({
  selector: 'app-my-insight-modal',
  templateUrl: './my-insight-modal.component.html',
  styleUrls: ['./my-insight-modal.component.scss']
})
export class MyInsightModalComponent implements OnInit {
  @Input() starId: string = '';

  isLoading = true;

  today = { visitors: 0, likes: 0, favorites: 0 };
  month = { visitors: 0, likes: 0, favorites: 0 };
  total = { visitors: 0, likes: 0, favorites: 0 };

  // 🌟 [신규] 활동 유저 로그
  activeLogTab: 'VISITOR' | 'LIKE' | 'FAVORITE' = 'VISITOR';
  activityLogs: any[] = [];
  isLoadingLogs = false;

  constructor(
    private modalCtrl: ModalController,
    private http: HttpService
  ) {}

  ngOnInit() {
    this.loadInsight();
    this.loadActivityLogs('VISITOR');
  }

  close() {
    this.modalCtrl.dismiss();
  }

  loadInsight() {
    this.isLoading = true;
    const starToken = localStorage.getItem('starToken') || '';

    this.http.post('/api/super/my-insight', {
      starId: this.starId,
      starToken: starToken
    }).subscribe(
      (res: any) => {
        if (res.result === 'OK') {
          this.today = res.today || this.today;
          this.month = res.month || this.month;
          this.total = res.total || this.total;
        }
        this.isLoading = false;
      },
      () => { this.isLoading = false; }
    );
  }

  // 🌟 [신규] 탭 전환 시 활동 로그 재로드
  switchLogTab(tab: 'VISITOR' | 'LIKE' | 'FAVORITE') {
    if (this.activeLogTab === tab) return;
    this.activeLogTab = tab;
    this.loadActivityLogs(tab);
  }

  // 🌟 [신규] 활동 유저 500명 로그 조회
  loadActivityLogs(type: string) {
    this.isLoadingLogs = true;
    this.activityLogs = [];
    const starToken = localStorage.getItem('starToken') || '';

    this.http.post('/api/super/my-insight/logs', {
      starId: this.starId,
      starToken: starToken,
      type: type
    }).subscribe({
      next: (res: any) => {
        if (res.result === 'OK') {
          this.activityLogs = res.list || [];
        }
        this.isLoadingLogs = false;
      },
      error: () => {
        this.isLoadingLogs = false;
      }
    });
  }

  formatNumber(num: number): string {
    if (num == null) return '0';
    return num.toLocaleString();
  }

  // 🌟 [신규] 기본 아바타
  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';

  handleImageError(event: any) {
    event.target.src = this.defaultAvatar;
  }
}
