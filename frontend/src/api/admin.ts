import client from './client'
import type { ConsultationType, ConsultationTopic, Consultation, StudentSession, StudentSessionUpdatePayload } from '../types/consultation'

/**
 * 상담 테마 목록 조회 (관리자용 1depth)
 */
export const getAdminTopics = async (): Promise<ConsultationTopic[]> => {
  const response = await client.get<ConsultationTopic[]>('/admin/topics')
  return response.data
}

/**
 * 상담 유형 목록 조회
 */
export const getAdminTypes = async (): Promise<ConsultationType[]> => {
  const response = await client.get<ConsultationType[]>('/admin/types')
  return response.data
}

/**
 * 전체 상담 내역 목록 조회 (관리자용)
 */
export const getAllConsultationsForAdmin = async (): Promise<Consultation[]> => {
  const response = await client.get<Consultation[]>('/admin/consultations')
  return response.data
}

export const createAdminType = async (
  payload: { topicId: number; name: string; description: string },
): Promise<ConsultationType> => {
  const response = await client.post<ConsultationType>('/admin/types', payload)
  return response.data
}

/**
 * 상담 유형 수정
 */
export const updateAdminType = async (
  id: number,
  payload: { topicId: number; name: string; description: string },
): Promise<ConsultationType> => {
  const response = await client.put<ConsultationType>(`/admin/types/${id}`, payload)
  return response.data
}

export const deleteAdminType = async (id: number): Promise<void> => {
  await client.delete(`/admin/types/${id}`)
}

/**
 * 학생 세션 목록 조회 (관리자용)
 */
export const getAdminStudents = async (): Promise<StudentSession[]> => {
  const response = await client.get<StudentSession[]>('/admin/students')
  return response.data
}

/**
 * 학생 정보 수정 (관리자용)
 */
export const updateAdminStudent = async (
  id: number,
  payload: StudentSessionUpdatePayload
): Promise<StudentSession> => {
  const response = await client.put<StudentSession>(`/admin/students/${id}`, payload)
  return response.data
}
