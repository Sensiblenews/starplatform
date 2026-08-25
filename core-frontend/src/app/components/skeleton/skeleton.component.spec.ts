import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SkeletonComponent } from './skeleton.component';

describe('SkeletonComponent', () => {
  let fixture: ComponentFixture<SkeletonComponent>;
  let component: SkeletonComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonComponent);
    component = fixture.componentInstance;
  });

  const render = (): HTMLElement => {
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  it('생성된다', () => {
    expect(component).toBeTruthy();
  });

  it('기본 variant는 list다', () => {
    expect(component.variant).toBe('list');
    expect(render().querySelectorAll('.sk-list-row').length).toBeGreaterThan(0);
  });

  it('count만큼 행을 그린다', () => {
    component.variant = 'ranking';
    component.count = 10;
    expect(render().querySelectorAll('.sk-ranking-row').length).toBe(10);
  });

  it('ranking 행은 순위·아바타·3단 텍스트를 갖는다', () => {
    component.variant = 'ranking';
    component.count = 1;
    const row = render().querySelector('.sk-ranking-row') as HTMLElement;

    expect(row.querySelector('.sk-rank-no')).toBeTruthy();
    expect(row.querySelector('.sk-avatar')).toBeTruthy();
    expect(row.querySelectorAll('.sk-line').length).toBe(3);
  });

  it('vs variant는 좌우 아바타와 하단 버튼을 그린다', () => {
    component.variant = 'vs';
    const el = render();

    // 아바타 동그라미는 표시하지 않는다(클라이언트 요청) — 빈 본문이 높이만 차지한다
    expect(el.querySelectorAll('.sk-vs-avatar').length).toBe(0);
    expect(el.querySelector('.sk-vs-body')).toBeTruthy();
    expect(el.querySelector('.sk-vs-foot')).toBeTruthy();
  });

  it('page variant는 프로필 헤더와 지표 3칸을 그린다', () => {
    component.variant = 'page';
    const el = render();

    expect(el.querySelector('.sk-avatar-lg')).toBeTruthy();
    expect(el.querySelectorAll('.sk-stat').length).toBe(3);
  });

  it('card variant는 horizontal 지정 시 클래스를 붙인다', () => {
    component.variant = 'card';
    component.count = 3;
    component.horizontal = true;
    const el = render();

    expect(el.querySelectorAll('.sk-card').length).toBe(3);
    expect(el.querySelector('.sk-card-strip')!.classList).toContain('horizontal');
  });

  it('지정한 variant 외의 마크업은 그리지 않는다', () => {
    component.variant = 'vs';
    const el = render();

    expect(el.querySelector('.sk-ranking-row')).toBeNull();
    expect(el.querySelector('.sk-list-row')).toBeNull();
    expect(el.querySelector('.sk-page')).toBeNull();
  });
});
