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
  // 슬롯 6개(Today's TOP 위 / Discover 위 / New Creators 위 / 세로형 Latest Content 안 3개)가 컨테이너 1~6(tag 201~206)에 1:1 로 대응
  private var lobbyMode = false

  /// 빠른 스크롤 중 페이드 상태 (2026-10-23 클라이언트 10번). 좌표가 브리지를 건너 늦게 적용되는 동안
  /// 광고가 주변과 어긋나 떨려 보이므로 웹이 faded=true 를 보내면 투명하게 두고, 멈추면 제자리에서 되살린다.
  /// 뷰는 그대로 붙어 있어 광고 로드·측정에는 영향이 없다
  private var lobbyFaded = false
  private let fadeDuration: TimeInterval = 0.12

  /// 웹이 보내는 로비 슬롯 한 개의 좌표 (css px = pt)
  private struct SlotPosition {
    let index: Int
    let y: CGFloat
    let hideAbove: CGFloat
  }

  // 로비 스크롤 직결 (2026-10-25 iOS 미세떨림).
  // 웹 좌표는 브리지를 건너 메인 스레드에 1~3프레임 늦게, 그것도 고르지 않은 간격으로 도착한다. 그 사이 콘텐츠는
  // UI 프로세스의 스크롤 뷰(ion-content 의 overflow 스크롤 = WKChildScrollView)가 이미 움직여 광고만 뒤처졌다 따라붙는다.
  // 웹은 좌표를 잰 순간의 scrollTop 을 함께 보내고, 네이티브는 그 스크롤 뷰의 contentOffset 변화를 KVO 로 받아
  // "웹 좌표 − (현재 offset − 잰 순간 scrollTop)" 으로 같은 프레임에 옮긴다. 스크롤 뷰를 못 찾으면 예전처럼 웹 좌표만 쓴다
  private var lobbySlots: [SlotPosition] = []
  private var lobbyAnchorScrollTop: CGFloat?
  private weak var lobbyScrollView: UIScrollView?
  private var lobbyScrollObservation: NSKeyValueObservation?

  #if DEBUG
  /// 진단 로그 (Xcode 콘솔, 1초 단위): 받은 좌표 수·실제 transform/표시/제약 변경 수·브리지 지연(pt)
  private var stat = (calls: 0, kvo: 0, transforms: 0, hiddenToggles: 0, wrapperMoves: 0, maxLag: CGFloat(0), since: Date())
  private func statFlush() {
    let elapsed = Date().timeIntervalSince(stat.since)
    guard elapsed >= 1 else { return }
    if stat.calls + stat.kvo > 0 {
      NSLog("[lobbyAd] %.1fs calls=%d kvo=%d transforms=%d hiddenToggles=%d wrapperMoves=%d maxLag=%.1fpt linked=%@",
            elapsed, stat.calls, stat.kvo, stat.transforms, stat.hiddenToggles, stat.wrapperMoves, stat.maxLag,
            lobbyScrollView == nil ? "no" : "yes")
    }
    stat = (0, 0, 0, 0, 0, 0, Date())
  }
  /// 비교용: UserDefaults lobbyAdLegacySync=YES 면 스크롤 직결을 끄고 웹 좌표만 쓴다 (디버그 빌드 전용)
  private var legacySync: Bool { UserDefaults.standard.bool(forKey: "lobbyAdLegacySync") }
  #else
  private var legacySync: Bool { false }
  #endif
  
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
    // 스크롤마다 호출된다. main.sync 는 브리지 스레드를 메인 스레드가 비길 때까지 세워 지연을 키우므로 async 로 넘긴다
    guard let scrollOffset = call.getDouble("value") else {
      call.reject("Missing scrollOffset")
      return
    }
    DispatchQueue.main.async {
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
            // 로비 모드: 슬롯 6개 모두 사용. 웹 좌표(setSlotPositions) 수신 전까지 화면 밖에 대기
            self.lobbyMode = true
            self.resetLobbyFade()
            self.resetLobbyLayout()
            for c in self.lobbyContainers() {
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
            self.resetLobbyFade()
            self.resetLobbyLayout()
            self.clearLobbyClipping()
            (self.bridge?.viewController as? MainViewController)?.resetWrapperTopOffset()
            self.fixedPositionY1 = self.starPosY1
            self.fixedPositionY2 = self.starPosY2
            self.fixedPositionY3 = self.starPosY3

            c1?.isHidden = false
            c2?.isHidden = false
            c3?.isHidden = false
            self.hideLobbyOnlyContainers()
        } else {
            // 일반 탭/로비 페이지: 상단에 고정 (예: 128pt)
            self.lobbyMode = false
            self.resetLobbyLayout()
            self.clearLobbyClipping()
            (self.bridge?.viewController as? MainViewController)?.resetWrapperTopOffset()
            self.fixedPositionY1 = 128.0

            // 일반 페이지는 광고 1개만 필요하므로 숨김
            c1?.isHidden = false
            c2?.isHidden = true
            c3?.isHidden = true
            self.hideLobbyOnlyContainers()
        }

        self.reinitializePosition()

      } else {
        self.lobbyMode = false
        self.resetLobbyFade()
        self.resetLobbyLayout()
        self.clearLobbyClipping()
        c1?.isHidden = true
        c2?.isHidden = true
        c3?.isHidden = true
        self.hideLobbyOnlyContainers()
      }

      call.resolve([:])
    }
  }
  
  // 로비 슬롯 위치 갱신 — 웹이 펼쳐진 슬롯마다 플레이스홀더의 뷰포트 기준 좌표(css px = pt)와 클리핑 경계를 보낸다.
  // wrapper 상단은 가장 위 경계(헤더 하단)에 맞춰 광고가 헤더 밑으로 잘려 들어가게 하고,
  // 경계가 더 아래인 슬롯(sticky 탭 바 아래의 슬롯 3)은 컨테이너 레이어 마스크로 그 차이만큼 상단을 더 잘라낸다.
  // 목록에 없는 슬롯(아직 no-fill 또는 로드 중)은 화면 밖에 둔다 — 전체 숨김으로 인한 빈 구멍 방지
  @objc func setSlotPositions(_ call: CAPPluginCall) {
    let faded = call.getBool("faded") ?? false
    let scrollTop = call.getDouble("scrollTop").map { CGFloat($0) }
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
      self.lobbySlots = slots
      self.lobbyAnchorScrollTop = scrollTop
      if let scrollTop = scrollTop, !self.legacySync {
        self.linkLobbyScrollView(webScrollTop: scrollTop)
      } else {
        self.unlinkLobbyScrollView()
      }
      #if DEBUG
      self.stat.calls += 1
      if let sv = self.lobbyScrollView, let scrollTop = scrollTop {
        self.stat.maxLag = max(self.stat.maxLag, abs(sv.contentOffset.y - scrollTop))
      }
      #endif
      self.layoutLobbySlots()
      self.applyLobbyFade(self.lobbyContainers(), faded: faded)
    }
  }

  /// 마지막으로 받은 웹 좌표를 현재 스크롤 위치로 보정해 컨테이너에 적용한다. 좌표 수신과 스크롤(KVO) 양쪽에서 불린다.
  /// 값이 실제로 바뀐 속성만 건드리고(같은 값 재설정도 레이어를 더럽혀 커밋 비용이 든다), 좌표는 기기 픽셀 단위로 맞춘다
  private func layoutLobbySlots() {
    guard lobbyMode else { return }
    let containers = lobbyContainers()
    let shift = currentScrollShift()

    // wrapper 경계 = 받은 경계 중 가장 위 (슬롯 1·2 의 헤더 하단). 헤더·sticky 탭 바는 화면에 고정이라 보정하지 않는다.
    // 웹뷰는 화면 최상단(풀스크린)에서 시작하므로 y·hideAbove는 화면 기준 pt와 동일
    let wrapperHide = pixelRound(lobbySlots.map { $0.hideAbove }.min() ?? 0)
    if let mainVC = bridge?.viewController as? MainViewController,
       mainVC.setWrapperTopOffset(fromScreenTop: wrapperHide) {
      #if DEBUG
      stat.wrapperMoves += 1
      #endif
    }

    // UIView 속성 변경이 다른 애니메이션 블록에 휩쓸려 보간되지 않게 한다
    CATransaction.begin()
    CATransaction.setDisableActions(true)
    var placed = [Bool](repeating: false, count: containers.count)
    for s in lobbySlots where s.index >= 0 && s.index < containers.count {
      guard let c = containers[s.index] else { continue }
      let y = pixelRound(s.y - shift)
      if c.isHidden {
        c.isHidden = false
        #if DEBUG
        stat.hiddenToggles += 1
        #endif
      }
      let ty = y - wrapperHide
      if c.transform.tx != 0 || abs(c.transform.ty - ty) > 0.01 {
        c.transform = CGAffineTransform(translationX: 0, y: ty)
        #if DEBUG
        stat.transforms += 1
        #endif
      }

      // 이 슬롯만의 추가 경계(탭 바 하단)가 wrapper 경계보다 아래이고 카드 상단이 그 경계 위로 올라갔으면 그만큼 자른다.
      // 마스크는 뷰 자신의 bounds 좌표계라 transform 과 무관하다
      let extraClip = s.hideAbove - y
      if s.hideAbove > wrapperHide + 0.5 && extraClip > 0 {
        applyTopClip(c, top: min(extraClip, c.bounds.height))
      } else if c.layer.mask != nil {
        c.layer.mask = nil
      }
      placed[s.index] = true
    }
    for (i, c) in containers.enumerated() where !placed[i] {
      if let c = c, !c.isHidden {
        parkOffscreen(c)
        #if DEBUG
        stat.hiddenToggles += 1
        #endif
      }
    }
    CATransaction.commit()
    #if DEBUG
    statFlush()
    #endif
  }

  /// 웹이 좌표를 잰 뒤 실제로 더 스크롤된 양(pt). 스크롤 뷰가 연결되지 않았으면 0 (웹 좌표 그대로)
  private func currentScrollShift() -> CGFloat {
    guard let anchor = lobbyAnchorScrollTop, let sv = lobbyScrollView, sv.window != nil else { return 0 }
    return sv.contentOffset.y - anchor
  }

  /// 화면 배율(2x/3x) 픽셀 경계로 반올림 — 소수점 좌표는 광고 글자·이미지를 매 프레임 다시 보간해 흔들려 보이게 한다
  private func pixelRound(_ v: CGFloat) -> CGFloat {
    let scale = bridge?.viewController?.view.window?.screen.scale ?? UIScreen.main.scale
    return (v * scale).rounded() / scale
  }

  /// ion-content 의 스크롤 뷰를 찾아 contentOffset 을 구독한다. 이미 연결돼 있으면 그대로 둔다.
  /// 후보는 웹뷰 안의 세로 스크롤 뷰 중 contentOffset 이 웹 scrollTop 과 맞는 것. 맨 위(scrollTop≈0)에서는
  /// 다른 스크롤 영역과 구분이 안 되므로 확실히 하나로 좁혀질 때만 연결한다
  private func linkLobbyScrollView(webScrollTop: CGFloat) {
    if let sv = lobbyScrollView, sv.window != nil {
      // 브리지 지연만큼의 차이는 정상이다. 터무니없이 어긋나면(페이지 교체 등) 다시 찾는다
      if abs(sv.contentOffset.y - webScrollTop) < max(800, sv.bounds.height) { return }
      unlinkLobbyScrollView()
    }
    guard webScrollTop > 20, let webView = bridge?.webView else { return }
    var candidates: [UIScrollView] = []
    func walk(_ v: UIView) {
      for sub in v.subviews {
        if let sv = sub as? UIScrollView, !sv.isHidden,
           sv.contentSize.height > sv.bounds.height + 1,
           sv.bounds.height > webView.bounds.height * 0.5,
           abs(sv.contentOffset.y - webScrollTop) < 2 {
          candidates.append(sv)
        }
        walk(sub)
      }
    }
    walk(webView.scrollView)
    guard candidates.count == 1, let sv = candidates.first else { return }
    lobbyScrollView = sv
    lobbyScrollObservation = sv.observe(\.contentOffset, options: []) { [weak self] _, _ in
      guard let self = self, self.lobbyMode else { return }
      #if DEBUG
      self.stat.kvo += 1
      #endif
      self.layoutLobbySlots()
    }
  }

  private func unlinkLobbyScrollView() {
    lobbyScrollObservation?.invalidate()
    lobbyScrollObservation = nil
    lobbyScrollView = nil
  }

  /// 로비 모드 진입·이탈 시 지난 좌표와 스크롤 연결을 버린다
  private func resetLobbyLayout() {
    lobbySlots = []
    lobbyAnchorScrollTop = nil
    unlinkLobbyScrollView()
  }

  /// 페이드 상태가 바뀔 때만 모든 로비 컨테이너의 알파를 짧게 애니메이션한다. 위치는 이미 맞춰진 뒤라 제자리에서 나타난다
  private func applyLobbyFade(_ containers: [UIView?], faded: Bool) {
    if faded == lobbyFaded { return }
    lobbyFaded = faded
    let target: CGFloat = faded ? 0 : 1
    let views = containers.compactMap { $0 }
    UIView.animate(withDuration: fadeDuration, delay: 0, options: [.beginFromCurrentState, .allowUserInteraction]) {
      views.forEach { $0.alpha = target }
    }
  }

  /// 로비 모드를 벗어나거나 광고를 숨길 때 페이드 상태를 원래대로 (스타 페이지는 페이드를 쓰지 않는다)
  private func resetLobbyFade() {
    lobbyFaded = false
    for c in lobbyContainers() {
      c?.layer.removeAllAnimations()
      c?.alpha = 1
    }
  }

  /// 로비 슬롯 순서(0~5)대로의 컨테이너 배열 (tag 201~206). 4~6 은 2-34차 세로형 Latest Content 안
  private func lobbyContainers() -> [UIView?] {
    return [getAdContainer1(), getAdContainer2(), getAdContainer3()] + lobbyOnlyContainers()
  }

  /// 로비에서만 쓰는 컨테이너 (tag 204~206)
  private func lobbyOnlyContainers() -> [UIView?] {
    return MainViewController.lobbyOnlyTags.map { self.bridge?.viewController?.view.viewWithTag($0) }
  }

  /// 스타 페이지·일반 모드·숨김 전환 시 로비 전용 컨테이너를 숨긴다
  private func hideLobbyOnlyContainers() {
    for c in lobbyOnlyContainers() {
      c?.layer.mask = nil
      c?.isHidden = true
    }
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
