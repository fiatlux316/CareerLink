<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getConsultation, cancelConsultation } from '../api/consultation'
import type { Consultation } from '../types/consultation'

const props = defineProps<{
  id: string
}>()

const router = useRouter()

// 상태 관리
const consultation = ref<Consultation | null>(null)
const isLoading = ref(true)
const isRefreshing = ref(false)
const errorMessage = ref('')
const pollingIntervalId = ref<number | null>(null)
const isCancelling = ref(false)
const cancelErrorMessage = ref('')

// 라우트 param 또는 localStorage에서 consultation id 조회
const getConsultationId = (): string | null => {
  if (props.id) {
    return props.id
  }
  return localStorage.getItem('careerlink_consultation_id')
}

// 상태 한글 레이블
const getStatusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    RECEIVED: '접수완료',
    ACCEPTED: '수락완료',
    IN_PROGRESS: '진행중',
    COMPLETED: '상담완료',
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

// 상태 단계 진행도 (0~100%)
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
const fetchConsultation = async (consultationId: string) => {
  isRefreshing.value = true
  errorMessage.value = ''

  try {
    consultation.value = await getConsultation(consultationId)
    isLoading.value = false

    // COMPLETED 또는 CANCELLED 상태이면 폴링 중단
    if (consultation.value.status === 'COMPLETED' || consultation.value.status === 'CANCELLED') {
      stopPolling()
    }
  } catch (error: unknown) {
    const err = error as any
    if (err?.response?.status === 404) {
      errorMessage.value = '상담 접수 내역을 찾을 수 없습니다'
    } else {
      errorMessage.value = '상담 정보를 조회할 수 없습니다. 잠시 후 다시 시도해주세요'
    }
    isLoading.value = false
  } finally {
    isRefreshing.value = false
  }
}

// 폴링 시작 (10초 주기)
const startPolling = (consultationId: string) => {
  pollingIntervalId.value = window.setInterval(() => {
    fetchConsultation(consultationId)
  }, 10000)
}

// 폴링 중단
const stopPolling = () => {
  if (pollingIntervalId.value !== null) {
    clearInterval(pollingIntervalId.value)
    pollingIntervalId.value = null
  }
}

// 수동 재조회
const handleRefresh = async () => {
  const consultationId = getConsultationId()
  if (consultationId) {
    await fetchConsultation(consultationId)
  }
}

// 새로 접댈하기 버튼 클릭
const handleNewConsultation = () => {
  const savedName = localStorage.getItem('careerlink_student_name')
  const savedPhone = localStorage.getItem('careerlink_student_phone')
  localStorage.removeItem('careerlink_consultation_id')
  
  if (savedName && savedPhone) {
    // 이미 입장 정보가 있으면 홈 화면으로
    router.push('/home')
  } else {
    // 없으면 입장 화면으로
    router.push('/receive')
  }
}

// 상담 취소
const handleCancelConsultation = async () => {
  const consultationId = getConsultationId()
  if (!consultationId) {
    return
  }

  const confirmed = window.confirm('정말로 상담을 취소하시겠습니까?\n\n접수→ 수락 전 단계에서만 취소 가능합니다.')
  if (!confirmed) {
    return
  }

  isCancelling.value = true
  cancelErrorMessage.value = ''

  try {
    const cancelled = await cancelConsultation(consultationId)
    consultation.value = cancelled
    stopPolling()
  } catch (error: unknown) {
    const err = error as any
    const status = err?.response?.status
    const data = err?.response?.data as any

    if (status === 409) {
      cancelErrorMessage.value = '이미 상담사가 처리를 시작하여 취소할 수 없습니다'
    } else if (status === 400) {
      cancelErrorMessage.value = data?.message || '취소 요청 메시지가 올바르지 않습니다'
    } else {
      cancelErrorMessage.value = '취소 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요'
    }
    // 최신 상태 재조회
    await fetchConsultation(consultationId)
  } finally {
    isCancelling.value = false
  }
}

// 전체 목록 보기 버튼 클릭
const handleBackToList = () => {
  router.push('/status')
}

// 컴포넌트 마운트
onMounted(async () => {
  const consultationId = getConsultationId()

  if (!consultationId) {
    errorMessage.value = '상담 접수 내역을 찾을 수 없습니다'
    isLoading.value = false
    return
  }

  await fetchConsultation(consultationId)

  // COMPLETED나 CANCELLED아니면 폴링 시작
  if (consultation.value && !['COMPLETED', 'CANCELLED'].includes(consultation.value.status)) {
    startPolling(consultationId)
  }
})

// 컴포넌트 언마운트
onUnmounted(() => {
  stopPolling()
})

// 상담 정보 없음 여부
const hasNoConsultation = computed(() => {
  return !isLoading.value && !consultation.value && errorMessage.value
})
</script>

<template>
  <article class="status-view">
    <div class="status-view__container">
      <!-- 에러 상태 -->
      <div v-if="hasNoConsultation" class="status-view__error-state">
        <div class="error-card">
          <div class="error-card__icon">⚠️</div>
          <h2 class="error-card__title">상담 접수 내역을 찾을 수 없습니다</h2>
          <p class="error-card__message">{{ errorMessage }}</p>
          <button class="btn btn-primary btn-large" @click="handleNewConsultation">
            새로 접수하기
          </button>
        </div>
      </div>

      <!-- 로딩 상태 -->
      <div v-else-if="isLoading" class="status-view__loading">
        <div class="spinner"></div>
        <p>상담 정보를 조회 중입니다...</p>
      </div>

      <!-- 상담 정보 표시 -->
      <div v-else-if="consultation" class="status-view__content">
        <!-- 헤더 -->
        <div class="status-view__header">
          <span class="badge">학생 현황</span>
          <h1 class="status-view__title">상담 진행 현황</h1>
        </div>

        <!-- 상태 배지 및 재조회 버튼 -->
        <div class="status-view__top-bar">
          <div :class="['badge', getStatusColorClass(consultation.status)]">
            {{ getStatusLabel(consultation.status) }}
          </div>
          <button
            class="btn btn-small btn-secondary"
            @click="handleRefresh"
            :disabled="isRefreshing"
            :class="{ loading: isRefreshing }"
            aria-label="상담 정보 새로고침"
          >
            <span v-if="!isRefreshing">🔄 새로고침</span>
            <span v-else>갱신 중...</span>
          </button>
        </div>

        <!-- 진행 상태 바 -->
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

        <!-- 상담 정보 카드 -->
        <div class="info-section">
          <h2 class="info-section__title">상담 정보</h2>

          <div class="info-item">
            <span class="info-item__label">학생 이름</span>
            <span class="info-item__value">{{ consultation.studentName }}</span>
          </div>

          <div class="info-item">
            <span class="info-item__label">상담 테마</span>
            <span class="info-item__value">{{ consultation.topicName || '-' }}</span>
          </div>

          <div class="info-item">
            <span class="info-item__label">상담 유형</span>
            <span class="info-item__value">{{ consultation.typeName }}</span>
          </div>

          <div class="info-item">
            <span class="info-item__label">현재 상태</span>
            <span class="info-item__value">{{ getStatusLabel(consultation.status) }}</span>
          </div>

          <div v-if="consultation.counselorName" class="info-item">
            <span class="info-item__label">담당 상담사</span>
            <span class="info-item__value">{{ consultation.counselorName }}</span>
          </div>

          <div class="info-item">
            <span class="info-item__label">접수 시각</span>
            <span class="info-item__value">{{ formatDateTime(consultation.createdAt) }}</span>
          </div>

          <div class="info-item">
            <span class="info-item__label">최종 갱신</span>
            <span class="info-item__value">{{ formatDateTime(consultation.updatedAt) }}</span>
          </div>
        </div>

        <!-- 취소 에러 메시지 -->
        <div v-if="cancelErrorMessage" class="cancel-error-message">
          <span class="close-icon" @click="cancelErrorMessage = ''">×</span>
          {{ cancelErrorMessage }}
        </div>

        <!-- 폴링 상태 표시 (RECEIVED단계) -->
        <div v-if="consultation.status === 'RECEIVED'" class="polling-status">
          <span class="polling-indicator"></span>
          <span class="polling-text">10초마다 자동으로 상태를 갱신하고 있습니다</span>
        </div>

        <!-- 완료 상태 메시지 -->
        <div v-if="consultation.status === 'COMPLETED'" class="completion-message">
          <span class="completion-icon">✓</span>
          <p>상담이 완료되었습니다</p>
        </div>

        <!-- 취소 상태 메시지 -->
        <div v-if="consultation.status === 'CANCELLED'" class="cancelled-message">
          <span class="cancelled-icon">✕</span>
          <p>상담이 취소되었습니다</p>
        </div>

        <!-- 단추 및 목록 버튼 -->
        <div class="action-buttons">
          <button
            v-if="consultation.status === 'RECEIVED'"
            class="btn btn-danger btn-large"
            @click="handleCancelConsultation"
            :disabled="isCancelling"
            :class="{ loading: isCancelling }"
          >
            <span v-if="!isCancelling">상담 취소</span>
            <span v-else>취소 중...</span>
          </button>
          <button class="btn btn-secondary btn-large" @click="handleBackToList">
            전체 목록 보기
          </button>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.status-view {
  width: 100%;
}

.status-view__container {
  padding: 1.5rem 1rem;
}

/* 에러 상태 */
.error-card {
  text-align: center;
  padding: 2rem 1.5rem;
  border: 2px dashed var(--border);
  border-radius: 1.5rem;
  background: var(--primary-soft);
}

.error-card__icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.error-card__title {
  margin: 0 0 0.5rem;
  font-size: 1.375rem;
  color: #0f172a;
}

.error-card__message {
  margin: 0.5rem 0 1.5rem;
  color: #475569;
}

/* 로딩 상태 */
.status-view__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  gap: 1rem;
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

/* 헤더 */
.status-view__header {
  margin-bottom: 1.5rem;
}

.status-view__title {
  margin: 0.75rem 0 0;
  font-size: 1.875rem;
  line-height: 1.2;
  color: #0f172a;
}

/* 상단 바 */
.status-view__top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
  gap: 1rem;
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
  margin-bottom: 0.75rem;
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
  background: linear-gradient(90deg, #fb923c, #3b82f6, #22c55e);
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

/* 정보 섹션 */
.info-section {
  background: var(--surface-strong);
  border: 1px solid var(--border);
  border-radius: 0.5rem;
  padding: 0.35rem 0.625rem;
  margin-bottom: 0.5rem;
}

.info-section__title {
  margin: 0 0 0.25rem;
  font-size: 1rem;
  color: #0f172a;
}

.info-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.5rem;
  align-items: center;
  padding: 0.15rem 0;
  border-bottom: 1px solid var(--border);
}

.info-item:last-child {
  border-bottom: none;
}

.info-item__label {
  color: #475569;
  font-size: 0.9125rem;
  font-weight: 500;
  min-width: 4.5rem;
}

.info-item__value {
  color: #0f172a;
  font-weight: 600;
  font-size: 0.975rem;
  word-break: break-word;
}

/* 취소 에러 메시지 */
.cancel-error-message {
  padding: 1rem;
  margin-bottom: 1.5rem;
  border-radius: 0.75rem;
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.3);
  color: #991b1b;
  font-size: 0.9375rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.close-icon {
  cursor: pointer;
  font-weight: bold;
  font-size: 1.25rem;
  line-height: 1;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.close-icon:hover {
  opacity: 1;
}

/* 폴링 상태 표시 */
.polling-status {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--primary-soft);
  border-radius: 0.5rem;
  font-size: 0.8125rem;
  color: var(--primary);
}

.polling-indicator {
  display: inline-block;
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  background: var(--primary);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

.polling-text {
  flex: 1;
}

/* 완료 메시지 */
.completion-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  background: rgba(34, 197, 94, 0.1);
  border-radius: 0.625rem;
  color: #166534;
  margin-bottom: 1rem;
}

.completion-icon {
  display: inline-block;
  font-size: 1.125rem;
  font-weight: bold;
  line-height: 1;
}

.completion-message p {
  margin: 0;
  font-weight: 600;
  font-size: 0.9375rem;
}

/* 취소 메시지 */
.cancelled-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  background: rgba(107, 114, 128, 0.1);
  border-radius: 0.625rem;
  color: #374151;
  margin-bottom: 1rem;
}

.cancelled-icon {
  display: inline-block;
  font-size: 1.125rem;
  font-weight: bold;
  line-height: 1;
}

.cancelled-message p {
  margin: 0;
  font-weight: 600;
  font-size: 0.9375rem;
}

/* 단추 및 목록 버튼 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

/* 버튼 */
.btn {
  padding: 0.75rem 1rem;
  border: none;
  border-radius: 0.75rem;
  font-weight: 600;
  font-size: 0.9375rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  white-space: nowrap;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--primary);
  color: white;
  min-height: 2.75rem;
}

.btn-primary:not(:disabled):hover {
  background: #1e40af;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(29, 78, 216, 0.3);
}

.btn-large {
  width: 100%;
  justify-content: center;
}

.btn-small {
  min-height: auto;
  padding: 0.5rem 0.875rem;
  font-size: 0.8125rem;
}

.btn-secondary {
  background: var(--border);
  color: #0f172a;
}

.btn-secondary:not(:disabled):hover {
  background: rgba(148, 163, 184, 0.3);
  transform: translateY(-1px);
}

.btn-danger {
  background: #dc2626;
  color: white;
  min-height: 2.75rem;
}

.btn-danger:not(:disabled):hover {
  background: #991b1b;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(220, 38, 38, 0.3);
}

.btn.loading {
  opacity: 0.8;
}

/* 반응형 */
@media (min-width: 768px) {
  .status-view__container {
    max-width: 36rem;
    margin: 0 auto;
    padding: 2rem;
  }

  .info-section {
    padding: 2rem;
  }

  .info-item {
    grid-template-columns: 8rem 1fr;
    padding: 1rem 0;
  }

  .progress-steps {
    gap: 2rem;
  }
}
</style>
