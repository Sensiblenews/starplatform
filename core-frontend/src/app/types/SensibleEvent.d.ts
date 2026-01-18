interface SensibleEvent {
  EVT_BTN_TEXT?: string;
  EVT_CHG_YN?: number;
  EVT_ID?: number;
  EVT_STAR?: number;
  EVT_TEXT?: string;
  EVT_URL?: string;
}

interface SensibleEventResponse {
  RESULT: 'OK' | 'FAIL';
  item?: SensibleEvent;
  MEM_STAR?: number;
  MEM_STAR_CHG?: number;
}

interface SensiblePopupManageObject {
  CON_ID: number;
  timestamp: number;
}

export type { SensibleEvent, SensibleEventResponse, SensiblePopupManageObject };
