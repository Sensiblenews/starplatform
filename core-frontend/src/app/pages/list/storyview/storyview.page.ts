import { NavController } from '@ionic/angular';
import { Component } from '@angular/core';
import { ContentService } from 'src/app/services/content.service';
import { map } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-storyview',
  templateUrl: './storyview.page.html',
  styleUrls: ['./storyview.page.scss'],
})
export class StoryviewPage {
  public pageTitle = '스토리 상세보기';
  private category = 0;
  constructor(
    private contentService: ContentService,
    private route: ActivatedRoute,
    private navController: NavController,
  ) {
    this.route.paramMap
      .pipe(map((paramMap) => paramMap.get('viewId')))
      .subscribe((viewId) => {
        const contentType: number = Number(viewId) || 0;
        this.category = contentType;
      });
  }

  public getContentSubscriber() {
    return (page: number, itemCount: number) =>
      this.contentService.getStoryContents(this.category, page, itemCount);
  }

  moveBack() {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }
}
