const CONTENT_TYPES = {
  fairy: '0', // 언론사
  monster: '1', // 학원..?
  faq: '2', // FAQ
  promotion: '3', // 홍보
  popup: '4', // 팝업
  // 5 없음
  story: '6', // 스토리
  // 7 없음
  witch: '8',
  goblin: '9',
  nonri: '11',
} as const;

type ContentType = typeof CONTENT_TYPES[keyof typeof CONTENT_TYPES];

export { CONTENT_TYPES, ContentType };
