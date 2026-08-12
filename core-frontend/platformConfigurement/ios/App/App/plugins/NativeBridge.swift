import Capacitor

@objc(NativeBridge)
public class NativeBridge: CAPPlugin, CAPBridgedPlugin {
  
  public let identifier = "NativeBridgePlugin"
  public let jsName = "NativeBridge"
  public let pluginMethods: [CAPPluginMethod] = [
    CAPPluginMethod(name: "updateAdPosition", returnType: CAPPluginReturnPromise),
    CAPPluginMethod(name: "setShow", returnType: CAPPluginReturnPromise),
    CAPPluginMethod(name: "setViewportHeight", returnType: CAPPluginReturnPromise)
  ]
  
  // 현재 적용 중인 위치
  private var fixedPositionY1: CGFloat = 0
  private var fixedPositionY2: CGFloat = 0
  private var fixedPositionY3: CGFloat = 0
  
  // [신규] load()에서 계산한 스타 페이지 전용 위치 백업
  private var starPosY1: CGFloat = 0
  private var starPosY2: CGFloat = 0
  private var starPosY3: CGFloat = 0
  
  public override func load() {
    super.load()
    
    // UI 관련 속성은 메인 스레드에서 접근해야 함
    DispatchQueue.main.async {
      let sw = UIScreen.main.bounds.width
      
      // 1. CSS 요소들의 정확한 높이 도출 (안드로이드와 완벽 동기화)
      let headerProfileHeight: CGFloat = 178.0 // 상단 헤더(43) + 프로필 영역(85)
      
      // 일반 피드 카드 1개의 전체 높이: 여백(16) + 사진(screenWidth) + 본문(37) + 푸터(48)
      let feedItemHeight: CGFloat = sw + 136.0
      
      // 광고 슬롯 전체 높이: 여백(16) + 패딩(20) + 빈칸박스(screenWidth + 85)
      let adSlotHeight: CGFloat = sw + 101.0
      
      // 2. 각 광고 슬롯의 시작점(Y 좌표) 계산
      // Ad 1 (배열 인덱스 3): 앞에 일반 피드 3개가 있음
      let slot1Y = headerProfileHeight + (feedItemHeight * 3.0)
      
      // Ad 2 (배열 인덱스 10): 앞에 일반 피드 9개 + Ad 1개 있음
      let slot2Y = headerProfileHeight + (feedItemHeight * 9.0) + (adSlotHeight * 1.0)
      
      // Ad 3 (배열 인덱스 15): 앞에 일반 피드 13개 + Ad 2개 있음
      let slot3Y = headerProfileHeight + (feedItemHeight * 16.0) + (adSlotHeight * 2.0)
      
      // 3. 네이티브 광고(315pt)를 웹의 빈칸 박스(sw + 85) 수직 중앙에 배치하기 위한 보정치
      let placeholderTopMargin: CGFloat = 0.0
      let placeholderBoxHeight: CGFloat = sw + 85.0
      let centeringOffset: CGFloat = (placeholderBoxHeight - 315.0) / 2.0
      
      // 4. 최종 위치 계산 및 백업
      // (iOS는 포인트(pt) 단위가 안드로이드의 dp와 동일한 역할을 하므로 density를 곱할 필요가 없습니다)
      self.starPosY1 = slot1Y + placeholderTopMargin + centeringOffset
      self.starPosY2 = slot2Y + placeholderTopMargin + centeringOffset
      self.starPosY3 = slot3Y + placeholderTopMargin + centeringOffset
      
      // 기본값 세팅
      self.fixedPositionY1 = self.starPosY1
      self.fixedPositionY2 = self.starPosY2
      self.fixedPositionY3 = self.starPosY3
    }
  }
  
  @objc func updateAdPosition(_ call: CAPPluginCall) {
    DispatchQueue.main.sync {
      guard let scrollOffset = call.getDouble("value") else {
        call.reject("Missing scrollOffset")
        return
      }
      self.updateAd(scrollOffset: scrollOffset)
      call.resolve([:])
    }
  }
  
  @objc func setShow(_ call: CAPPluginCall) {
    DispatchQueue.main.async {
      guard let show = call.getBool("show") else {
        call.reject("Missing parameter 'show'")
        return
      }
      
      // 프론트엔드에서 넘겨준 page(url) 값 받기
      let page = call.getString("page") ?? ""
      
      let c1 = self.getAdContainer1()
      let c2 = self.getAdContainer2()
      let c3 = self.getAdContainer3()
      
      if show {
        // [핵심] 페이지에 따른 fixedPosition 분기 처리
        if page.contains("star") {
            // 스타 페이지: 미리 계산해둔 3개의 피드 슬롯 위치 복구
            self.fixedPositionY1 = self.starPosY1
            self.fixedPositionY2 = self.starPosY2
            self.fixedPositionY3 = self.starPosY3
            
            c1?.isHidden = false
            c2?.isHidden = false
            c3?.isHidden = false
        } else {
            // 일반 탭/로비 페이지: 상단에 고정 (예: 128pt)
            self.fixedPositionY1 = 128.0
            
            // 일반 페이지는 광고 1개만 필요하므로 숨김
            c1?.isHidden = false
            c2?.isHidden = true
            c3?.isHidden = true
        }
        
        self.reinitializePosition()
        
      } else {
        c1?.isHidden = true
        c2?.isHidden = true
        c3?.isHidden = true
      }
      
      call.resolve([:])
    }
  }
  
  @objc func setViewportHeight(_ call: CAPPluginCall) {
    call.resolve([:])
  }
  
  func updateAd(scrollOffset: Double) {
    let scrollY = CGFloat(scrollOffset)
        
    // 3개 모두 스크롤 위치 반영
    if let c1 = self.getAdContainer1() {
        c1.transform = CGAffineTransform(translationX: 0, y: fixedPositionY1 - scrollY)
    }
    if let c2 = self.getAdContainer2() {
        c2.transform = CGAffineTransform(translationX: 0, y: fixedPositionY2 - scrollY)
    }
    if let c3 = self.getAdContainer3() {
        c3.transform = CGAffineTransform(translationX: 0, y: fixedPositionY3 - scrollY)
    }
  }

  func reinitializePosition() {
    if let c1 = self.getAdContainer1() {
        c1.transform = CGAffineTransform(translationX: 0, y: fixedPositionY1)
    }
    if let c2 = self.getAdContainer2() {
        c2.transform = CGAffineTransform(translationX: 0, y: fixedPositionY2)
    }
    if let c3 = self.getAdContainer3() {
        c3.transform = CGAffineTransform(translationX: 0, y: fixedPositionY3)
    }
    
    // 광고 내용 새로고침
    DispatchQueue.main.async {
      if let mainVC = self.bridge?.viewController as? MainViewController {
        mainVC.loadNativeAd()
      }
    }
  }
  
  func getAdWrapper() -> UIView? { return self.bridge?.viewController?.view.viewWithTag(200) }
  func getAdContainer1() -> UIView? { return self.bridge?.viewController?.view.viewWithTag(201) }
  func getAdContainer2() -> UIView? { return self.bridge?.viewController?.view.viewWithTag(202) }
  func getAdContainer3() -> UIView? { return self.bridge?.viewController?.view.viewWithTag(203) }
}
