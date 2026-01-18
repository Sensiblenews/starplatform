import type { UserInfo } from 'src/app/types/Auth';
interface Press {
  PRS_ID: number;
  PRS_NAME: string;
  PRS_HOMEPAGE: string;
  PRS_EMAIL: string;
}

interface Content {
  CON_ID?: number;
  CON_TITLE?: string;
  CON_BODY?: string;
  CON_CREATE_DATE?: Date;
  CON_EXPD?: number;
  CON_IMPORTANCE?: number;
  CON_ORIGIN_URL?: string;
  CON_PREFIX?: string;
  CON_PRICE?: number;
  CON_SUBTITLE?: string;
  CON_VIDEO?: string;
  CON_THUMNAIL?: string;
  CON_VIDEO_THUMNAIL?: string;
  CON_UPDATE_DATE?: Date;
  CON_USE_YN?: number;
  CON_WRITE_YN?: number;
  CON_YOUTUBE_URL?: string;
  CON_CNT?: number;
  CON_META_YN?: number;
  CON_TOP_YN?: any;
  CON_PRIOT?: any;
  CON_TYPE?: string;
  CON_UDATE?: string;
  CON_CDATE?: string;
  CON_CATEGORY?: string;
  CON_TOP_DAT?: string;
  PRICE?: number;
  PRICE_CHG?: number;
  HEART?: number;
  HEART_CHG?: number;
  PRS_ID?: string;
  MEM_NAME?: string;
  MEM_PICTURE?: string;
  press?: Press;
  images?: ContentImage[];
  comments?: Comment[];
}

interface ContentImage {
  IMG_URL?: string;
  IMG_ORIGIN_URL?: string;
}

interface Comment {
  COM_ID?: number;
  COM_BODY?: string;
  COM_CREATE_DATE?: number;
  COM_UPDATE_DATE?: number;
  COM_USE_YN?: number;
  CON_ID?: number;
  MEM_ID?: string;
  CON_WATCH_COUNT?: number;
  User?: UserInfo;
}

interface ContentResponse {
  contents: Content[];
  result?: unknown;
}

interface BlockUser {
  BLOCK_MEM_ID?: string;
}

interface BlockUserResponse {
  result?: unknown;
}

interface ViewersRequest {
  CON_ID?: number,
  // MEM_ID?: string,
}

interface ViewersResponse {
  MEM_ID?: string,
  CON_ID?: number,
  VIEWER_ID?: string,
  VIEWER_NAME?: string,
  VIEWER_PICTURE?: string,
  IS_FOLLOWING?: number,
}

type SendHeartResult = Partial<UserInfo> & Partial<Content>;
export type {
  Press,
  Content,
  ContentResponse,
  Comment,
  ContentImage,
  SendHeartResult,
  BlockUser,
  BlockUserResponse,
  ViewersRequest,
  ViewersResponse,
};
