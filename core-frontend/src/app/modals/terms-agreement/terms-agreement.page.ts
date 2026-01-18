import { Content } from 'src/app/types/Content';
import { ModalController } from '@ionic/angular';
import { Component, Input, OnInit } from '@angular/core';
import { HelperService } from 'src/app/services/helper.service';

// interface TermsText {
//   usage: Content;
//   personal: Content;
// }

// interface TermsAgreement {
//   usage: boolean;
//   personal: boolean;
// }

@Component({
  selector: 'app-terms-agreement',
  templateUrl: './terms-agreement.page.html',
  styleUrls: ['./terms-agreement.page.scss'],
})
export class TermsAgreementPage implements OnInit {
  @Input() terms: Content[];

  public testTerms: boolean[] = [];
  public allAgreed = false;
  public agreements = {
    termsOfService: false,
    privacyPolicy: false,
  };

  constructor(
    private modalCtrl: ModalController,
    private helper: HelperService,
  ) {}

  public get isAllAgreed(): boolean {
    return this.agreements.privacyPolicy && this.agreements.termsOfService;
  }

  toggleAll() {
    const newValue = this.allAgreed;
    this.agreements.termsOfService = newValue;
    this.agreements.privacyPolicy = newValue;
  }

  updateAllAgree() {
    this.allAgreed = this.agreements.termsOfService && this.agreements.privacyPolicy;
  }

  ngOnInit() {
    const { length } = this.terms;
    this.testTerms = Array.from({ length }, () => false);
  }

  dismiss() {
    this.modalCtrl.dismiss(false);
  }

  async handleClickOnSpecific(termsIndex: number) {
    const alert = await this.helper.alert({
      cssClass: 'my-custom-class',
      backdropDismiss: true,
      message: this.terms[termsIndex].CON_BODY,
      buttons: [
        {
          text: '닫기',
          role: 'cancel',
        }
      ]
    });

    await alert.present();
    // const { role } = await alert.onDidDismiss();
    return;
  }

  agreeAll() {
    if (!this.isAllAgreed) {
      return;
    }
    this.modalCtrl.dismiss(this.allAgreed);
  }
}
