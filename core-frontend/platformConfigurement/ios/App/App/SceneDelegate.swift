import UIKit
import Capacitor
import FirebaseAuth
import KakaoSDKAuth
import FBSDKCoreKit

/**
 * UIScene 생명주기 어댑터.
 *
 * Xcode 27(iOS 26 SDK)부터 UIScene 을 채택하지 않은 앱은 실행 즉시 종료된다.
 *   "UIScene life cycle is required for apps built with this SDK"
 * Capacitor 6 템플릿은 아직 AppDelegate 단독 구조라 이 파일을 직접 둔다.
 *
 * ⚠️ URL 열기와 유니버설 링크는 더 이상 AppDelegate 로 오지 않고 전부 여기로 온다.
 *    카카오·페이스북 로그인 복귀와 딥링크가 이 파일을 거치므로, 판정 순서를
 *    AppDelegate 쪽 구현과 똑같이 유지한다. 한쪽만 고치면 로그인이 조용히 깨진다.
 */
class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(_ scene: UIScene,
               willConnectTo session: UISceneSession,
               options connectionOptions: UIScene.ConnectionOptions) {
        // window 와 rootViewController 는 Info.plist 의 UISceneStoryboardFile 이 만들어 준다.
        // 여기서는 "앱이 URL·유니버설 링크로 시작된" 경우만 따로 받는다 —
        // 이 경로는 아래 openURLContexts 나 continue 로 오지 않는다.
        for context in connectionOptions.urlContexts {
            _ = SceneDelegate.handle(url: context.url, options: SceneDelegate.options(from: context))
        }
        for activity in connectionOptions.userActivities {
            _ = ApplicationDelegateProxy.shared.application(
                UIApplication.shared, continue: activity, restorationHandler: { _ in })
        }

        // 웹 인스펙터: AppDelegate 시절에는 self.window 를 봤지만 이제 window 는 씬이 갖는다
        #if DEBUG
        if #available(iOS 16.4, *) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
                if let vc = self.window?.rootViewController as? CAPBridgeViewController {
                    vc.bridge?.webView?.isInspectable = true
                }
            }
        }
        #endif
    }

    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        for context in URLContexts {
            _ = SceneDelegate.handle(url: context.url, options: SceneDelegate.options(from: context))
        }
    }

    func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
        _ = ApplicationDelegateProxy.shared.application(
            UIApplication.shared, continue: userActivity, restorationHandler: { _ in })
    }

    /// AppDelegate 의 application(_:open:options:) 와 판정 순서가 같아야 한다
    static func handle(url: URL, options: [UIApplication.OpenURLOptionsKey: Any]) -> Bool {
        // Firebase(전화·이메일 링크 등) 가 먼저 가져간다
        if Auth.auth().canHandle(url) {
            return true
        }

        // 카카오톡 로그인 복귀
        if AuthApi.isKakaoTalkLoginUrl(url) {
            return AuthController.handleOpenUrl(url: url)
        }

        // 페이스북 로그인 복귀
        if FBSDKCoreKit.ApplicationDelegate.shared.application(
            UIApplication.shared,
            open: url,
            sourceApplication: options[.sourceApplication] as? String,
            annotation: options[.annotation]
        ) {
            return true
        }

        // 나머지는 Capacitor 로 넘겨 appUrlOpen 이벤트가 웹까지 가게 한다
        return ApplicationDelegateProxy.shared.application(UIApplication.shared, open: url, options: options)
    }

    /// UIOpenURLContext 의 옵션을 AppDelegate 시절의 형태로 되돌린다
    static func options(from context: UIOpenURLContext) -> [UIApplication.OpenURLOptionsKey: Any] {
        var options: [UIApplication.OpenURLOptionsKey: Any] = [:]
        if let sourceApplication = context.options.sourceApplication {
            options[.sourceApplication] = sourceApplication
        }
        if let annotation = context.options.annotation {
            options[.annotation] = annotation
        }
        return options
    }
}
