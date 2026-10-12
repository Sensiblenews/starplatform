import Capacitor

@objc(NativeBridge)
public class NativeBridge: CAPPlugin, CAPBridgedPlugin {
  
  public let identifier = "NativeBridgePlugin"
  public let jsName = "NativeBridge"
  public let pluginMethods: [CAPPluginMethod] = [
    CAPPluginMethod(name: "updateAdPosition", returnType: CAPPluginReturnPromise),
    CAPPluginMethod(name: "setShow", returnType: CAPPluginReturnPromise),
    CAPPluginMethod(name: "setSlotPositions", returnType: CAPPluginReturnPromise),
    CAPPluginMethod(name: "setViewportHeight", returnType: CAPPluginReturnPromise)
  ]

  // 현재 적용 중인 위치
  private var fixedPositionY1: CGFloat = 0
  private var fixedPositionY2: CGFloat = 0
  private var fixedPositionY3: CGFloat = 0

  // 로비 모드: 위치를 하드코딩 수식이 아니라 웹이 보내는 좌표(setSlotPositions)로 제어.
  // 슬롯 3개(Today's TOP 위 / Discover 위 / New Creators 위)가 컨테이너 1~3(tag 201~203)에 1:1 로 대응
  private var lobbyMode = false

  /// 웹이 보내는 로비 슬롯 한 개의 좌표 (css px = pt)
  private struct SlotPosition {
    let index: Int
    let y: CGFloat
    let hideAbove: CGFloat
  }
  
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
        if page == "lobby" {
            // 로비 모드: 슬롯 3개 모두 사용. 웹 좌표(setSlotPositions) 수신 전까지 화면 밖에 대기
            self.lobbyMode = true
            for c in [c1, c2, c3] {
              self.parkOffscreen(c)
            }

            if let mainVC = self.bridge?.viewController as? MainViewController {
              mainVC.loadLobbyAd()
            }
            call.resolve([:])
            return
        } else if page.contains("star") {
            // 스타 페이지: 미리 계산해둔 3개의 피드 슬롯 위치 복구
            self.lobbyMode = false
            self.clearLobbyClipping()
            (self.bridge?.viewController as? MainViewController)?.resetWrapperTopOffset()
            self.fixedPositionY1 = self.starPosY1
            self.fixedPositionY2 = self.starPosY2
            self.fixedPositionY3 = self.starPosY3

            c1?.isHidden = false
            c2?.isHidden = false
            c3?.isHidden = false
        } else {
            // 일반 탭/로비 페이지: 상단에 고정 (예: 128pt)
            self.lobbyMode = false
            self.clearLobbyClipping()
            (self.bridge?.viewController as? MainViewController)?.resetWrapperTopOffset()
            self.fixedPositionY1 = 128.0

            // 일반 페이지는 광고 1개만 필요하므로 숨김
            c1?.isHidden = false
            c2?.isHidden = true
            c3?.isHidden = true
        }

        self.reinitializePosition()

      } else {
        self.lobbyMode = false
        self.clearLobbyClipping()
        c1?.isHidden = true
        c2?.isHidden = true
        c3?.isHidden = true
      }

      call.resolve([:])
    }
  }
  
  // 로비 슬롯 위치 갱신 — 웹이 펼쳐진 슬롯마다 플레이스홀더의 뷰포트 기준 좌표(css px = pt)와 클리핑 경계를 보낸다.
  // wrapper 상단은 가장 위 경계(헤더 하단)에 맞춰 광고가 헤더 밑으로 잘려 들어가게 하고,
  // 경계가 더 아래인 슬롯(sticky 탭 바 아래의 슬롯 3)은 컨테이너 레이어 마스크로 그 차이만큼 상단을 더 잘라낸다.
  // 목록에 없는 슬롯(아직 no-fill 또는 로드 중)은 화면 밖에 둔다 — 전체 숨김으로 인한 빈 구멍 방지
  @objc func setSlotPositions(_ call: CAPPluginCall) {
    var slots: [SlotPosition] = []
    for item in call.getArray("slots") ?? [] {
      guard let dict = item as? [String: Any],
            let y = (dict["y"] as? NSNumber)?.doubleValue else { continue }
      let index = (dict["index"] as? NSNumber)?.intValue ?? -1
      let hideAbove = (dict["hideAbove"] as? NSNumber)?.doubleValue ?? 0
      slots.append(SlotPosition(index: index, y: CGFloat(y), hideAbove: CGFloat(hideAbove)))
    }

    DispatchQueue.main.async {
      defer { call.resolve([:]) }
      guard self.lobbyMode else { return }
      let containers = self.lobbyContainers()

      // wrapper 경계 = 받은 경계 중 가장 위 (슬롯 1·2 의 헤더 하단).
      // 웹뷰는 화면 최상단(풀스크린)에서 시작하므로 y·hideAbove는 화면 기준 pt와 동일
      let wrapperHide = slots.map { $0.hideAbove }.min() ?? 0
      if let mainVC = self.bridge?.viewController as? MainViewController {
        mainVC.setWrapperTopOffset(fromScreenTop: wrapperHide)
      }

      var placed = [Bool](repeating: false, count: containers.count)
      for s in slots where s.index >= 0 && s.index < containers.count {
        guard let c = containers[s.index] else { continue }
        c.isHidden = false
        c.transform = CGAffineTransform(translationX: 0, y: s.y - wrapperHide)

        // 이 슬롯만의 추가 경계(탭 바 하단)가 wrapper 경계보다 아래이고 카드 상단이 그 경계 위로 올라갔으면 그만큼 자른다.
        // 마스크는 뷰 자신의 bounds 좌표계라 transform 과 무관하다
        let extraClip = s.hideAbove - s.y
        if s.hideAbove > wrapperHide + 0.5 && extraClip > 0 {
          self.applyTopClip(c, top: min(extraClip, c.bounds.height))
        } else {
          c.layer.mask = nil
        }
        placed[s.index] = true
      }
      for (i, c) in containers.enumerated() where !placed[i] {
        self.parkOffscreen(c)
      }
    }
  }

  /// 로비 슬롯 순서(0~2)대로의 컨테이너 배열
  private func lobbyContainers() -> [UIView?] {
    return [getAdContainer1(), getAdContainer2(), getAdContainer3()]
  }

  /// 로비 모드에서 좌표를 아직 못 받은(또는 광고가 없는) 컨테이너를 화면 밖에 둔다
  private func parkOffscreen(_ c: UIView?) {
    c?.layer.mask = nil
    c?.isHidden = true
    c?.transform = CGAffineTransform(translationX: 0, y: 100000)
  }

  /// 컨테이너 상단 top(pt)만큼을 가리는 마스크. 스크롤마다 호출되므로 암시적 애니메이션은 끈다
  private func applyTopClip(_ c: UIView, top: CGFloat) {
    let mask = (c.layer.mask as? CALayer) ?? CALayer()
    CATransaction.begin()
    CATransaction.setDisableActions(true)
    mask.backgroundColor = UIColor.black.cgColor
    mask.frame = CGRect(x: 0, y: top, width: c.bounds.width, height: max(0, c.bounds.height - top))
    CATransaction.commit()
    if c.layer.mask !== mask {
      c.layer.mask = mask
    }
  }

  /// 로비 모드를 벗어날 때 슬롯별 마스크를 모두 해제한다
  private func clearLobbyClipping() {
    for c in lobbyContainers() {
      c?.layer.mask = nil
    }
  }

  @objc func setViewportHeight(_ call: CAPPluginCall) {
    call.resolve([:])
  }

  func updateAd(scrollOffset: Double) {
    // 로비 모드에서는 위치를 setSlotPositions가 전담한다 (스타 페이지 수식 오염 방지)
    if lobbyMode {
      return
    }
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
