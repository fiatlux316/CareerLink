<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getConsultationsByStudentPhone } from '../api/consultation'
import type { Consultation } from '../types/consultation'

const router = useRouter()

// 상태 관리
const consultations = ref<Consultation[]>([])
const isLoading = ref(true)
const errorMessage = ref('')
const studentPhone = ref('')

// 상태 한글 레이블
const getStatusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    RECEIVED: '접수완료',
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
    IN_PROGRESS: 'badge-in-progress',
    COMPLETED: 'badge-completed',
    CANCELLED: 'badge-cancelled',
  }
  return colors[status] || ''
}

// 날짜 포맷팅 (날짜만 표시)
const formatDate = (dateString: string): string => {
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

// 상담 목록 조회
const fetchConsultations = async (phone: string) => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    consultations.value = await getConsultationsByStudentPhone(phone)
  } catch (error: unknown) {
    const err = error as any
    if (err?.response?.status === 400) {
      errorMessage.value = '유효한 휴대폰 번호가 아닙니다'
    } else {
      errorMessage.value = '상담 목록을 조회할 수 없습니다. 잠시 후 다시 시도해주세요'
    }
  } finally {
    isLoading.value = false
  }
}

// 상담 항목 클릭
const handleConsultationClick = (consultationId: string) => {
  router.push(`/status/${consultationId}`)
}

// 새로 접댈하기 버튼 클릭
const handleNewConsultation = () => {
  const savedName = localStorage.getItem('careerlink_student_name')
  const savedPhone = localStorage.getItem('careerlink_student_phone')
  
  if (savedName && savedPhone) {
    // 이미 입장 정보가 있으면 홈 화면으로
    router.push('/home')
  } else {
    // 없으면 입장 화면으로
    router.push('/receive')
  }
}

// 컴포넌트 마운트
onMounted(async () => {
  const phone = localStorage.getItem('careerlink_student_phone')

  if (!phone) {
    errorMessage.value = '한번도 상담을 접수하지 않으셨습니다'
    isLoading.value = false
    return
  }

  studentPhone.value = phone
  await fetchConsultations(phone)
})
</script>

<template>
  <article class="list-view">
    <div class="list-view__container">
      <!-- 헤더 -->
      <div class="list-view__header">
        <span class="badge">학생 현황</span>
        <h1 class="list-view__title">내 상담 목록</h1>
        <p class="list-view__subtitle">접수하신 상담 목록을 확인하세요</p>
      </div>

      <!-- 로딩 상태 -->
      <div v-if="isLoading" class="list-view__loading">
        <div class="spinner"></div>
        <p>상담 목록을 불러오는 중입니다...</p>
      </div>

      <!-- 에러 상태: 한번도 접수한 적 없음 -->
      <div v-else-if="!studentPhone && !isLoading" class="list-view__empty">
        <div class="empty-card">
          <div class="empty-card__icon">📋</div>
          <h2 class="empty-card__title">접수한 상담이 없습니다</h2>
          <p class="empty-card__message">새로운 상담을 접수해보세요</p>
          <button class="btn btn-primary btn-large" @click="handleNewConsultation">
            새로 접수하기
          </button>
        </div>
      </div>

      <!-- 에러 상태: 조회 실패 -->
      <div v-else-if="errorMessage && !studentPhone" class="list-view__error-state">
        <div class="empty-card">
          <div class="empty-card__icon">⚠️</div>
          <h2 class="empty-card__title">{{ errorMessage }}</h2>
          <p class="empty-card__message">새로운 상담을 접수해보세요</p>
          <button class="btn btn-primary btn-large" @click="handleNewConsultation">
            새로 접수하기
          </button>
        </div>
      </div>

      <!-- 목록 비어있음: 폰번호는 있지만 상담이 없음 -->
      <div v-else-if="!isLoading && consultations.length === 0 && studentPhone" class="list-view__empty">
        <div class="empty-card">
          <div class="empty-card__icon">📋</div>
          <h2 class="empty-card__title">접수한 상담이 없습니다</h2>
          <p class="empty-card__message">새로운 상담을 접수해보세요</p>
          <button class="btn btn-primary btn-large" @click="handleNewConsultation">
            새로 접수하기
          </button>
        </div>
      </div>

      <!-- 상담 목록 -->
      <div v-else-if="!isLoading && consultations.length > 0" class="list-view__content">
        <div class="consultations-list">
          <button
            v-for="consultation in consultations"
            :key="consultation.id"
            class="consultation-item"
            @click="handleConsultationClick(consultation.id)"
          >
            <div class="consultation-item__header">
              <div class="consultation-item__info">
                <div class="consultation-item__type">{{ consultation.typeName }}</div>
                <div class="consultation-item__date">{{ formatDate(consultation.createdAt) }}</div>
              </div>
              <div :class="['badge', getStatusColorClass(consultation.status)]">
                {{ getStatusLabel(consultation.status) }}
              </div>
            </div>
            <div class="consultation-item__footer">
              <span class="consultation-item__name">{{ consultation.studentName }}</span>
              <span class="consultation-item__arrow">→</span>
            </div>
          </button>
        </div>

        <!-- 새로 접수하기 버튼 -->
        <button class="btn btn-secondary btn-large btn-bottom" @click="handleNewConsultation">
          새로 접수하기
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.list-view {
  width: 100%;
}

.list-view__container {
  padding: 1.5rem 1rem;
}

.list-view__header {
  margin-bottom: 2rem;
}

.list-view__title {
  margin: 0.75rem 0 0.5rem;
  font-size: 1.875rem;
  line-height: 1.2;
  color: #0f172a;
}

.list-view__subtitle {
  margin: 0.5rem 0 0;
  color: #475569;
  font-size: 0.9375rem;
}

/* 로딩 상태 */
.list-view__loading {
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

/* 빈 상태 */
.list-view__empty,
.list-view__error-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  min-height: 400px;
}

.empty-card {
  text-align: center;
  padding: 2rem 1.5rem;
  border: 2px dashed var(--border);
  border-radius: 1.5rem;
  background: var(--primary-soft);
  max-width: 20rem;
}

.empty-card__icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-card__title {
  margin: 0 0 0.5rem;
  font-size: 1.375rem;
  color: #0f172a;
}

.empty-card__message {
  margin: 0.5rem 0 1.5rem;
  color: #475569;
}

/* 목록 */
.consultations-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.consultation-item {
  padding: 1.25rem;
  border: 1px solid var(--border);
  border-radius: 0.875rem;
  background: var(--surface-strong);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  text-align: left;
  font-size: 1rem;
}

.consultation-item:hover {
  border-color: var(--primary);
  background: var(--primary-soft);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.15);
}

.consultation-item:active {
  transform: translateY(0);
}

.consultation-item__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.consultation-item__info {
  flex: 1;
}

.consultation-item__type {
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 0.25rem;
}

.consultation-item__date {
  font-size: 0.8125rem;
  color: #475569;
}

.consultation-item__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.consultation-item__name {
  font-size: 0.9375rem;
  color: #475569;
}

.consultation-item__arrow {
  color: var(--primary);
  font-weight: 600;
}

/* 배지 색상 */
.badge-received {
  background: rgba(251, 146, 60, 0.15);
  color: #9a3412;
}

.badge-in-progress {
  background: rgba(59, 130, 246, 0.15);
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

/* 버튼 */
.btn {
  padding: 0.875rem 1rem;
  border: none;
  border-radius: 0.75rem;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 2.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
  background: var(--border);
  color: #0f172a;
}

.btn-secondary:not(:disabled):hover {
  background: rgba(148, 163, 184, 0.3);
  transform: translateY(-1px);
}

.btn-large {
  width: 100%;
}

.btn-bottom {
  margin-top: 1rem;
}

/* 반응형 */
@media (min-width: 768px) {
  .list-view__container {
    max-width: 36rem;
    margin: 0 auto;
    padding: 2rem;
  }

  .consultations-list {
    gap: 1rem;
  }

  .consultation-item {
    padding: 1.5rem;
  }
}
</style>
