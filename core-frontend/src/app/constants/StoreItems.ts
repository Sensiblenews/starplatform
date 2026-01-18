import 'cordova-plugin-purchase';

const PRODUCTS_MAP = {
  'kr.co.sensiblenews.witchhunting.inapp.babystar': {
    name: '아기별',
    starCount: '1,000개',
    amount: 1000,
  },
  'kr.co.sensiblenews.witchhunting.inapp.casiopea': {
    name: '카시오페아',
    starCount: '5,000개',
    amount: 5000,
  },
  'kr.co.sensiblenews.witchhunting.inapp.bigdipper': {
    name: '북두칠성',
    starCount: '12,000개',
    amount: 12000,
  },
  'kr.co.sensiblenews.witchhunting.inapp.sirius': {
    name: '시리우스',
    starCount: '32,000개',
    amount: 32000,
  },
  'kr.co.sensiblenews.witchhunting.inapp.andromeda': {
    name: '안드로메다',
    starCount: '130,000개',
    amount: 13000,
  },
};

interface PurchaseState {
  state: 'success' | 'cancelled';
  id: string;
  chargeInfo: typeof PRODUCTS_MAP[keyof typeof PRODUCTS_MAP];
}
interface CustomIAPProduct {
  id: string;
  display: PurchaseState['chargeInfo'];
}

export { PRODUCTS_MAP, PurchaseState, CustomIAPProduct };
