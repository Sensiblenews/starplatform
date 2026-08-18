import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { VsCard, VsCarouselComponent } from './vs-carousel.component';

describe('VsCarouselComponent', () => {
  let component: VsCarouselComponent;
  let fixture: ComponentFixture<VsCarouselComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  const makeCard = (leftScore: number, rightScore: number | null): VsCard => ({
    vsId: 1,
    type: 'GLOBAL',
    category: 'STAR',
    left: { id: 'star_1', name: 'A', image: '', score: leftScore },
    right: rightScore === null ? null : { id: 'star_2', name: 'B', image: '', score: rightScore }
  });

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [VsCarouselComponent],
      providers: [{ provide: Router, useValue: routerSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(VsCarouselComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    component.stopAutoPlay();
  });

  describe('아바타 모션 (자동 순환과 함께 on/off)', () => {
    it('startAutoPlay는 모션을 켠다', () => {
      component.stopAutoPlay();
      component.startAutoPlay();
      expect(component.isMotionActive).toBeTrue();
    });

    it('stopAutoPlay는 모션도 함께 멈춘다 (화면 이탈 시 배터리 낭비 방지)', () => {
      component.startAutoPlay();
      component.stopAutoPlay();
      expect(component.isMotionActive).toBeFalse();
    });
  });

  describe('getScoreUnit (점수 단위 표기)', () => {
    it('DAILY 카드는 views로 표기한다', () => {
      const card = { ...makeCard(1, 1), type: 'DAILY' as const };
      expect(component.getScoreUnit(card)).toBe('views');
    });

    it('GLOBAL·CUSTOM 카드는 종합점수라 pts로 표기한다', () => {
      expect(component.getScoreUnit(makeCard(1, 1))).toBe('pts');
      const custom = { ...makeCard(1, 1), type: 'CUSTOM' as const };
      expect(component.getScoreUnit(custom)).toBe('pts');
    });
  });

  describe('getLeftRatio (비율 게이지 예외 처리)', () => {
    it('둘 다 0점이면 50:50으로 나눈다 (0 나눗셈 방지)', () => {
      expect(component.getLeftRatio(makeCard(0, 0))).toBe(50);
    });

    it('격차가 극단적이어도 10~90%로 보정한다', () => {
      expect(component.getLeftRatio(makeCard(1000000, 0))).toBe(90);
      expect(component.getLeftRatio(makeCard(0, 1000000))).toBe(10);
    });

    it('일반적인 경우 실제 비율을 반환한다', () => {
      expect(component.getLeftRatio(makeCard(60, 40))).toBe(60);
    });

    it('도전자(right)가 없으면 100을 반환한다 (게이지 자체는 미노출)', () => {
      expect(component.getLeftRatio(makeCard(10, null))).toBe(100);
    });
  });

  describe('카드 순환', () => {
    beforeEach(() => {
      component.cards = [makeCard(1, 2), makeCard(3, 4), makeCard(5, 6)];
    });

    it('next()는 마지막 카드에서 처음으로 되돌아간다', () => {
      component.currentIndex = 2;
      component.next();
      expect(component.currentIndex).toBe(0);
    });

    it('prev()는 첫 카드에서 마지막으로 되돌아간다', () => {
      component.currentIndex = 0;
      component.prev();
      expect(component.currentIndex).toBe(2);
    });

    it('폴링 갱신으로 카드 수가 줄면 인덱스를 처음으로 복귀시킨다', () => {
      component.currentIndex = 2;
      component.cards = [makeCard(1, 2)];
      expect(component.currentIndex).toBe(0);
    });

    it('카드가 없으면 next()가 아무것도 하지 않는다', () => {
      component.cards = [];
      component.next();
      expect(component.currentIndex).toBe(0);
    });
  });

  describe('클릭 동선', () => {
    it('좌/우 영역 탭 시 해당 스타 프로필로 이동한다', () => {
      const card = makeCard(1, 2);
      component.goToProfile(card.left, new Event('click'));
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/star', 'star_1']);
    });

    it('중앙 VS 탭 시 centerSelect 이벤트를 발행한다', () => {
      const card = makeCard(1, 2);
      spyOn(component.centerSelect, 'emit');
      component.onCenterClick(card);
      expect(component.centerSelect.emit).toHaveBeenCalledWith(card);
    });
  });

  describe('점수 표기', () => {
    it('천/백만 단위를 K/M으로 축약한다', () => {
      expect(component.formatScore(999)).toBe('999');
      expect(component.formatScore(12300)).toBe('12.3K');
      expect(component.formatScore(3920000)).toBe('3.9M');
    });
  });
});
