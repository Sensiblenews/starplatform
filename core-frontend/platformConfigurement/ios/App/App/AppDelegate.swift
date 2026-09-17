import UIKit
import Capacitor
import KakaoSDKCommon
import KakaoSDKUser
import KakaoSDKAuth
import Firebase
import GoogleMobileAds
import FBSDKCoreKit
import FBAudienceNetwork
import AppTrackingTransparency
import FirebaseAuth

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.

        // Meta Audience Network 광고 추적 플래그 (2-29차).
        // Meta는 iOS 14+에서 이 값을 퍼블리셔가 직접, 그것도 GMA 초기화 전에 넘기라고 요구한다.
        // ATT 요청 자체는 웹 쪽(ad-mob.service.ts)이 AdMob 초기화 뒤에 하므로 이 시점에 알 수
        // 있는 것은 지난 실행에서 결정된 상태뿐이다. 첫 실행은 notDetermined → false가 정상이고,
        // 사용자가 허용하면 다음 실행부터 true로 입찰 요청이 나간다.
        // 배포 대상이 iOS 17이라 ATT는 항상 존재하지만, 가용성 검사는 그대로 둔다 —
        // 이 분기를 지우면 배포 대상을 다시 낮출 때 조용히 깨진다.
        if #available(iOS 14, *) {
            FBAdSettings.setAdvertiserTrackingEnabled(
                ATTrackingManager.trackingAuthorizationStatus == .authorized
            )
        } else {
            FBAdSettings.setAdvertiserTrackingEnabled(true)
        }

        MobileAds.shared.start(completionHandler: nil)
        
        // initialize FB
        FBSDKCoreKit.ApplicationDelegate.shared.application(
            application,
            didFinishLaunchingWithOptions: launchOptions
        )
        FBSDKCoreKit.Settings.shared.appID = "1283614445756373"
        // initialize FB END

        // Initialize Kakao
        let key = Bundle.main.infoDictionary?["KAKAO_APP_KEY"] as? String
        KakaoSDK.initSDK(appKey: key!)
        // Initialize Kakao END
        
        // initialize Firebase
        FirebaseApp.configure()
        // initialize Firebase END
        
        // 웹 인스펙터 설정은 SceneDelegate 로 옮겼다 — 이제 window 를 씬이 갖는다

        return true
    }

    /**
     * UIScene 생명주기 진입점 (Xcode 27 / iOS 26 SDK 필수).
     * 실제 처리는 Info.plist 의 UISceneDelegateClassName 이 가리키는 SceneDelegate 가 한다.
     */
    func application(_ application: UIApplication,
                     configurationForConnecting connectingSceneSession: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        return UISceneConfiguration(name: "Default Configuration",
                                    sessionRole: connectingSceneSession.role)
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
         Messaging.messaging().apnsToken = deviceToken
         Messaging.messaging().token(completion: { (token, error) in
           if let error = error {
               NotificationCenter.default.post(name: .capacitorDidFailToRegisterForRemoteNotifications, object: error)
           } else if let token = token {
               NotificationCenter.default.post(name: .capacitorDidRegisterForRemoteNotifications, object: token)
           }
         })
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
         NotificationCenter.default.post(name: .capacitorDidFailToRegisterForRemoteNotifications, object: error)
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }

    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
        // Called when the app was launched with a url. Feel free to add additional processing here,
        // but if you want the App API to support tracking app url opens, make sure to keep this call
        
        // Need for Login with KakaoTalk
      
      if Auth.auth().canHandle(url) {
        return true
      }
      
        if (AuthApi.isKakaoTalkLoginUrl(url)) {
            return AuthController.handleOpenUrl(url: url)
        }

        // Need for FB
        if (FBSDKCoreKit.ApplicationDelegate.shared.application(
            app,
            open: url,
            sourceApplication: options[UIApplication.OpenURLOptionsKey.sourceApplication] as? String,
            annotation: options[UIApplication.OpenURLOptionsKey.annotation]
        )) {
            return true;
        } else {
            return ApplicationDelegateProxy.shared.application(app, open: url, options: options)
        }
        // Need for FB END

        // Need for Login with KakaoTalk END
        return ApplicationDelegateProxy.shared.application(app, open: url, options: options)
    }

    func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
        // Called when the app was launched with an activity, including Universal Links.
        // Feel free to add additional processing here, but if you want the App API to support
        // tracking app url opens, make sure to keep this call
        return ApplicationDelegateProxy.shared.application(application, continue: userActivity, restorationHandler: restorationHandler)
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesBegan(touches, with: event)

        // 씬 구조에서는 AppDelegate.window 가 비어 있다. 연결된 씬에서 찾는다.
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }

        let statusBarRect = keyWindow?.windowScene?.statusBarManager?.statusBarFrame ?? .zero
        guard let touchPoint = event?.allTouches?.first?.location(in: keyWindow) else { return }

        if statusBarRect.contains(touchPoint) {
            NotificationCenter.default.post(name: .capacitorStatusBarTapped, object: nil)
        }
    }

}
