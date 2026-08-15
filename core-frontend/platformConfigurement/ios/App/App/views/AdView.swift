import UIKit
import GoogleMobileAds

class AdView: NativeAdView {
  
  // MARK: - Outlets
  
  // XIB 파일의 UI 요소들과 연결되는 아울렛들입니다.
  
  /// "AD" 텍스트 라벨
  @IBOutlet weak var adLabel: UILabel!
  
  /// 동영상 또는 이미지를 표시하는 미디어 뷰
  @IBOutlet weak var adMediaView: MediaView!
  
  /// 광고 제목 라벨
  @IBOutlet weak var adHeadline: UILabel!
  
  /// 광고주 아이콘 이미지 뷰
  @IBOutlet weak var adIcon: UIImageView!
  
  /// 광고 본문 라벨 (기존 Advertiser 라벨을 대체)
  @IBOutlet weak var adBody: UILabel!
  
  /// 클릭 유도(Call To Action) 버튼
  @IBOutlet weak var adCallToAction: UIButton!
  
  
  // MARK: - View Lifecycle
  
  override func awakeFromNib() {
    super.awakeFromNib()
    
    // 기존 로직을 그대로 유지합니다.
    adCallToAction.accessibilityIdentifier = "adCTA"
  }
}
