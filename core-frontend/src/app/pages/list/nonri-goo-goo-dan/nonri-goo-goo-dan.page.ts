import { NavController } from '@ionic/angular';
import { Component } from '@angular/core';
import { ContentService } from 'src/app/services/content.service';

@Component({
  selector: 'app-nonri-goo-goo-dan',
  templateUrl: './nonri-goo-goo-dan.page.html',
  styleUrls: ['./nonri-goo-goo-dan.page.scss'],
})
export class NonriGooGooDanPage {
  constructor(
    private contentService: ContentService,
    private navController: NavController,
  ) {}

  public getContentSubscriber() {
    return (page: number, itemCount: number) =>
      this.contentService.getNonriGooGooDan(page, itemCount);
  }

  moveBack() {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }
}
