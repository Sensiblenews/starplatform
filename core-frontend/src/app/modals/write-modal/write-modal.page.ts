import { AuthService } from './../../services/auth.service';
import { Router } from '@angular/router';
import { PhotoService } from './../../services/photo.service';
import { ContentManagerService } from 'src/app/services/content-manager.service';
import { ContentService } from 'src/app/services/content.service';
import { HelperService } from './../../services/helper.service';
import { Content, ContentResponse } from 'src/app/types/Content';
import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { CONTENT_TYPES } from 'src/app/constants/ContentTypes';
import { AdMobService } from 'src/app/services/ad-mob.service';
import { VideoService } from 'src/app/services/video.service';
@Component({
  selector: 'app-write-modal',
  templateUrl: './write-modal.page.html',
  styleUrls: ['./write-modal.page.scss'],
})
export class WriteModalPage {
  @Input() type = 'write';
  @Input() content: Content = {
    CON_TITLE: '',
    CON_VIDEO: '',
    CON_THUMNAIL: '',
    CON_BODY: '',
    CON_PRICE: 0,
    CON_WRITE_YN: 1,
    CON_META_YN: 0,
    CON_USE_YN: 1,
  };
  public imgWebPath: string;
  constructor(
    public modalController: ModalController,
    private contentService: ContentService,
    private router: Router,
    private cm: ContentManagerService,
    private auth: AuthService,
    private helper: HelperService,
    private photo: PhotoService,
    private admob: AdMobService,
    private videoService: VideoService,
  ) {}

  ionViewDidEnter() {
    this.imgWebPath = this.content?.CON_THUMNAIL || '';
  }

  closeModal() {
    this.modalController.dismiss();
  }

  // 비디오 첨부
  async uploadVideo() {
    try {
      const { base64String, format } = await this.videoService.getVideos();

      // 파일 형식 확인
      if (!format.startsWith('3gp') && !format.startsWith('avi') && !format.startsWith('mp4') && !format.startsWith('mov') && !format.startsWith('quicktime')) {
        this.helper.toast('동영상을 첨부해 주세요.', 'middle');
        return; // 이미지일 경우 실행 중단
      }

      this.content.CON_VIDEO = base64String.replace(/^data:video\/[a-zA-Z]+;base64,/, '');
      this.imgWebPath = `data:video/${format};base64,${base64String}`;
    } catch (e) {
      if (e.name === 'permission') {
        this.helper.toast(e.message, 'middle');
      }
    }
  }

  // 이미지 첨부
  async uploadImage() {
    try {
      const { base64String, format } = await this.photo.getPhotos();

      // 파일 형식 확인
      if (!format.startsWith('jpg') && !format.startsWith('jpeg') && !format.startsWith('png') && !format.startsWith('webp')) {
        this.helper.toast('이미지를 첨부해 주세요.', 'middle');
        return; // 이미지일 경우 실행 중단
      }

      this.content.CON_THUMNAIL = base64String;
      this.imgWebPath = `data:image/${format};base64,${base64String}`;
    } catch (e) {
      if (e.name === 'permission') {
        this.helper.toast(e.message, 'middle');
      }
    }
  }

  contentValidation({ CON_TITLE, CON_THUMNAIL, CON_BODY }) {
    if (!CON_TITLE?.length) {
      throw new Error('제목을 입력해 주세요.');
    }
    // if (!CON_THUMNAIL?.length) {
    //   throw new Error('이미지를 등록해 주세요.');
    // }
    if (!CON_BODY?.length) {
      throw new Error('내용을 입력해 주세요.');
    }
  }

  sanitizeContentBody(CON_BODY: string) {
    const replacer: [RegExp, string][] = [
      [/(\n|\r\n)/g, '<br>'],
      [
        /(http[s]?\:\/\/)?[a-zA-Z]*\.[a-zA-Z]*\.[a-zA-Z]{2,}/g,
        '<a href="http://$&">$&</a>',
      ],
    ];
    return replacer.reduce((contentBody, [regex, replaceStr]) => {
      contentBody.replace(regex, replaceStr);
      return contentBody;
    }, CON_BODY);
  }

  async updatePost() {
    try {
      await this.helper.loading();
      const {
        CON_TITLE,
        CON_THUMNAIL,
        CON_VIDEO,
        CON_BODY,
        CON_PRICE,
        CON_WRITE_YN,
        CON_META_YN,
        CON_USE_YN,
      } = this.content;
      const CON_CATEGORY = '8';

      const sanitizedContentBody = this.sanitizeContentBody(CON_BODY);

      this.contentValidation({
        CON_TITLE,
        CON_THUMNAIL,
        CON_BODY: sanitizedContentBody,
      });

      const sendObject: Content = {
        CON_CATEGORY,
        CON_TITLE,
        CON_THUMNAIL,
        CON_VIDEO,
        CON_PRICE: Number(CON_PRICE),
        CON_WRITE_YN: Number(CON_WRITE_YN),
        CON_META_YN: Number(CON_META_YN),
        CON_USE_YN: Number(CON_USE_YN),
        CON_BODY: sanitizedContentBody,
      };

      let resultMsg: string;
      if (this.type === 'update') {
        sendObject.CON_ID = this.content.CON_ID;
        if (sendObject.CON_WRITE_YN !== 0) {
          sendObject.MEM_NAME = this.auth.getUserProperty('MEM_NAME') as string;
        }
        await this.contentService.updateContent(sendObject).toPromise();
        resultMsg = '수정';
      } else {
        await this.contentService.postContent(sendObject).toPromise();
        this.cm.setChange({
          action: 'created',
          content: { CON_TYPE: CONTENT_TYPES.goblin },
        });
        resultMsg = '작성';
        this.router.navigate(['/tabs/goblin']);
      }

      this.cm.setChange({
        action: 'goblin-writed',
        content: {},
      });

      this.helper.toast(`게시글이 ${resultMsg} 되었습니다!`, 'middle');

      try{
        await this.admob.showInterstitial();
      }
      catch(error){
      }

      return this.modalController.dismiss(sendObject);
    } catch (error) {
      this.helper.toast(error.message, 'middle');
    } finally {
      await this.helper.loadEnd();
    }
  }
}
