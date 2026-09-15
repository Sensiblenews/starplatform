import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AlertController, IonicModule, ModalController } from '@ionic/angular';
import { DmBlocked, DmService } from 'src/app/services/dm.service';
import { HelperService } from 'src/app/services/helper.service';

/** 차단 목록 모달 열기 (대화 목록 헤더) */
export async function openDmBlocked(modalCtrl: ModalController): Promise<boolean> {
  const modal = await modalCtrl.create({
    component: DmBlockedComponent,
  });
  await modal.present();
  const { data } = await modal.onDidDismiss();
  // 하나라도 해제했으면 대화 목록을 다시 불러야 한다
  return !!(data && data.changed);
}

/**
 * 차단한 사용자 목록 (2-28차).
 *
 * 스토어 심사(Apple 1.2)가 신고 기능과 함께 "차단 목록 관리·해제"를 점검한다.
 * 메신저는 스타 계정끼리만 쓰므로 일반 더보기 화면이 아니라 대화 목록 헤더에 둔다.
 */
@Component({
  selector: 'app-dm-blocked',
  standalone: true,
  imports: [CommonModule, IonicModule],
  templateUrl: './dm-blocked.component.html',
  styleUrls: ['./dm-blocked.component.scss'],
})
export class DmBlockedComponent implements OnInit {
  readonly defaultAvatar = 'assets/img/defaultImg/avatar.svg';
  blocked: DmBlocked[] = [];
  loaded = false;
  errorMsg = '';
  // 해제가 한 번이라도 있었는지. 닫을 때 대화 목록에 갱신이 필요한지 알려준다
  private changed = false;

  constructor(
    private modalCtrl: ModalController,
    private dm: DmService,
    private helper: HelperService,
    private alertCtrl: AlertController,
  ) { }

  ngOnInit() {
    this.load();
  }

  dismiss() {
    this.modalCtrl.dismiss({ changed: this.changed });
  }

  load() {
    this.dm.blockedList().subscribe({
      next: (res: any) => {
        if (res && res.result === 'OK') {
          this.blocked = res.blocked || [];
          this.errorMsg = '';
        } else {
          this.blocked = [];
          this.errorMsg = (res && res.msg) || 'Could not load the blocked list.';
        }
        this.loaded = true;
      },
      error: () => {
        this.loaded = true;
        this.errorMsg = 'Could not load the blocked list.';
      }
    });
  }

  async confirmUnblock(item: DmBlocked) {
    const alert = await this.alertCtrl.create({
      header: 'Unblock user',
      message: `Unblock ${item.peerName}? You will be able to exchange messages again.`,
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        { text: 'Unblock', handler: () => { this.unblock(item); } },
      ],
    });
    await alert.present();
  }

  private unblock(item: DmBlocked) {
    this.dm.unblock(item.peerId).subscribe({
      next: (res: any) => {
        if (res && res.result === 'OK') {
          // 서버가 지웠으므로 화면에서도 바로 뺀다 (다시 불러오면 스크롤이 튄다)
          this.blocked = this.blocked.filter(b => b.peerId !== item.peerId);
          this.changed = true;
          this.helper.toast('User unblocked.', 'middle');
        } else {
          this.helper.toast(res && res.msg ? res.msg : 'Could not unblock this user.', 'middle');
        }
      },
      error: () => {
        this.helper.toast('Could not unblock this user.', 'middle');
      }
    });
  }

  handleImageError(event: any) {
    if (event && event.target) event.target.src = this.defaultAvatar;
  }
}
