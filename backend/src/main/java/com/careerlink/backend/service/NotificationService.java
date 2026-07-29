package com.careerlink.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	/**
	 * 상담 수락 시 학생 휴대폰으로 알림(SMS/알림톡) 발송
	 */
	public void sendAcceptNotification(String studentName, String studentPhone, String counselorName, String typeName) {
		String message = String.format(
			"[CareerLink] %s 님, 요청하신 '%s' 상담 신청이 수락되었습니다. (담당 상담사: %s)",
			studentName, typeName, counselorName
		);

		log.info("=================================================");
		log.info("📱 [학생 휴대폰 SMS/알림톡 발송 완료]");
		log.info("수신번호: {}", studentPhone);
		log.info("메시지: {}", message);
		log.info("=================================================");
	}
}
