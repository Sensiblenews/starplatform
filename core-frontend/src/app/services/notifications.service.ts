import { Injectable } from '@angular/core';
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
          PushNotifications.createChannel({
            id: '500',
            name: 'High Importance Channel',
            importance: 4,
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
