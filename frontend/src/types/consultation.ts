/**
 * 학교 타입
 */
export type SchoolType = 'MIDDLE_SCHOOL' | 'HIGH_SCHOOL'

/**
 * 상담 타입 인터페이스
 */
export interface ConsultationType {
  id: number
  name: string
  description: string
}

/**
 * 학생 입장 요청 페이로드
 */
export interface StudentEnterPayload {
  studentName: string
  studentPhone: string
  schoolType: SchoolType
  grade: number
}

/**
 * 학생 세션 응답
 */
export interface StudentSession {
  id: number
  studentName: string
  studentPhone: string
  schoolType: SchoolType
  grade: number
  enteredAt: string
}

/**
 * 상담 접수 요청 페이로드
 */
export interface ConsultationCreatePayload {
  studentSessionId: number
  typeId: number
}

/**
 * 상담 접수 응답
 */
export interface Consultation {
  id: string
  studentSessionId: number
  studentName: string
  studentPhone: string
  schoolType: SchoolType
  grade: number
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
