import { Component, OnInit } from '@angular/core';
import { NavController } from '@ionic/angular';

@Component({
  selector: 'app-tinker-bell',
  templateUrl: './tinker-bell.page.html',
  styleUrls: ['./tinker-bell.page.scss'],
})
export class TinkerBellPage implements OnInit {
  // 화면에 보여줄 더미 아이템 (3개)
  dummyImages = [1, 2, 3];
  // 사용할 임시 이미지 경로 (assets 폴더에 해당 파일이 있어야 함)
  mockImageSrc = 'assets/temp.jpg';

  constructor(private navController: NavController) { }

  ngOnInit() {
  }

  moveBack() {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }

  onSubscribe() {
    console.log('Subscribe clicked');
    // 여기에 구독 페이지 이동 등의 로직 추가
    // this.navController.navigateForward('/subscribe');
  }
}