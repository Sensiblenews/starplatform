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
        // 앱 타깃 배포 대상이 13.0이라 ATT는 가용성 확인이 필요하다. iOS 13에는 ATT 자체가
        // 없고 IDFA가 그대로 나가므로 true로 둔다.
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
        
        // web inspector settings
        #if DEBUG
          if #available(macOS 13.3, iOS 16.4, tvOS 16.4, *) {
                DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
                      if let vc = self.window?.rootViewController as? CAPBridgeViewController {
                          vc.bridge?.webView?.isInspectable = true;
                      }
                }
          }
        #endif
        
        return true
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

        let statusBarRect = UIApplication.shared.statusBarFrame
        guard let touchPoint = event?.allTouches?.first?.location(in: self.window) else { return }

        if statusBarRect.contains(touchPoint) {
            NotificationCenter.default.post(name: .capacitorStatusBarTapped, object: nil)
        }
    }

}
