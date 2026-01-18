interface UserInfo {
  MEM_ID?: string;
  MEM_TYPE?: AuthType;
  MEM_NAME?: string;
  MEM_STAR?: number;
  MEM_STAR_CHG?: number;
  MEM_TOKEN?: string;
  MEM_AGREEMENT?: number;
  MEM_USE_YN?: number;
  MEM_PICTURE?: string;
  MEM_INTRODUCTION?: string;
  FCM_TOKEN?: string;
  fbUser?: FBUser;
  ktUser?: KTUser;
  apUser?: APUser;
  goUser?: GOUser;
  PRICE?: number;
  PRICE_CHG?: number;
  HEART?: number;
  HEART_CHG?: number;
  CONTENT_CNT?: number;
  FOLLOWING_CNT?: number;
  FOLLOWER_CNT?: number;
  RECENT_HRT_CHG?: number;
  RECENT_HRT_NAME?: string;
}

interface ProfileInfo {
  MEM_ID?: string;
  MEM_NAME?: string;
  MEM_INTRODUCTION?: string;
  MEM_PICTURE?: string;
  FOLLOWING_CNT?: number;
  FOLLOWER_CNT?: number;
  IS_FOLLOWING: number;
}

interface ProfileRequest {
  PROFILE_ID?: string;
  MEM_ID?: string;
  PAGE?: number;
}

interface FollowRequest {
  MEM_ID?: string;
  FOLLOW_ID?: string;
}

interface UnFollowRequest {
  MEM_ID?: string;
  FOLLOW_ID?: string;
}

interface FollowListRequest {
  PROFILE_ID?: string;
  MEM_ID?: string;
}

interface FollowingListRequest {
  PROFILE_ID?: string;
  MEM_ID?: string;
}

interface ModifyNicknameInfo {
  MEM_ID?: string;
  MEM_NAME?: string;
}

interface ModifyNicknameResponse {
  result?: unknown;
}

interface ModifyIntroductionInfo {
  MEM_ID?: string;
  MEM_INTRODUCTION?: string;
}

interface ModifyIntroductionResponse {
  result?: unknown;
}

interface FollowInfo {
  MEM_ID?: string,
  IS_FOLLOWING?: number,
  FOLLOW_ID?: string,
  FOLLOW_NAME?: string,
  FOLLOW_PICTURE?: string,
}

interface FollowSearchInfo {
  KEYWORD?: string,
  PROFILE_ID?: string,
}

interface ModifyProfileImgRequest {
  MEM_ID?: string;
  PROFILE_THUMNAIL?: string;
}

interface FBUser {
  FB_KEY: string;
  FB_TOKEN: string;
  FB_TOKEN_EXPD: string;
  FB_PICTURE: string;
}

interface KTUser {
  KT_KEY: string;
  KT_TOKEN: string;
  KT_TOKEN_EXPD: string;
  KT_PICTURE: string;
}

interface APUser {
  AP_KEY: string;
  AP_TOKEN: string;
  AP_TOKEN_EXPD: string;
  AP_PICTURE: string;
}

interface GOUser {
  GO_KEY: string;
  GO_TOKEN: string;
  GO_TOKEN_EXPD: string;
  GO_PICTURE: string;
}

type AuthType = 'KT' | 'FB' | 'AP' | 'GO';
export type { UserInfo, ProfileInfo, ProfileRequest, FollowRequest, UnFollowRequest, FollowListRequest, FollowingListRequest, ModifyProfileImgRequest, FollowInfo, FollowSearchInfo, KTUser, FBUser, APUser, GOUser, AuthType, ModifyNicknameInfo, ModifyNicknameResponse, ModifyIntroductionInfo, ModifyIntroductionResponse };
