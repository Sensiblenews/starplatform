package com.sensible.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 파일을 HTTP Range(부분 요청) 지원으로 내보낸다 (2-32차).
 *
 * 어드민 검수 화면의 &lt;video controls&gt; 는 탐색(seek)할 때 Range 요청을 보내는데,
 * 서버가 206 으로 답하지 않으면 브라우저가 처음부터 다시 받거나 재생을 포기한다.
 * 단일 범위(bytes=start-end, bytes=start-, bytes=-suffix)만 다룬다 — 브라우저 영상 재생은 그걸로 충분하다.
 *
 * 범위 계산은 순수 함수(parseRange)로 분리해 단위 테스트한다.
 */
public final class RangeStreamUtil {

	private RangeStreamUtil() {
	}

	/**
	 * Range 헤더를 [start, end](둘 다 포함) 로 푼다.
	 *
	 * @return 범위가 없거나 형식이 아니면 null(전체 전송). 만족할 수 없는 범위면 길이 0 배열(416 응답용)
	 */
	public static long[] parseRange(String header, long length) {
		if (header == null || !header.startsWith("bytes=") || length <= 0) {
			return null;
		}
		String spec = header.substring("bytes=".length()).trim();
		// 다중 범위(a-b,c-d)는 지원하지 않는다 → 전체 전송
		if (spec.isEmpty() || spec.indexOf(',') >= 0) {
			return null;
		}
		int dash = spec.indexOf('-');
		if (dash < 0) {
			return null;
		}
		String startText = spec.substring(0, dash).trim();
		String endText = spec.substring(dash + 1).trim();
		try {
			long start;
			long end;
			if (startText.isEmpty()) {
				// bytes=-500 : 마지막 500바이트
				if (endText.isEmpty()) {
					return null;
				}
				long suffix = Long.parseLong(endText);
				if (suffix <= 0) {
					return new long[0];
				}
				start = Math.max(0, length - suffix);
				end = length - 1;
			} else {
				start = Long.parseLong(startText);
				end = endText.isEmpty() ? length - 1 : Long.parseLong(endText);
			}
			if (start < 0 || start >= length || end < start) {
				return new long[0];
			}
			if (end >= length) {
				end = length - 1;
			}
			return new long[] { start, end };
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 파일을 응답으로 내보낸다. Range 가 있으면 206 + Content-Range, 없으면 200 전체.
	 * 캐시 정책·권한 검사는 호출부가 한다 (이 메서드는 상태 코드와 본문만 책임진다).
	 */
	public static void write(File file, String contentType, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		long length = file.length();
		long[] range = parseRange(request.getHeader("Range"), length);

		response.setHeader("Accept-Ranges", "bytes");
		response.setContentType(contentType);

		if (range != null && range.length == 0) {
			response.setStatus(416); // Requested Range Not Satisfiable
			response.setHeader("Content-Range", "bytes */" + length);
			return;
		}

		long start = 0;
		long end = length - 1;
		if (range != null) {
			start = range[0];
			end = range[1];
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
			response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
		}
		long toSend = Math.max(0, end - start + 1);
		// servlet-api 2.5 에는 setContentLengthLong 이 없다. 2GB 를 넘지 않는 범위에서 int 로 내려준다
		if (toSend <= Integer.MAX_VALUE) {
			response.setContentLength((int) toSend);
		}

		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
			raf.seek(start);
			OutputStream out = response.getOutputStream();
			byte[] buffer = new byte[64 * 1024];
			long remaining = toSend;
			while (remaining > 0) {
				int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
				if (read < 0) {
					break;
				}
				out.write(buffer, 0, read);
				remaining -= read;
			}
			out.flush();
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException ignore) {
				}
			}
		}
	}

	/** 확장자로 content-type 을 정한다. 검수 화면이 다루는 형식만 (이미지 3종 + 영상 3종) */
	public static String contentTypeFor(String fileName) {
		String ext = ImageModerationUtil.extensionOf(fileName);
		if ("png".equals(ext)) return "image/png";
		if ("webp".equals(ext)) return "image/webp";
		if ("mp4".equals(ext)) return "video/mp4";
		if ("mov".equals(ext)) return "video/quicktime";
		if ("webm".equals(ext)) return "video/webm";
		return "image/jpeg";
	}

	/** 사용하지 않는 import 경고 방지용이 아니라, 호출부가 스트림을 직접 닫을 때를 위한 보조 */
	static void closeQuietly(InputStream in) {
		if (in != null) {
			try {
				in.close();
			} catch (IOException ignore) {
			}
		}
	}
}
