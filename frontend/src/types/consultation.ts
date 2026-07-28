/**
 * 상담 타입 인터페이스
 */
export interface ConsultationType {
  id: number
  name: string
  description: string
}

/**
 * 상담 접수 요청 페이로드
 */
export interface ConsultationCreatePayload {
  studentName: string
  studentPhone: string
  typeId: number
}

/**
 * 상담 접수 응답
 */
export interface Consultation {
  id: string
  studentName: string
  studentPhone: string
  typeId: number
  typeName: string
  status: 'RECEIVED' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
  counselorName: string | null
  createdAt: string
  updatedAt: string
}

/**
 * API 에러 응답
 */
export interface ErrorResponse {
  status: number
  message: string
  timestamp?: string
  fieldErrors?: Record<string, string>
  errors?: Record<string, string>
}
