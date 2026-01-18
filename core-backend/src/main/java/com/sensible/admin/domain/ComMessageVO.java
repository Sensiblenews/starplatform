package com.sensible.admin.domain;

import jbit.core.domain.DefaultVO;

public class ComMessageVO extends DefaultVO<ComMessageVO>{

	private static final long serialVersionUID = -44809931448339812L;
	
	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
