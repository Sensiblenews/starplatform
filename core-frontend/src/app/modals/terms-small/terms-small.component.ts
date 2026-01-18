import { Component, Input, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { HelperService } from 'src/app/services/helper.service';
import { Content } from 'src/app/types/Content';

@Component({
  selector: 'terms-small-modal',
  templateUrl: './terms-small.component.html',
  styleUrls: ['./terms-small.component.scss'],
})
export class TermsSmallModal implements OnInit {
  @Input() terms: Content[];

  public testTerms: boolean[] = [];
  public agreeAll = false;
  public agreeTerms = false;
  public agreePrivacy = false;

  constructor(
    private modalCtrl: ModalController,
    private helper: HelperService
  ) {}

  ngOnInit(): void {
    const { length } = this.terms;
    this.testTerms = Array.from({ length }, () => false);
  }

  toggleAll() {
    this.agreeAll = !this.agreeAll;
    this.agreeTerms = this.agreeAll;
    this.agreePrivacy = this.agreeAll;
  }

  updateTerms() {
    console.log(this.agreeTerms)
    this.agreeTerms = !this.agreeTerms;
    console.log(this.agreeTerms);
    this.updateAllAgree();
  }

  updatePrivacy() {
    this.agreePrivacy = !this.agreePrivacy;
    this.updateAllAgree();
  }

  updateAllAgree() {
    this.agreeAll = this.agreeTerms && this.agreePrivacy;
  }

  dismiss() {
    this.modalCtrl.dismiss(null, 'cancel');
  }

  confirm() {
    const result = {
      terms: this.agreeTerms,
      privacy: this.agreePrivacy,
    };
    this.modalCtrl.dismiss(result, 'confirm');
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
}