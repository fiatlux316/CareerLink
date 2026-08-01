import client from './client'
import type { CounselorEnterPayload, CounselorSession } from '../types/counselor'
import type { Consultation, ConsultationCompletePayload } from '../types/consultation'

/**
 * 상담사 입장
 */
export const enterCounselor = async (
  payload: CounselorEnterPayload,
): Promise<CounselorSession> => {
  const response = await client.post<CounselorSession>('/counselor/enter', payload)
  return response.data
}

/**
 * 상담사 담당 상담 목록 조회
 */
export const getCounselorConsultations = async (
  typeId: number,
  status: string,
  counselorName?: string,
): Promise<Consultation[]> => {
  const response = await client.get<Consultation[]>('/counselor/consultations', {
    params: { typeId, status, counselorName },
  })
  return response.data
}

/**
 * 상담 수락
 */
export const acceptConsultation = async (
  id: string,
  counselorName: string,
): Promise<Consultation> => {
  const response = await client.patch<Consultation>(`/consultations/${id}/accept`, {
    counselorName,
  })
  return response.data
}

/**
 * 수락 취소 (ACCEPTED -> RECEIVED)
 */
export const cancelAcceptConsultation = async (id: string): Promise<Consultation> => {
  const response = await client.patch<Consultation>(`/consultations/${id}/cancel-accept`, {})
  return response.data
}

/**
 * 상담 진행 시작 (ACCEPTED -> IN_PROGRESS)
 */
export const startProgressConsultation = async (id: string): Promise<Consultation> => {
  const response = await client.patch<Consultation>(`/consultations/${id}/start-progress`, {})
  return response.data
}



/**
 * 상담 완료 및 결과 등록
 */
export const completeConsultation = async (
  id: string,
  payload?: ConsultationCompletePayload,
): Promise<Consultation> => {
  const response = await client.patch<Consultation>(`/consultations/${id}/complete`, payload || {})
  return response.data
}