<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { enterStudent } from '../api/student'
import type { SchoolType, ErrorResponse } from '../types/consultation'

const router = useRouter()
// 상태 관리
const isSubmitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref<Record<string, string>>({})

// 폼 데이터
const formData = ref({
  studentName: '',
  studentPhone: '',
  schoolType: 'MIDDLE_HIGH_SCHOOL' as SchoolType,
  grade: 0,
  gender: 0,
})

// 핸드폰 번호 유효성 검증 (010-1234-5678 또는 01012345678 형식)
const validatePhoneNumber = (phone: string): boolean => {
  const phoneRegex = /^01[0-9](-?\d{3,4}){2}$/
  return phoneRegex.test(phone.replace(/\s/g, ''))
}

// 클라이언트 사이드 유효성 검증
const validateForm = (): boolean => {
  fieldErrors.value = {}
  let isValid = true

  if (!formData.value.studentName.trim()) {
    fieldErrors.value.studentName = '이름을 입력해주세요'
    isValid = false
  }

  if (!formData.value.studentPhone.trim()) {
    fieldErrors.value.studentPhone = '휴대폰 번호를 입력해주세요'
    isValid = false
  } else if (!validatePhoneNumber(formData.value.studentPhone)) {
    fieldErrors.value.studentPhone = '휴대폰 번호 형식이 올바르지 않습니다 (예: 010-1234-5678)'
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
  formData.value.studentPhone = formatted
}

// 폼 제출 - localStorage에 저장 후 홈 화면으로 이동
const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    const studentSession = await enterStudent({
      studentName: formData.value.studentName,
      studentPhone: formData.value.studentPhone,
      schoolType: formData.value.schoolType,
      grade: formData.value.grade,
      gender: formData.value.gender,
    })

    // localStorage에 학생 정보 저장
    localStorage.setItem('careerlink_student_name', studentSession.studentName)
    localStorage.setItem('careerlink_student_phone', studentSession.studentPhone)
    localStorage.setItem('careerlink_student_session_id', studentSession.id.toString())

    // 홈 화면으로 이동
    router.push('/home')
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
    } else {
      errorMessage.value = '입장 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요'
    }
  } finally {
    isSubmitting.value = false
  }
}

// 컴포넌트 마운트 - 이미 입장 정보가 있으면 홈 화면으로 리다이렉트
onMounted(() => {
  const savedSessionId = localStorage.getItem('careerlink_student_session_id')
  const savedName = localStorage.getItem('careerlink_student_name')
  const savedPhone = localStorage.getItem('careerlink_student_phone')

  // session_id가 있으면 단성 입장 직루
  if (savedSessionId && savedName && savedPhone) {
    router.push('/home')
  }
})
</script>

<template>
  <article class="receive-view">
    <div class="receive-view__container">
      <!-- 헤더 -->
      <div class="receive-view__header">
        <span class="badge">학생 입장</span>
        <h1 class="receive-view__title">홈 화면 입장</h1>
        <p class="receive-view__subtitle">기본 정보를 입력하고 입장해주세요</p>
      </div>

      <!-- 에러 메시지 -->
      <div v-if="errorMessage" class="receive-view__error">
        {{ errorMessage }}
      </div>

      <!-- 폼 -->
      <form @submit.prevent="handleSubmit" class="receive-view__form">
        <!-- 이름 입력 -->
        <div class="form-group">
          <label for="studentName" class="form-label">이름</label>
          <input
            id="studentName"
            v-model="formData.studentName"
            type="text"
            class="form-input"
            :class="{ error: fieldErrors.studentName }"
            placeholder="이름을 입력해주세요"
            autocomplete="name"
            :disabled="isSubmitting"
          />
          <div v-if="fieldErrors.studentName" class="form-error">
            {{ fieldErrors.studentName }}
          </div>
        </div>

        <!-- 휴대폰 번호 입력 -->
        <div class="form-group">
          <label for="studentPhone" class="form-label">휴대폰 번호</label>
          <input
            id="studentPhone"
            :value="formData.studentPhone"
            type="tel"
            class="form-input"
            :class="{ error: fieldErrors.studentPhone }"
            placeholder="010-1234-5678"
            autocomplete="tel"
            :disabled="isSubmitting"
            @input="handlePhoneInput"
          />
          <div v-if="fieldErrors.studentPhone" class="form-error">
            {{ fieldErrors.studentPhone }}
          </div>
        </div>

        <!-- 학교 구분 선택 -->
        <div class="form-group">
          <label for="schoolType" class="form-label">학교 구분</label>
          <select
            id="schoolType"
            v-model="formData.schoolType"
            class="form-input form-select"
            :disabled="isSubmitting"
          >
            <option value="MIDDLE_HIGH_SCHOOL">중/고등학교</option>
            <option value="MIDDLE_SCHOOL">중학교</option>
            <option value="HIGH_SCHOOL">고등학교</option>
          </select>
        </div>

        <!-- 학년 선택 -->
        <div class="form-group">
          <label for="grade" class="form-label">학년</label>
          <select
            id="grade"
            v-model.number="formData.grade"
            class="form-input form-select"
            :disabled="isSubmitting"
          >
            <option :value="0">선택 안함</option>
            <option :value="1">1학년</option>
            <option :value="2">2학년</option>
            <option :value="3">3학년</option>
          </select>
        </div>

        <!-- 성별 선택 -->
        <div class="form-group">
          <label for="gender" class="form-label">성별</label>
          <select
            id="gender"
            v-model.number="formData.gender"
            class="form-input form-select"
            :disabled="isSubmitting"
          >
            <option :value="0">선택 안함</option>
            <option :value="1">남성</option>
            <option :value="2">여성</option>
          </select>
        </div>
        <!-- 제출 버튼 -->
        <button
          type="submit"
          class="btn btn-primary btn-large"
          :disabled="isSubmitting"
          :class="{ loading: isSubmitting }"
        >
          <span v-if="!isSubmitting">입장하기</span>
          <span v-else>입장 중...</span>
        </button>
      </form>
    </div>
  </article>
</template>

<style scoped>
.receive-view {
  width: 100%;
}

.receive-view__container {
  padding: 1.5rem 1rem;
}

.receive-view__header {
  margin-bottom: 2rem;
}

.receive-view__title {
  margin: 0.75rem 0 0.5rem;
  font-size: 1.875rem;
  line-height: 1.2;
  color: #0f172a;
}

.receive-view__subtitle {
  margin: 0.5rem 0 0;
  color: #475569;
  font-size: 0.9375rem;
}

.receive-view__error {
  padding: 1rem;
  margin-bottom: 1.5rem;
  border-radius: 0.75rem;
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.3);
  color: #991b1b;
  font-size: 0.9375rem;
  font-weight: 500;
}

.receive-view__loading {
  padding: 2rem 1rem;
  text-align: center;
  color: #475569;
}

.receive-view__form {
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

/* 학교 및 학년 선택 */
.school-grade-group {
  border: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.school-options,
.grade-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.grade-options {
  grid-template-columns: repeat(3, 1fr);
}

.radio-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border: 2px solid var(--border);
  border-radius: 0.75rem;
  background: var(--surface-strong);
  cursor: pointer;
  transition: all 0.2s ease;
}

.radio-item:hover {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.radio-input {
  cursor: pointer;
  accent-color: var(--primary);
  width: 1.25rem;
  height: 1.25rem;
  margin: 0;
}

.radio-input:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.radio-label {
  font-weight: 500;
  font-size: 0.9375rem;
  color: #0f172a;
  user-select: none;
}

.radio-item input:checked + .radio-label {
  color: var(--primary);
  font-weight: 600;
}

.radio-item:has(input:checked) {
  border-color: var(--primary);
  background: var(--primary-soft);
  box-shadow: 0 0 0 3px var(--primary-soft);
}
/* 상담 유형 선택 */
.consultation-types {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.consultation-type-item {
  position: relative;
  padding: 1rem;
  border: 2px solid var(--border);
  border-radius: 0.875rem;
  background: var(--surface-strong);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  gap: 1rem;
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
  margin: 0.25rem 0 0 0;
  cursor: pointer;
  min-width: 1.25rem;
  width: 1.25rem;
  height: 1.25rem;
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
  margin-top: 0.25rem;
  font-size: 0.8125rem;
  color: #475569;
  line-height: 1.4;
}

.selected-type-info {
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 0.8125rem;
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

.btn-large {
  width: 100%;
}

.btn.loading {
  opacity: 0.8;
}

/* 반응형 */
@media (min-width: 768px) {
  .receive-view__container {
    max-width: 36rem;
    margin: 0 auto;
    padding: 2rem;
  }

  .receive-view__form {
    gap: 2rem;
  }

  .consultation-types {
    gap: 1rem;
  }

  .consultation-type-item {
    padding: 1.25rem;
  }
}
</style>
