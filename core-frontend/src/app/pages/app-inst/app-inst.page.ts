import { Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { ModalController, NavController } from '@ionic/angular';

@Component({
  selector: 'app-app-inst',
  templateUrl: './app-inst.page.html',
  styleUrls: ['./app-inst.page.scss'],
})
export class AppInstPage {
  slideOpts = {
    initialSlide: 0,
  };

  slideInfo = [
    {
      mainText: '하루 일과의 시작과 끝!',
      subText: `<span>스타플랫폼과</span> 함께 해요!`,
    },
    {
      mainText: '스타와 유명인들을 만나보세요',
      subText: `클릭만으로 후원도 가능해요!`,
    },
    {
      mainText: '누구나 수익창출이 가능해요',
      subText: `자동 저축 + 조회수 수익 + 이벤트 참여`,
    },
    {
      mainText: '실시간 채팅도 가능해요!',
      subText: `팔로워, 팔로잉 OK`,
    },
  ];

  constructor(private navCtrl: NavController, private router: Router) {}

  modalClose() {
    this.navCtrl.back();
  }

  goDetail(i) {
    this.router.navigate(['/tutorial', i]);
  }
}
