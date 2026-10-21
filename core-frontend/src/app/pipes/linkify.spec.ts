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

  // ===== 2-35차: https:// 없이 도메인 접미사만으로도 링크 =====
  const a = (url: string, href = `https://${url}`) => `<a class="body-link" href="${href}" rel="noopener noreferrer">${url}</a>`;

  it('앞에 아무것도 없어도 .com·.co.kr·.org 등으로 끝나면 링크', () => {
    expect(linkifyText('naver.com')).toBe(a('naver.com'));
    expect(linkifyText('홈페이지 witch-hunting.com 방문')).toBe(`홈페이지 ${a('witch-hunting.com')} 방문`);
    expect(linkifyText('daum.co.kr/news?id=1')).toBe(a('daum.co.kr/news?id=1'));
    expect(linkifyText('wikipedia.org')).toBe(a('wikipedia.org'));
    expect(linkifyText('m.blog.naver.com/abc')).toBe(a('m.blog.naver.com/abc'));
    expect(linkifyText('youtu.be/xyz')).toBe(a('youtu.be/xyz'));
  });

  it('한글 조사·괄호·따옴표가 붙어도 주소만 링크', () => {
    expect(linkifyText('naver.com에서 확인')).toBe(`${a('naver.com')}에서 확인`);
    expect(linkifyText('(naver.com)')).toBe(`(${a('naver.com')})`);
    expect(linkifyText('"naver.com"')).toBe(`&quot;${a('naver.com')}&quot;`);
  });

  it('파일명·버전·이메일·약어는 링크로 만들지 않는다', () => {
    expect(linkifyText('photo.jpg')).toBe('photo.jpg');
    expect(linkifyText('v2.0 update')).toBe('v2.0 update');
    expect(linkifyText('e.g. this')).toBe('e.g. this');
    expect(linkifyText('mail me@naver.com')).toBe('mail me@naver.com');
    expect(linkifyText('example.community')).toBe('example.community');
  });

  it('https:// 주소 안의 도메인을 두 번 링크하지 않는다', () => {
    expect(linkifyText('https://naver.com/a.com')).toBe(a('https://naver.com/a.com', 'https://naver.com/a.com'));
  });
});

