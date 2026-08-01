package com.careerlink.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ConsultationCompleteRequest(
	@Size(max = 100, message = "상담 결과는 100자 이내로 입력해주세요.")
	String resultContent,

	@Min(value = 1, message = "재상담 필요 여부는 1(필요) 또는 2(불필요)여야 합니다.")
	@Max(value = 2, message = "재상담 필요 여부는 1(필요) 또는 2(불필요)여야 합니다.")
	Integer reConsultationNeeded,

	@Min(value = 1, message = "만족도 점수는 1점 이상이어야 합니다.")
	@Max(value = 5, message = "만족도 점수는 5점 이하여야 합니다.")
	Integer satisfactionScore
) {
}
