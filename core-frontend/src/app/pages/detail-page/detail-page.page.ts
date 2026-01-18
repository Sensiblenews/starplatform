import { AfterViewInit, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ActionSheetController, IonContent, NavController } from '@ionic/angular';
import { App as CapacitorApp } from '@capacitor/app';
import { map } from 'rxjs/operators';
import { ItemCountPopoverComponent } from 'src/app/components/item-count-popover/item-count-popover.component';
import { CONTENT_TYPES } from 'src/app/constants/ContentTypes';
import { AdMobService } from 'src/app/services/ad-mob.service';
import { ContentService } from 'src/app/services/content.service';
import { UserInfo } from 'src/app/types/Auth';
import {
  BlockUser,
  BlockUserResponse,
  Comment,
  Content,
} from 'src/app/types/Content';
import { WriteModalPage } from './../../modals/write-modal/write-modal.page';
import { AuthService } from './../../services/auth.service';
import { ContentManagerService } from './../../services/content-manager.service';
import { HelperService } from './../../services/helper.service';

@Component({
  selector: 'app-detail-page',
  templateUrl: './detail-page.page.html',
  styleUrls: ['./detail-page.page.scss'],
})
export class DetailPagePage implements OnInit, AfterViewInit, OnDestroy {
  public content: Content = null;
  public commentInput = '';
  public tempContent: string;
  public comments: Comment[] = [];
  private contentApiMap = {
    content: 'viewContentDetails',
    tutorial: 'getInstDetail',
  };
  public defaultAvatar: string = '../../../assets/img/defaultImg/avatar.svg';
  private backButtonListener: any;
  @ViewChild(IonContent) ionContent: IonContent;

  constructor(
    public navController: NavController,
    private router: Router,
    private route: ActivatedRoute,
    public actionSheetController: ActionSheetController,
    private contentService: ContentService,
    private auth: AuthService,
    private helper: HelperService,
    private cm: ContentManagerService,
    private admob: AdMobService,
  ) {}

  ngOnInit() {
    this.route.paramMap
      .pipe(
        map((paramMap) => {
          const contentId = paramMap.get('contentId');
          if (contentId) {
            return { type: 'content', CON_ID: contentId };
          }
          const tutorialId = paramMap.get('tutorialId');
          if (tutorialId) {
            return { type: 'tutorial', CON_ID: tutorialId };
          }
        }),
      )
      .subscribe((contentInfo) => this.checkContentAvailable(contentInfo));
  }

  ngAfterViewInit(): void {
    this.backButtonListener = CapacitorApp.addListener('backButton', ({ canGoBack }) => {
      if (document.fullscreenElement) {
        this.exitFullscreen();
        // prevent page pop
      } else if (canGoBack) {
        // allow default behavior (go back)
        history.back();
      }
    });
  }

  ngOnDestroy(): void {
    this.backButtonListener?.remove();
  }

  // 동영상 재생 시 전체화면 전환
  onVideoPlay(videoElement: HTMLVideoElement): void {
    if (videoElement.requestFullscreen) {
      videoElement.requestFullscreen();
    }
  }

  exitFullscreen() {
    if (document.fullscreenElement) {
      document.exitFullscreen();
    } else if ((document as any).webkitExitFullscreen) {
      (document as any).webkitExitFullscreen();
    }
  }

  async checkContentAvailable({
    type,
    CON_ID,
    updated = false,
    contentData = null,
  }): Promise<void> {
    try {
      await this.helper.loading();
      if (!CON_ID) {
        throw new Error('컨텐츠조회에 실패하였습니다. 다시 시도해주세요.');
      }

      const contentSubscribeFn = this.contentApiMap[type];
      if (!contentSubscribeFn) {
        throw new Error('올바른 컨텐츠 타입이 아닙니다.');
      }

      let recievedContent: Content;
      if (updated) {
        recievedContent = { ...this.content, ...contentData };
      } else {
        const contentDetails =
          await this.contentService[contentSubscribeFn](CON_ID).toPromise();
        recievedContent = contentDetails.contents as Content;
        const { MEM_STAR, MEM_STAR_CHG }: UserInfo = contentDetails.result;
        this.auth.updateUserProperty({ MEM_STAR, MEM_STAR_CHG });
      }

      console.log(recievedContent);
      this.content = recievedContent;

      this.comments = recievedContent.comments?.length
        ? recievedContent.comments.reverse()
        : [];

      this.cm.setChange({
        action: 'changed',
        content: {
          CON_ID: recievedContent.CON_ID,
          CON_THUMNAIL: recievedContent.CON_THUMNAIL,
          PRICE: recievedContent.PRICE,
          PRICE_CHG: recievedContent.PRICE_CHG,
          HEART: recievedContent.HEART,
          HEART_CHG: recievedContent.HEART_CHG,
          CON_EXPD: recievedContent.CON_EXPD,
          CON_META_YN: recievedContent.CON_META_YN,
          CON_WRITE_YN: recievedContent.CON_WRITE_YN,
          CON_PRICE: recievedContent.CON_PRICE,
        },
      });

      const tab = this.route.snapshot.queryParamMap.get('tab');
      if (tab === 'comment') {
        setTimeout(() => this.scrollToComments(), 300);
      }
    } catch (error) {
      this.helper.alert({ message: error.message });
      this.router.navigate(['/'], { relativeTo: this.route });
      return;
    } finally {
      await this.helper.loadEnd();
    }
  }

  moveBack(): void {
    try {
      this.navController.back();
    } catch (e) {
      this.navController.navigateRoot('/');
    }
  }

  async onClickBack(): Promise<void> {
    this.moveBack();
    await this.admob.showInterstitial();
  }

  openPictureURL(picURL: string): void {
    return this.helper.openURL({ url: picURL });
  }

  async shareContent(): Promise<void> {
    const { CON_TITLE, CON_THUMNAIL } = this.content;
    const title = CON_TITLE.split('☆')[0];
    await this.helper.share({
      subject: '스타플랫폼',
      message: `\r\n${title}\r\nhttps://witch-hunting.com${this.router.url}\r\n`,
    });
  }

  async checkCount(type: 'star' | 'heart'): Promise<void> {
    const countInfo = {
      type,
      free: 0,
      charged: 0,
      earning: 0,
    };

    if (type === 'star') {
      const { PRICE, PRICE_CHG } = this.content;
      countInfo.free = PRICE;
      countInfo.charged = PRICE_CHG;
    }

    if (type === 'heart') {
      const { HEART, HEART_CHG } = this.content;
      countInfo.free = HEART;
      countInfo.charged = HEART_CHG;
    }

    this.helper.popOver({
      component: ItemCountPopoverComponent,
      componentProps: { countInfo },
    });
  }

  async giveHeart(heartCount: number): Promise<void> {
    try {
      const { MEM_ID, MEM_STAR, MEM_STAR_CHG } = this.auth.getUser();
      const { CON_ID } = this.content;

      if (MEM_STAR + MEM_STAR_CHG < heartCount) {
        const noPointAlert = await this.helper.alert({
          header: '앗! 별이 부족합니다.',
          message: '별똥별 이동해 보세요!',
          buttons: [
            { text: '아니오', role: 'cancel' },
            {
              text: '네',
              handler: () => this.router.navigate(['/more/charge-star']),
            },
          ],
        });
        return noPointAlert.present();
      }

      // TODO: asdf
      const heartResult = await this.contentService
        .postHeart({ MEM_ID, CON_ID, HEART: heartCount,  })
        .toPromise();

      this.content.HEART = heartResult.HEART;
      this.content.HEART_CHG = heartResult.HEART_CHG;

      this.cm.setChange({
        action: 'changed',
        content: {
          CON_ID,
          HEART: heartResult.HEART,
          HEART_CHG: heartResult.HEART_CHG,
        },
      });

      this.auth.updateUserProperty({
        MEM_STAR: heartResult.MEM_STAR,
        MEM_STAR_CHG: heartResult.MEM_STAR_CHG,
      });

      const sendHeartAlert = await this.helper.alert({
        cssClass: 'my-custom-class',
        header: '감사합니다!',
        message: `하트 ${heartCount} 개가 전달됐습니다.^^`,
        buttons: ['확인'],
      });
      sendHeartAlert.present();
    } catch (error) {
      this.helper.toast(error.message, 'middle');
    }
  }

  async commentClick(comment: Comment): Promise<void> {
    if (!this.auth.checkUserProperty('MEM_ID', comment.MEM_ID)) {
      return;
    }

    const actionSheet = await this.actionSheetController.create({
      cssClass: 'commentActionSheet',
      buttons: [
        {
          text: '수정하기',
          role: 'update',
        },
        {
          text: '삭제하기',
          role: 'delete',
        },
        {
          text: '취소',
          role: 'cancel',
        },
      ],
    });
    await actionSheet.present();
    const { role } = await actionSheet.onDidDismiss();
    switch (role) {
      case 'update':
        return await this.confirmUpdateComment(comment);
      case 'delete':
        return await this.confirmDeleteComment(comment);
      default:
        return;
    }
  }

  validateCommentText(commentText: string): string {
    const message = commentText.replace(/\s/gi, '');
    const MAX_LENGTH = 30;
    if (!message.length) {
      throw new Error('댓글을 입력해 주세요.');
    }
    if (message.length < 2) {
      throw new Error('글자수가 너무 적습니다.');
    }
    return message.length > MAX_LENGTH
      ? message.substring(MAX_LENGTH, message.length)
      : message;
  }

  async confirmUpdateComment(comment: Comment): Promise<void> {
    if (!this.auth.checkUserProperty('MEM_ID', comment.MEM_ID)) {
      return;
    }
    const handleUpdate = (data) => {
      try {
        if (!data?.comment || typeof data.comment !== 'string') {
          throw new Error('댓글을 올바르게 입력해 주세요.');
        }
        this.validateCommentText(data.comment);
        this.UpdateComment({ ...comment, ...{ COM_BODY: data.comment } });
        return true;
      } catch (error) {
        this.helper.toast(error.message, 'middle');
        return false;
      }
    };
    const editModal = await this.helper.alert({
      header: '댓글 수정',
      subHeader: '댓글 수정 후 확인 버튼을 눌러주세요',
      inputs: [
        {
          name: 'comment',
          placeholder: '내용을 입력 해주세요',
          type: 'text',
          value: comment.COM_BODY,
        },
      ],
      buttons: [
        { text: '취소', role: 'cancel' },
        { text: '수정', handler: handleUpdate },
      ],
    });
    editModal.present();
  }

  async confirmDeleteComment(comment: Comment): Promise<void> {
    if (!this.auth.checkUserProperty('MEM_ID', comment.MEM_ID)) {
      return;
    }
    const alert = await this.helper.alert({
      header: '댓글 삭제',
      subHeader: '댓글을 삭제하시겠습니까?',
      message: '한번 지워진 댓글은 복구할 수 없습니다.',
      buttons: [
        { text: '취소', role: 'cancel' },
        { text: '삭제', role: 'delete' },
      ],
    });
    alert.present();
    const { role } = await alert.onDidDismiss();
    if ('delete' !== role) {
      return;
    }
    return this.DeleteComment(comment);
  }

  async UpdateComment(comment: Comment): Promise<void> {
    try {
      const { COM_ID, CON_ID, COM_BODY, MEM_ID } = comment;
      if (!this.auth.checkUserProperty('MEM_ID', comment.MEM_ID)) {
        throw new Error('다른 유저의 메시지는 수정하실 수 없습니다.');
      }
      await this.helper.loading();
      const commentBody: Comment = {
        COM_ID,
        CON_ID,
        COM_BODY,
        MEM_ID,
      };

      const commentRs: Comment[] = await this.contentService
        .updateComment(commentBody)
        .toPromise();

      this.comments = commentRs.reverse();
      this.helper.toast('댓글이 수정되었습니다', 'middle');
    } catch (error) {
      this.helper.alert({ message: error.message });
    } finally {
      await this.helper.loadEnd();
    }
  }

  async DeleteComment(comment: Comment): Promise<void> {
    try {
      const { COM_ID, CON_ID, MEM_ID } = comment;
      if (!this.auth.checkUserProperty('MEM_ID', comment.MEM_ID)) {
        throw new Error('다른 유저의 메시지는 삭제하실 수 없습니다.');
      }
      await this.helper.loading();
      const commentBody: Comment = {
        COM_ID,
        CON_ID,
      };
      const commentRs: Comment[] = await this.contentService
        .deleteComment(commentBody)
        .toPromise();
      this.comments = commentRs.reverse();
      this.helper.toast('댓글이 삭제되었습니다', 'middle');
    } catch (error) {
      this.helper.alert({ message: error.message });
    } finally {
      await this.helper.loadEnd();
    }
  }

  async postComment(): Promise<void> {
    try {
      const message = this.validateCommentText(this.commentInput);
      await this.helper.loading();
      const MEM_ID = this.auth.getUserProperty('MEM_ID') as string;
      const commentBody: Comment = {
        CON_ID: this.content.CON_ID,
        COM_BODY: message,
        MEM_ID,
      };
      const commentRs: Comment[] = await this.contentService
        .postComment(commentBody)
        .toPromise();
      this.comments = commentRs.reverse();
      this.commentInput = '';
      this.helper.toast('댓글이 작성되었습니다', 'middle');
    } catch (error) {
      this.helper.alert({ message: error.message });
    } finally {
      await this.helper.loadEnd();
    }
  }

  checkUniqueComment(index: number, comment: any) {
    return comment.COM_ID;
  }

  async openUserMenu() {
    const userContent = this.auth.checkUserProperty(
      'MEM_ID',
      this.content.PRS_ID,
    );

    const actionSheetOptions = {
      cssClass: 'action-sheets-basic-page',
      buttons: [],
    };

    if (userContent) {
      actionSheetOptions.buttons = [
        { text: '수정하기', role: 'update' },
        { text: '취소', role: 'cancel' },
      ];
    } else {
      actionSheetOptions.buttons = [
        { text: '차단하기', role: 'block' },
        { text: '신고하기', role: 'report' },
        { text: '취소', role: 'cancel' },
      ];
    }

    const userMenu =
      await this.actionSheetController.create(actionSheetOptions);
    userMenu.present();

    const { role } = await userMenu.onDidDismiss();
    switch (role) {
      case 'update':
        return this.updateContent();
      case 'report':
        return this.reportContent();
      case 'block':
        return await this.blockUser();
    }
  }

  async updateContent() {
    try {
      if (!this.auth.checkUserProperty('MEM_ID', this.content.PRS_ID)) {
        throw new Error('다른사람의 글은 수정하실 수 없습니다.');
      }
      const {
        CON_ID,
        CON_TITLE,
        CON_THUMNAIL,
        CON_BODY,
        CON_PRICE,
        CON_WRITE_YN,
        CON_META_YN,
        CON_USE_YN,
      } = this.content;
      const updateModal = await this.helper.callModal({
        component: WriteModalPage,
        componentProps: {
          type: 'update',
          content: {
            CON_ID,
            CON_TITLE,
            CON_THUMNAIL,
            CON_BODY,
            CON_PRICE,
            CON_WRITE_YN,
            CON_META_YN,
            CON_USE_YN,
          },
        },
      });
      updateModal.present();
      const { data } = await updateModal.onDidDismiss();
      if (data) {
        // 수정 후에 수정 내용만 반영 하면 좋겠지만
        // 수정 응답이 RESULT - FAIL / OK 로 와서 변경 된 이미지 경로를 알수 없음.
        // 컨텐츠 재조회 하여  반영

        if (data.CON_USE_YN === 0) {
          this.cm.setChange({
            action: 'deleted',
            content: { CON_TYPE: CONTENT_TYPES.goblin, CON_ID: data.CON_ID },
          });
          return this.moveBack();
        }

        this.checkContentAvailable({
          type: 'content',
          CON_ID: data.CON_ID,
          updated: true,
          contentData: data,
        });
      }
    } catch (error) {
      this.helper.toast(error.message, 'middle');
    }
  }

  reportContent() {
    this.helper.sendEmail({
      to: 'witchhunting777@gmail.com',
      subject: '게시글 신고',
      body: `
        게시글 제목: ${this.content.CON_TITLE} <br>
        신고 내용 : &nbsp;
      `,
      isHtml: true,
    });
  }

  async blockUser() {
    console.log(this.content);
    const confirmModal = await this.helper.alert({
      header: '차단 확인',
      message:
        '해당 게시글 작성자와, 작성자의 게시글들를 정말로 차단하시겠습니까?',
      buttons: [
        { text: '취소', role: 'cancel' },
        { text: '차단', role: 'block' },
      ],
    });
    confirmModal.present();

    const { role } = await confirmModal.onDidDismiss();

    if (role === 'block') {
      try {
        await this.helper.loading();
        const blockBody: BlockUser = {
          BLOCK_MEM_ID: this.content.PRS_ID,
        };
        const blockRs: BlockUserResponse = await this.contentService
          .blockUser(blockBody)
          .toPromise();
        console.log(blockRs);

        this.cm.setChange({
          action: 'blocked',
          content: {},
        });

        this.helper.toast('사용자 차단이 완료되었습니다.', 'middle');
        this.router.navigate(['/tabs/goblin']);
      } catch (error) {
        this.helper.toast(
          `사용자 차단에 실패하였습니다. ${error.message}`,
          'middle',
        );
      } finally {
        await this.helper.loadEnd();
      }
    }
  }

  writerClick(event, prsId, memName){
    if(this.content.CON_TYPE === '9'){
      if(prsId){
        console.log(prsId);
        this.router.navigate(['profile-page'], {
          state: { prsId, memName },
        });
      }
    }
  }

  enterFullscreen(videoElement: HTMLVideoElement) {
    const el = videoElement as any;

    // 🔊 소리 켜기
    el.muted = false;
    el.volume = 1.0;

    // 🔁 풀스크린 종료 감지 후 다시 음소거
    const handleFullscreenChange = () => {
      const isFullscreen =
        document.fullscreenElement === el ||
        (document as any).webkitFullscreenElement === el ||
        (document as any).msFullscreenElement === el;

      if (!isFullscreen) {
        el.muted = true;
        el.volume = 0.0;

        // 이벤트 리스너 제거
        document.removeEventListener('fullscreenchange', handleFullscreenChange);
        document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
        document.removeEventListener('msfullscreenchange', handleFullscreenChange);
      }
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', handleFullscreenChange); // Safari
    document.addEventListener('msfullscreenchange', handleFullscreenChange); // IE

    // 풀스크린 진입
    if (el.requestFullscreen) {
      el.requestFullscreen();
    } else if (el.webkitEnterFullscreen) {
      // iOS Safari (전체 화면이 아닌 전체화면 유사 모드)
      el.webkitEnterFullscreen();
    } else if (el.webkitRequestFullscreen) {
      el.webkitRequestFullscreen();
    } else if (el.msRequestFullscreen) {
      el.msRequestFullscreen();
    } else {
      console.warn('Fullscreen API is not supported.');
    }
  }

  scrollToComments() {
    this.ionContent.scrollToBottom(500);

    setTimeout(() => {
      this.ionContent.scrollToBottom(500);
    }, 300);
  }
}
