package com.sensible.admin.domain;

import java.io.Serializable;

/**
 * 비밀번호 1차 인증은 통과했지만 OTP 2차 인증은 아직인 상태.
 *
 * 세션에 이 객체만 있고 SUPER_USER_SESSION 과 SUPER_TOKEN 쿠키는 없다.
 * 즉 이 상태로는 관리자 화면·API 어디에도 들어갈 수 없다 — SuperJwtInterceptor 가
 * 토큰 없는 요청을 전부 로그인으로 돌려보내기 때문이다. OTP 검증 전용 경로만 열려 있다.
 */
public class MfaPendingVO implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 2차 인증 입력 (이미 OTP 등록된 관리자) */
	public static final String PURPOSE_VERIFY = "VERIFY";

	/** OTP 최초 등록 (등록 의무 대상인데 미등록) */
	public static final String PURPOSE_SETUP = "SETUP";

	private UserVO user;
	private long createdAt;
	private String purpose;
	/** 로그인 중이 아니라 이미 로그인된 세션에서 재등록하는 경우 true (완료 후 세션을 새로 만들지 않는다) */
	private boolean reenroll;
	/** 이 대기 상태에서 시도한 횟수 (DB 잠금과 별개로 세션 단위 상한) */
	private int attempts;

	public MfaPendingVO() {
	}

	public MfaPendingVO(UserVO user, String purpose, boolean reenroll) {
		this.user = user;
		this.purpose = purpose;
		this.reenroll = reenroll;
		this.createdAt = System.currentTimeMillis();
	}

	public boolean isExpired(long nowMillis, long ttlMillis) {
		return nowMillis - createdAt > ttlMillis;
	}

	public UserVO getUser() {
		return user;
	}

	public void setUser(UserVO user) {
		this.user = user;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public boolean isReenroll() {
		return reenroll;
	}

	public void setReenroll(boolean reenroll) {
		this.reenroll = reenroll;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public int incrementAttempts() {
		return ++attempts;
	}
}
