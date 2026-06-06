import { Component, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { PurchaseModalComponent } from './purchase-modal.component'; 
import { HttpService } from 'src/app/services/http.service';

@Component({
  selector: 'app-board-modal',
  templateUrl: './board-modal.component.html',
  styleUrls: ['./board-modal.component.scss']
})
export class BoardModalComponent implements OnInit {
  boardList: any[] = [];
  sortType: string = 'price_asc'; 

  constructor(
    private modalCtrl: ModalController,
    private http: HttpService
  ) {}

  ngOnInit() {
    this.loadBoardData();
  }

  closeModal() {
    this.modalCtrl.dismiss();
  }

  loadBoardData() {
    this.http.get(`/api/super/ipo/list?sortType=${this.sortType}`).subscribe((res: any) => {
      if(res.result === 'OK') this.boardList = res.list;
    });
  }

  sortList() {
    this.loadBoardData(); 
  }

  async openPurchaseModal(item: any) {
    const modal = await this.modalCtrl.create({
      component: PurchaseModalComponent,
      componentProps: {
        targetStar: item 
      }
    });
    await modal.present();
  }
}