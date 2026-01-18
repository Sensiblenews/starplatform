import { NavController } from '@ionic/angular';
import { Component } from '@angular/core';
import { HelperService } from '../../services/helper.service';
import { ContentService } from 'src/app/services/content.service';

@Component({
  selector: 'app-peter-pan',
  templateUrl: './peter-pan.page.html',
  styleUrls: ['./peter-pan.page.scss'],
})
export class PeterPanPage {

  constructor(
    private navController: NavController,
    private contentService: ContentService,
    private helper: HelperService,
  ) { }

  public get pageTitle(): string {
    return '피터팬';
  }

  moveBack() {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }

  public getContentSubscriber() {
    return (page: number, itemCount: number) =>
      this.contentService.getMonsterContents(page, itemCount);
  }
}
