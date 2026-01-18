import { Component } from '@angular/core';
import { ContentService } from 'src/app/services/content.service';

@Component({
  selector: 'app-tinker-bell',
  templateUrl: './tinker-bell.page.html',
  styleUrls: ['./tinker-bell.page.scss'],
})
export class TinkerBellPage {

  constructor(
    private contentService: ContentService
  ) { }

  
  public getContentSubscriber() {
    return (page: number, itemCount: number) =>
      this.contentService.getExclusiveContents(page, itemCount);
  }

}