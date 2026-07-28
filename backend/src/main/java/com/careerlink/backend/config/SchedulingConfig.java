package com.careerlink.backend.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PrivacyProperties.class)
public class SchedulingConfig {

	@Bean
	Clock systemClock() {
		return Clock.systemDefaultZone();
	}
}
