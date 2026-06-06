import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Haptics, ImpactStyle } from '@capacitor/haptics';
import { HttpService } from '../../../../services/http.service';

@Component({
  selector: 'app-hall-of-fame-modal',
  templateUrl: './hall-of-fame-modal.component.html',
  styleUrls: ['./hall-of-fame-modal.component.scss']
})
export class HallOfFameModalComponent implements OnInit {
  public defaultAvatar = 'assets/img/defaultImg/avatar.svg';

  activeTab: 'daily' | 'top100' | 'monthly' | 'yearly' = 'daily';

  // Daily Kings
  dailyKings: any[] = [];
  dailyKingsLoading = false;

  // All-time Top 100
  top100Stars: any[] = [];
  top100Loading = false;

  // Monthly Champions
  monthlyStars: any[] = [];
  monthlyLoading = false;
  monthlyChampion: any = null;
  selectedYear: number;
  selectedMonth: number;
  availableYears: number[] = [];
  availableMonths: { value: number; label: string }[] = [
    { value: 1, label: 'Jan' }, { value: 2, label: 'Feb' }, { value: 3, label: 'Mar' },
    { value: 4, label: 'Apr' }, { value: 5, label: 'May' }, { value: 6, label: 'Jun' },
    { value: 7, label: 'Jul' }, { value: 8, label: 'Aug' }, { value: 9, label: 'Sep' },
    { value: 10, label: 'Oct' }, { value: 11, label: 'Nov' }, { value: 12, label: 'Dec' }
  ];

  // Yearly Champions
  yearlyStars: any[] = [];
  yearlyLoading = false;
  yearlyChampion: any = null;
  selectedYearForYearly: number;
  availableYearsForYearly: number[] = [];



  constructor(
    private modalCtrl: ModalController,
    private router: Router,
    private http: HttpService
  ) {
    const now = new Date();
    this.selectedYear = now.getFullYear();
    this.selectedMonth = now.getMonth() + 1;
    this.selectedYearForYearly = now.getFullYear();

    // Generate year range: 2024 ~ current year
    const currentYear = now.getFullYear();
    for (let y = currentYear; y >= 2024; y--) {
      this.availableYears.push(y);
      this.availableYearsForYearly.push(y);
    }
  }

  ngOnInit() {
    this.loadDailyKings();
  }

  close() {
    this.modalCtrl.dismiss();
  }

  // ── Tab Navigation ──────────────────────────────────────
  private tabOrder: ('daily' | 'top100' | 'monthly' | 'yearly')[] = ['daily', 'top100', 'monthly', 'yearly'];

  setTab(tab: 'daily' | 'top100' | 'monthly' | 'yearly') {
    this.activeTab = tab;
    this.loadTabData(tab);
  }



  private loadTabData(tab: string) {
    switch (tab) {
      case 'daily':
        if (this.dailyKings.length === 0) this.loadDailyKings();
        break;
      case 'top100':
        if (this.top100Stars.length === 0) this.loadTop100();
        break;
      case 'monthly':
        if (this.monthlyStars.length === 0) this.loadMonthly();
        break;
      case 'yearly':
        if (this.yearlyStars.length === 0) this.loadYearly();
        break;
    }
  }

  // ── 1. Daily Kings ──────────────────────────────────────
  loadDailyKings() {
    this.dailyKingsLoading = true;
    this.http.post('/api/super/hall-of-fame/daily-kings', {}).subscribe(
      (res: any) => {
        if (res.result === 'OK') {
          this.dailyKings = res.list || [];
        }
        this.dailyKingsLoading = false;
      },
      () => { this.dailyKingsLoading = false; }
    );
  }

  // ── 2. All-time Top 100 ─────────────────────────────────
  loadTop100() {
    this.top100Loading = true;
    this.http.post('/api/super/hall-of-fame/top100', {}).subscribe(
      (res: any) => {
        if (res.result === 'OK') {
          this.top100Stars = res.list || [];
        }
        this.top100Loading = false;
      },
      () => { this.top100Loading = false; }
    );
  }

  // ── 3. Monthly Champions ────────────────────────────────
  loadMonthly() {
    this.monthlyLoading = true;
    const targetMonth = `${this.selectedYear}-${String(this.selectedMonth).padStart(2, '0')}`;
    this.http.post('/api/super/hall-of-fame/monthly', { targetMonth }).subscribe(
      (res: any) => {
        if (res.result === 'OK') {
          this.monthlyStars = res.list || [];
          this.monthlyChampion = this.monthlyStars.length > 0 ? this.monthlyStars[0] : null;
        }
        this.monthlyLoading = false;
      },
      () => { this.monthlyLoading = false; }
    );
  }

  onMonthChanged() {
    this.monthlyStars = [];
    this.monthlyChampion = null;
    this.loadMonthly();
  }

  // ── 4. Yearly Champions ─────────────────────────────────
  loadYearly() {
    this.yearlyLoading = true;
    const targetYear = String(this.selectedYearForYearly);
    this.http.post('/api/super/hall-of-fame/yearly', { targetYear }).subscribe(
      (res: any) => {
        if (res.result === 'OK') {
          this.yearlyStars = res.list || [];
          this.yearlyChampion = this.yearlyStars.length > 0 ? this.yearlyStars[0] : null;
        }
        this.yearlyLoading = false;
      },
      () => { this.yearlyLoading = false; }
    );
  }

  onYearChanged() {
    this.yearlyStars = [];
    this.yearlyChampion = null;
    this.loadYearly();
  }

  // ── Helpers ─────────────────────────────────────────────
  getStarImage(imageUrl: string | null): string {
    return (imageUrl && imageUrl.length > 0) ? imageUrl : this.defaultAvatar;
  }

  handleImageError(event: any) {
    event.target.src = this.defaultAvatar;
  }

  formatNumber(num: number): string {
    if (!num) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    else if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    else return num.toString();
  }

  getMonthLabel(): string {
    const m = this.availableMonths.find(m => m.value === this.selectedMonth);
    return m ? m.label : '';
  }

  async goToStarPage(starId: string) {
    try { await Haptics.impact({ style: ImpactStyle.Light }); } catch (e) {}
    this.modalCtrl.dismiss();
    this.router.navigate(['/star', starId]);
  }
}
