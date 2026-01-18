import { HelperService } from './../../../services/helper.service';
import { AuthService } from './../../../services/auth.service';
import { Component } from '@angular/core';
import { ContentService } from 'src/app/services/content.service';
import { WriteModalPage } from 'src/app/modals/write-modal/write-modal.page';
@Component({
  selector: 'app-fairy',
  templateUrl: './fairy.page.html',
  styleUrls: ['./fairy.page.scss'],
})
export class FairyPage {
  constructor(
    private contentService: ContentService,
    private helper: HelperService,
    private auth: AuthService,
  ) {}

  public get isAuthed(): boolean {
    return this.auth.isAuthed();
  }

  public getContentSubscriber() {
    return (page: number, itemCount: number) =>
      this.contentService.getFairyContents(page, itemCount);
  }

  async doWrite() {
    const modal = await this.helper.callModal({
      component: WriteModalPage,
    });
    return await modal.present();
  }
}
