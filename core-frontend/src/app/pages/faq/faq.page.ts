import { HelperService } from './../../services/helper.service';
import { Component } from '@angular/core';
import { ContentService } from 'src/app/services/content.service';
import { NavController } from '@ionic/angular';

@Component({
  selector: 'app-faq',
  templateUrl: './faq.page.html',
  styleUrls: ['./faq.page.scss'],
})
export class FaqPage {
  public viewType = 'minimal';

  constructor(
    private contentService: ContentService,
    private navCtrl: NavController,
    private helper: HelperService,
  ) {}

  public getContentSubscriber() {
    return (page: number, itemCount: number) =>
      this.contentService.getFAQContents(page, itemCount);
  }

  async askFAQ() {
    await this.helper.sendEmail({
      to: 'witchhunting777@gmail.com',
      subject: '문의 사항',
      body: `
        스타플랫폼 서비스 이용 중 궁금한 점은 언제든지 이메일로 보내 주세요.<br>
      `,
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
