package com.careerlink.backend.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.ConsultationTopic;
import com.careerlink.backend.exception.ResourceNotFoundException;
import com.careerlink.backend.repository.ConsultationTopicRepository;

@Service
@Transactional(readOnly = true)
public class ConsultationTopicService {

	private final ConsultationTopicRepository consultationTopicRepository;

	public ConsultationTopicService(ConsultationTopicRepository consultationTopicRepository) {
		this.consultationTopicRepository = consultationTopicRepository;
	}

	public List<ConsultationTopic> getAllTopics() {
		return consultationTopicRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
	}

	public ConsultationTopic getTopic(Long id) {
		return consultationTopicRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("ConsultationTopic", id));
	}
}
