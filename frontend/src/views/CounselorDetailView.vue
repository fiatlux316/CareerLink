<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getConsultation } from '../api/consultation'
import {
  acceptConsultation,
  cancelAcceptConsultation,
  startProgressConsultation,
  completeConsultation,
} from '../api/counselor'
import type { Consultation } from '../types/consultation'
import type { CounselorSessionStorage } from '../types/counselor'

const props = defineProps<{
  id: string
}>()

const router = useRouter()

// 상태 관리
const consultation = ref<Consultation | null>(null)
const isLoading = ref(true)
const isRefreshing = ref(false)
const isProcessing = ref(false)
const errorMessage = ref('')
const actionErrorMessage = ref('')
let pollingIntervalId: number | null = null
const counselorSessionInfo = ref<CounselorSessionStorage | null>(null)

// 상태 한글 레이블
const getStatusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    RECEIVED: '대기 중',
    ACCEPTED: '수락 완료',
    IN_PROGRESS: '진행 중',
    COMPLETED: '상담 완료',
    CANCELLED: '취소됨',
  }
  return labels[status] || status
}

// 상태별 배지 색상 클래스
const getStatusColorClass = (status: string): string => {
  const colors: Record<string, string> = {
    RECEIVED: 'badge-received',
    ACCEPTED: 'badge-accepted',
    IN_PROGRESS: 'badge-in-progress',
    COMPLETED: 'badge-completed',
    CANCELLED: 'badge-cancelled',
  }
  return colors[status] || ''
}

// 진행도 계산 (0~100%)
const getStatusProgress = (status: string): number => {
  const progress: Record<string, number> = {
    RECEIVED: 25,
    ACCEPTED: 50,
    IN_PROGRESS: 75,
    COMPLETED: 100,
    CANCELLED: 0,
  }
  return progress[status] || 0
}

// 학교, 학년 및 성별 정보 포맷팅 (0인 경우 노출하지 않음)
const formatStudentInfo = (schoolType: string, grade?: number, gender?: number): string => {
  let typeLabel = '중/고등학교'
  if (schoolType === 'MIDDLE_SCHOOL') typeLabel = '중학교'
  if (schoolType === 'HIGH_SCHOOL') typeLabel = '고등학교'
  if (schoolType === 'MIDDLE_HIGH_SCHOOL') typeLabel = '중/고등학교'

  const parts = [typeLabel]
  if (grade && grade > 0) {
    parts.push(`${grade}학년`)
  }
  if (gender === 1) {
    parts.push('남')
  } else if (gender === 2) {
    parts.push('여')
  }

  return parts.join(' ')
}

// 날짜 포맷팅
const formatDateTime = (dateString: string): string => {
  try {
    const date = new Date(dateString)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return dateString
  }
}

// 상담 정보 조회
const fetchConsultation = async (isSilent = false) => {
  if (!isSilent) {
    isRefreshing.value = true
  }
  errorMessage.value = ''

  try {
    consultation.value = await getConsultation(props.id)
    isLoading.value = false
  } catch (error: unknown) {
    const err = error as any
    if (err?.response?.status === 404) {
      errorMessage.value = '상담 접수 내역을 찾을 수 없습니다'
    } else {
      errorMessage.value = '상담 정보를 불러올 수 없습니다'
    }
  } finally {
    if (!isSilent) {
      isRefreshing.value = false
    }
  }
}

// 새로고침 핸들러
const handleRefresh = () => {
  fetchConsultation(false)
}

// 수락 처리
const handleAccept = async () => {
  if (!consultation.value) return
  const counselorName = counselorSessionInfo.value?.counselorName || '담당상담사'

  isProcessing.value = true
  actionErrorMessage.value = ''

  try {
    await acceptConsultation(consultation.value.id, counselorName)
    await fetchConsultation(false)
  } catch (error: unknown) {
    const err = error as any
    actionErrorMessage.value = err?.response?.data?.message || '상담 수락 처리 중 오류가 발생했습니다'
  } finally {
    isProcessing.value = false
  }
}

// 수락 취소 처리 (ACCEPTED -> RECEIVED)
const handleCancelAccept = async () => {
  if (!consultation.value) return

  isProcessing.value = true
  actionErrorMessage.value = ''

  try {
    await cancelAcceptConsultation(consultation.value.id)
    await fetchConsultation(false)
  } catch (error: unknown) {
    const err = error as any
    actionErrorMessage.value = err?.response?.data?.message || '수락 취소 처리 중 오류가 발생했습니다'
  } finally {
    isProcessing.value = false
  }
}

// 상담 진행 시작 처리
const handleStartProgress = async () => {
  if (!consultation.value) return

  isProcessing.value = true
  actionErrorMessage.value = ''

  try {
    await startProgressConsultation(consultation.value.id)
    await fetchConsultation(false)
  } catch (error: unknown) {
    const err = error as any
    actionErrorMessage.value = err?.response?.data?.message || '상담 진행 처리 중 오류가 발생했습니다'
  } finally {
    isProcessing.value = false
  }
}

// 상담 완료 처리
const handleComplete = async () => {
  if (!consultation.value) return

  isProcessing.value = true
  actionErrorMessage.value = ''

  try {
    await completeConsultation(consultation.value.id)
    await fetchConsultation(false)
  } catch (error: unknown) {
    const err = error as any
    actionErrorMessage.value = err?.response?.data?.message || '상담 완료 처리 중 오류가 발생했습니다'
  } finally {
    isProcessing.value = false
  }
}

// 뒤로가기 (접수 현황 목록)
const handleBack = () => {
  router.push('/counselor')
}

// 5초 주기로 폴링
const startPolling = () => {
  pollingIntervalId = window.setInterval(() => {
    fetchConsultation(true)
  }, 5000)
}

const stopPolling = () => {
  if (pollingIntervalId !== null) {
    clearInterval(pollingIntervalId)
    pollingIntervalId = null
  }
}

onMounted(async () => {
  // 세션 정보 확인
  const storedSession = localStorage.getItem('careerlink_counselor_session')
  if (storedSession) {
    counselorSessionInfo.value = JSON.parse(storedSession)
  }

  await fetchConsultation(false)
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <article class="counselor-detail-view">
    <div class="detail-container">
      <!-- 헤더 및 뒤로가기 -->
      <div class="top-nav">
        <button class="back-btn" @click="handleBack">
          &larr; 접수 현황 목록으로
        </button>
      </div>

      <!-- 로딩 중 -->
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <p>상담 상세 정보를 불러오는 중입니다...</p>
      </div>

      <!-- 에러 상태 -->
      <div v-else-if="errorMessage" class="error-card">
        <div class="error-card__icon">⚠️</div>
        <h2 class="error-card__title">{{ errorMessage }}</h2>
        <button class="btn btn-primary" @click="handleBack">
          목록으로 돌아가기
        </button>
      </div>

      <!-- 상세 내용 -->
      <div v-else-if="consultation" class="detail-content">
        <!-- 상단 서머리 바 -->
        <div class="status-top-bar">
          <div class="status-badge-wrap">
            <span class="badge" :class="getStatusColorClass(consultation.status)">
              {{ getStatusLabel(consultation.status) }}
            </span>
          </div>
          <button
            class="btn btn-small btn-secondary"
            @click="handleRefresh"
            :disabled="isRefreshing"
          >
            <span v-if="!isRefreshing">🔄 새로고침</span>
            <span v-else>갱신 중...</span>
          </button>
        </div>

        <!-- 진행 단계 표시 (Progress Steps) -->
        <div class="progress-section">
          <div class="progress-bar">
            <div
              class="progress-bar__fill"
              :style="{ width: `${getStatusProgress(consultation.status)}%` }"
            ></div>
          </div>
          <div class="progress-steps">
            <div class="progress-step" :class="{ active: true }">
              <span class="progress-step__label">접수</span>
            </div>
            <div
              class="progress-step"
              :class="{ active: ['ACCEPTED', 'IN_PROGRESS', 'COMPLETED'].includes(consultation.status) }"
            >
              <span class="progress-step__label">수락</span>
            </div>
            <div
              class="progress-step"
              :class="{ active: ['IN_PROGRESS', 'COMPLETED'].includes(consultation.status) }"
            >
              <span class="progress-step__label">진행</span>
            </div>
            <div
              class="progress-step"
              :class="{ active: consultation.status === 'COMPLETED' }"
            >
              <span class="progress-step__label">완료</span>
            </div>
          </div>
        </div>

        <!-- 상담 상세 정보 카드 -->
        <div class="info-card">
          <h2 class="info-card__title">상담 접수 상세 정보</h2>

          <div class="info-row">
            <span class="info-row__label">학생 이름</span>
            <span class="info-row__value">{{ consultation.studentName }}</span>
          </div>

          <div class="info-row">
            <span class="info-row__label">학생 연락처</span>
            <span class="info-row__value">{{ consultation.studentPhone }}</span>
          </div>

          <div class="info-row">
            <span class="info-row__label">학생 정보</span>
            <span class="info-row__value">
              {{ formatStudentInfo(consultation.schoolType, consultation.grade, consultation.gender) }}
            </span>
          </div>

          <div class="info-row">
            <span class="info-row__label">상담 테마</span>
            <span class="info-row__value">{{ consultation.topicName || '-' }}</span>
          </div>

          <div class="info-row">
            <span class="info-row__label">상담 유형</span>
            <span class="info-row__value">{{ consultation.typeName }}</span>
          </div>

          <div class="info-row">
            <span class="info-row__label">현재 상태</span>
            <span class="info-row__value highlight">{{ getStatusLabel(consultation.status) }}</span>
          </div>

          <div class="info-row">
            <span class="info-row__label">담당 상담사</span>
            <span class="info-row__value">
              {{ consultation.counselorName || '미정' }}
            </span>
          </div>

          <div class="info-row">
            <span class="info-row__label">접수 일시</span>
            <span class="info-row__value">{{ formatDateTime(consultation.createdAt) }}</span>
          </div>

          <div class="info-row">
            <span class="info-row__label">최종 갱신</span>
            <span class="info-row__value">{{ formatDateTime(consultation.updatedAt) }}</span>
          </div>
        </div>

        <!-- 액션 에러 메시지 -->
        <div v-if="actionErrorMessage" class="action-error">
          {{ actionErrorMessage }}
        </div>

        <!-- 상담사 액션 버튼 섹션 -->
        <div class="action-section">
          <!-- RECEIVED 상태: 수락 버튼 -->
          <template v-if="consultation.status === 'RECEIVED'">
            <button
              class="btn btn-warning btn-large"
              @click="handleAccept"
              :disabled="isProcessing"
            >
              <span v-if="!isProcessing">상담 수락하기</span>
              <span v-else>수락 처리 중...</span>
            </button>
            <p class="action-desc">상담을 수락하면 담당 상담사로 지정됩니다.</p>
          </template>

          <!-- ACCEPTED 상태: 상담 진행 버튼 및 수락 취소 버튼 -->
          <template v-else-if="consultation.status === 'ACCEPTED'">
            <div class="action-btn-group">
              <button
                class="btn btn-success btn-flex-1"
                @click="handleStartProgress"
                :disabled="isProcessing"
              >
                <span v-if="!isProcessing">상담 진행 시작</span>
                <span v-else>진행 처리 중...</span>
              </button>
              <button
                class="btn btn-outline btn-flex-1"
                @click="handleCancelAccept"
                :disabled="isProcessing"
              >
                <span v-if="!isProcessing">수락 취소</span>
                <span v-else>취소 중...</span>
              </button>
            </div>
            <p class="action-desc">상담 진행을 시작하거나 수락을 취소하여 대기 목록으로 되돌릴 수 있습니다.</p>
          </template>

          <!-- IN_PROGRESS 상태: 상담 완료 버튼 -->
          <template v-else-if="consultation.status === 'IN_PROGRESS'">
            <button
              class="btn btn-primary btn-large"
              @click="handleComplete"
              :disabled="isProcessing"
            >
              <span v-if="!isProcessing">상담 완료 처리</span>
              <span v-else>완료 처리 중...</span>
            </button>
            <p class="action-desc">상담을 최종 완료 처리합니다.</p>
          </template>

          <!-- COMPLETED 상태: 완료 메시지 -->
          <div v-else-if="consultation.status === 'COMPLETED'" class="status-message completed">
            <span class="icon">✓</span>
            <span>상담이 성공적으로 완료되었습니다</span>
          </div>

          <!-- CANCELLED 상태: 취소 메시지 -->
          <div v-else-if="consultation.status === 'CANCELLED'" class="status-message cancelled">
            <span class="icon">✕</span>
            <span>취소된 상담 건입니다</span>
          </div>
        </div>

        <!-- 하단 목록으로 돌아가기 버튼 -->
        <button class="btn btn-secondary btn-large btn-bottom" @click="handleBack">
          접수 현황 목록으로 돌아가기
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.counselor-detail-view {
  width: 100%;
}

.detail-container {
  padding: 1.5rem 1rem;
  max-width: 36rem;
  margin: 0 auto;
}

.top-nav {
  margin-bottom: 1rem;
}

.back-btn {
  background: none;
  border: none;
  color: var(--primary);
  font-size: 0.9375rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  transition: opacity 0.2s;
}

.back-btn:hover {
  opacity: 0.8;
}

/* 로딩 & 에러 상태 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  gap: 1rem;
  color: #64748b;
}

.spinner {
  width: 2rem;
  height: 2rem;
  border: 3px solid var(--primary-soft);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-card {
  padding: 2rem 1.5rem;
  border-radius: 0.75rem;
  background: #f8fafc;
  border: 1px solid var(--border);
  text-align: center;
}

.error-card__icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.error-card__title {
  font-size: 1.25rem;
  color: #0f172a;
  margin-bottom: 1.5rem;
}

/* 상단 서머리 바 */
.status-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

/* 배지 색상 */
.badge-received {
  background-color: #fef3c7;
  color: #92400e;
}

.badge-accepted {
  background-color: #e0f2fe;
  color: #0369a1;
}

.badge-in-progress {
  background-color: #dbeafe;
  color: #1e40af;
}

.badge-completed {
  background: rgba(34, 197, 94, 0.15);
  color: #166534;
}

.badge-cancelled {
  background: rgba(107, 114, 128, 0.15);
  color: #374151;
}

/* 진행 상태 바 */
.progress-section {
  margin-bottom: 1rem;
}

.progress-bar {
  height: 0.4rem;
  border-radius: 9999px;
  background: var(--border);
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.progress-bar__fill {
  height: 100%;
  background: linear-gradient(90deg, #f97316, #0ea5e9, #22c55e);
  transition: width 0.3s ease;
}

.progress-steps {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}

.progress-step {
  flex: 1;
  text-align: center;
}

.progress-step__label {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 0.375rem;
  background: var(--border);
  color: #94a3b8;
  font-size: 0.775rem;
  font-weight: 600;
  transition: all 0.2s ease;
}

.progress-step.active .progress-step__label {
  background: var(--primary);
  color: white;
}

/* 정보 카드 */
.info-card {
  background: var(--surface-strong);
  border: 1px solid var(--border);
  border-radius: 0.625rem;
  padding: 1rem;
  margin-bottom: 1.25rem;
}

.info-card__title {
  margin: 0 0 0.75rem;
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
  border-bottom: 1px solid var(--border);
  padding-bottom: 0.5rem;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.45rem 0;
  border-bottom: 1px dashed #f1f5f9;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row__label {
  color: #64748b;
  font-size: 0.875rem;
  font-weight: 500;
}

.info-row__value {
  color: #0f172a;
  font-weight: 600;
  font-size: 0.9375rem;
}

.info-row__value.highlight {
  color: var(--primary);
}

/* 액션 에러 */
.action-error {
  padding: 0.75rem;
  margin-bottom: 1rem;
  border-radius: 0.5rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 0.875rem;
}

/* 액션 섹션 */
.action-section {
  margin-bottom: 1.25rem;
}

.action-btn-group {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.btn-flex-1 {
  flex: 1;
}

.btn-outline {
  background: transparent;
  border: 1px solid #cbd5e1;
  color: #475569;
}

.btn-outline:not(:disabled):hover {
  background: #f1f5f9;
  color: #0f172a;
}

.action-desc {
  margin: 0.5rem 0 0;
  font-size: 0.8125rem;
  color: #64748b;
  text-align: center;
}

.status-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem;
  border-radius: 0.625rem;
  font-weight: 600;
  font-size: 0.9375rem;
}

.status-message.completed {
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.status-message.cancelled {
  background: #f8fafc;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

/* 버튼 스타일 */
.btn {
  padding: 0.75rem 1rem;
  border: none;
  border-radius: 0.625rem;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 2.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-small {
  padding: 0.25rem 0.5rem;
  font-size: 0.8125rem;
  min-height: auto;
}

.btn-large {
  width: 100%;
}

.btn-warning {
  background: #f97316;
  color: white;
}

.btn-warning:not(:disabled):hover {
  background: #ea580c;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(249, 115, 22, 0.3);
}

.btn-success {
  background: #22c55e;
  color: white;
}

.btn-success:not(:disabled):hover {
  background: #16a34a;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(34, 197, 94, 0.3);
}

.btn-primary {
  background: var(--primary);
  color: white;
}

.btn-primary:not(:disabled):hover {
  background: #1e40af;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(29, 78, 216, 0.3);
}

.btn-secondary {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-secondary:not(:disabled):hover {
  background: #cbd5e1;
}

.btn-bottom {
  margin-top: 1rem;
}
</style>
