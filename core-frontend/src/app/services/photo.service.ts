import { ActionSheetController } from '@ionic/angular';
import { BehaviorSubject } from 'rxjs';
import { Injectable } from '@angular/core';
import {
  Camera,
  CameraPermissionState,
  CameraPermissionType,
  CameraPluginPermissions,
  CameraResultType,
  CameraSource,
  ImageOptions,
  PermissionStatus,
  Photo,
} from '@capacitor/camera';
import { ManualError } from '../constants/Exceptions';

@Injectable({
  providedIn: 'root',
})
export class PhotoService {
  private permissionStatus: BehaviorSubject<PermissionStatus> =
    new BehaviorSubject({
      camera: 'denied',
      photos: 'denied',
    });

  private allowed: PermissionState = 'granted';
  constructor(private actionSheet: ActionSheetController) {
    this.checkPermissionAll();
  }

  setPermissionStatus(state: Partial<PermissionStatus>): void {
    const current = this.permissionStatus.value;
    this.permissionStatus.next({ ...current, ...state });
  }

  getPermissionStatus(permission: CameraPermissionType): CameraPermissionState {
    const { [permission]: value } = this.permissionStatus.value;
    return value || 'denied';
  }

  isPermissionAllowed(permission: CameraPermissionType): boolean {
    return this.getPermissionStatus(permission) === this.allowed;
  }

  async checkPermission(photoType: CameraPermissionType): Promise<boolean> {
    if (this.isPermissionAllowed(photoType)) {
      return true;
    }
    const { [photoType]: permission } = await this.requestPermission({
      permissions: [photoType],
    });
    const isAllowed = permission === this.allowed;
    if (isAllowed) {
      this.setPermissionStatus({ [photoType]: permission });
      this.setPermissionStatus({ camera: 'denied', photos: 'denied' });
    }
    return isAllowed;
  }

  async getPhotos(): Promise<Photo | null> {
    const sheet = await this.actionSheet.create({
      cssClass: 'commentActionSheet',
      buttons: [
        { text: '직접 촬영', role: 'camera' },
        { text: '앨범 선택', role: 'photos' },
        { text: '취소', role: 'cancel' },
      ],
    });
    sheet.present();
    const { role } = await sheet.onDidDismiss();
    if (!role || role === 'cancel') {
      return null;
    }
    return this.getPhotoBySpecificType(role as CameraPermissionType);
  }

  async getPhotoBySpecificType(
    specificType: CameraPermissionType,
  ): Promise<Photo | null> {
    switch (specificType) {
      case 'camera':
        return this.getPhotoByCamera('camera');
      case 'photos':
        return this.getPhotoByCamera('photos');
      default:
        return null;
    }
  }

  async getPhotoByCamera(from: 'camera' | 'photos'): Promise<Photo | null> {
    const hasPermission = await this.checkPermission('camera');
    if (!hasPermission) {
      throw new ManualError(
        'permission',
        '사진 접근을 위해 권한을 허용해 주세요.',
      );
    }
    const options: ImageOptions = {
      quality: 75,
      resultType: CameraResultType.Base64,
      source: from === 'camera' ? CameraSource.Camera : CameraSource.Photos,
      width: 900,
      height: 900,
    };
    return await Camera.getPhoto(options);
  }

  async checkPermissionAll(): Promise<void> {
    const currentPermission = await Camera.checkPermissions();
    const denied = Object.keys(currentPermission).filter(
      (pms: CameraPermissionType) => currentPermission[pms] !== this.allowed,
    ) as CameraPermissionType[];

    if (!denied?.length) {
      this.setPermissionStatus(currentPermission);
      return;
    }

    const retryPermission = await this.requestPermission({
      permissions: denied,
    });
    this.setPermissionStatus(retryPermission);
  }

  async requestPermission(
    permissions: CameraPluginPermissions,
  ): Promise<PermissionStatus> {
    return await Camera.requestPermissions(permissions);
  }
}
