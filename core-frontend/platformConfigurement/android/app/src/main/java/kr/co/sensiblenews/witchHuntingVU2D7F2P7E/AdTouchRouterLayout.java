package kr.co.sensiblenews.witchHuntingVU2D7F2P7E;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/**
 * 네이티브 광고 컨테이너. 탭은 광고로, 드래그는 웹뷰 스크롤로 보낸다 (2-32차 후속).
 *
 * 배경: 광고 카드는 웹뷰 "위에" 띄운 오버레이라 웹뷰의 자식이 아니라 형제다.
 * 2-32차에서 AdMob 규격대로 헤드라인·아이콘·본문·미디어를 전부 광고 자산으로 등록하자
 * SDK 가 그 뷰들에 클릭 리스너를 걸어 카드 전체가 터치를 삼키게 됐고, 손가락이 광고 위에서
 * 출발하면 웹뷰가 스크롤되지 않았다(실기기 확인). 예전에는 CTA 버튼만 광고 뷰라 나머지는 통과됐다.
 *
 * 부모가 스크롤 뷰가 아니므로 requestDisallowInterceptTouchEvent 로는 해결되지 않는다.
 * 여기서 제스처 전체를 가로채 두고, 터치 슬롭을 넘겨 움직이면 광고 쪽에는 CANCEL 을 보내고
 * 그 뒤 이벤트를 좌표 보정해서 웹뷰에 직접 넘긴다. 탭(움직이지 않고 뗌)은 그대로 광고가 받는다.
 */
public class AdTouchRouterLayout extends FrameLayout {

  private final View webView;
  private final int touchSlop;
  private final int[] myLocation = new int[2];
  private final int[] webLocation = new int[2];

  private float downX;
  private float downY;
  private boolean forwarding;    // 이번 제스처를 웹뷰로 넘기는 중인가
  private boolean childTookDown; // 광고 쪽 자식이 DOWN 을 받았는가 (CANCEL 을 보내야 하는지)

  public AdTouchRouterLayout(Context context, View webView) {
    super(context);
    this.webView = webView;
    this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (webView == null) {
      return super.dispatchTouchEvent(ev);
    }

    switch (ev.getActionMasked()) {
      case MotionEvent.ACTION_DOWN: {
        downX = ev.getX();
        downY = ev.getY();
        forwarding = false;
        childTookDown = super.dispatchTouchEvent(ev);
        if (!childTookDown) {
          // 광고 자식이 안 받는 자리(여백 등)면 처음부터 웹뷰 제스처로 취급한다
          forwarding = true;
          forwardToWebView(ev);
        }
        // 제스처 전체를 우리가 소유해야 이후 MOVE/UP 이 계속 들어온다
        return true;
      }

      case MotionEvent.ACTION_MOVE: {
        if (forwarding) {
          forwardToWebView(ev);
          return true;
        }
        float dx = Math.abs(ev.getX() - downX);
        float dy = Math.abs(ev.getY() - downY);
        if (dx > touchSlop || dy > touchSlop) {
          // 드래그로 판정: 광고 쪽 제스처를 취소하고 웹뷰에서 같은 지점부터 다시 시작한다
          forwarding = true;
          if (childTookDown) {
            MotionEvent cancel = MotionEvent.obtain(ev);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel);
            cancel.recycle();
          }
          MotionEvent down = MotionEvent.obtain(ev.getDownTime(), ev.getDownTime(),
            MotionEvent.ACTION_DOWN, downX, downY, ev.getMetaState());
          forwardToWebView(down);
          down.recycle();
          forwardToWebView(ev);
          return true;
        }
        if (childTookDown) {
          super.dispatchTouchEvent(ev);
        }
        return true;
      }

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
        if (forwarding) {
          forwardToWebView(ev);
          forwarding = false;
          return true;
        }
        if (childTookDown) {
          super.dispatchTouchEvent(ev); // 탭 → 광고 클릭·영상 컨트롤
        }
        return true;
      }

      default:
        if (forwarding) {
          forwardToWebView(ev);
          return true;
        }
        return super.dispatchTouchEvent(ev);
    }
  }

  /** 이 뷰 기준 좌표를 웹뷰 기준 좌표로 옮겨 넘긴다. translationY 로 움직인 위치까지 반영된다 */
  private void forwardToWebView(MotionEvent source) {
    getLocationOnScreen(myLocation);
    webView.getLocationOnScreen(webLocation);
    MotionEvent copy = MotionEvent.obtain(source);
    copy.offsetLocation(myLocation[0] - webLocation[0], myLocation[1] - webLocation[1]);
    webView.dispatchTouchEvent(copy);
    copy.recycle();
  }
}
