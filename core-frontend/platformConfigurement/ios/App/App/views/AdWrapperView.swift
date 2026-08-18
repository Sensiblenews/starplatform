// AdWrapper.swift
import UIKit

class AdWrapper: UIView {
  
  // Ad Choices 아이콘이 나타날 것으로 예상되는 영역의 크기 (임의 설정, 조정 필요)
  private let adChoicesAreaSize: CGFloat = 40.0
  
  override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
    for subview in self.subviews {
      guard !subview.isHidden else { continue }
      
      if let adView = subview.subviews.first(where: { $0 is AdView }) as? AdView {
        
        // 1. CTA 버튼 영역 확인 로직 (기존 로직)
        if let ctaButton = adView.adCallToAction {
          let pointInCta = self.convert(point, to: ctaButton)
          if ctaButton.bounds.contains(pointInCta) {
            return super.hitTest(point, with: event) // CTA 터치 시 전달
          }
        }
        
        // 2. Ad Choices 아이콘 영역 확인 로직 (추가)
        let pointInAdView = self.convert(point, to: adView)
        
        // Ad Choices 아이콘은 AdView의 우측 상단 모서리(topRightCorner)에 위치함.
        // AdView 좌표계에서 Ad Choices 영역을 추정합니다.
        let adChoicesRect = CGRect(
          x: adView.bounds.width - adChoicesAreaSize, // 우측 끝에서 왼쪽으로
          y: 0,
          width: adChoicesAreaSize,
          height: adChoicesAreaSize
        )
        
        if adChoicesRect.contains(pointInAdView) {
          // Ad Choices 예상 영역 터치 시 전달
          return super.hitTest(point, with: event)
        }
      }
    }
    
    // CTA나 Ad Choices 영역이 아니면 nil을 반환하여 웹뷰(스크롤)로 이벤트를 전달
    return nil
  }
}
