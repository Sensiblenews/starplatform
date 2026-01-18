import { HelperService } from '../../services/helper.service';
import { AuthService } from '../../services/auth.service';
import { Component } from '@angular/core';
import { ContentService } from 'src/app/services/content.service';
import { WriteModalPage } from 'src/app/modals/write-modal/write-modal.page';
import { NavController } from '@ionic/angular';
@Component({
  selector: 'app-monster',
  templateUrl: './monster.page.html',
  styleUrls: ['./monster.page.scss'],
})
export class MonsterPage {
  public loaded = false;
  public contentList = [];
  constructor(
    private contentService: ContentService,
    private helper: HelperService,
    private auth: AuthService,
    private navController: NavController,
  ) {}

  public get isAuthed(): boolean {
    return this.auth.isAuthed();
  }

  public get pageTitle(): string {
    return 'Posting';
  }

  public getContentSubscriber() {
    return (page: number, itemCount: number) =>
      this.contentService.getGoblinContents(page, itemCount);
  }

  async doWrite() {
    const modal = await this.helper.callModal({
      component: WriteModalPage,
    });
    return await modal.present();
  }

  moveBack() {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }
}
