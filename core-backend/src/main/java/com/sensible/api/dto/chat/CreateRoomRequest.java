package com.sensible.api.dto.chat;

public class CreateRoomRequest {
    private Long opponentId;   // 대화 상대방 ID

	public Long getOpponentId() {
		return opponentId;
	}

	public void setOpponentId(Long opponentId) {
		this.opponentId = opponentId;
	}
}

