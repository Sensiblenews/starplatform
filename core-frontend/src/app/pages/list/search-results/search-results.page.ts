import { NavController } from '@ionic/angular';
import { Component } from '@angular/core';
import { ContentService } from 'src/app/services/content.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-search-results',
  templateUrl: './search-results.page.html',
  styleUrls: ['./search-results.page.scss'],
})
export class SearchResultsPage {
  private keyword: string = null;
  constructor(
    private contentService: ContentService,
    private router: Router,
    private navController: NavController,
  ) {
    const keyword: string =
      this.router?.getCurrentNavigation()?.extras?.state?.keyword || null;
    if (!keyword) {
      this.moveBack();
    }
    this.keyword = keyword;
  }

  public get pageTitle(): string {
    return this.keyword ? `'${this.keyword}' 검색 결과` : '검색 결과 없음';
  }

  public getContentSubscriber() {
    return (page: number, itemCount: number) =>
      this.contentService.getSearchResults(page, this.keyword, itemCount);
  }

  moveBack() {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }
}
