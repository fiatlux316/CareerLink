package com.careerlink.backend.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.ConsultationTopic;
import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.exception.ResourceNotFoundException;
import com.careerlink.backend.exception.TypeInUseException;
import com.careerlink.backend.repository.ConsultationRepository;
import com.careerlink.backend.repository.ConsultationTopicRepository;
import com.careerlink.backend.repository.ConsultationTypeRepository;

@Service
@Transactional(readOnly = true)
public class ConsultationTypeService {

	private final ConsultationTypeRepository consultationTypeRepository;
	private final ConsultationTopicRepository consultationTopicRepository;
	private final ConsultationRepository consultationRepository;

	public ConsultationTypeService(
		ConsultationTypeRepository consultationTypeRepository,
		ConsultationTopicRepository consultationTopicRepository,
		ConsultationRepository consultationRepository
	) {
		this.consultationTypeRepository = consultationTypeRepository;
		this.consultationTopicRepository = consultationTopicRepository;
		this.consultationRepository = consultationRepository;
	}

	public List<ConsultationType> getAllTypes() {
		return consultationTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
	}

	@Transactional
	public ConsultationType updateType(Long id, Long topicId, String name, String description) {
		ConsultationType consultationType = consultationTypeRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("ConsultationType", id));
		ConsultationTopic topic = consultationTopicRepository.findById(topicId)
			.orElseThrow(() -> new ResourceNotFoundException("ConsultationTopic", topicId));

		consultationType.update(topic, name, description);

		return consultationTypeRepository.saveAndFlush(consultationType);
	}

	@Transactional
	public ConsultationType updateType(Long id, String name, String description) {
		ConsultationType consultationType = consultationTypeRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("ConsultationType", id));
		consultationType.update(name, description);

		return consultationTypeRepository.saveAndFlush(consultationType);
	}

	@Transactional
	public ConsultationType createType(Long topicId, String name, String description) {
		ConsultationTopic topic = consultationTopicRepository.findById(topicId)
			.orElseThrow(() -> new ResourceNotFoundException("ConsultationTopic", topicId));
		return consultationTypeRepository.saveAndFlush(new ConsultationType(topic, name, description));
	}

	@Transactional
	public ConsultationType createType(String name, String description) {
		List<ConsultationTopic> topics = consultationTopicRepository.findAll();
		ConsultationTopic topic = topics.isEmpty() ? null : topics.get(0);
		return consultationTypeRepository.saveAndFlush(new ConsultationType(topic, name, description));
	}

	@Transactional
	public void deleteType(Long id) {
		ConsultationType consultationType = consultationTypeRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("ConsultationType", id));
		long consultationCount = consultationRepository.countByType(consultationType);

		if (consultationCount > 0) {
			throw new TypeInUseException(id, consultationCount);
		}

		consultationTypeRepository.delete(consultationType);
	}
}
