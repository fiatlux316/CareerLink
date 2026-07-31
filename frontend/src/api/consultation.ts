import client from './client'
import type { ConsultationType, ConsultationTopic, Consultation, ConsultationCreatePayload } from '../types/consultation'

/**
 * 상담 테마 목록 조회 (1depth)
 */
export const getConsultationTopics = async (): Promise<ConsultationTopic[]> => {
  const response = await client.get<ConsultationTopic[]>('/topics')
  return response.data
}

/**
 * 상담 유형 목록 조회 (2depth)
 */
export const getConsultationTypes = async (): Promise<ConsultationType[]> => {
  const response = await client.get<ConsultationType[]>('/types')
  return response.data
}

/**
 * 상담 접수
 */
export const createConsultation = async (
  payload: ConsultationCreatePayload,
): Promise<Consultation> => {
  const response = await client.post<Consultation>('/consultations', payload)
  return response.data
}

/**
 * 상담 현황 조회
 */
export const getConsultation = async (id: string): Promise<Consultation> => {
  const response = await client.get<Consultation>(`/consultations/${id}`)
  return response.data
}

/**
 * 학생 휴대폰 번호로 상담 목록 조회
 */
export const getConsultationsByStudentPhone = async (phone: string): Promise<Consultation[]> => {
  const response = await client.get<Consultation[]>(`/consultations?studentPhone=${encodeURIComponent(phone)}`)
  return response.data
}

/**
 * 상담 취소
 */
export const cancelConsultation = async (id: string): Promise<Consultation> => {
  const response = await client.patch<Consultation>(`/consultations/${id}/cancel`)
  return response.data
}