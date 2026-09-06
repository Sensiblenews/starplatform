import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { IonicModule, ModalController } from '@ionic/angular';
import { DmRoom, DmService } from 'src/app/services/dm.service';
import { openDmChat } from '../dm-chat/dm-chat.component';

/** 대화 목록 모달 열기 (로비 헤더 아바타의 미읽음 점 탭) */
export async function openDmRooms(modalCtrl: ModalController, dm: DmService): Promise<void> {
  // 시트가 아니라 전체 높이 모달 (클라이언트 확정)
  const modal = await modalCtrl.create({
    component: DmRoomsComponent,
  });
  await modal.present();
  await modal.onDidDismiss();
  dm.refreshUnread();
}

/**
 * 대화 목록 (2-29차). 푸시에는 발신자 이름이 없으므로 수신자가 상대를 찾는 유일한 화면이다.
 * 살아 있는(폭파 전) 메시지가 있는 상대만 나온다.
 */
@Component({
  selector: 'app-dm-rooms',
  standalone: true,
  imports: [CommonModule, IonicModule],
  templateUrl: './dm-rooms.component.html',
  styleUrls: ['./dm-rooms.component.scss'],
})
export class DmRoomsComponent implements OnInit {
  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  rooms: DmRoom[] = [];
  loaded = false;
  // 서버가 거부한 사유 (토큰 만료 등). 빈 목록과 구분해 보여준다
  errorMsg = '';

  constructor(private modalCtrl: ModalController, private dm: DmService) { }

  ngOnInit() {
    this.load();
  }

  dismiss() {
    this.modalCtrl.dismiss();
  }

  load() {
    this.dm.rooms().subscribe({
      next: (res: any) => {
        if (res && res.result === 'OK') {
          this.rooms = res.rooms || [];
          this.errorMsg = '';
        } else {
          this.rooms = [];
          this.errorMsg = (res && res.msg) || 'Could not load messages.';
        }
        this.loaded = true;
      },
      error: () => {
        this.loaded = true;
        this.errorMsg = 'Could not load messages.';
      }
    });
  }

  preview(room: DmRoom): string {
    if (room.lastType === 'IMAGE') return '📷 Photo';
    if (room.lastType === 'VIDEO') return '🎬 Video';
    return room.lastText || '';
  }

  async open(room: DmRoom) {
    await openDmChat(this.modalCtrl, this.dm, { peerId: room.peerId, peerName: room.peerName, peerImage: room.peerImage });
    this.load();
  }

  handleImageError(event: any) {
    if (event && event.target) event.target.src = this.defaultAvatar;
  }
}
