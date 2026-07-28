<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getConsultationTypes, createConsultation } from '../api/consultation'
import type { ConsultationType, ErrorResponse } from '../types/consultation'

const router = useRouter()

// 상태 관리
const types = ref<ConsultationType[]>([])
const isLoadingTypes = ref(true)
const isSubmitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref<Record<string, string>>({})
const studentName = ref('')
const studentPhone = ref('')
const lastConsultationInfo = ref<{ id: string; typeName: string } | null>(null)

// 폼 데이터
const formData = ref({
  typeId: 0,
})

// 클라이언트 사이드 유효성 검증
const validateForm = (): boolean => {
  fieldErrors.value = {}
  let isValid = true

  if (!formData.value.typeId) {
    fieldErrors.value.typeId = '상담 유형을 선택해주세요'
    isValid = false
  }

  return isValid
}

// 폼 제출 - 상담 신청
const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    const consultation = await createConsultation({
      studentName: studentName.value,
      studentPhone: studentPhone.value,
      typeId: formData.value.typeId,
    })

    // localStorage에 상담 id 저장 (마지막 신청 건)
    localStorage.setItem('careerlink_consultation_id', consultation.id)

    // 마지막 신청 정보 표시
    lastConsultationInfo.value = {
      id: consultation.id,
      typeName: consultation.typeName,
    }

    // 폼 초기화 (연속 신청 가능하도록)
    formData.value.typeId = 0

    // 3초 후 자동으로 메시지 숨김
    setTimeout(() => {
      lastConsultationInfo.value = null
    }, 5000)
  } catch (error: unknown) {
    const err = error as any
    const status = err?.response?.status
    const data = err?.response?.data as ErrorResponse | undefined

    const responseFieldErrors = data?.fieldErrors ?? data?.errors

    if (status === 400) {
      if (responseFieldErrors) {
        fieldErrors.value = responseFieldErrors
        errorMessage.value = '입력 정보를 다시 확인해주세요'
      } else {
        errorMessage.value = data?.message || '입력 정보가 올바르지 않습니다'
      }
    } else if (status === 404) {
      errorMessage.value = '선택하신 상담 유형을 찾을 수 없습니다'
    } else {
      errorMessage.value = '접수 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요'
    }
  } finally {
    isSubmitting.value = false
  }
}

// 상담 유형 목록 로드
onMounted(async () => {
  // localStorage에서 입장 정보 조회
  const savedName = localStorage.getItem('careerlink_student_name')
  const savedPhone = localStorage.getItem('careerlink_student_phone')

  // 입장 정보가 없으면 입장 화면으로 리다이렉트
  if (!savedName || !savedPhone) {
    router.push('/receive')
    return
  }

  studentName.value = savedName
  studentPhone.value = savedPhone

  try {
    types.value = await getConsultationTypes()
  } catch (error) {
    console.error('Failed to load consultation types:', error)
    errorMessage.value = '상담 유형을 불러올 수 없습니다'
  } finally {
    isLoadingTypes.value = false
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

// 다시 입장하기 - 로그아웃
const handleLogout = () => {
  localStorage.removeItem('careerlink_student_name')
  localStorage.removeItem('careerlink_student_phone')
  localStorage.removeItem('careerlink_consultation_id')
  router.push('/receive')
}

// 내 상담 목록 보기
const handleViewConsultationList = () => {
  router.push('/status')
}

// 상세보기 클릭
const handleViewDetails = (id: string) => {
  router.push(`/status/${id}`)
}
</script>

<template>
  <article class="home-view">
    <div class="home-view__container">
      <!-- 헤더 -->
      <div class="home-view__header">
        <span class="badge">학생 상담</span>
        <h1 class="home-view__title">{{ studentName }}님, 환영합니다</h1>
        <p class="home-view__subtitle">상담 유형을 선택하고 상담을 신청해주세요</p>
      </div>

      <!-- 에러 메시지 -->
      <div v-if="errorMessage" class="home-view__error">
        {{ errorMessage }}
      </div>

      <!-- 신청 완료 메시지 -->
      <div v-if="lastConsultationInfo" class="completion-alert">
        <div class="completion-alert__content">
          <span class="completion-alert__icon">✓</span>
          <div class="completion-alert__text">
            <div class="completion-alert__title">
              신청 완료: {{ lastConsultationInfo.typeName }}
            </div>
            <div class="completion-alert__actions">
              <button
                class="completion-alert__link"
                @click="handleViewDetails(lastConsultationInfo.id)"
              >
                상세보기
              </button>
              <span class="completion-alert__divider">·</span>
              <button class="completion-alert__link" @click="handleViewConsultationList">
                전체 목록 보기
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 상담 유형 로딩 -->
      <div v-if="isLoadingTypes" class="home-view__loading">
        <p>상담 유형을 불러오는 중입니다...</p>
      </div>

      <!-- 폼 -->
      <form v-if="!isLoadingTypes" @submit.prevent="handleSubmit" class="home-view__form">
        <!-- 상담 유형 선택 -->
        <fieldset class="form-group">
          <legend class="form-label">상담 유형</legend>
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
          <strong>선택된 상담:</strong> {{ selectedType.name }}
        </div>

        <!-- 제출 버튼 -->
        <button
          type="submit"
          class="btn btn-primary btn-large"
          :disabled="isSubmitDisabled"
          :class="{ loading: isSubmitting }"
        >
          <span v-if="!isSubmitting">상담신청</span>
          <span v-else>신청 중...</span>
        </button>
      </form>

      <!-- 하단 액션 버튼 -->
      <div class="home-view__actions">
        <button class="btn btn-secondary btn-large" @click="handleViewConsultationList">
          내 상담 목록 보기
        </button>
        <button class="btn btn-outline btn-large" @click="handleLogout">
          다시 입장하기
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.home-view {
  width: 100%;
}

.home-view__container {
  padding: 1.5rem 1rem;
}

.home-view__header {
  margin-bottom: 2rem;
}

.home-view__title {
  margin: 0.75rem 0 0.5rem;
  font-size: 1.875rem;
  line-height: 1.2;
  color: #0f172a;
}

.home-view__subtitle {
  margin: 0.5rem 0 0;
  color: #475569;
  font-size: 0.9375rem;
}

.home-view__error {
  padding: 1rem;
  margin-bottom: 1.5rem;
  border-radius: 0.75rem;
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.3);
  color: #991b1b;
  font-size: 0.9375rem;
  font-weight: 500;
}

.home-view__loading {
  padding: 2rem 1rem;
  text-align: center;
  color: #475569;
}

.home-view__form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

/* 완료 알림 */
.completion-alert {
  padding: 1rem;
  margin-bottom: 1.5rem;
  border-radius: 0.75rem;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
  color: #166534;
  font-size: 0.9375rem;
  font-weight: 500;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-1rem);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.completion-alert__content {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
}

.completion-alert__icon {
  display: block;
  font-size: 1.25rem;
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.completion-alert__text {
  flex: 1;
}

.completion-alert__title {
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.completion-alert__actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  font-size: 0.8125rem;
  margin-top: 0.375rem;
}

.completion-alert__link {
  background: none;
  border: none;
  color: #166534;
  cursor: pointer;
  text-decoration: underline;
  font-weight: 600;
  padding: 0;
  font-size: inherit;
}

.completion-alert__link:hover {
  text-decoration: none;
}

.completion-alert__divider {
  color: #d1d5db;
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
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.25);
}

.btn-secondary {
  background: var(--border);
  color: #0f172a;
}

.btn-secondary:not(:disabled):hover {
  background: rgba(148, 163, 184, 0.3);
  transform: translateY(-1px);
}

.btn-outline {
  background: transparent;
  border: 1px solid var(--border);
  color: #0f172a;
}

.btn-outline:not(:disabled):hover {
  background: #f8fafc;
  border-color: var(--primary);
  color: var(--primary);
  transform: translateY(-1px);
}

.btn-large {
  width: 100%;
}

.btn.loading {
  opacity: 0.8;
}

.home-view__actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* 반응형 */
@media (min-width: 768px) {
  .home-view__container {
    max-width: 36rem;
    margin: 0 auto;
    padding: 2rem;
  }

  .home-view__form {
    gap: 1.5rem;
  }

  .consultation-types {
    gap: 0.75rem;
  }

  .consultation-type-item {
    padding: 0.6rem 1rem;
  }

  .home-view__actions {
    gap: 0.75rem;
    margin-top: 0.75rem;
  }
}
</style>
