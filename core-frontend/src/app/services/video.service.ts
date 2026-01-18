import { Injectable } from '@angular/core';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { FilePicker } from '@capawesome/capacitor-file-picker';
import {
  Camera, CameraPermissionState,
  CameraPermissionType,
  CameraPluginPermissions, CameraSource,
  PermissionStatus,
} from '@capacitor/camera';
import { ManualError } from '../constants/Exceptions';
import { BehaviorSubject } from 'rxjs';
import { ActionSheetController } from '@ionic/angular';

@Injectable({
  providedIn: 'root',
})
export class VideoService {
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

  /**
   * Select video from the device's gallery or file picker.
   * Returns a base64-encoded string and file format.
   */
  async getVideos(): Promise<{ base64String: string; format: string }> {
    try {
      // 권한 체크
      const hasPermission = await this.checkPermission('camera');
      if (!hasPermission) {
        throw new ManualError(
          'permission',
          '사진 접근을 위해 권한을 허용해 주세요.',
        );
      }
      //
      // // Use FilePicker to allow video selection
      // const result = await FilePicker.pickFiles({
      //   types: ['video/*']
      // });
      const result = await FilePicker.pickVideos();
      if (result.files.length === 0) {
        throw new Error('첨부된 동영상이 없습니다.');
      }

      const file = result.files[0];

      // 시간 제한
      if (file.duration > 61) {
        throw new Error('1분 이하의 동영상을 첨부해주세요.');
      }

      //140메가바이트 이하의 동영상 첨부
      if (file.size > 146800640) {
        throw new Error('140MB 이하의 동영상을 첨부해주세요.');
      }

      // Read the file as a base64 string
      const readResult = await Filesystem.readFile({
        path: file.path ||'',
      });

      // Base64 데이터 확인
      const base64String = readResult.data;

      if (typeof base64String !== 'string') {
        throw new Error('Base64 string 형태의 동영상을 첨부해주세요.');
      }

      let format = file.mimeType?.split('/')[1] || 'mp4';

      // QuickTime 형식이라면 MP4로 저장
      // if (file.mimeType === 'video/quicktime') {
      //   format = 'mp4';
      // }

      return {
        base64String,
        format,
      };
    } catch (error) {
      console.error('Error selecting video:', error);
      throw new Error('동영상을 첨부하는 중 일시적인 오류가 발생했습니다.');
    }
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
