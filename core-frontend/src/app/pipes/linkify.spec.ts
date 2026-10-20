import { linkifyText } from './linkify';

describe('linkifyText (2-35차 본문·소개문 링크)', () => {
  it('http(s) 주소를 링크로 바꾼다', () => {
    expect(linkifyText('see https://witch-hunting.com/star/1 now'))
      .toBe('see <a class="body-link" href="https://witch-hunting.com/star/1" rel="noopener noreferrer">https://witch-hunting.com/star/1</a> now');
  });

  it('www. 주소는 https 를 붙여 연다', () => {
    expect(linkifyText('www.example.com'))
      .toBe('<a class="body-link" href="https://www.example.com" rel="noopener noreferrer">www.example.com</a>');
  });

  it('문장 끝 마침표는 링크에서 뺀다', () => {
    expect(linkifyText('visit https://a.com.'))
      .toBe('visit <a class="body-link" href="https://a.com" rel="noopener noreferrer">https://a.com</a>.');
  });

  it('HTML 은 실행되지 않게 escape 하고 줄바꿈은 <br /> 로', () => {
    expect(linkifyText('<b>hi</b>\n🌍 STAR')).toBe('&lt;b&gt;hi&lt;/b&gt;<br />🌍 STAR');
  });

  it('주소 안 따옴표로 속성을 깨지 못한다', () => {
    const html = linkifyText('https://a.com/"onmouseover="x');
    expect(html).not.toContain('"onmouseover');
  });
});
