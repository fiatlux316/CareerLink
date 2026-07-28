<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getConsultationTypes } from '../api/consultation'
import { enterCounselor, getCounselorConsultations, acceptConsultation, startProgressConsultation, completeConsultation } from '../api/counselor'
import type { ConsultationType, ErrorResponse, Consultation } from '../types/consultation'
import type { CounselorSessionStorage } from '../types/counselor'

// 상태 관리 - 입장 폼
const types = ref<ConsultationType[]>([])
const isLoadingTypes = ref(true)
const isSubmitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref<Record<string, string>>({})

// 입장 완료 상태
const hasEntered = ref(false)
const counselorSessionInfo = ref<CounselorSessionStorage | null>(null)

// 폼 데이터
const formData = ref({
  counselorName: '',
  counselorPhone: '',
  typeId: 0,
})

// 대시보드 상태 관리
const receivedConsultations = ref<Consultation[]>([])
const acceptedConsultations = ref<Consultation[]>([])
const inProgressConsultations = ref<Consultation[]>([])
const isLoadingDashboard = ref(false)
const dashboardErrorMessage = ref('')
const processingItemIds = ref<Set<string>>(new Set())
let pollingInterval: number | null = null

// 핸드폰 번호 유효성 검증 (010-1234-5678 또는 01012345678 형식)
const validatePhoneNumber = (phone: string): boolean => {
  const phoneRegex = /^01[0-9](-?\d{3,4}){2}$/
  return phoneRegex.test(phone.replace(/\s/g, ''))
}

// 클라이언트 사이드 유효성 검증
const validateForm = (): boolean => {
  fieldErrors.value = {}
  let isValid = true

  if (!formData.value.counselorName.trim()) {
    fieldErrors.value.counselorName = '이름을 입력해주세요'
    isValid = false
  }

  const phoneTrimmed = formData.value.counselorPhone.trim()
  if (phoneTrimmed && !validatePhoneNumber(phoneTrimmed)) {
    fieldErrors.value.counselorPhone = '휴대폰 번호 형식이 올바르지 않습니다 (예: 010-1234-5678)'
    isValid = false
  }

  if (!formData.value.typeId) {
    fieldErrors.value.typeId = '담당 상담 유형을 선택해주세요'
    isValid = false
  }

  return isValid
}

// 핸드폰 번호 자동 포맷팅
const formatPhoneNumber = (phone: string): string => {
  const digits = phone.replace(/\D/g, '')
  if (digits.length <= 3) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7, 11)}`
}

const handlePhoneInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  const formatted = formatPhoneNumber(target.value)
  formData.value.counselorPhone = formatted
}

// 폼 제출
const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    const response = await enterCounselor({
      counselorName: formData.value.counselorName,
      counselorPhone: formData.value.counselorPhone,
      typeId: formData.value.typeId,
    })

    // localStorage에 상담사 세션 정보 저장
    const sessionInfo: CounselorSessionStorage = {
      typeId: response.typeId,
      typeName: response.typeName,
      counselorName: response.counselorName,
      id: response.id,
    }
    localStorage.setItem('careerlink_counselor_session', JSON.stringify(sessionInfo))
    counselorSessionInfo.value = sessionInfo
    hasEntered.value = true
    // 입장 후 즉시 대시보드 로드
    await loadDashboard()
  } catch (error: unknown) {
    const err = error as any
    const status = err?.response?.status
    const data = err?.response?.data as ErrorResponse | undefined

    const responseFieldErrors = data?.fieldErrors ?? data?.errors

    if (status === 400) {
      // 필드별 에러 처리
      if (responseFieldErrors) {
        fieldErrors.value = responseFieldErrors
        errorMessage.value = '입력 정보를 다시 확인해주세요'
      } else {
        errorMessage.value = data?.message || '입력 정보가 올바르지 않습니다'
      }
    } else if (status === 404) {
      errorMessage.value = '선택하신 상담 유형을 찾을 수 없습니다'
    } else {
      errorMessage.value = '입장 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요'
    }
  } finally {
    isSubmitting.value = false
  }
}

// 다시 입장하기 (세션 정보 삭제)
const handleLogout = () => {
  localStorage.removeItem('careerlink_counselor_session')
  counselorSessionInfo.value = null
  hasEntered.value = false
  receivedConsultations.value = []
  acceptedConsultations.value = []
  inProgressConsultations.value = []

  if (pollingInterval !== null) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
}

// 대시보드 로드
const loadDashboard = async () => {
  if (!counselorSessionInfo.value) return

  isLoadingDashboard.value = true
  dashboardErrorMessage.value = ''

  try {
    // RECEIVED 상태 조회
    const received = await getCounselorConsultations(counselorSessionInfo.value.typeId, 'RECEIVED')
    receivedConsultations.value = received

    // ACCEPTED 상태 조회
    const accepted = await getCounselorConsultations(counselorSessionInfo.value.typeId, 'ACCEPTED')
    acceptedConsultations.value = accepted

    // IN_PROGRESS 상태 조회
    const inProgress = await getCounselorConsultations(counselorSessionInfo.value.typeId, 'IN_PROGRESS')
    inProgressConsultations.value = inProgress
  } catch (error: unknown) {
    const err = error as any
    const status = err?.response?.status

    if (status === 404) {
      dashboardErrorMessage.value = '상담 유형을 찾을 수 없습니다'
    } else {
      dashboardErrorMessage.value = '접수 목록을 불러올 수 없습니다'
    }
    console.error('Failed to load consultations:', error)
  } finally {
    isLoadingDashboard.value = false
  }
}

// 상담 수락 (RECEIVED -> ACCEPTED)
const handleAcceptConsultation = async (consultationId: string) => {
  if (!counselorSessionInfo.value) return

  processingItemIds.value.add(consultationId)

  try {
    await acceptConsultation(consultationId, counselorSessionInfo.value.counselorName)
    // 수락 성공 후 목록 재조회
    await loadDashboard()
  } catch (error: unknown) {
    const err = error as any
    const status = err?.response?.status
    const data = err?.response?.data as ErrorResponse | undefined

    if (status === 409) {
      dashboardErrorMessage.value = '이미 다른 상담사가 수락한 상담입니다. 목록을 새로고침합니다.'
    } else {
      dashboardErrorMessage.value = data?.message || '상담 수락 중 오류가 발생했습니다'
    }

    // 에러 후에도 목록 재조회하여 최신 상태 반영
    await loadDashboard()
  } finally {
    processingItemIds.value.delete(consultationId)
  }
}

// 상담 진행 (ACCEPTED -> IN_PROGRESS)
const handleStartProgressConsultation = async (consultationId: string) => {
  if (!counselorSessionInfo.value) return

  processingItemIds.value.add(consultationId)

  try {
    await startProgressConsultation(consultationId)
    // 성공 후 목록 재조회
    await loadDashboard()
  } catch (error: unknown) {
    const err = error as any
    const data = err?.response?.data as ErrorResponse | undefined

    dashboardErrorMessage.value = data?.message || '상담 진행 처리 중 오류가 발생했습니다'

    // 에러 후에도 목록 재조회하여 최신 상태 반영
    await loadDashboard()
  } finally {
    processingItemIds.value.delete(consultationId)
  }
}

// 상담 완료 (IN_PROGRESS -> COMPLETED)
const handleCompleteConsultation = async (consultationId: string) => {
  if (!counselorSessionInfo.value) return

  processingItemIds.value.add(consultationId)

  try {
    await completeConsultation(consultationId)
    // 완료 성공 후 목록 재조회
    await loadDashboard()
  } catch (error: unknown) {
    const err = error as any
    const data = err?.response?.data as ErrorResponse | undefined

    dashboardErrorMessage.value = data?.message || '상담 완료 중 오류가 발생했습니다'

    // 에러 후에도 목록 재조회하여 최신 상태 반영
    await loadDashboard()
  } finally {
    processingItemIds.value.delete(consultationId)
  }
}

// 시간 포맷팅 (상대시간)
const formatTime = (dateString: string): string => {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMinutes = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)

  if (diffMinutes < 1) return '방금 전'
  if (diffMinutes < 60) return `${diffMinutes}분 전`
  if (diffHours < 24) return `${diffHours}시간 전`

  // 날짜 포맷
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')

  return `${month}/${day} ${hours}:${minutes}`
}

// 상담 유형 목록 로드 및 대시보드 초기화
onMounted(async () => {
  try {
    // 기존 세션 확인
    const storedSession = localStorage.getItem('careerlink_counselor_session')
    if (storedSession) {
      counselorSessionInfo.value = JSON.parse(storedSession)
      hasEntered.value = true
      // 세션이 있으면 대시보드 로드
      await loadDashboard()
    }

    // 상담 유형 로드
    types.value = await getConsultationTypes()
  } catch (error) {
    console.error('Failed to load consultation types:', error)
    errorMessage.value = '상담 유형을 불러올 수 없습니다'
  } finally {
    isLoadingTypes.value = false
  }

  // 5초 주기로 폴링 시작 (입장 후에만)
  if (hasEntered.value) {
    pollingInterval = setInterval(async () => {
      await loadDashboard()
    }, 5000)
  }
})

// 컴포넌트 언마운트 시 폴링 중지
onUnmounted(() => {
  if (pollingInterval !== null) {
    clearInterval(pollingInterval)
  }
})

// 선택된 상담 유형 정보
const selectedType = computed(() => {
  return types.value.find((t) => t.id === formData.value.typeId)
})

// 제출 버튼 비활성화 여부
const isSubmitDisabled = computed(() => {
  return isSubmitting.value || isLoadingTypes.value
})

// 수동 새로고침 버튼 비활성화 여부
const isRefreshDisabled = computed(() => {
  return isLoadingDashboard.value || processingItemIds.value.size > 0
})
</script>

<template>
  <article class="counselor-view">
    <div class="counselor-view__container">
      <!-- 입장 전: 폼 표시 -->
      <template v-if="!hasEntered">
        <!-- 헤더 -->
        <div class="counselor-view__header">
          <span class="badge">상담사 입장</span>
          <h1 class="counselor-view__title">상담사 입장</h1>
          <p class="counselor-view__subtitle">정보를 입력하고 상담 시스템에 입장해주세요</p>
        </div>

        <!-- 에러 메시지 -->
        <div v-if="errorMessage" class="counselor-view__error">
          {{ errorMessage }}
        </div>

        <!-- 상담 유형 로딩 -->
        <div v-if="isLoadingTypes" class="counselor-view__loading">
          <p>상담 유형을 불러오는 중입니다...</p>
        </div>

        <!-- 폼 -->
        <form v-if="!isLoadingTypes" @submit.prevent="handleSubmit" class="counselor-view__form">
          <!-- 상담 유형 선택 -->
          <fieldset class="form-group">
            <legend class="form-label">담당 상담 유형</legend>
            <div class="consultation-types">
              <label
                v-for="type in types"
                :key="type.id"
                class="consultation-type-item"
                :class="{ active: formData.typeId === type.id }"
              >
                <input
                  v-model.number="formData.typeId"
                  type="radio"
                  :value="type.id"
                  name="typeId"
                  class="consultation-type-input"
                />
                <div class="consultation-type-content">
                  <div class="consultation-type-name">{{ type.name }}</div>
                  <div class="consultation-type-description">{{ type.description }}</div>
                </div>
              </label>
            </div>
            <div v-if="fieldErrors.typeId" class="form-error">{{ fieldErrors.typeId }}</div>
          </fieldset>

          <!-- 선택된 유형 설명 (모바일용) -->
          <div v-if="selectedType" class="selected-type-info">
            <strong>담당 상담:</strong> {{ selectedType.name }}
          </div>

          <!-- 이름 입력 -->
          <div class="form-group">
            <label for="counselorName" class="form-label">이름</label>
            <input
              id="counselorName"
              v-model="formData.counselorName"
              type="text"
              class="form-input"
              :class="{ error: fieldErrors.counselorName }"
              placeholder="상담사 이름을 입력해주세요"
              autocomplete="name"
              :disabled="isSubmitting"
            />
            <div v-if="fieldErrors.counselorName" class="form-error">
              {{ fieldErrors.counselorName }}
            </div>
          </div>

          <!-- 휴대폰 번호 입력 -->
          <div class="form-group">
            <label for="counselorPhone" class="form-label">
              휴대폰 번호 <span class="form-label__optional">(선택)</span>
            </label>
            <input
              id="counselorPhone"
              :value="formData.counselorPhone"
              type="tel"
              class="form-input"
              :class="{ error: fieldErrors.counselorPhone }"
              placeholder="010-1234-5678 (선택 입력)"
              autocomplete="tel"
              :disabled="isSubmitting"
              @input="handlePhoneInput"
            />
            <p class="form-help">휴대폰 번호는 선택사항입니다.</p>
            <div v-if="fieldErrors.counselorPhone" class="form-error">
              {{ fieldErrors.counselorPhone }}
            </div>
          </div>

          <!-- 제출 버튼 -->
          <button
            type="submit"
            class="btn btn-primary btn-large"
            :disabled="isSubmitDisabled"
            :class="{ loading: isSubmitting }"
          >
            <span v-if="!isSubmitting">입장하기</span>
            <span v-else>입장 중...</span>
          </button>
        </form>
      </template>

      <!-- 입장 후: 완료 상태 표시 -->
      <template v-else>
        <!-- 헤더 -->
        <div class="counselor-view__header">
          <span class="badge badge-success">상담사 입장</span>
          <h1 class="counselor-view__title">입장 완료</h1>
          <p class="counselor-view__subtitle">{{ counselorSessionInfo?.counselorName }}님의 상담 접수 현황</p>
        </div>

        <!-- 입장 정보 표시 -->
        <div class="session-info">
          <div class="session-info__item">
            <span class="session-info__label">상담사 이름</span>
            <span class="session-info__value">{{ counselorSessionInfo?.counselorName }}</span>
          </div>
          <div class="session-info__item">
            <span class="session-info__label">담당 상담 유형</span>
            <span class="session-info__value">{{ counselorSessionInfo?.typeName }}</span>
          </div>
        </div>

        <!-- 대시보드 에러 메시지 -->
        <div v-if="dashboardErrorMessage" class="counselor-view__error">
          {{ dashboardErrorMessage }}
        </div>

        <!-- 대시보드 로딩 -->
        <div v-if="isLoadingDashboard && receivedConsultations.length === 0 && acceptedConsultations.length === 0 && inProgressConsultations.length === 0" class="counselor-view__loading">
          <p>접수 목록을 불러오는 중입니다...</p>
        </div>

        <!-- 접수 목록 영역 -->
        <div v-if="!isLoadingDashboard || receivedConsultations.length > 0 || acceptedConsultations.length > 0 || inProgressConsultations.length > 0" class="counselor-dashboard">
          <!-- 새로고침 버튼 -->
          <div class="dashboard-header">
            <h2 class="dashboard-title">접수 현황</h2>
            <button
              type="button"
              class="btn btn-small btn-secondary"
              @click="loadDashboard"
              :disabled="isRefreshDisabled"
            >
              <span v-if="!isLoadingDashboard">새로고침</span>
              <span v-else>로딩 중...</span>
            </button>
          </div>

          <!-- RECEIVED 섹션 -->
          <div class="consultation-section">
            <h3 class="section-title">
              <span class="section-title__label">대기 중</span>
              <span class="section-title__count">{{ receivedConsultations.length }}</span>
            </h3>

            <div v-if="receivedConsultations.length === 0" class="empty-state">
              <p>대기 중인 접수가 없습니다</p>
            </div>

            <div v-else class="consultation-items">
              <div v-for="consultation in receivedConsultations" :key="consultation.id" class="consultation-item">
                <div class="consultation-item__header">
                  <div class="consultation-item__name">{{ consultation.studentName }} <span class="consultation-item__phone">({{ consultation.studentPhone }})</span> <span class="consultation-item__type">·</span> <span class="consultation-item__type">{{ consultation.typeName }}</span></div>
                  <span class="badge badge-received">대기</span>
                </div>
                <div class="consultation-item__meta">
                  <span class="consultation-item__time">{{ formatTime(consultation.createdAt) }}</span>
                </div>
                <div class="consultation-item__actions">
                  <button
                    type="button"
                    class="btn btn-primary btn-small"
                    @click="handleAcceptConsultation(consultation.id)"
                    :disabled="processingItemIds.has(consultation.id)"
                    :class="{ loading: processingItemIds.has(consultation.id) }"
                  >
                    <span v-if="!processingItemIds.has(consultation.id)">수락</span>
                    <span v-else>처리 중...</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- ACCEPTED 섹션 -->
          <div class="consultation-section">
            <h3 class="section-title">
              <span class="section-title__label">수락 완료</span>
              <span class="section-title__count">{{ acceptedConsultations.length }}</span>
            </h3>

            <div v-if="acceptedConsultations.length === 0" class="empty-state">
              <p>수락 완료된 상담이 없습니다</p>
            </div>

            <div v-else class="consultation-items">
              <div v-for="consultation in acceptedConsultations" :key="consultation.id" class="consultation-item">
                <div class="consultation-item__header">
                  <div class="consultation-item__name">{{ consultation.studentName }} <span class="consultation-item__phone">({{ consultation.studentPhone }})</span> <span class="consultation-item__type">·</span> <span class="consultation-item__type">{{ consultation.typeName }}</span></div>
                  <span class="badge badge-accepted">수락완료</span>
                </div>
                <div class="consultation-item__meta">
                  <span class="consultation-item__time">{{ formatTime(consultation.createdAt) }}</span>
                </div>
                <div class="consultation-item__actions">
                  <button
                    type="button"
                    class="btn btn-primary btn-small"
                    @click="handleStartProgressConsultation(consultation.id)"
                    :disabled="processingItemIds.has(consultation.id)"
                    :class="{ loading: processingItemIds.has(consultation.id) }"
                  >
                    <span v-if="!processingItemIds.has(consultation.id)">상담진행</span>
                    <span v-else>처리 중...</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- IN_PROGRESS 섹션 -->
          <div class="consultation-section">
            <h3 class="section-title">
              <span class="section-title__label">진행 중</span>
              <span class="section-title__count">{{ inProgressConsultations.length }}</span>
            </h3>

            <div v-if="inProgressConsultations.length === 0" class="empty-state">
              <p>진행 중인 상담이 없습니다</p>
            </div>

            <div v-else class="consultation-items">
              <div v-for="consultation in inProgressConsultations" :key="consultation.id" class="consultation-item">
                <div class="consultation-item__header">
                  <div class="consultation-item__name">{{ consultation.studentName }} <span class="consultation-item__phone">({{ consultation.studentPhone }})</span> <span class="consultation-item__type">·</span> <span class="consultation-item__type">{{ consultation.typeName }}</span></div>
                  <span class="badge badge-in-progress">진행중</span>
                </div>
                <div class="consultation-item__meta">
                  <span class="consultation-item__time">{{ formatTime(consultation.createdAt) }}</span>
                </div>
                <div class="consultation-item__actions">
                  <button
                    type="button"
                    class="btn btn-success btn-small"
                    @click="handleCompleteConsultation(consultation.id)"
                    :disabled="processingItemIds.has(consultation.id)"
                    :class="{ loading: processingItemIds.has(consultation.id) }"
                  >
                    <span v-if="!processingItemIds.has(consultation.id)">상담완료</span>
                    <span v-else>처리 중...</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 로그아웃 버튼 -->
        <button type="button" class="btn btn-secondary btn-large" @click="handleLogout">
          다시 입장하기
        </button>
      </template>
    </div>
  </article>
</template>

<style scoped>
.counselor-view {
  width: 100%;
}

.counselor-view__container {
  padding: 1.5rem 1rem;
}

.counselor-view__header {
  margin-bottom: 2rem;
}

.counselor-view__title {
  margin: 0.75rem 0 0.5rem;
  font-size: 1.875rem;
  line-height: 1.2;
  color: #0f172a;
}

.counselor-view__subtitle {
  margin: 0.5rem 0 0;
  color: #475569;
  font-size: 0.9375rem;
}

.counselor-view__error {
  padding: 1rem;
  margin-bottom: 1.5rem;
  border-radius: 0.75rem;
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.3);
  color: #991b1b;
  font-size: 0.9375rem;
  font-weight: 500;
}

.counselor-view__loading {
  padding: 2rem 1rem;
  text-align: center;
  color: #475569;
}

.counselor-view__form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  display: block;
  margin-bottom: 0.75rem;
  font-weight: 600;
  font-size: 0.9375rem;
  color: #0f172a;
}

.form-label__optional {
  font-size: 0.8125rem;
  font-weight: 400;
  color: #64748b;
  margin-left: 0.25rem;
}

.form-help {
  margin-top: 0.375rem;
  font-size: 0.8125rem;
  color: #64748b;
}

.form-input {
  padding: 0.875rem 1rem;
  border: 1px solid var(--border);
  border-radius: 0.75rem;
  background: var(--surface-strong);
  font-size: 1rem;
  transition: all 0.2s ease;
  min-height: 2.75rem;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.form-input:disabled {
  background: #f1f5f9;
  cursor: not-allowed;
  color: #94a3b8;
}

.form-input.error {
  border-color: #dc2626;
  background: rgba(220, 38, 38, 0.05);
}

.form-error {
  margin-top: 0.375rem;
  font-size: 0.8125rem;
  color: #991b1b;
  font-weight: 500;
}

/* 상담 유형 선택 */
.consultation-types {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.consultation-type-item {
  position: relative;
  padding: 0.45rem 0.875rem;
  border: 2px solid var(--border);
  border-radius: 0.625rem;
  background: var(--surface-strong);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  gap: 0.75rem;
}

.consultation-type-item:hover {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.consultation-type-item.active {
  border-color: var(--primary);
  background: var(--primary-soft);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.consultation-type-input {
  margin: 0.15rem 0 0 0;
  cursor: pointer;
  min-width: 1.125rem;
  width: 1.125rem;
  height: 1.125rem;
  accent-color: var(--primary);
}

.consultation-type-content {
  flex: 1;
}

.consultation-type-name {
  font-weight: 600;
  color: #0f172a;
  font-size: 0.9375rem;
}

.consultation-type-description {
  margin-top: 0.15rem;
  font-size: 0.8125rem;
  color: #475569;
  line-height: 1.3;
}

.selected-type-info {
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 0.8125rem;
}

/* 입장 정보 */
.session-info {
  padding: 1rem;
  margin-bottom: 1.5rem;
  border-radius: 0.75rem;
  background: var(--primary-soft);
  border: 2px solid var(--primary);
}

.session-info__item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
}

.session-info__item:not(:last-child) {
  border-bottom: 1px solid rgba(29, 78, 216, 0.2);
}

.session-info__label {
  font-size: 0.875rem;
  color: #0f172a;
  font-weight: 600;
}

.session-info__value {
  font-size: 1rem;
  color: var(--primary);
  font-weight: 700;
}

/* 대시보드 */
.counselor-dashboard {
  margin-bottom: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.dashboard-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
}

/* 상담 섹션 */
.consultation-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.section-title {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
}

.section-title__label {
  display: block;
}

.section-title__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.75rem;
  height: 1.75rem;
  padding: 0 0.5rem;
  border-radius: 0.5rem;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 0.875rem;
  font-weight: 700;
}

.empty-state {
  padding: 1.5rem 1rem;
  border: 2px dashed #cbd5e1;
  border-radius: 0.75rem;
  background: #f8fafc;
  text-align: center;
  color: #94a3b8;
  font-size: 0.9375rem;
}

.empty-state p {
  margin: 0;
}

/* 상담 항목 목록 */
.consultation-items {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.consultation-item {
  padding: 0.6rem 0.875rem;
  border: 1px solid var(--border);
  border-radius: 0.625rem;
  background: var(--surface-strong);
  transition: all 0.2s ease;
}

.consultation-item:hover {
  border-color: var(--primary);
  box-shadow: 0 2px 8px rgba(29, 78, 216, 0.1);
}

.consultation-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.4rem;
  gap: 0.5rem;
}

.consultation-item__name {
  font-weight: 600;
  color: #0f172a;
  font-size: 0.9375rem;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.consultation-item__phone {
  font-weight: 400;
  color: #64748b;
  font-size: 0.8125rem;
  margin-left: 0.375rem;
}

.consultation-item__type {
  font-weight: 400;
  color: #64748b;
  font-size: 0.8125rem;
  margin: 0 0.25rem;
}

.consultation-item__meta {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 0.4rem;
  font-size: 0.8125rem;
  color: #475569;
}

.consultation-item__time {
  display: block;
}

.consultation-item__actions {
  display: flex;
  gap: 0.375rem;
}

/* 버튼 */
.btn {
  padding: 0.45rem 1rem;
  border: none;
  border-radius: 0.625rem;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 2.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-small {
  padding: 0.3rem 0.625rem;
  font-size: 0.875rem;
  min-height: auto;
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

.btn-success {
  background: #22c55e;
  color: white;
}

.btn-success:not(:disabled):hover {
  background: #16a34a;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(34, 197, 94, 0.3);
}

.btn-secondary {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-secondary:not(:disabled):hover {
  background: #cbd5e1;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.1);
}

.btn-large {
  width: 100%;
}

.btn.loading {
  opacity: 0.8;
}

/* 배지 */
.badge {
  display: inline-block;
  padding: 0.375rem 0.75rem;
  border-radius: 0.5rem;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.badge-success {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

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

/* 반응형 */
@media (min-width: 768px) {
  .counselor-view__container {
    max-width: 48rem;
    margin: 0 auto;
    padding: 2rem;
  }

  .counselor-view__form {
    gap: 2rem;
  }

  .consultation-types {
    gap: 0.75rem;
  }

  .consultation-type-item {
    padding: 0.6rem 1rem;
  }

  .session-info {
    margin-bottom: 2rem;
  }

  .dashboard-header {
    margin-bottom: 0.5rem;
  }

  .consultation-section {
    margin-bottom: 1rem;
  }

  .consultation-item {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 1rem;
    align-items: center;
  }

  .consultation-item__header {
    grid-column: 1;
    margin-bottom: 0;
  }

  .consultation-item__meta {
    grid-column: 1;
    margin-bottom: 0;
  }

  .consultation-item__actions {
    grid-column: 2;
    grid-row: 1 / 3;
  }
}
</style>
