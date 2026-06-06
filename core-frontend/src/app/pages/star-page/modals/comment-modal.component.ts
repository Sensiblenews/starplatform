import { Component, Input, OnInit } from '@angular/core';
import { ModalController, AlertController } from '@ionic/angular';
import { HttpService } from 'src/app/services/http.service';

@Component({
  selector: 'app-comment-modal',
  templateUrl: './comment-modal.component.html',
  styleUrls: ['./comment-modal.component.scss']
})
export class CommentModalComponent implements OnInit {
  @Input() type!: string; // 'star' 또는 'feed'
  @Input() targetId!: string | number; // 스타 ID 또는 피드 ID
  @Input() deviceId!: string;

  nickname = '';
  password = '';
  content = '';

  // API 연동 전 임시 데이터
  comments: any[] = [];
  allComments: any[] = [];
  blockedDeviceIds = new Set<string>();

  isAdmin: boolean = false;
  isStar: boolean = false;
  adminId: string = '';
  adminPw: string = '';
  starId: string = '';
  starToken: string = '';

  constructor(
    private modalCtrl: ModalController,
    private alertCtrl: AlertController,
    private http: HttpService
  ) {}

  ngOnInit() {
    this.isAdmin = localStorage.getItem('isAdmin') === 'true';
    this.adminId = localStorage.getItem('adminId') || '';
    this.adminPw = localStorage.getItem('adminPw') || '';

    // 🌟 [신규] 스타 로그인 정보 불러오기
    this.isStar = localStorage.getItem('isStar') === 'true';
    this.starId = localStorage.getItem('starId') || '';
    this.starToken = localStorage.getItem('starToken') || '';

    this.loadBlockedUsers();
    this.loadComments();
  }

  close() {
    this.modalCtrl.dismiss({ count: this.comments.length }); // 닫을 때 바뀐 댓글 갯수를 넘겨줌
  }

  loadComments() {
    const payload = { targetType: this.type.toUpperCase(), targetId: this.targetId };
    this.http.post('/api/super/comment/list', payload).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.allComments = res.comments || [];
        this.applyBlockFilter();
      }
    });
  }

  loadBlockedUsers() {
    const payload = { blockerDeviceId: this.deviceId };
    this.http.post('/api/super/block/list', payload).subscribe((res: any) => {
      if (res.result === 'OK') {
        const list = res.list || [];
        this.blockedDeviceIds = new Set(
          list
            .map((item: any) => item.blockedDeviceId)
            .filter((id: any) => typeof id === 'string' && id.length > 0)
        );
        this.applyBlockFilter();
      }
    });
  }

  applyBlockFilter() {
    if (this.blockedDeviceIds.size === 0) {
      this.comments = [...this.allComments];
      return;
    }

    this.comments = this.allComments.filter((comment) => {
      const authorDeviceId = this.getCommentDeviceId(comment);
      return !authorDeviceId || !this.blockedDeviceIds.has(authorDeviceId);
    });
  }

  async addComment() {
    if (!this.nickname || !this.password || !this.content) {
      const alert = await this.alertCtrl.create({ header: 'Alert', message: 'Please fill in all fields.', buttons: ['OK'] });
      return alert.present();
    }

    const payload = {
      targetType: this.type.toUpperCase(),
      targetId: this.targetId,
      deviceId: this.deviceId,
      nickname: this.nickname,
      password: this.password,
      content: this.content
    };

    this.http.post('/api/super/comment/add', payload).subscribe((res: any) => {
      if (res.result === 'OK') {
        this.content = ''; // 내용은 비우고
        this.loadComments(); // 리스트 새로고침
      } else if (res.result === 'FAIL') {
        this.showError(res.msg);
      } else {
        this.showError('Failed to register comment.');
      }
    });
  }

  async deleteComment(index: number) {
    
    // 1️⃣ 🌟 [수정] 관리자이거나 로그인된 스타(페이지 주인)일 경우: 프리패스 삭제 팝업
    if (this.isAdmin || this.isStar) {
      const roleName = this.isAdmin ? 'Admin' : 'Creator'; // 알림창 문구 동적 변경
      
      const alert = await this.alertCtrl.create({
        header: `Delete Comment`,
        message: `Are you sure you want to delete this comment?`,
        buttons: [
          { text: 'No', role: 'cancel' },
          {
            text: 'Yes',
            handler: () => {
              const payload = {
                cmtId: this.comments[index].CMT_ID,
                
                // 관리자 정보 (백엔드에서 isAdminCheck 플래그 확인)
                isAdminCheck: this.isAdmin, 
                adminId: this.adminId, 
                adminPw: this.adminPw,

                // 🌟 스타 정보 (백엔드에서 isStarCheck 플래그 확인)
                isStarCheck: this.isStar,
                starId: this.starId,
                starToken: this.starToken
              };
              
              this.http.post('/api/super/comment/delete', payload).subscribe((res: any) => {
                if (res.result === 'OK') {
                  this.loadComments();
                } else {
                  this.showError(res.msg || 'Failed to delete comment.');
                }
              });
            }
          }
        ]
      });
      await alert.present();
      return; // 프리패스 삭제 실행 후 즉시 종료
    }


    // 2️⃣ 일반 유저(Guest)일 경우: 기존 비밀번호 입력 팝업
    const alert = await this.alertCtrl.create({
      header: 'Delete Comment',
      inputs: [{ name: 'pw', type: 'password', placeholder: 'Type your password' }],
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Delete',
          handler: (data) => {
            const payload = {
              cmtId: this.comments[index].CMT_ID, 
              password: data.pw
            };
            
            this.http.post('/api/super/comment/delete', payload).subscribe((res: any) => {
              if (res.result === 'OK') {
                this.loadComments(); 
              } else {
                this.showError(res.msg); 
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }

  async showError(msg: string) {
    const alert = await this.alertCtrl.create({ header: 'Error', message: msg, buttons: ['Continue'] });
    await alert.present();
  }

  async showSuccess(msg: string) {
    const alert = await this.alertCtrl.create({ header: 'Success', message: msg, buttons: ['OK'] });
    await alert.present();
  }

  async reportComment(comment: any) {
    const alert = await this.alertCtrl.create({
      header: 'Report Comment',
      message: 'Please select a reporting reason.',
      inputs: [
        { name: 'reason', type: 'radio', label: 'Inappropriate Content', value: 'SPAM', checked: true },
        { name: 'reason', type: 'radio', label: 'Swearing/Insulting Statements', value: 'ABUSE' },
        { name: 'reason', type: 'radio', label: 'Advertising Post', value: 'AD' }
      ],
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Report',
          handler: (data) => {
            const payload = {
              cmtId: comment.CMT_ID,
              deviceId: this.deviceId,
              reason: data
            };
            this.http.post('/api/super/comment/report', payload).subscribe((res: any) => {
              this.showSuccess('Report submitted. We will review it and take action.');
            });
          }
        }
      ]
    });
    await alert.present();
  }

  async blockCommentUser(comment: any) {
    const blockedDeviceId = this.getCommentDeviceId(comment);
    const blockedNickname = this.getCommentNickname(comment);

    if (!blockedDeviceId) {
      return this.showError('Unable to block this user. Missing device ID.');
    }

    if (blockedDeviceId === this.deviceId) {
      return this.showError('You cannot block yourself.');
    }

    if (this.blockedDeviceIds.has(blockedDeviceId)) {
      return this.showError('This user is already blocked.');
    }

    const alert = await this.alertCtrl.create({
      header: 'Block User',
      message: `Block ${blockedNickname}? You will no longer see their comments.`,
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Block',
          handler: () => {
            const payload = {
              blockerDeviceId: this.deviceId,
              blockedDeviceId: blockedDeviceId,
              blockedNickname: blockedNickname
            };

            this.http.post('/api/super/block/add', payload).subscribe((res: any) => {
              if (res.result === 'OK') {
                this.blockedDeviceIds.add(blockedDeviceId);
                this.applyBlockFilter();
                this.showSuccess('User blocked.');
              } else {
                this.showError(res.msg || 'Failed to block user.');
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }

  getCommentDeviceId(comment: any): string | null {
    return comment?.deviceId || comment?.DEVICE_ID || null;
  }

  getCommentNickname(comment: any): string {
    return comment?.nickname || comment?.NICKNAME || 'this user';
  }
}