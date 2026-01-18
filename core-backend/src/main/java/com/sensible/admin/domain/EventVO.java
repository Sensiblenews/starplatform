package com.sensible.admin.domain;

import jbit.core.domain.DefaultVO;

public class EventVO extends DefaultVO<EventVO>{


	private static final long serialVersionUID = 6988404044714251398L;
	
	private Integer EVT_ID;
	private String APP_ID;
	private String EVT_TEXT;
	private String EVT_BTN_TEXT;
	private Integer EVT_STAR;
	private String EVT_URL;
	private Integer EVT_CHG_YN;
	private Integer EVT_USE_YN;
	private String EVT_CREATE_DATE;
	private String EVT_UPDATE_DATE;
	
	public Integer getEVT_ID() {
		return EVT_ID;
	}
	public void setEVT_ID(Integer eVT_ID) {
		EVT_ID = eVT_ID;
	}
	public String getAPP_ID() {
		return APP_ID;
	}
	public void setAPP_ID(String aPP_ID) {
		APP_ID = aPP_ID;
	}
	public String getEVT_TEXT() {
		return EVT_TEXT;
	}
	public void setEVT_TEXT(String eVT_TEXT) {
		EVT_TEXT = eVT_TEXT;
	}
	public String getEVT_BTN_TEXT() {
		return EVT_BTN_TEXT;
	}
	public void setEVT_BTN_TEXT(String eVT_BTN_TEXT) {
		EVT_BTN_TEXT = eVT_BTN_TEXT;
	}
	public Integer getEVT_STAR() {
		return EVT_STAR;
	}
	public void setEVT_STAR(Integer eVT_STAR) {
		EVT_STAR = eVT_STAR;
	}
	public String getEVT_URL() {
		return EVT_URL;
	}
	public void setEVT_URL(String eVT_URL) {
		EVT_URL = eVT_URL;
	}
	public Integer getEVT_CHG_YN() {
		return EVT_CHG_YN;
	}
	public void setEVT_CHG_YN(Integer eVT_CHG_YN) {
		EVT_CHG_YN = eVT_CHG_YN;
	}
	public Integer getEVT_USE_YN() {
		return EVT_USE_YN;
	}
	public void setEVT_USE_YN(Integer eVT_USE_YN) {
		EVT_USE_YN = eVT_USE_YN;
	}
	public String getEVT_CREATE_DATE() {
		return EVT_CREATE_DATE;
	}
	public void setEVT_CREATE_DATE(String eVT_CREATE_DATE) {
		EVT_CREATE_DATE = eVT_CREATE_DATE;
	}
	public String getEVT_UPDATE_DATE() {
		return EVT_UPDATE_DATE;
	}
	public void setEVT_UPDATE_DATE(String eVT_UPDATE_DATE) {
		EVT_UPDATE_DATE = eVT_UPDATE_DATE;
	}
}
