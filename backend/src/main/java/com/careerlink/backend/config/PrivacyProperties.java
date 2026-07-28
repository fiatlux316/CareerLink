package com.careerlink.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "careerlink.privacy")
public class PrivacyProperties {

	private int retentionDays = 30;

	private String maskingCron = "0 0 3 * * *";

	public int getRetentionDays() {
		return retentionDays;
	}

	public void setRetentionDays(int retentionDays) {
		this.retentionDays = retentionDays;
	}

	public String getMaskingCron() {
		return maskingCron;
	}

	public void setMaskingCron(String maskingCron) {
		this.maskingCron = maskingCron;
	}
}
