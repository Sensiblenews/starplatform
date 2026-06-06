import { Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Haptics, ImpactStyle } from '@capacitor/haptics';

@Component({
  selector: 'app-revenue-ranking-modal',
  templateUrl: './revenue-ranking-modal.component.html',
  styleUrls: ['./revenue-ranking-modal.component.scss']
})
export class RevenueRankingModalComponent implements OnInit {
  @Input() stars: any[] = [];
  public defaultAvatar = 'assets/img/defaultImg/avatar.svg';

  constructor(
    private modalCtrl: ModalController,
    private router: Router
  ) {}

  ngOnInit() {}

  close() {
    this.modalCtrl.dismiss();
  }

  getStarImage(imageUrl: string | null): string {
    return (imageUrl && imageUrl.length > 0) ? imageUrl : this.defaultAvatar;
  }

  handleImageError(event: any) {
    event.target.src = this.defaultAvatar;
  }

  getPoints(views: number): number {
    return Math.floor((views || 0) / 10);
  }

  formatNumber(num: number): string {
    if (!num) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    else if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    else return num.toString();
  }

  async goToStarPage(starId: string) {
    try { await Haptics.impact({ style: ImpactStyle.Light }); } catch (e) {}
    this.modalCtrl.dismiss();
    this.router.navigate(['/star', starId]);
  }
}
