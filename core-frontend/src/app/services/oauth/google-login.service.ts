import { Injectable } from '@angular/core';
import { GOUser, UserInfo } from '../../types/Auth'
import { GoogleAuth, User } from '@codetrix-studio/capacitor-google-auth'
import config from '../../../../capacitor.config';

@Injectable({
  providedIn: 'root'
})
export class GoogleLoginService {

  constructor() { }

  async ready(): Promise<boolean> {
    await GoogleAuth.initialize();
    return true;
  }

  async login(): Promise<UserInfo> {
    try{
      const user: User = await GoogleAuth.signIn();

      if(!user){
        throw new Error('No UserData');
      }

      const MEM_TYPE = 'GO';
      const MEM_NAME = user.name;
      const goUser: GOUser = {
        GO_KEY: user.id,
        GO_TOKEN: user.authentication.accessToken,
        GO_TOKEN_EXPD: '',
        GO_PICTURE: user.imageUrl
      };

      return { MEM_TYPE, MEM_NAME, goUser };
    }
    catch(error){
      console.error(error);
      console.error(`[GoogleAuth] loginFailed - ${error.message}`);
      return null;
    }
  }

  async logout(): Promise<boolean>{
    try{
      GoogleAuth.signOut();
      return true;
    }
    catch(error){
      console.error(`[GoogleAuth] logoutFailed - $(error.message)`);
      return false;
    }
  }
}
