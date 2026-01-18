import { HelperService } from './../../services/helper.service';
import { Component } from '@angular/core';
import { ContentService } from 'src/app/services/content.service';
import { NavController } from '@ionic/angular';
@Component({
  selector: 'app-promotion-zone',
  templateUrl: './promotion-zone.page.html',
  styleUrls: ['./promotion-zone.page.scss'],
})
export class PromotionZonePage {
  public viewType = 'minimal';

  constructor(
    private contentService: ContentService,
    private navCtrl: NavController,
    private helper: HelperService,
  ) {}

  public getContentSubscriber() {
    return (page: number, itemCount: number) =>
      this.contentService.getPromotionContents(page, itemCount);
  }

  async sendContent() {
    await this.helper.sendEmail({
      to: 'witchhunting777@gmail.com',
      subject: '홍보문의',
      body: ``,
      isHtml: true,
    });
  }

  moveBack() {
    try {
      this.navCtrl.back();
    } catch (e) {
      this.navCtrl.navigateRoot('/');
    }
  }
}
