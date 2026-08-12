import UIKit
import Capacitor
import GoogleMobileAds

class MainViewController: CAPBridgeViewController, NativeAdLoaderDelegate, NativeAdDelegate {
  var nativeAdLoader: AdLoader!
  
  // [수정] 컨테이너 하나만 남김
  var adContainer1: UIView!
   var adContainer2: UIView! // 삭제
   var adContainer3: UIView! // 삭제
  
  var adLoader1: AdLoader!
   var adLoader2: AdLoader! // 삭제
   var adLoader3: AdLoader! // 삭제
  
  var adHeight: Int!
  
  private var initialScrollOffset: CGPoint = .zero
  
  private func makeLoader() -> AdLoader {
    let opts = NativeAdViewAdOptions()
    opts.preferredAdChoicesPosition = .topRightCorner
    // 실제 운영 ID 사용 (Android와 동일한 계정의 iOS ID여야 함)
    let l = AdLoader(adUnitID: "ca-app-pub-9109251900558498/7761452822",
                     rootViewController: self,
                     adTypes: [.native],
                     options: [opts])
    l.delegate = self
    return l
  }
  
  override func capacitorDidLoad() {
    super.capacitorDidLoad()
    bridge?.registerPluginInstance(NativeBridge())
  }
  
  override func viewDidLoad() {
    super.viewDidLoad()
    
    // 광고 높이 계산 (기존 로직 유지)
    let sw = UIScreen.main.bounds.width
    let ih = (sw) * 0.8
    adHeight = Int(sw) + 85
    
    // Wrapper 설정
    let adWrapper = AdWrapper(frame: self.view.bounds)
    adWrapper.isUserInteractionEnabled = true
    adWrapper.backgroundColor = .clear
    adWrapper.tag = 200
    adWrapper.clipsToBounds = true
    
    // [수정] 컨테이너 1개만 생성
    let screenWidth = UIScreen.main.bounds.width
    // 높이는 넉넉하게 잡거나 기존 로직 유지
    let containerHeight = CGFloat(adHeight)
    
    // [복구] 1번 컨테이너
    adContainer1 = UIView(frame: CGRect(x: 0, y: 0, width: sw, height: containerHeight))
    adContainer1.backgroundColor = .clear
    adContainer1.tag = 201
    adContainer1.isHidden = true
    adWrapper.addSubview(adContainer1)
    
    // [복구] 2번 컨테이너
    adContainer2 = UIView(frame: CGRect(x: 0, y: 0, width: sw, height: containerHeight))
    adContainer2.backgroundColor = .clear
    adContainer2.tag = 202
    adContainer2.isHidden = true
    adWrapper.addSubview(adContainer2)

    // [복구] 3번 컨테이너
    adContainer3 = UIView(frame: CGRect(x: 0, y: 0, width: sw, height: containerHeight))
    adContainer3.backgroundColor = .clear
    adContainer3.tag = 203
    adContainer3.isHidden = true
    adWrapper.addSubview(adContainer3)
    
    self.view.addSubview(adWrapper)
    
    adWrapper.translatesAutoresizingMaskIntoConstraints = false
    adContainer1.translatesAutoresizingMaskIntoConstraints = false
    
    NSLayoutConstraint.activate([
      // Wrapper 위치 (헤더 아래 43pt 지점부터 시작)
      adWrapper.leadingAnchor.constraint(equalTo: self.view.leadingAnchor),
      adWrapper.trailingAnchor.constraint(equalTo: self.view.trailingAnchor),
      adWrapper.topAnchor.constraint(equalTo: self.view.safeAreaLayoutGuide.topAnchor, constant: 43), // Ionic Header 높이 고려
      adWrapper.bottomAnchor.constraint(equalTo: self.view.safeAreaLayoutGuide.bottomAnchor, constant: -50),
      
      // Container 제약조건
      adContainer1.heightAnchor.constraint(equalToConstant: containerHeight),
      adContainer1.leadingAnchor.constraint(equalTo: adWrapper.leadingAnchor),
      adContainer1.trailingAnchor.constraint(equalTo: adWrapper.trailingAnchor),
      adContainer1.topAnchor.constraint(equalTo: adWrapper.topAnchor), // Wrapper 상단에 붙임 (나중에 transform으로 이동)
      
      adContainer2.heightAnchor.constraint(equalToConstant: containerHeight),
      adContainer2.leadingAnchor.constraint(equalTo: adWrapper.leadingAnchor),
      adContainer2.trailingAnchor.constraint(equalTo: adWrapper.trailingAnchor),
      adContainer2.topAnchor.constraint(equalTo: adWrapper.topAnchor),
      
      adContainer3.heightAnchor.constraint(equalToConstant: containerHeight),
      adContainer3.leadingAnchor.constraint(equalTo: adWrapper.leadingAnchor),
      adContainer3.trailingAnchor.constraint(equalTo: adWrapper.trailingAnchor),
      adContainer3.topAnchor.constraint(equalTo: adWrapper.topAnchor),
    ])
    
    loadNativeAd()
  }
  
  func loadNativeAd() {
    // 기존 뷰 제거
    for subview in adContainer1.subviews {
      subview.removeFromSuperview()
    }
    
    for subview in adContainer2.subviews {
      subview.removeFromSuperview()
    }
    
    for subview in adContainer3.subviews {
      subview.removeFromSuperview()
    }
    
    adLoader1 = makeLoader()
    adLoader1.load(Request())
    
    adLoader2 = makeLoader()
    adLoader2.load(Request())
    
    adLoader3 = makeLoader()
    adLoader3.load(Request())
  }
  
  // 광고 로드 성공
  func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) {
    // [수정] 무조건 adContainer에 부착
    if adLoader == adLoader1 {
        attach(nativeAd, into: adContainer1, delegate: self)
    } else if adLoader == adLoader2 {
        attach(nativeAd, into: adContainer2, delegate: self)
    } else if adLoader == adLoader3 {
        attach(nativeAd, into: adContainer3, delegate: self)
    }
  }
  
  // 광고 로드 실패
  func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) {
    print("native ad failed:", error.localizedDescription)
    adContainer1.isHidden = true
    adContainer2.isHidden = true
    adContainer3.isHidden = true
  }
  
  private func attach(_ nativeAd: NativeAd, into target: UIView, delegate: NativeAdDelegate) {
    // XIB 로드 및 설정 (기존 코드와 동일)
    let v = Bundle.main.loadNibNamed("AdView", owner: nil, options: nil)!.first as! AdView
    v.nativeAd = nativeAd
    nativeAd.delegate = delegate
    v.isUserInteractionEnabled = true
    
    v.mediaView = v.adMediaView
    v.headlineView = v.adHeadline
    v.bodyView = v.adBody
    v.iconView = v.adIcon
    v.callToActionView = v.adCallToAction
    
    v.adHeadline.text = nativeAd.headline
    v.adIcon.image = nativeAd.icon?.image
    v.adIcon.isHidden = nativeAd.icon == nil
    v.adBody.text = nativeAd.body
    v.adBody.isHidden = nativeAd.body == nil
    v.adCallToAction.setTitle(nativeAd.callToAction, for: .normal)
    
    let panGesture = UIPanGestureRecognizer(target: self, action: #selector(handleButtonPan))
    v.adCallToAction.addGestureRecognizer(panGesture)
    
    // [수정] 안드로이드와 높이 비율(315dp 근사치) 맞춤
    // 315dp는 대략 screenWidth * 0.8 + 여백 정도입니다. 기존 adHeight 로직 유지.
    let adViewHeight: CGFloat = CGFloat(adHeight)
    let horizontalMargin: CGFloat = 8.0
    
    target.addSubview(v)
    v.translatesAutoresizingMaskIntoConstraints = false
    NSLayoutConstraint.activate([
      v.leadingAnchor.constraint(equalTo: target.leadingAnchor, constant: horizontalMargin),
      v.trailingAnchor.constraint(equalTo: target.trailingAnchor, constant: -horizontalMargin),
      v.topAnchor.constraint(equalTo: target.topAnchor), // 상단 정렬
      v.heightAnchor.constraint(equalToConstant: adViewHeight)
    ])
  }
  
  @objc func handleButtonPan(_ sender: UIPanGestureRecognizer) {
      // (기존 스크롤 제스처 로직 유지)
      guard let webView = self.bridge?.webView,
            let button = sender.view as? UIButton else {
        return
      }

      switch sender.state {
      case .began:
        initialScrollOffset = webView.scrollView.contentOffset
        button.isHighlighted = false
      case .changed:
        let translation = sender.translation(in: webView)
        let newOffset = CGPoint(x: initialScrollOffset.x, y: initialScrollOffset.y - translation.y)
        webView.scrollView.setContentOffset(newOffset, animated: false)
      case .ended, .cancelled:
        initialScrollOffset = .zero
      default:
        break
      }
  }
  
  func nativeAdDidRecordClick(_ nativeAd: NativeAd) {
      print("⚡️ iOS Native Ad Click Detected!")
      
      // [핵심] 웹뷰(Angular)로 "야! 클릭됐어!"라고 이벤트를 쏴줍니다.
      let js = "window.dispatchEvent(new CustomEvent('ad_click_detected'));"
      
      DispatchQueue.main.async {
          self.bridge?.webView?.evaluateJavaScript(js, completionHandler: nil)
      }
    }
}
