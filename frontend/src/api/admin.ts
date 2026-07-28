import client from './client'
import type { ConsultationType } from '../types/consultation'

/**
 * 상담 유형 목록 조회
 */
export const getAdminTypes = async (): Promise<ConsultationType[]> => {
  const response = await client.get<ConsultationType[]>('/admin/types')
  return response.data
}

export const createAdminType = async (
  payload: { name: string; description: string },
): Promise<ConsultationType> => {
  const response = await client.post<ConsultationType>('/admin/types', payload)
  return response.data
}

/**
 * 상담 유형 수정
 */
export const updateAdminType = async (
  id: number,
  payload: { name: string; description: string },
): Promise<ConsultationType> => {
  const response = await client.put<ConsultationType>(`/admin/types/${id}`, payload)
  return response.data
}

export const deleteAdminType = async (id: number): Promise<void> => {
  await client.delete(`/admin/types/${id}`)
}
