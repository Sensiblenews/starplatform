import { Injectable } from '@angular/core';
import { PUSH_CHANNEL_BROADCAST } from '../constants/push-channels';
import { Capacitor } from '@capacitor/core';
import { PushNotifications } from '@capacitor/push-notifications';
import { FCM } from "@capacitor-community/fcm"
@Injectable({
  providedIn: 'root'
})
export class NotificationsService {

  constructor() { }

  initPush() {
    if(Capacitor.getPlatform() !== 'web'){
      this.registerPush();
    }
  }

  public registerPush() {
    PushNotifications.requestPermissions().then(permission=>{
      if(permission.receive === 'granted'){
        PushNotifications.register();
      }
    });

    PushNotifications.addListener('registration', (token)=>{
      console.log(token);

      if(Capacitor.getPlatform() === 'android'){
          // [2-31차 후속] 전체 푸시 채널도 소리·진동이 있는 새 id 로 (서버 FirebaseService.CHANNEL_BROADCAST)
          PushNotifications.createChannel({
            id: PUSH_CHANNEL_BROADCAST,
            name: 'Announcements',
            importance: 4,
            sound: 'tick',
            vibration: true
          });
      }
      
      FCM.subscribeTo({topic: "WitchHuntingPush"})
      .then((r)=>console.log(r))
      .catch((err)=>console.log(err));
    });

    PushNotifications.addListener('pushNotificationReceived', (notifications)=>{
      console.log(notifications);
    });
  }

  public unregisterPush(){
      FCM.unsubscribeFrom({topic: "WitchHuntingPush"})
      .then((r)=>console.log(r))
      .catch((err)=>console.log(err));
  }
}
