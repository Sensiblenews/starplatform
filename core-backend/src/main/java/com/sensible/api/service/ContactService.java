package com.sensible.api.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.sensible.common.dao.DefaultDAO;
import com.sensible.common.util.ClientIpUtil;

/**
 * 공개 웹 문의 폼(/contact) 접수 서비스.
 *
 * 저장이 원본이고 메일은 알림이다. 메일 발송이 실패해도 접수는 성공으로 처리하고
 * MAIL_SENT_YN 으로 구분만 남긴다 — 반대로 하면 SMTP 장애 때 문의가 통째로 사라진다.
 *
 * 스팸 방어는 셋을 겹쳐 쓴다.
 *  1. 허니팟: 사람에게 보이지 않는 입력칸이 채워져 있으면 조용히 버린다(성공 응답).
 *     봇에게 실패를 알려주면 우회 방법을 찾아간다.
 *  2. 최소 작성 시간: 폼을 연 지 3초도 안 돼 제출된 건은 사람이 쓴 것으로 보지 않는다.
 *  3. Redis rate limit: IP당 시간·일 단위 상한.
 * Redis 장애 시에는 LandingVisitService 와 같은 fail-open 을 따른다. 문의를 놓치는 쪽이
 * 스팸을 조금 더 받는 쪽보다 나쁘다.
 */
@Service("contactService")
public class ContactService {

	@Resource(name = "DefaultDAO")
	private DefaultDAO dao;

	@Resource(name = "redisTemplate")
	private RedisTemplate<String, Object> redisTemplate;

	/** SMTP 미설정 환경(로컬 등)에서는 주입되지 않을 수 있다 (SuperAppService와 같은 방식) */
	@Autowired(required = false)
	private JavaMailSender mailSender;

	/**
	 * 관리자 알림 수신 주소.
	 * 앱 FAQ 화면의 "문의하기"가 이미 이 주소로 메일을 보내고 있어 같은 곳으로 모은다
	 * (core-frontend/src/app/pages/faq/faq.page.ts).
	 */
	private static final String ADMIN_MAIL_TO = "witchhunting777@gmail.com";

	/** 이메일 형식 검증. 완벽한 판별은 불가능하므로 명백한 오타만 걸러낸다 */
	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

	private static final int MAX_NAME = 100;
	private static final int MAX_EMAIL = 255;
	private static final int MAX_SUBJECT = 200;
	private static final int MIN_MESSAGE = 10;
	private static final int MAX_MESSAGE = 5000;

	/** 폼을 열고 이 시간 안에 제출되면 사람이 작성한 것으로 보지 않는다 */
	private static final long MIN_FILL_MS = 3000L;
	/** 폼 토큰이 너무 오래되면(하루) 재사용으로 본다 */
	private static final long MAX_FORM_AGE_MS = 24 * 60 * 60 * 1000L;

	private static final int LIMIT_PER_HOUR = 3;
	private static final int LIMIT_PER_DAY = 10;

	/** 문의 유형. 화면 select 와 저장 값이 같은 목록을 보도록 여기 한 곳에만 둔다 */
	public static final class Type {
		private final String code;
		private final String label;

		Type(String code, String label) {
			this.code = code;
			this.label = label;
		}

		public String getCode() {
			return code;
		}

		public String getLabel() {
			return label;
		}
	}

	private static final List<Type> TYPES = Collections.unmodifiableList(Arrays.asList(
		new Type("GENERAL", "General enquiry"),
		new Type("ACCOUNT", "Account and star page"),
		new Type("REPORT", "Report content"),
		new Type("COPYRIGHT", "Copyright or rights infringement"),
		new Type("ADVERTISING", "Advertising and affiliate"),
		new Type("BRAND", "Brand and business"),
		new Type("INSTITUTION", "University and institution"),
		new Type("PARTNERSHIP", "Global operating partner"),
		new Type("PRIVACY", "Privacy and personal data"),
		new Type("OTHER", "Other")
	));

	public static List<Type> types() {
		return TYPES;
	}

	/** 유형 코드가 목록에 있는지. 목록 밖 값은 저장하지 않는다 */
	public static boolean isKnownType(String code) {
		for (Type type : TYPES) {
			if (type.getCode().equals(code)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 폼 제출을 접수한다.
	 *
	 * @return result=OK 면 접수 완료(또는 봇으로 판단해 조용히 버림), FAIL 이면 msg 에 사유.
	 *         field 가 있으면 화면에서 해당 입력칸에 표시한다.
	 */
	public Map<String, Object> submit(Map<String, String> form, HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();

		// 1. 허니팟 — 채워져 있으면 봇이다. 성공한 것처럼 돌려보낸다
		if (notBlank(form.get("website"))) {
			result.put("result", "OK");
			return result;
		}

		// 2. 최소 작성 시간
		long openedAt = parseLong(form.get("formTime"));
		long elapsed = System.currentTimeMillis() - openedAt;
		if (openedAt <= 0 || elapsed < MIN_FILL_MS || elapsed > MAX_FORM_AGE_MS) {
			return fail(result, null, "This form could not be verified. Please reload the page and try again.");
		}

		// 3. 입력값 검증
		String type = trim(form.get("contactType"));
		if (!isKnownType(type)) {
			return fail(result, "contactType", "Please choose an enquiry type.");
		}

		String name = sanitize(form.get("senderName"));
		if (name.isEmpty() || name.length() > MAX_NAME) {
			return fail(result, "senderName", "Please enter your name (up to " + MAX_NAME + " characters).");
		}

		String email = trim(form.get("senderEmail"));
		if (email.length() > MAX_EMAIL || !EMAIL.matcher(email).matches()) {
			return fail(result, "senderEmail", "Please enter a valid email address so we can reply.");
		}

		String subject = sanitize(form.get("subject"));
		if (subject.isEmpty() || subject.length() > MAX_SUBJECT) {
			return fail(result, "subject", "Please enter a subject (up to " + MAX_SUBJECT + " characters).");
		}

		String message = sanitize(form.get("message"));
		if (message.length() < MIN_MESSAGE) {
			return fail(result, "message", "Please describe your enquiry in at least " + MIN_MESSAGE + " characters.");
		}
		if (message.length() > MAX_MESSAGE) {
			return fail(result, "message", "Your message is too long (up to " + MAX_MESSAGE + " characters).");
		}

		if (!"Y".equals(trim(form.get("privacyAgree")))) {
			return fail(result, "privacyAgree", "Please agree to the handling of your contact details before sending.");
		}

		// 4. IP rate limit
		String ip = ClientIpUtil.getClientIp(request);
		if (!withinRateLimit(ip)) {
			return fail(result, null, "Too many enquiries have been sent from here recently. Please try again later.");
		}

		// 5. 저장 — 여기가 원본이다
		Map<String, Object> row = new HashMap<>();
		row.put("contactType", type);
		row.put("senderName", name);
		row.put("senderEmail", email);
		row.put("subject", subject);
		row.put("message", message);
		row.put("clientIp", ip);
		row.put("userAgent", cut(request.getHeader("User-Agent"), 500));

		try {
			dao.insert("super.insertWebContact", row);
		} catch (Exception e) {
			e.printStackTrace();
			return fail(result, null, "We could not accept your enquiry right now. Please try again in a few minutes.");
		}

		// 6. 관리자 알림 — 실패해도 접수는 유효하다
		if (sendAdminMail(type, name, email, subject, message)) {
			try {
				dao.update("super.updateWebContactMailSent", row);
			} catch (Exception e) {
				// 메일은 이미 나갔다. 표시 갱신 실패는 접수 결과에 영향을 주지 않는다
				e.printStackTrace();
			}
		}

		result.put("result", "OK");
		return result;
	}

	/** 시간·일 단위 상한을 함께 본다. Redis 장애 시에는 통과시킨다 */
	private boolean withinRateLimit(String ip) {
		if (ip == null || ip.isEmpty()) {
			return true;
		}
		try {
			return incrementWithin("contact:rate:h:" + ip, LIMIT_PER_HOUR, 1, TimeUnit.HOURS)
				&& incrementWithin("contact:rate:d:" + ip, LIMIT_PER_DAY, 1, TimeUnit.DAYS);
		} catch (Exception e) {
			// fail-open: Redis가 죽었다고 문의를 못 받는 쪽이 더 나쁘다
			e.printStackTrace();
			return true;
		}
	}

	private boolean incrementWithin(String key, int limit, long ttl, TimeUnit unit) {
		Long count = redisTemplate.opsForValue().increment(key, 1L);
		if (count != null && count == 1L) {
			redisTemplate.expire(key, ttl, unit);
		}
		return count == null || count <= limit;
	}

	private boolean sendAdminMail(String type, String name, String email, String subject, String message) {
		if (mailSender == null) {
			System.out.println("[CONTACT] JavaMailSender 미설정 — 알림 메일 생략 (접수는 저장 완료)");
			return false;
		}
		try {
			SimpleMailMessage mail = new SimpleMailMessage();
			mail.setTo(ADMIN_MAIL_TO);
			mail.setSubject("[StarPlatform] " + typeLabel(type) + " - " + subject);
			// 회신은 문의자에게 바로 가야 한다
			mail.setReplyTo(email);
			mail.setText("Type: " + typeLabel(type) + "\n"
				+ "From: " + name + " <" + email + ">\n"
				+ "Subject: " + subject + "\n\n"
				+ message + "\n");
			mailSender.send(mail);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private String typeLabel(String code) {
		for (Type type : TYPES) {
			if (type.getCode().equals(code)) {
				return type.getLabel();
			}
		}
		return code;
	}

	/**
	 * 저장 전 정리: 태그를 통째로 걷어내고 제어문자를 지운다.
	 * 출력 시점에도 JSP 에서 c:out 으로 이스케이프하지만, 저장값 자체를 깨끗이 둬야
	 * 어드민 화면·메일 본문 등 다른 출력 경로에서 매번 같은 실수를 반복하지 않는다.
	 */
	static String sanitize(String raw) {
		if (raw == null) {
			return "";
		}
		// script/style 은 내용까지 통째로 제거한 뒤 남은 태그를 지운다
		String s = raw.replaceAll("(?is)<\\s*script[^>]*>.*?<\\s*/\\s*script\\s*>", " ");
		s = s.replaceAll("(?is)<\\s*style[^>]*>.*?<\\s*/\\s*style\\s*>", " ");
		s = s.replaceAll("(?s)<[^>]*>", " ");
		// 닫히지 않은 채 끝난 태그 조각(<img src=x onerror=...)은 위 패턴에 걸리지 않는다.
		// 출력 시점에 이스케이프되므로 실행되지는 않지만, 저장값에 남겨둘 이유도 없다
		s = s.replaceAll("(?s)<[^>]*$", " ");
		// 줄바꿈은 살리고 나머지 제어문자만 공백으로 바꾼다
		s = s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ");
		// 태그를 걷어내며 생긴 연속 공백 정리 (줄바꿈은 보존)
		s = s.replaceAll("[ \\t]{2,}", " ");
		return s.trim();
	}

	private static Map<String, Object> fail(Map<String, Object> result, String field, String msg) {
		result.put("result", "FAIL");
		result.put("msg", msg);
		if (field != null) {
			result.put("field", field);
		}
		return result;
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}

	private static boolean notBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}

	private static long parseLong(String s) {
		try {
			return Long.parseLong(trim(s));
		} catch (Exception e) {
			return 0L;
		}
	}

	private static String cut(String s, int max) {
		if (s == null) {
			return null;
		}
		return s.length() <= max ? s : s.substring(0, max);
	}

	/** 어드민 문의 목록 */
	public List<Map<String, Object>> list(int offset, int size) {
		try {
			Map<String, Object> param = new HashMap<>();
			param.put("offset", offset);
			param.put("size", size);
			return dao.selectList("super.selectWebContactList", param);
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/** 어드민 문의 총 건수 */
	public int count() {
		try {
			Object count = dao.selectOne("super.selectWebContactCount", new HashMap<>());
			return count == null ? 0 : ((Number) count).intValue();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	/** 처리 완료 표시 */
	public boolean markDone(String contactId) {
		try {
			Map<String, Object> param = new HashMap<>();
			param.put("contactId", contactId);
			return dao.update("super.updateWebContactDone", param) > 0;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
