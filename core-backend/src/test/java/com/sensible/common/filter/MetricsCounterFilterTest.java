package com.sensible.common.filter;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

/**
 * 응답 바이트 카운팅 스트림 검증.
 * 세는 것과 별개로 원본 스트림에 그대로 흘려보내야 한다.
 */
public class MetricsCounterFilterTest {

	@Test
	public void countingStream_쓴_바이트를_센다() throws Exception {
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		MetricsCounterFilter.CountingServletOutputStream out =
				new MetricsCounterFilter.CountingServletOutputStream(sink);

		out.write('A');
		out.write(new byte[] { 1, 2, 3 });
		out.write(new byte[] { 4, 5, 6, 7, 8 }, 1, 3);
		out.flush();

		assertEquals(7L, out.getCount());
	}

	@Test
	public void countingStream_원본_스트림에_그대로_흘려보낸다() throws Exception {
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		MetricsCounterFilter.CountingServletOutputStream out =
				new MetricsCounterFilter.CountingServletOutputStream(sink);

		out.write("hello".getBytes("UTF-8"));
		out.flush();

		assertEquals("hello", new String(sink.toByteArray(), "UTF-8"));
		assertEquals(5L, out.getCount());
	}

	@Test
	public void countingStream_아무것도_쓰지_않으면_0이다() throws Exception {
		MetricsCounterFilter.CountingServletOutputStream out =
				new MetricsCounterFilter.CountingServletOutputStream(new ByteArrayOutputStream());

		assertEquals(0L, out.getCount());
	}
}
