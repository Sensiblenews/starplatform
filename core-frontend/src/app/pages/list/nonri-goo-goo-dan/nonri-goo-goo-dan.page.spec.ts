import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { NonriGooGooDanPage } from './nonri-goo-goo-dan.page';

describe('NonriGooGooDanPage', () => {
  let component: NonriGooGooDanPage;
  let fixture: ComponentFixture<NonriGooGooDanPage>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ NonriGooGooDanPage ],
      imports: [IonicModule.forRoot()]
    }).compileComponents();

    fixture = TestBed.createComponent(NonriGooGooDanPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
