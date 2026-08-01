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

// 상담 완료 결과 등록 모달 상태
const isResultModalOpen = ref(false)
const isSubmittingResult = ref(false)
const resultErrorMessage = ref('')

const completeFormData = ref({
  resultContent: '',
  reConsultationNeeded: 2, // 1: 필요, 2: 불필요
  satisfactionScore: 5,   // 1~5점
})

const openResultModal = () => {
  completeFormData.value = {
    resultContent: '',
    reConsultationNeeded: 2,
    satisfactionScore: 5,
  }
  resultErrorMessage.value = ''
  isResultModalOpen.value = true
}

const closeResultModal = () => {
  isResultModalOpen.value = false
  resultErrorMessage.value = ''
}

const submitCompleteResult = async () => {
  if (!consultation.value) return

  if (!completeFormData.value.resultContent.trim()) {
    resultErrorMessage.value = '상담 결과 및 조언 내용을 입력해주세요 (최대 100자)'
    return
  }

  isSubmittingResult.value = true
  resultErrorMessage.value = ''

  try {
    await completeConsultation(consultation.value.id, {
      resultContent: completeFormData.value.resultContent.trim(),
      reConsultationNeeded: completeFormData.value.reConsultationNeeded,
      satisfactionScore: completeFormData.value.satisfactionScore,
    })
    closeResultModal()
    await fetchConsultation(false)
  } catch (error: any) {
    console.error('Failed to complete consultation:', error)
    resultErrorMessage.value = error?.response?.data?.message || '상담 완료 처리 중 오류가 발생했습니다.'
  } finally {
    isSubmittingResult.value = false
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
              @click="openResultModal"
              :disabled="isProcessing"
            >
              <span>상담 완료 및 결과 등록</span>
            </button>
            <p class="action-desc">상담 결과 및 만족도 점수를 등록하고 최종 완료 처리합니다.</p>
          </template>

          <!-- COMPLETED 상태: 완료 메시지 및 상담 결과 요약 -->
          <template v-else-if="consultation.status === 'COMPLETED'">
            <div class="status-message completed">
              <span class="icon">✓</span>
              <span>상담이 성공적으로 완료되었습니다</span>
            </div>

            <div v-if="consultation.resultContent || consultation.satisfactionScore" class="completed-result-card">
              <h4 class="result-card-title">등록된 상담 결과</h4>
              <div class="result-row">
                <span class="result-label">상담 결과 요약:</span>
                <div class="result-text-box">{{ consultation.resultContent || '등록된 결과 내용이 없습니다.' }}</div>
              </div>
              <div class="result-row-flex">
                <div class="meta-item">
                  <span class="result-label">재상담 필요 여부:</span>
                  <span class="badge-tag" :class="consultation.reConsultationNeeded === 1 ? 'badge-warn' : 'badge-info'">
                    {{ consultation.reConsultationNeeded === 1 ? '1. 재상담 필요' : '2. 재상담 불필요' }}
                  </span>
                </div>
                <div class="meta-item">
                  <span class="result-label">만족도 점수:</span>
                  <span class="star-rating-display">
                    <span class="stars-yellow">{{ '★'.repeat(consultation.satisfactionScore || 0) }}{{ '☆'.repeat(5 - (consultation.satisfactionScore || 0)) }}</span>
                    <strong class="score-text">{{ consultation.satisfactionScore }}점</strong>
                  </span>
                </div>
              </div>
            </div>
          </template>

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

    <!-- 상담 결과 등록 모달 -->
    <div v-if="isResultModalOpen && consultation" class="modal-backdrop" @click.self="closeResultModal">
      <div class="modal-card">
        <div class="modal-header">
          <h3 class="modal-title">상담 결과 등록 및 완료</h3>
          <button type="button" class="btn-close" @click="closeResultModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="target-info-box">
            <span class="info-label">학생 이름:</span>
            <strong>{{ consultation.studentName }}</strong>
            <span class="info-divider">|</span>
            <span class="info-label">상담 유형:</span>
            <span>{{ consultation.typeName }}</span>
          </div>

          <!-- 상담 결과 텍스트 (100자 이내) -->
          <div class="form-group">
            <div class="label-with-counter">
              <label for="resultContentInputDetail" class="form-label">상담 결과 요약 <span class="required">*</span></label>
              <span class="char-counter" :class="{ 'char-counter--limit': completeFormData.resultContent.length >= 100 }">
                {{ completeFormData.resultContent.length }} / 100자
              </span>
            </div>
            <textarea
              id="resultContentInputDetail"
              v-model="completeFormData.resultContent"
              class="form-textarea"
              maxlength="100"
              rows="3"
              placeholder="상담 결과 및 조언 내용을 입력하세요 (최대 100자)"
            ></textarea>
          </div>

          <!-- 재상담 필요 여부 -->
          <div class="form-group">
            <label class="form-label">재상담 필요 여부 <span class="required">*</span></label>
            <div class="radio-segmented">
              <label class="segmented-option" :class="{ active: completeFormData.reConsultationNeeded === 1 }">
                <input type="radio" v-model.number="completeFormData.reConsultationNeeded" :value="1" hidden />
                <span>1. 재상담 필요</span>
              </label>
              <label class="segmented-option" :class="{ active: completeFormData.reConsultationNeeded === 2 }">
                <input type="radio" v-model.number="completeFormData.reConsultationNeeded" :value="2" hidden />
                <span>2. 재상담 불필요</span>
              </label>
            </div>
          </div>

          <!-- 만족도 점수 (별표 5개) -->
          <div class="form-group">
            <label class="form-label">학생 만족도 점수 <span class="required">*</span></label>
            <div class="star-rating">
              <span
                v-for="star in 5"
                :key="star"
                class="star-icon"
                :class="{ active: star <= completeFormData.satisfactionScore }"
                @click="completeFormData.satisfactionScore = star"
              >
                ★
              </span>
              <span class="star-score-text">{{ completeFormData.satisfactionScore }}점</span>
            </div>
          </div>

          <div v-if="resultErrorMessage" class="form-error-alert">
            {{ resultErrorMessage }}
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeResultModal" :disabled="isSubmittingResult">
            취소
          </button>
          <button type="button" class="btn btn-primary" @click="submitCompleteResult" :disabled="isSubmittingResult">
            <span v-if="!isSubmittingResult">상담 완료 저장</span>
            <span v-else>저장 중...</span>
          </button>
        </div>
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

/* 상담 결과 모달 및 결과 카드 스타일 */
.completed-result-card {
  margin-top: 1rem;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 0.75rem;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.result-card-title {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 700;
  color: #1e293b;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 0.375rem;
}

.result-row {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.result-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #475569;
}

.result-text-box {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.75rem;
  border-radius: 0.5rem;
  font-size: 0.9375rem;
  line-height: 1.5;
  color: #0f172a;
  white-space: pre-wrap;
}

.result-row-flex {
  display: flex;
  gap: 1.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.badge-tag {
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.8125rem;
  font-weight: 700;
}

.badge-warn {
  background: #fef3c7;
  color: #b45309;
}

.badge-info {
  background: #e0f2fe;
  color: #0369a1;
}

.star-rating-display {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.stars-yellow {
  color: #f59e0b;
  font-size: 1.125rem;
  letter-spacing: 0.1em;
}

.score-text {
  color: #b45309;
  font-size: 0.9375rem;
}

/* 모달 스타일 */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-card {
  background: white;
  width: 100%;
  max-width: 32rem;
  border-radius: 1rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f8fafc;
}

.modal-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
}

.btn-close {
  border: none;
  background: transparent;
  font-size: 1.25rem;
  color: #64748b;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 0.375rem;
}

.btn-close:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.target-info-box {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  color: #1e40af;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.info-divider {
  color: #93c5fd;
}

.label-with-counter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.375rem;
}

.char-counter {
  font-size: 0.75rem;
  color: #64748b;
}

.char-counter--limit {
  color: #ef4444;
  font-weight: 700;
}

.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
}

.form-textarea:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.radio-segmented {
  display: flex;
  gap: 0.75rem;
}

.segmented-option {
  flex: 1;
  padding: 0.625rem 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.5rem;
  text-align: center;
  font-size: 0.875rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f8fafc;
}

.segmented-option:hover {
  background: #f1f5f9;
}

.segmented-option.active {
  background: #2563eb;
  color: white;
  border-color: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.star-rating {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.star-icon {
  font-size: 1.75rem;
  color: #cbd5e1;
  cursor: pointer;
  transition: transform 0.15s ease, color 0.15s ease;
}

.star-icon:hover,
.star-icon.active {
  color: #f59e0b;
  transform: scale(1.15);
}

.star-score-text {
  margin-left: 0.5rem;
  font-size: 1rem;
  font-weight: 700;
  color: #d97706;
}

.form-error-alert {
  padding: 0.75rem 1rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  background: #f8fafc;
}
</style>
