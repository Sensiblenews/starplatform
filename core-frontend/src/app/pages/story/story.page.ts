import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { NavController } from '@ionic/angular';

@Component({
  selector: 'app-story',
  templateUrl: './story.page.html',
  styleUrls: ['./story.page.scss'],
})
export class StoryPage {
  public menuArr: any[] = [
    {
      img: 'assets/img/job-01.png',
      text: '싱글남',
    },
    {
      img: 'assets/img/job-02.png',
      text: '싱글녀',
    },
    {
      img: 'assets/img/job-03.png',
      text: '전문직',
    },
    {
      img: 'assets/img/job-04.png',
      text: '직장인',
    },
    {
      img: 'assets/img/job-05.png',
      text: '고교생',
    },
    {
      img: 'assets/img/job-06.png',
      text: '대학생',
    },
    {
      img: 'assets/img/job-07.png',
      text: '공무원',
    },
    {
      img: 'assets/img/job-08.png',
      text: 'CEO(사장님)',
    },
  ];

  constructor(public router: Router, private navCtrl: NavController) {}

  goStoryDetail(contentType: number) {
    this.router.navigate(['/more/story', contentType]);
  }

  moveBack() {
    this.navCtrl.back();
  }
}
