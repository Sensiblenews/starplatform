import { Component, Input } from '@angular/core';
import { ModalController, AlertController } from '@ionic/angular';
import { HttpService } from 'src/app/services/http.service';

@Component({
  selector: 'app-purchase-modal',
  templateUrl: './purchase-modal.component.html',
  styleUrls: ['./purchase-modal.component.scss']
})
export class PurchaseModalComponent {
  
  @Input() targetStar: any; 

  formData = {
    companyName: '',
    managerName: '',
    contact: '',
    email: '',
    message: ''
  };

  constructor(
    private modalCtrl: ModalController,
    private alertCtrl: AlertController,
    private http: HttpService
  ) {}

  closeModal() {
    this.modalCtrl.dismiss();
  }

  async submitForm() {
    if (!this.formData.companyName || !this.formData.managerName || !this.formData.contact || !this.formData.email) {
      alert('Please fill in all required contact information.');
      return;
    }

    const payload = {
      target_ipo_id: this.targetStar.id,
      ...this.formData
    };

    this.http.post('/api/super/purchase/request', payload).subscribe(async (res: any) => {
      if (res.result === 'OK') {
        this.showSuccessAlert();
      } else {
        alert('Submission failed.');
      }
    });
  }

  async showSuccessAlert() {
    const alert = await this.alertCtrl.create({
      header: 'Inquiry Submitted',
      message: 'Your inquiry has been successfully submitted.<br><br>Our global team will contact you within 24 hours.',
      buttons: ['OK']
    });
    await alert.present();
    this.closeModal(); 
  }
}