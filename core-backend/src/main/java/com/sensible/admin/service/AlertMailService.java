package com.sensible.admin.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * 경보 메일 발송기. 기존 mailSender 빈(context-common.xml, SMTP)을 재사용한다.
 *
 * 원칙:
 *  - 발송은 전용 스레드 1개에서 비동기로 한다. SMTP 가 멈춰도 스케줄러·요청 스레드가 같이 멈추면 안 된다.
 *  - 연결·응답 타임아웃이 설정에 없으면 10초를 넣는다. 기본값은 무한 대기다.
 *  - 실패는 3회까지 재시도(5초 → 15초). 그래도 실패면 호출 측에 실패를 알린다 — 이력에 MAIL_FAIL 로 남긴다.
 *  - 비밀번호·토큰 등 비밀값은 본문에 넣지 않는다.
 *
 * 설정 키(globals.properties): alert.mail.from(선택, 기본은 SMTP 계정), platform.id(기본 WITCH)
 */
@Service("alertMailService")
public class AlertMailService {

	private static final Logger logger = LoggerFactory.getLogger(AlertMailService.class);

	private static final int MAX_ATTEMPTS = 3;
	private static final long[] RETRY_DELAY_MS = { 5000L, 15000L };
	private static final String DEFAULT_TIMEOUT_MS = "10000";

	/** SMTP 미설정 환경에서는 주입되지 않을 수 있다 (ContactService 와 같은 방식) */
	@Autowired(required = false)
	private JavaMailSender mailSender;

	@Resource(name = "config")
	private Properties config;

	private ExecutorService executor;

	@PostConstruct
	public void init() {
		executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "alert-mail");
				t.setDaemon(true);
				return t;
			}
		});
		// 기존 빈에 타임아웃이 없으면 넣는다. XML 을 바꾸지 않고도 무한 대기를 막는 방법
		if (mailSender instanceof JavaMailSenderImpl) {
			Properties p = ((JavaMailSenderImpl) mailSender).getJavaMailProperties();
			putIfAbsent(p, "mail.smtp.connectiontimeout", DEFAULT_TIMEOUT_MS);
			putIfAbsent(p, "mail.smtp.timeout", DEFAULT_TIMEOUT_MS);
			putIfAbsent(p, "mail.smtp.writetimeout", DEFAULT_TIMEOUT_MS);
		}
		if (mailSender == null) {
			logger.warn("[AlertMail] mailSender 빈이 없어 경보 메일을 보낼 수 없다 (어드민 표시만 동작)");
		}
	}

	@PreDestroy
	public void shutdown() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	public boolean isAvailable() {
		return mailSender != null;
	}

	public String platformId() {
		String v = (config != null) ? config.getProperty("platform.id") : null;
		return (v == null || v.trim().isEmpty()) ? "WITCH" : v.trim();
	}

	/** 결과 통지. 실행 스레드는 발송 스레드다 — 가벼운 일(DB 한 줄, 상태 플래그)만 할 것 */
	public interface Callback {
		void done(boolean success, String error, int attempts);
	}

	/** 비동기 발송. 즉시 반환 */
	public void sendAsync(final List<String> to, final String subject, final String body, final Callback callback) {
		if (to == null || to.isEmpty()) {
			if (callback != null) {
				callback.done(false, "수신자 미설정", 0);
			}
			return;
		}
		if (mailSender == null) {
			if (callback != null) {
				callback.done(false, "mailSender 미구성", 0);
			}
			return;
		}
		executor.submit(new Runnable() {
			@Override
			public void run() {
				String error = null;
				int attempt = 0;
				while (attempt < MAX_ATTEMPTS) {
					attempt++;
					try {
						send(to, subject, body);
						if (callback != null) {
							callback.done(true, null, attempt);
						}
						return;
					} catch (Throwable t) {
						error = t.getClass().getSimpleName() + ": " + shortMessage(t);
						logger.warn("[AlertMail] 발송 실패 (" + attempt + "/" + MAX_ATTEMPTS + "): {}", error);
						if (attempt < MAX_ATTEMPTS) {
							try {
								Thread.sleep(RETRY_DELAY_MS[Math.min(attempt - 1, RETRY_DELAY_MS.length - 1)]);
							} catch (InterruptedException ie) {
								Thread.currentThread().interrupt();
								break;
							}
						}
					}
				}
				if (callback != null) {
					callback.done(false, error, attempt);
				}
			}
		});
	}

	/**
	 * 동기 발송 (어드민 "시험 메일" 버튼용). 발송 스레드에서 1회만 시도하고 결과를 기다린다.
	 * 큐에 다른 메일이 밀려 있으면 그 뒤에 처리되므로 대기 상한을 둔다.
	 */
	public String sendTestMail(final List<String> to, long waitMs) {
		if (to == null || to.isEmpty()) {
			return "수신자가 설정되지 않았습니다.";
		}
		if (mailSender == null) {
			return "mailSender 빈이 구성되지 않았습니다 (SMTP 미설정).";
		}
		final String subject = "[TEST] StarPlatform 서버 알림 — 플랫폼 " + platformId();
		final String body = "이 메일은 StarPlatform 경보 시스템의 시험 발송입니다.\n\n"
				+ "플랫폼: " + platformId() + "\n"
				+ "발송 시각: " + now() + "\n"
				+ "수신 확인 후 어드민 경보 센터의 발송 이력에 TEST 가 기록됐는지 확인하세요.\n";
		Future<String> f = executor.submit(new Callable<String>() {
			@Override
			public String call() {
				try {
					send(to, subject, body);
					return null;
				} catch (Throwable t) {
					return t.getClass().getSimpleName() + ": " + shortMessage(t);
				}
			}
		});
		try {
			return f.get(waitMs, TimeUnit.MILLISECONDS);
		} catch (java.util.concurrent.TimeoutException e) {
			return "발송 대기 시간(" + (waitMs / 1000) + "초) 초과 — SMTP 응답 없음";
		} catch (Exception e) {
			return e.getClass().getSimpleName() + ": " + shortMessage(e);
		}
	}

	private void send(List<String> to, String subject, String body) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setTo(to.toArray(new String[to.size()]));
		String from = (config != null) ? config.getProperty("alert.mail.from") : null;
		if (from == null || from.trim().isEmpty()) {
			if (mailSender instanceof JavaMailSenderImpl) {
				from = ((JavaMailSenderImpl) mailSender).getUsername();
			}
		}
		if (from != null && !from.trim().isEmpty()) {
			mail.setFrom(from.trim());
		}
		mail.setSubject(subject);
		mail.setText(body);
		mailSender.send(mail);
	}

	// ==========================================
	// 본문 구성 (순수 함수 — 테스트 대상)
	// ==========================================

	/** 경보 메일 제목: [등급] 플랫폼 · 항목 · 요약 */
	public static String alertSubject(String severityLabel, String platformId, String itemLabel, String kindLabel) {
		return "[" + severityLabel + "] StarPlatform " + platformId + " — " + itemLabel + " " + kindLabel;
	}

	/**
	 * 경보 본문. 요건의 항목: 등급·플랫폼·항목과 현재 수치·발생 시각·권장 조치·경보 ID.
	 */
	public static String alertBody(String kindLabel, String severityLabel, String platformId, String itemLabel,
			String valueText, String openedAt, String nowText, long alertId, String recommendation, String adminUrl) {
		StringBuilder sb = new StringBuilder();
		sb.append("StarPlatform 서버 경보 — ").append(kindLabel).append("\n\n");
		sb.append("경보 등급 : ").append(severityLabel).append("\n");
		sb.append("플랫폼    : ").append(platformId).append("\n");
		sb.append("항목      : ").append(itemLabel).append("\n");
		sb.append("현재 수치 : ").append(valueText).append("\n");
		sb.append("발생 시각 : ").append(openedAt).append("\n");
		sb.append("알림 시각 : ").append(nowText).append("\n");
		sb.append("경보 ID   : ").append(alertId > 0 ? String.valueOf(alertId) : "(DB 미기록)").append("\n\n");
		sb.append("권장 조치\n").append(recommendation).append("\n\n");
		if (adminUrl != null && !adminUrl.isEmpty()) {
			// 2-35차: 경보 센터 목록이 아니라 이 경보의 상세(현황·그래프·권장 조치·원클릭 조치)로 바로 간다
			sb.append("▶ 이 경보 확인·조치 (로그인·OTP 후 바로 열림)\n").append(adminUrl).append("\n");
		}
		sb.append("\n이 메일은 자동 발송입니다. 같은 장애는 하나의 경보로 관리되며, 등급이 높아지거나\n");
		sb.append("긴급 경보가 확인되지 않은 경우에만 다시 발송됩니다.\n");
		return sb.toString();
	}

	/** 복구 본문. 최초 경보와 같은 ID, 지속 시간, 복구 후 측정값 */
	public static String recoveryBody(String platformId, String itemLabel, String peakLabel, String openedAt,
			String resolvedAt, long durationMs, String valueText, long alertId) {
		return recoveryBody(platformId, itemLabel, peakLabel, openedAt, resolvedAt, durationMs, valueText, alertId, null);
	}

	/** 복구 본문 + 경보 상세 링크 (2-35차) */
	public static String recoveryBody(String platformId, String itemLabel, String peakLabel, String openedAt,
			String resolvedAt, long durationMs, String valueText, long alertId, String adminUrl) {
		StringBuilder sb = new StringBuilder();
		sb.append("StarPlatform 서버 경보 — 복구\n\n");
		sb.append("플랫폼      : ").append(platformId).append("\n");
		sb.append("항목        : ").append(itemLabel).append("\n");
		sb.append("최고 등급   : ").append(peakLabel).append("\n");
		sb.append("발생 시각   : ").append(openedAt).append("\n");
		sb.append("복구 시각   : ").append(resolvedAt).append("\n");
		sb.append("지속 시간   : ").append(durationText(durationMs)).append("\n");
		sb.append("복구 후 수치: ").append(valueText).append("\n");
		sb.append("경보 ID     : ").append(alertId > 0 ? String.valueOf(alertId) : "(DB 미기록)").append("\n");
		if (adminUrl != null && !adminUrl.isEmpty()) {
			sb.append("\n▶ 경과·조치 기록 보기\n").append(adminUrl).append("\n");
		}
		return sb.toString();
	}

	/**
	 * 메일에 넣을 경보 상세 주소 (순수 함수, 2-35차).
	 * 설정(alert.admin.url)이 경보 센터 주소(…/center.do)면 상세(…/detail.do?id=N)로 바꾸고,
	 * 다른 주소면 alertId 파라미터를 붙인다 (경보 센터가 그 파라미터를 받으면 상세로 보낸다).
	 */
	public static String detailUrl(String centerUrl, long alertId) {
		if (centerUrl == null || centerUrl.trim().isEmpty() || alertId <= 0) {
			return centerUrl;
		}
		String base = centerUrl.trim();
		if (base.endsWith("/center.do")) {
			return base.substring(0, base.length() - "/center.do".length()) + "/detail.do?id=" + alertId;
		}
		return base + (base.contains("?") ? "&" : "?") + "alertId=" + alertId;
	}

	public static String durationText(long ms) {
		if (ms < 0) {
			ms = 0;
		}
		long totalMin = ms / 60000L;
		long h = totalMin / 60;
		long m = totalMin % 60;
		if (h == 0) {
			return m + "분";
		}
		return h + "시간 " + m + "분";
	}

	/** 쉼표·세미콜론·공백으로 구분된 수신자 목록 정리 */
	public static List<String> parseRecipients(String raw) {
		List<String> out = new ArrayList<String>();
		if (raw == null) {
			return out;
		}
		for (String s : raw.split("[,;\\s]+")) {
			String t = s.trim();
			if (!t.isEmpty() && t.contains("@") && !out.contains(t)) {
				out.add(t);
			}
		}
		return out;
	}

	public static String now() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

	public static String format(long millis) {
		if (millis <= 0) {
			return "-";
		}
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(millis));
	}

	private static void putIfAbsent(Properties p, String key, String value) {
		if (p.getProperty(key) == null) {
			p.setProperty(key, value);
		}
	}

	/** 예외 메시지에서 첫 줄만 (SMTP 응답 전체가 로그에 들어가지 않게) */
	private static String shortMessage(Throwable t) {
		String m = t.getMessage();
		if (m == null) {
			return "";
		}
		int nl = m.indexOf('\n');
		m = (nl > 0) ? m.substring(0, nl) : m;
		return (m.length() > 200) ? m.substring(0, 200) : m;
	}
}
