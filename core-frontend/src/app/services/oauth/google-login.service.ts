import { Injectable } from '@angular/core';
import { GOUser, UserInfo } from '../../types/Auth'

@Injectable({
  providedIn: 'root'
})
export class GoogleLoginService {

  constructor() { }

  async ready(): Promise<boolean> {
    return false;
  }

  async login(): Promise<UserInfo> {
    try{
      return { MEM_TYPE: undefined, MEM_NAME: undefined, goUser: undefined, email: undefined };
    }
    catch(error){
      console.error(error);
      console.error(`[GoogleAuth] loginFailed - ${error.message}`);
      return null;
    }
  }

  async logout(): Promise<boolean>{
    return false;
  }
}
