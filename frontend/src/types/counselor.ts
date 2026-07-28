/**
 * 상담사 입장 요청 페이로드
 */
export interface CounselorEnterPayload {
  counselorName: string
  counselorPhone: string
  typeId: number
}

/**
 * 상담사 입장 응답
 */
export interface CounselorSession {
  id: string
  counselorName: string
  counselorPhone: string
  typeId: number
  typeName: string
  enteredAt: string
}

/**
 * 로컬스토리지에 저장될 상담사 세션 정보
 */
export interface CounselorSessionStorage {
  typeId: number
  typeName: string
  counselorName: string
  id?: string
}

/**
 * API 에러 응답
 */
export interface ErrorResponse {
  status: number
  message: string
  timestamp?: string
  errors?: Record<string, string>
}
