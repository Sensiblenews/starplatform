import { PerfTraceService } from './perf-trace.service';

describe('PerfTraceService', () => {

  afterEach(() => {
    localStorage.removeItem('perfTrace');
  });

  it('기본값은 꺼짐이다 (배포 빌드에 부담을 주지 않는다)', () => {
    localStorage.removeItem('perfTrace');
    expect(new PerfTraceService().isEnabled).toBeFalse();
  });

  it("localStorage의 perfTrace가 '1'이면 켜진다", () => {
    localStorage.setItem('perfTrace', '1');
    expect(new PerfTraceService().isEnabled).toBeTrue();
  });

  it('꺼져 있으면 아무것도 출력하지 않는다', () => {
    localStorage.removeItem('perfTrace');
    const service = new PerfTraceService();
    const table = spyOn(console, 'table');
    const log = spyOn(console, 'log');

    service.start('t');
    service.mark('a');
    service.end();

    expect(table).not.toHaveBeenCalled();
    expect(log).not.toHaveBeenCalled();
  });

  it('켜져 있으면 구간을 표로 출력한다', () => {
    localStorage.setItem('perfTrace', '1');
    const service = new PerfTraceService();
    const table = spyOn(console, 'table');
    spyOn(console, 'log');

    service.start('스타페이지 진입');
    service.mark('api');
    service.end('render');

    expect(table).toHaveBeenCalled();
    const rows = table.calls.mostRecent().args[0] as any[];
    expect(rows.length).toBe(3);
    expect(rows.map(r => r['구간'])).toEqual(['start', 'api', 'render']);
  });

  it('start() 없이 mark/end만 부르면 무시한다', () => {
    localStorage.setItem('perfTrace', '1');
    const service = new PerfTraceService();
    const table = spyOn(console, 'table');

    service.mark('a');
    service.end();

    expect(table).not.toHaveBeenCalled();
  });

  it('end()를 두 번 불러도 한 번만 출력한다', () => {
    localStorage.setItem('perfTrace', '1');
    const service = new PerfTraceService();
    const table = spyOn(console, 'table');
    spyOn(console, 'log');

    service.start('t');
    service.end();
    service.end();

    expect(table).toHaveBeenCalledTimes(1);
  });
});
