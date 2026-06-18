package com.sensible.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import com.sensible.common.dao.DefaultDAO;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

@Service
public class MarketApiService {

	@Resource(name = "DefaultDAO")
    private DefaultDAO dao;
	
	@Autowired(required = false)
    private JavaMailSender mailSender;

    // 1. 신청 삽입
    public void insertIpoRequest(Map<String, Object> param) throws Exception {
        dao.insert("market.insertIpoRequest", param);
    }

    // 2. 목록 조회
    public List<Map<String, Object>> getIpoList(Map<String, Object> param) throws Exception {
        return dao.selectList("market.selectIpoList", param);
    }

    // 3. 기업 문의 삽입 및 알림 메일 발송
    public void insertPurchaseRequest(Map<String, Object> param) throws Exception {
    	// 1. DB에 제안 내용 저장 (이건 무조건 성공해야 함)
        dao.insert("market.insertPurchaseRequest", param);

        // 2. 글로벌 본사 관리자에게 알림 메일 발송
        try {
            if (mailSender != null) {
                SimpleMailMessage message = new SimpleMailMessage();
                
                // 수신자 명시
                message.setTo("witchhunting777@gmail.com"); 
                message.setSubject("[스타 협업 문의] 새로운 기업 제안이 접수되었습니다.");
                
                // 메일 본문 내용 조립
                String content = String.format(
                    "스타 플랫폼에 새로운 기업 협업 문의가 접수되었습니다.\n\n" +
                    "■ 제안 회사명 : %s\n" +
                    "■ 담당자 이름 : %s\n" +
                    "■ 연락처 : %s\n" +
                    "■ 이메일 : %s\n" +
                    "■ 문의 내용 및 희망가 :\n%s",
                    param.get("companyName"), 
                    param.get("managerName"), 
                    param.get("contact"), 
                    param.get("email"), 
                    param.get("message")
                );
                
                message.setText(content);
                
                // 메일 전송!
                mailSender.send(message);
                System.out.println("본사 관리자(witchhunting777@gmail.com)에게 알림 메일 발송 완료!");
                
            } else {
                System.out.println("알림: JavaMailSender가 설정되지 않아 메일 발송은 생략되었습니다. (DB 저장은 완료)");
            }
        } catch (Exception e) {
            // 메일 발송에 실패하더라도 사용자에게는 '접수 완료'를 띄우기 위해 에러만 로그로 남김
            System.err.println("메일 발송 실패 (데이터는 정상 저장됨): " + e.getMessage());
        }
    }
}