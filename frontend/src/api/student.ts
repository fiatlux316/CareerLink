import client from './client'
import type { StudentEnterPayload, StudentSession } from '../types/consultation'

/**
 * 학생 입장
 */
export const enterStudent = async (payload: StudentEnterPayload): Promise<StudentSession> => {
  const response = await client.post<StudentSession>('/students/enter', payload)
  return response.data
}
