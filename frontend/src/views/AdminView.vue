<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  getAdminTypes,
  getAdminTopics,
  getAllConsultationsForAdmin,
  createAdminType,
  updateAdminType,
  deleteAdminType,
  getAdminStudents,
  updateAdminStudent,
} from '../api/admin'
import type {
  ConsultationType,
  ConsultationTopic,
  Consultation,
  StudentSession,
  SchoolType,
  ErrorResponse,
} from '../types/consultation'

const activeTab = ref<'consultations' | 'types' | 'students'>('consultations')
const types = ref<ConsultationType[]>([])
const topics = ref<ConsultationTopic[]>([])
const consultations = ref<Consultation[]>([])
const students = ref<StudentSession[]>([])
const isLoadingTypes = ref(true)
const isLoadingConsultations = ref(true)
const isLoadingStudents = ref(true)
const errorMessage = ref('')
const successMessage = ref('')

// 상담 상세 및 결과 모달 상태 (관리자용)
const isDetailModalOpen = ref(false)
const selectedConsultationDetail = ref<Consultation | null>(null)

const openDetailModal = (consultation: Consultation) => {
  selectedConsultationDetail.value = consultation
  isDetailModalOpen.value = true
}

const closeDetailModal = () => {
  isDetailModalOpen.value = false
  selectedConsultationDetail.value = null
}
const editingId = ref<number | null>(null)
const processingIds = ref<Set<number>>(new Set())
const isCreating = ref(false)
let pollingInterval: number | null = null

// 학생 정보 관리 상태
const studentEditingId = ref<number | null>(null)
const studentProcessingIds = ref<Set<number>>(new Set())

const studentSearchFilter = ref({
  studentName: '',
  studentPhone: '',
  schoolType: '' as SchoolType | '',
  grade: '' as string | number,
  gender: '' as string | number,
})

const studentCurrentPage = ref(1)
const studentPageSize = ref<number>(10)
const studentPageSizeOptions = [10, 15, 20, 25, 30]

const studentEditFormData = ref({
  studentName: '',
  studentPhone: '',
  schoolType: 'MIDDLE_HIGH_SCHOOL' as SchoolType,
  grade: 0,
  gender: 0,
})

const studentEditFieldErrors = ref<Record<string, string>>({})

// 검색 필터 및 페이징 상태
const searchFilter = ref({
  studentName: '',
  topicId: '' as string | number,
  typeId: '' as string | number,
  counselorName: '',
})

const currentPage = ref(1)
const pageSize = ref<number>(10)
const pageSizeOptions = [10, 15, 20, 25, 30]

// 등록되어 있는 고유 상담사 목록 추출 (드롭다운 용)
const availableCounselors = computed(() => {
  const set = new Set<string>()
  consultations.value.forEach((item) => {
    if (item.counselorName && item.counselorName.trim()) {
      set.add(item.counselorName.trim())
    }
  })
  return Array.from(set).sort()
})

// 선택된 상담 테마에 따라 동적으로 필터링된 상담 유형 목록 (조회 조건용)
const filteredSearchTypes = computed(() => {
  if (searchFilter.value.topicId === '') {
    return types.value
  }
  const selectedTopicIdNum = Number(searchFilter.value.topicId)
  return types.value.filter((t) => t.topicId === selectedTopicIdNum)
})

const handleFilterTopicChange = () => {
  resetPage()
  if (searchFilter.value.typeId !== '') {
    const selectedType = types.value.find((t) => t.id === Number(searchFilter.value.typeId))
    if (
      selectedType &&
      searchFilter.value.topicId !== '' &&
      selectedType.topicId !== Number(searchFilter.value.topicId)
    ) {
      searchFilter.value.typeId = ''
    }
  }
}

// 검색 필터링 적용된 상담 목록
const filteredConsultations = computed(() => {
  return consultations.value.filter((item) => {
    // 1. 학생이름 검색
    if (
      searchFilter.value.studentName.trim() &&
      !item.studentName.toLowerCase().includes(searchFilter.value.studentName.trim().toLowerCase())
    ) {
      return false
    }

    // 2. 상담테마 필터
    if (
      searchFilter.value.topicId !== '' &&
      Number(searchFilter.value.topicId) !== item.topicId
    ) {
      return false
    }

    // 3. 상담유형 필터
    if (
      searchFilter.value.typeId !== '' &&
      Number(searchFilter.value.typeId) !== item.typeId
    ) {
      return false
    }

    // 4. 상담사 필터
    if (searchFilter.value.counselorName.trim()) {
      const counselor = item.counselorName || ''
      if (!counselor.toLowerCase().includes(searchFilter.value.counselorName.trim().toLowerCase())) {
        return false
      }
    }

    return true
  })
})

// 페이징 처리된 목록
const paginatedConsultations = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredConsultations.value.slice(start, start + pageSize.value)
})

// 총 페이지 수
const totalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredConsultations.value.length / pageSize.value))
})

// 네비게이션 표시 페이지 번호 배열 (최대 5개)
const visiblePages = computed(() => {
  const pages: number[] = []
  const maxVisible = 5
  let start = Math.max(1, currentPage.value - Math.floor(maxVisible / 2))
  let end = start + maxVisible - 1

  if (end > totalPages.value) {
    end = totalPages.value
    start = Math.max(1, end - maxVisible + 1)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

const resetPage = () => {
  currentPage.value = 1
}

const clearSearchFilter = () => {
  searchFilter.value = {
    studentName: '',
    topicId: '',
    typeId: '',
    counselorName: '',
  }
  currentPage.value = 1
}

const createFormData = ref({
  topicId: '' as number | '',
  name: '',
  description: '',
})

const editFormData = ref({
  topicId: 1 as number,
  name: '',
  description: '',
})

const createFieldErrors = ref<Record<string, string>>({})
const editFieldErrors = ref<Record<string, string>>({})

const formatDateTime = (dateTimeStr: string) => {
  if (!dateTimeStr) return '-'
  const date = new Date(dateTimeStr)
  if (isNaN(date.getTime())) return dateTimeStr
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${min}`
}

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'RECEIVED':
      return '접수'
    case 'ACCEPTED':
      return '수락'
    case 'IN_PROGRESS':
      return '진행중'
    case 'COMPLETED':
      return '완료'
    case 'CANCELLED':
      return '취소'
    default:
      return status
  }
}

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

const getStatusBadgeClass = (status: string) => {
  switch (status) {
    case 'RECEIVED':
      return 'status-badge--received'
    case 'ACCEPTED':
      return 'status-badge--accepted'
    case 'IN_PROGRESS':
      return 'status-badge--progress'
    case 'COMPLETED':
      return 'status-badge--completed'
    case 'CANCELLED':
      return 'status-badge--cancelled'
    default:
      return ''
  }
}

const refreshConsultations = async () => {
  isLoadingConsultations.value = true
  try {
    const consultationsData = await getAllConsultationsForAdmin()
    consultations.value = consultationsData.sort(
      (a: Consultation, b: Consultation) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    )
  } catch (error) {
    console.error('Failed to refresh consultations:', error)
  } finally {
    isLoadingConsultations.value = false
  }
}

const validateTypeForm = (payload: { topicId: number | ''; name: string; description: string }) => {
  const errors: Record<string, string> = {}

  if (!payload.topicId) {
    errors.topicId = '상담 테마를 선택해주세요'
  }

  if (!payload.name.trim()) {
    errors.name = '상담 유형명을 입력해주세요'
  }

  if (!payload.description.trim()) {
    errors.description = '상담 유형 설명을 입력해주세요'
  }

  return errors
}

const resetCreateForm = () => {
  createFormData.value = {
    topicId: topics.value.length > 0 ? topics.value[0].id : '',
    name: '',
    description: '',
  }
  createFieldErrors.value = {}
}

const clearMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const hideSuccessMessageLater = () => {
  setTimeout(() => {
    successMessage.value = ''
  }, 2000)
}

const applyResponseErrors = (
  status: number | undefined,
  data: ErrorResponse | undefined,
  fieldTarget: { value: Record<string, string> },
  fallbackMessages: {
    badRequest: string
    notFound?: string
    conflict?: string
    default: string
  },
) => {
  const responseFieldErrors = data?.fieldErrors ?? data?.errors

  if (status === 400) {
    if (responseFieldErrors) {
      fieldTarget.value = responseFieldErrors
      errorMessage.value = '입력 정보를 다시 확인해주세요'
    } else {
      errorMessage.value = data?.message || fallbackMessages.badRequest
    }
    return
  }

  if (status === 404 && fallbackMessages.notFound) {
    errorMessage.value = fallbackMessages.notFound
    return
  }

  if (status === 409 && fallbackMessages.conflict) {
    errorMessage.value = data?.message || fallbackMessages.conflict
    return
  }

  errorMessage.value = fallbackMessages.default
}

const startEdit = (type: ConsultationType) => {
  editingId.value = type.id
  editFormData.value = {
    topicId: type.topicId || (topics.value.length > 0 ? topics.value[0].id : 1),
    name: type.name,
    description: type.description,
  }
  editFieldErrors.value = {}
  clearMessages()
}

const cancelEdit = () => {
  editingId.value = null
  editFieldErrors.value = {}
  editFormData.value = {
    topicId: 1,
    name: '',
    description: '',
  }
}

const handleCreate = async () => {
  const errors = validateTypeForm(createFormData.value)
  createFieldErrors.value = errors
  if (Object.keys(errors).length > 0) {
    return
  }

  isCreating.value = true
  clearMessages()

  try {
    const created = await createAdminType({
      topicId: Number(createFormData.value.topicId),
      name: createFormData.value.name,
      description: createFormData.value.description,
    })

    types.value = [...types.value, created].sort((a, b) => a.id - b.id)
    resetCreateForm()
    successMessage.value = '상담 유형이 추가되었습니다'
    hideSuccessMessageLater()
  } catch (error: unknown) {
    const err = error as { response?: { status?: number; data?: ErrorResponse } }
    applyResponseErrors(err.response?.status, err.response?.data, createFieldErrors, {
      badRequest: '입력 정보가 올바르지 않습니다',
      default: '유형 추가 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요',
    })
  } finally {
    isCreating.value = false
  }
}

const handleSave = async (typeId: number) => {
  const errors = validateTypeForm(editFormData.value)
  editFieldErrors.value = errors
  if (Object.keys(errors).length > 0) {
    return
  }

  processingIds.value.add(typeId)
  clearMessages()

  try {
    const updated = await updateAdminType(typeId, {
      topicId: Number(editFormData.value.topicId),
      name: editFormData.value.name,
      description: editFormData.value.description,
    })

    const index = types.value.findIndex((type) => type.id === typeId)
    if (index !== -1) {
      types.value[index] = updated
    }

    editingId.value = null
    editFieldErrors.value = {}
    successMessage.value = '상담 유형이 수정되었습니다'
    hideSuccessMessageLater()
  } catch (error: unknown) {
    const err = error as { response?: { status?: number; data?: ErrorResponse } }
    applyResponseErrors(err.response?.status, err.response?.data, editFieldErrors, {
      badRequest: '입력 정보가 올바르지 않습니다',
      notFound: '선택하신 상담 유형을 찾을 수 없습니다',
      default: '수정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요',
    })
  } finally {
    processingIds.value.delete(typeId)
  }
}

const handleDelete = async (type: ConsultationType) => {
  const confirmed = window.confirm(`'${type.name}' 유형을 삭제하시겠습니까?`)
  if (!confirmed) {
    return
  }

  processingIds.value.add(type.id)
  clearMessages()

  try {
    await deleteAdminType(type.id)
    if (editingId.value === type.id) {
      cancelEdit()
    }
    types.value = types.value.filter((item) => item.id !== type.id)
    successMessage.value = '상담 유형이 삭제되었습니다'
    hideSuccessMessageLater()
  } catch (error: unknown) {
    const err = error as { response?: { status?: number; data?: ErrorResponse } }
    applyResponseErrors(err.response?.status, err.response?.data, editFieldErrors, {
      badRequest: '삭제 요청이 올바르지 않습니다',
      notFound: '선택하신 상담 유형을 찾을 수 없습니다',
      conflict: '기존 상담에서 사용 중인 유형은 삭제할 수 없습니다',
      default: '삭제 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요',
    })
  } finally {
    processingIds.value.delete(type.id)
  }
}

// ==================== 학생 정보 관리 로직 ====================
const formatSchoolTypeLabel = (st: SchoolType) => {
  if (st === 'MIDDLE_SCHOOL') return '중학교'
  if (st === 'HIGH_SCHOOL') return '고등학교'
  return '중/고등학교'
}

const formatGradeLabel = (g: number) => {
  if (!g || g === 0) return '미선택'
  return `${g}학년`
}

const formatGenderLabel = (g: number) => {
  if (g === 1) return '남성'
  if (g === 2) return '여성'
  return '미선택'
}

const filteredStudents = computed(() => {
  return students.value.filter((s) => {
    if (
      studentSearchFilter.value.studentName.trim() &&
      !s.studentName.toLowerCase().includes(studentSearchFilter.value.studentName.trim().toLowerCase())
    ) {
      return false
    }

    if (
      studentSearchFilter.value.studentPhone.trim() &&
      !s.studentPhone.includes(studentSearchFilter.value.studentPhone.trim())
    ) {
      return false
    }

    if (
      studentSearchFilter.value.schoolType &&
      s.schoolType !== studentSearchFilter.value.schoolType
    ) {
      return false
    }

    if (
      studentSearchFilter.value.grade !== '' &&
      Number(studentSearchFilter.value.grade) !== s.grade
    ) {
      return false
    }

    if (
      studentSearchFilter.value.gender !== '' &&
      Number(studentSearchFilter.value.gender) !== s.gender
    ) {
      return false
    }

    return true
  })
})

const paginatedStudents = computed(() => {
  const start = (studentCurrentPage.value - 1) * studentPageSize.value
  return filteredStudents.value.slice(start, start + studentPageSize.value)
})

const studentTotalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredStudents.value.length / studentPageSize.value))
})

const studentVisiblePages = computed(() => {
  const pages: number[] = []
  const maxVisible = 5
  let start = Math.max(1, studentCurrentPage.value - Math.floor(maxVisible / 2))
  let end = start + maxVisible - 1

  if (end > studentTotalPages.value) {
    end = studentTotalPages.value
    start = Math.max(1, end - maxVisible + 1)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

const resetStudentPage = () => {
  studentCurrentPage.value = 1
}

const clearStudentSearchFilter = () => {
  studentSearchFilter.value = {
    studentName: '',
    studentPhone: '',
    schoolType: '',
    grade: '',
    gender: '',
  }
  studentCurrentPage.value = 1
}

const startStudentEdit = (student: StudentSession) => {
  studentEditingId.value = student.id
  studentEditFormData.value = {
    studentName: student.studentName,
    studentPhone: student.studentPhone,
    schoolType: student.schoolType,
    grade: student.grade,
    gender: student.gender,
  }
  studentEditFieldErrors.value = {}
}

const cancelStudentEdit = () => {
  studentEditingId.value = null
  studentEditFieldErrors.value = {}
}

const validateStudentForm = (payload: { studentName: string; studentPhone: string }) => {
  const errors: Record<string, string> = {}

  if (!payload.studentName.trim()) {
    errors.studentName = '학생 이름을 입력해주세요'
  }

  const phoneTrimmed = payload.studentPhone.trim()
  if (!phoneTrimmed) {
    errors.studentPhone = '휴대폰 번호를 입력해주세요'
  } else if (!/^01[0-9]-?\d{3,4}-?\d{4}$/.test(phoneTrimmed)) {
    errors.studentPhone = '올바른 휴대폰 번호 형식(010-0000-0000)이 아닙니다'
  }

  return errors
}

const handleStudentSave = async (id: number) => {
  const errors = validateStudentForm(studentEditFormData.value)
  if (Object.keys(errors).length > 0) {
    studentEditFieldErrors.value = errors
    return
  }

  studentProcessingIds.value.add(id)
  clearMessages()

  try {
    const updated = await updateAdminStudent(id, {
      studentName: studentEditFormData.value.studentName.trim(),
      studentPhone: studentEditFormData.value.studentPhone.trim(),
      schoolType: studentEditFormData.value.schoolType,
      grade: Number(studentEditFormData.value.grade),
      gender: Number(studentEditFormData.value.gender),
    })

    const index = students.value.findIndex((s) => s.id === id)
    if (index !== -1) {
      students.value[index] = updated
    }

    studentEditingId.value = null
    successMessage.value = '학생 정보가 성공적으로 수정되었습니다'
    hideSuccessMessageLater()
  } catch (error: any) {
    console.error('Failed to update student session:', error)
    errorMessage.value = error?.response?.data?.message || '학생 정보 수정 중 오류가 발생했습니다'
  } finally {
    studentProcessingIds.value.delete(id)
  }
}

const refreshStudents = async () => {
  isLoadingStudents.value = true
  try {
    students.value = await getAdminStudents()
  } catch (error) {
    console.error('Failed to fetch students:', error)
  } finally {
    isLoadingStudents.value = false
  }
}

// 5초 주기로 폴링 시작
onMounted(async () => {
  try {
    const [typesResult, topicsResult, consultationsResult, studentsResult] = await Promise.allSettled([
      getAdminTypes(),
      getAdminTopics(),
      getAllConsultationsForAdmin(),
      getAdminStudents(),
    ])
    if (typesResult.status === 'fulfilled') {
      types.value = typesResult.value
    }
    if (topicsResult.status === 'fulfilled') {
      topics.value = topicsResult.value
      if (topicsResult.value.length > 0) {
        createFormData.value.topicId = topicsResult.value[0].id
      }
    }
    if (consultationsResult.status === 'fulfilled') {
      consultations.value = consultationsResult.value.sort(
        (a: Consultation, b: Consultation) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      )
    }
    if (studentsResult.status === 'fulfilled') {
      students.value = studentsResult.value
    }
  } catch (error) {
    console.error('Failed to load admin data:', error)
    errorMessage.value = '데이터를 불러올 수 없습니다'
  } finally {
    isLoadingTypes.value = false
    isLoadingConsultations.value = false
    isLoadingStudents.value = false
  }

  // 5초 주기로 폴링 시작
  pollingInterval = setInterval(async () => {
    try {
      const [consultationsData, studentsData] = await Promise.all([
        getAllConsultationsForAdmin(),
        getAdminStudents(),
      ])
      consultations.value = consultationsData.sort(
        (a: Consultation, b: Consultation) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      )
      if (studentEditingId.value === null) {
        students.value = studentsData
      }

      // currentPage가 totalPages를 초과하면 마지막 페이지로 보정
      const total = Math.max(1, Math.ceil(filteredConsultations.value.length / pageSize.value))
      if (currentPage.value > total) {
        currentPage.value = total
      }
      const studentTotal = Math.max(1, Math.ceil(filteredStudents.value.length / studentPageSize.value))
      if (studentCurrentPage.value > studentTotal) {
        studentCurrentPage.value = studentTotal
      }
    } catch (error) {
      console.error('Failed to refresh admin polling data:', error)
    }
  }, 5000)
})

// 컴포넌트 언마운트 시 폴링 중지
onUnmounted(() => {
  if (pollingInterval !== null) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
})
</script>

<template>
  <article class="admin-view">
    <div class="admin-view__container">
      <!-- 헤더 -->
      <div class="admin-view__header">
        <span class="badge">관리자</span>
        <h1 class="admin-view__title">관리자 마당</h1>
        <p class="admin-view__subtitle">전체 학생 상담 내역 조회 및 상담 유형 관리를 수행합니다</p>
      </div>

      <!-- 탭 버튼 목록 -->
      <nav class="admin-tabs" aria-label="관리자 메뉴 탭">
        <button
          type="button"
          class="admin-tab"
          :class="{ 'admin-tab--active': activeTab === 'consultations' }"
          @click="activeTab = 'consultations'"
        >
          <span>전체 상담 조회</span>
          <span v-if="!isLoadingConsultations" class="tab-count-badge">{{ consultations.length }}</span>
        </button>
        <button
          type="button"
          class="admin-tab"
          :class="{ 'admin-tab--active': activeTab === 'types' }"
          @click="activeTab = 'types'"
        >
          <span>상담 유형 관리</span>
          <span v-if="!isLoadingTypes" class="tab-count-badge">{{ types.length }}</span>
        </button>
        <button
          type="button"
          class="admin-tab"
          :class="{ 'admin-tab--active': activeTab === 'students' }"
          @click="activeTab = 'students'"
        >
          <span>학생 정보 관리</span>
          <span v-if="!isLoadingStudents" class="tab-count-badge">{{ students.length }}</span>
        </button>
      </nav>

      <!-- 성공 메시지 -->
      <div v-if="successMessage" class="admin-view__success">
        {{ successMessage }}
      </div>

      <!-- 에러 메시지 -->
      <div v-if="errorMessage" class="admin-view__error">
        {{ errorMessage }}
      </div>

      <!-- 탭 1: 전체 상담 조회 -->
      <section v-if="activeTab === 'consultations'" class="admin-tab-content">
        <div v-if="isLoadingConsultations" class="admin-view__loading">
          <p>전체 상담 목록을 불러오는 중입니다...</p>
        </div>

        <div v-else class="consultations-tab-body">
          <!-- 검색 조건 필터 패널 -->
          <div class="search-filter-panel">
            <div class="search-filter-panel__grid">
              <!-- 학생 이름 검색 -->
              <div class="filter-group">
                <label for="filterStudentName" class="filter-label">학생 이름</label>
                <input
                  id="filterStudentName"
                  v-model="searchFilter.studentName"
                  type="text"
                  class="form-input"
                  placeholder="학생 이름 입력"
                  @input="resetPage"
                />
              </div>

              <!-- 상담 테마 선택 -->
              <div class="filter-group">
                <label for="filterTopicId" class="filter-label">상담 테마</label>
                <select
                  id="filterTopicId"
                  v-model="searchFilter.topicId"
                  class="form-select"
                  @change="handleFilterTopicChange"
                >
                  <option value="">전체 (모든 테마)</option>
                  <option v-for="t in topics" :key="t.id" :value="t.id">
                    {{ t.name }}
                  </option>
                </select>
              </div>

              <!-- 상담 유형 선택 -->
              <div class="filter-group">
                <label for="filterTypeId" class="filter-label">상담 유형</label>
                <select
                  id="filterTypeId"
                  v-model="searchFilter.typeId"
                  class="form-select"
                  @change="resetPage"
                >
                  <option value="">전체 (모든 유형)</option>
                  <option v-for="t in filteredSearchTypes" :key="t.id" :value="t.id">
                    {{ t.name }}
                  </option>
                </select>
              </div>

              <!-- 상담사 선택 -->
              <div class="filter-group">
                <label for="filterCounselorName" class="filter-label">상담사</label>
                <select
                  id="filterCounselorName"
                  v-model="searchFilter.counselorName"
                  class="form-select"
                  @change="resetPage"
                >
                  <option value="">전체 (모든 상담사)</option>
                  <option v-for="counselor in availableCounselors" :key="counselor" :value="counselor">
                    {{ counselor }}
                  </option>
                </select>
              </div>

              <!-- 초기화 버튼 -->
              <div class="filter-group filter-group--actions">
                <button
                  type="button"
                  class="btn btn-secondary btn-reset"
                  @click="clearSearchFilter"
                >
                  필터 초기화
                </button>
              </div>
            </div>
          </div>


          <!-- 필터링 결과 통계 및 새로고침 버튼 -->
          <div class="filter-summary">
            <div class="filter-summary__left">
              <span>
                검색 결과 <strong>{{ filteredConsultations.length }}</strong>건
                <span v-if="filteredConsultations.length !== consultations.length" class="summary-total">
                  (전체 {{ consultations.length }}건 중)
                </span>
              </span>
            </div>
            <div class="filter-summary__right">
              <div class="page-size-selector">
                <label for="pageSizeSelectSummary" class="page-size-label">표시 개수:</label>
                <select
                  id="pageSizeSelectSummary"
                  v-model.number="pageSize"
                  class="form-select page-size-select"
                  @change="resetPage"
                >
                  <option v-for="size in pageSizeOptions" :key="size" :value="size">
                    {{ size }}개씩
                  </option>
                </select>
              </div>
              <button
                type="button"
                class="btn btn-secondary btn-refresh"
                @click="refreshConsultations"
                :disabled="isLoadingConsultations"
              >
                <span v-if="!isLoadingConsultations">새로고침</span>
                <span v-else>새로고침 중...</span>
              </button>
            </div>
          </div>

          <!-- 결과 없음 -->
          <div v-if="filteredConsultations.length === 0" class="empty-state">
            <p>검색 조건에 일치하는 상담 내역이 없습니다.</p>
          </div>

          <!-- 테이블 & 카드 -->
          <div v-else class="consultations-table-container">
            <!-- 데스크톱 테이블 -->
            <table class="consultations-table">
              <thead>
                <tr>
                  <th class="th-time">접수시간</th>
                  <th class="th-name">학생이름</th>
                  <!--th class="th-phone">학생휴대폰</th-->
                  <th class="th-school">학교/학년</th>
                  <th class="th-topic">상담테마</th>
                  <th class="th-type">상담유형</th>
                  <th class="th-status">상태</th>
                  <th class="th-score">만족도</th>
                  <th class="th-counselor">상담사</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="item in paginatedConsultations"
                  :key="item.id"
                  class="clickable-table-row"
                  @click="openDetailModal(item)"
                  title="클릭하여 상담 상세 내역 및 상담 결과 확인"
                >
                  <td class="td-time">{{ formatDateTime(item.createdAt) }}</td>
                  <td class="td-name"><strong>{{ item.studentName }}</strong></td>
                  <!--td class="td-phone">{{ item.studentPhone }}</td-->
                  <td class="td-school">{{ formatStudentInfo(item.schoolType, item.grade, item.gender) }}</td>
                  <td class="td-topic"><span class="topic-badge">{{ item.topicName || '-' }}</span></td>
                  <td class="td-type"><span class="type-tag">{{ item.typeName }}</span></td>
                  <td class="td-status">
                    <span class="status-badge" :class="getStatusBadgeClass(item.status)">
                      {{ getStatusLabel(item.status) }}
                    </span>
                  </td>
                  <td class="td-score">
                    <template v-if="item.status === 'COMPLETED' && item.satisfactionScore">
                      <span class="star-rating-badge" title="상담 만족도 점수">
                        ★{{ item.satisfactionScore }}
                      </span>
                    </template>
                    <template v-else>
                      <span class="text-muted">-</span>
                    </template>
                  </td>
                  <td class="td-counselor">{{ item.counselorName || '-' }}</td>
                </tr>
              </tbody>
            </table>

            <!-- 하단 페이징 네비게이션 -->
            <div class="pagination-container">
              <div class="pagination-info-group">
                <div class="pagination-info">
                  {{ (currentPage - 1) * pageSize + 1 }} - {{ Math.min(currentPage * pageSize, filteredConsultations.length) }}건 / 총 {{ filteredConsultations.length }}건
                </div>
                <div class="page-size-selector">
                  <select
                    id="pageSizeSelectPagination"
                    v-model.number="pageSize"
                    class="form-select page-size-select"
                    @change="resetPage"
                  >
                    <option v-for="size in pageSizeOptions" :key="size" :value="size">
                      {{ size }}개씩 보기
                    </option>
                  </select>
                </div>
              </div>

              <div class="pagination-nav">
                <button
                  type="button"
                  class="pagination-btn"
                  :disabled="currentPage === 1"
                  @click="currentPage--"
                >
                  &laquo; 이전
                </button>

                <button
                  v-for="page in visiblePages"
                  :key="page"
                  type="button"
                  class="pagination-num"
                  :class="{ 'pagination-num--active': currentPage === page }"
                  @click="currentPage = page"
                >
                  {{ page }}
                </button>

                <button
                  type="button"
                  class="pagination-btn"
                  :disabled="currentPage === totalPages"
                  @click="currentPage++"
                >
                  다음 &raquo;
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 탭 2: 상담 유형 관리 -->
      <section v-if="activeTab === 'types'" class="admin-tab-content">
        <!-- 로딩 상태 -->
        <div v-if="isLoadingTypes" class="admin-view__loading">
          <p>상담 유형을 불러오는 중입니다...</p>
        </div>

        <div v-else class="types-container">
          <section class="type-create-panel">
            <div class="type-create-panel__header">
              <h2 class="type-create-panel__title">새 상담 유형 추가</h2>
              <p class="type-create-panel__subtitle">학생과 상담사 화면에 즉시 반영됩니다</p>
            </div>

            <div class="type-create-panel__form">
              <div class="type-create-panel__field">
                <label class="type-create-panel__label" for="newTypeTopic">상담 테마</label>
                <select
                  id="newTypeTopic"
                  v-model="createFormData.topicId"
                  class="form-input form-select"
                  :class="{ error: createFieldErrors.topicId }"
                  :disabled="isCreating"
                >
                  <option value="" disabled>테마 선택</option>
                  <option v-for="t in topics" :key="t.id" :value="t.id">
                    {{ t.name }}
                  </option>
                </select>
                <div v-if="createFieldErrors.topicId" class="form-error">
                  {{ createFieldErrors.topicId }}
                </div>
              </div>

              <div class="type-create-panel__field">
                <label class="type-create-panel__label" for="newTypeName">상담 유형명</label>
                <input
                  id="newTypeName"
                  v-model="createFormData.name"
                  type="text"
                  class="form-input"
                  :class="{ error: createFieldErrors.name }"
                  placeholder="예: 고상해, 수비학"
                  :disabled="isCreating"
                />
                <div v-if="createFieldErrors.name" class="form-error">
                  {{ createFieldErrors.name }}
                </div>
              </div>

              <div class="type-create-panel__field type-create-panel__field--wide">
                <label class="type-create-panel__label" for="newTypeDescription">설명</label>
                <textarea
                  id="newTypeDescription"
                  v-model="createFormData.description"
                  class="form-textarea"
                  :class="{ error: createFieldErrors.description }"
                  placeholder="학생에게 보여줄 상담 유형 설명을 입력해주세요"
                  rows="3"
                  :disabled="isCreating"
                />
                <div v-if="createFieldErrors.description" class="form-error">
                  {{ createFieldErrors.description }}
                </div>
              </div>

              <button
                type="button"
                class="btn btn-success"
                :disabled="isCreating"
                :class="{ loading: isCreating }"
                @click="handleCreate"
              >
                <span v-if="!isCreating">유형 추가</span>
                <span v-else>추가 중...</span>
              </button>
            </div>
          </section>

          <!-- 테이블 -->
          <div class="types-table-container">
            <table class="types-table">
              <thead>
                <tr>
                  <th class="types-table__th types-table__th--id">유형 ID</th>
                  <th class="types-table__th types-table__th--topic">상담 테마</th>
                  <th class="types-table__th types-table__th--name">상담 유형</th>
                  <th class="types-table__th types-table__th--description">설명</th>
                  <th class="types-table__th types-table__th--actions">작업</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="type in types"
                  :key="type.id"
                  class="types-table__row"
                  :class="{ editing: editingId === type.id }"
                >
                  <!-- 유형 ID -->
                  <td class="types-table__cell types-table__cell--id">
                    <span class="type-id">{{ type.id }}</span>
                  </td>

                  <!-- 상담 테마 -->
                  <td class="types-table__cell types-table__cell--topic">
                    <div v-if="editingId !== type.id" class="type-field">
                      <span class="status-badge status-badge--accepted">{{ type.topicName || '미지정' }}</span>
                    </div>
                    <div v-else class="type-field-edit">
                      <select
                        v-model="editFormData.topicId"
                        class="form-input form-select"
                        :class="{ error: editFieldErrors.topicId }"
                      >
                        <option v-for="t in topics" :key="t.id" :value="t.id">
                          {{ t.name }}
                        </option>
                      </select>
                      <div v-if="editFieldErrors.topicId" class="form-error">
                        {{ editFieldErrors.topicId }}
                      </div>
                    </div>
                  </td>

                  <!-- 상담 유형명 -->
                  <td class="types-table__cell types-table__cell--name">
                    <div v-if="editingId !== type.id" class="type-field">
                      {{ type.name }}
                    </div>
                    <div v-else class="type-field-edit">
                      <input
                        v-model="editFormData.name"
                        type="text"
                        class="form-input"
                        :class="{ error: editFieldErrors.name }"
                        placeholder="상담 유형명"
                      />
                      <div v-if="editFieldErrors.name" class="form-error">
                        {{ editFieldErrors.name }}
                      </div>
                    </div>
                  </td>

                  <!-- 설명 -->
                  <td class="types-table__cell types-table__cell--description">
                    <div v-if="editingId !== type.id" class="type-field type-field--description">
                      {{ type.description }}
                    </div>
                    <div v-else class="type-field-edit">
                      <textarea
                        v-model="editFormData.description"
                        class="form-textarea"
                        :class="{ error: editFieldErrors.description }"
                        placeholder="상담 유형 설명"
                        rows="3"
                      />
                      <div v-if="editFieldErrors.description" class="form-error">
                        {{ editFieldErrors.description }}
                      </div>
                    </div>
                  </td>

                  <!-- 작업 버튼 -->
                  <td class="types-table__cell types-table__cell--actions">
                    <div v-if="editingId !== type.id" class="type-actions">
                      <button
                        type="button"
                        class="btn btn-primary btn-small"
                        @click="startEdit(type)"
                        :disabled="processingIds.has(type.id)"
                      >
                        수정
                      </button>
                      <button
                        type="button"
                        class="btn btn-danger btn-small"
                        @click="handleDelete(type)"
                        :disabled="processingIds.has(type.id)"
                      >
                        삭제
                      </button>
                    </div>
                    <div v-else class="type-actions-edit">
                      <button
                        type="button"
                        class="btn btn-success btn-small"
                        @click="handleSave(type.id)"
                        :disabled="processingIds.has(type.id)"
                        :class="{ loading: processingIds.has(type.id) }"
                      >
                        <span v-if="!processingIds.has(type.id)">저장</span>
                        <span v-else>저장 중...</span>
                      </button>
                      <button
                        type="button"
                        class="btn btn-secondary btn-small"
                        @click="cancelEdit"
                        :disabled="processingIds.has(type.id)"
                      >
                        취소
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 탭 3: 학생 정보 관리 -->
      <section v-else-if="activeTab === 'students'" class="admin-tab-content">
        <!-- 로딩 중 -->
        <div v-if="isLoadingStudents" class="admin-view__loading">
          <p>학생 목록을 불러오는 중입니다...</p>
        </div>

        <div v-else>
          <!-- 학생 검색 필터 패널 -->
          <div class="search-filter-panel">
            <div class="search-filter-panel__header">
              <h3 class="search-filter-title">학생 정보 검색</h3>
            </div>
            <div class="search-filter-panel__grid student-filter-grid">
              <!-- 학생 이름 검색 -->
              <div class="filter-group">
                <label for="filterStudentName" class="filter-label">학생 이름</label>
                <input
                  id="filterStudentName"
                  v-model="studentSearchFilter.studentName"
                  type="text"
                  class="form-input"
                  placeholder="이름 검색"
                  @input="resetStudentPage"
                />
              </div>

              <!-- 학생 휴대폰 번호 검색 -->
              <div class="filter-group">
                <label for="filterStudentPhone" class="filter-label">휴대폰 번호</label>
                <input
                  id="filterStudentPhone"
                  v-model="studentSearchFilter.studentPhone"
                  type="text"
                  class="form-input"
                  placeholder="번호 검색"
                  @input="resetStudentPage"
                />
              </div>

              <!-- 학교 구분 선택 -->
              <div class="filter-group">
                <label for="filterSchoolType" class="filter-label">학교 구분</label>
                <select
                  id="filterSchoolType"
                  v-model="studentSearchFilter.schoolType"
                  class="form-select"
                  @change="resetStudentPage"
                >
                  <option value="">전체 (모든 학교)</option>
                  <option value="MIDDLE_SCHOOL">중학교</option>
                  <option value="HIGH_SCHOOL">고등학교</option>
                  <option value="MIDDLE_HIGH_SCHOOL">중/고등학교</option>
                </select>
              </div>

              <!-- 학년 선택 -->
              <div class="filter-group">
                <label for="filterGrade" class="filter-label">학년</label>
                <select
                  id="filterGrade"
                  v-model="studentSearchFilter.grade"
                  class="form-select"
                  @change="resetStudentPage"
                >
                  <option value="">전체 학년</option>
                  <option value="0">미선택 (0학년)</option>
                  <option value="1">1학년</option>
                  <option value="2">2학년</option>
                  <option value="3">3학년</option>
                </select>
              </div>

              <!-- 성별 선택 -->
              <div class="filter-group">
                <label for="filterGender" class="filter-label">성별</label>
                <select
                  id="filterGender"
                  v-model="studentSearchFilter.gender"
                  class="form-select"
                  @change="resetStudentPage"
                >
                  <option value="">전체 성별</option>
                  <option value="0">미선택</option>
                  <option value="1">남성</option>
                  <option value="2">여성</option>
                </select>
              </div>

              <!-- 초기화 버튼 -->
              <div class="filter-group filter-group--actions">
                <button
                  type="button"
                  class="btn btn-secondary btn-reset"
                  @click="clearStudentSearchFilter"
                >
                  필터 초기화
                </button>
              </div>
            </div>
          </div>

          <!-- 요약 및 새로고침 -->
          <div class="filter-summary">
            <div class="filter-summary__left">
              <span>
                검색 결과 <strong>{{ filteredStudents.length }}</strong>명
                <span v-if="filteredStudents.length !== students.length" class="summary-total">
                  (전체 {{ students.length }}명 중)
                </span>
              </span>
            </div>
            <div class="filter-summary__right">
              <div class="page-size-selector">
                <label for="studentPageSizeSelectSummary" class="page-size-label">표시 개수:</label>
                <select
                  id="studentPageSizeSelectSummary"
                  v-model.number="studentPageSize"
                  class="form-select page-size-select"
                  @change="resetStudentPage"
                >
                  <option v-for="size in studentPageSizeOptions" :key="size" :value="size">
                    {{ size }}개씩
                  </option>
                </select>
              </div>
              <button
                type="button"
                class="btn btn-secondary btn-refresh"
                @click="refreshStudents"
                :disabled="isLoadingStudents"
              >
                <span>새로고침</span>
              </button>
            </div>
          </div>

          <!-- 검색 결과 없음 -->
          <div v-if="filteredStudents.length === 0" class="empty-state">
            <p>검색 조건에 일치하는 학생 정보가 없습니다.</p>
          </div>

          <!-- 학생 정보 목록 테이블 -->
          <div v-else class="consultations-table-container">
            <table class="consultations-table students-table">
              <thead>
                <tr>
                  <th class="th-id" style="width: 70px;">ID</th>
                  <th class="th-name" style="width: 140px;">학생 이름</th>
                  <th class="th-phone" style="width: 160px;">휴대폰 번호</th>
                  <th class="th-school" style="width: 130px;">학교 구분</th>
                  <th class="th-grade" style="width: 100px;">학년</th>
                  <th class="th-gender" style="width: 100px;">성별</th>
                  <th class="th-time">최초 등록일시</th>
                  <th class="th-actions" style="width: 140px; text-align: center;">관리</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in paginatedStudents" :key="item.id">
                  <!-- ID -->
                  <td class="td-id">
                    <span class="type-id">#{{ item.id }}</span>
                  </td>

                  <!-- 학생 이름 -->
                  <td class="td-name">
                    <template v-if="studentEditingId === item.id">
                      <input
                        v-model="studentEditFormData.studentName"
                        type="text"
                        class="form-input form-input--sm"
                        :class="{ error: studentEditFieldErrors.studentName }"
                        placeholder="이름 입력"
                      />
                      <span v-if="studentEditFieldErrors.studentName" class="error-message">{{ studentEditFieldErrors.studentName }}</span>
                    </template>
                    <template v-else>
                      <strong>{{ item.studentName }}</strong>
                    </template>
                  </td>

                  <!-- 휴대폰 번호 -->
                  <td class="td-phone">
                    <template v-if="studentEditingId === item.id">
                      <input
                        v-model="studentEditFormData.studentPhone"
                        type="text"
                        class="form-input form-input--sm"
                        :class="{ error: studentEditFieldErrors.studentPhone }"
                        placeholder="010-0000-0000"
                      />
                      <span v-if="studentEditFieldErrors.studentPhone" class="error-message">{{ studentEditFieldErrors.studentPhone }}</span>
                    </template>
                    <template v-else>
                      {{ item.studentPhone }}
                    </template>
                  </td>

                  <!-- 학교 구분 -->
                  <td class="td-school">
                    <template v-if="studentEditingId === item.id">
                      <select v-model="studentEditFormData.schoolType" class="form-select form-select--sm">
                        <option value="MIDDLE_SCHOOL">중학교</option>
                        <option value="HIGH_SCHOOL">고등학교</option>
                        <option value="MIDDLE_HIGH_SCHOOL">중/고등학교</option>
                      </select>
                    </template>
                    <template v-else>
                      <span class="status-badge status-badge--accepted">{{ formatSchoolTypeLabel(item.schoolType) }}</span>
                    </template>
                  </td>

                  <!-- 학년 -->
                  <td class="td-grade">
                    <template v-if="studentEditingId === item.id">
                      <select v-model.number="studentEditFormData.grade" class="form-select form-select--sm">
                        <option :value="0">미선택</option>
                        <option :value="1">1학년</option>
                        <option :value="2">2학년</option>
                        <option :value="3">3학년</option>
                      </select>
                    </template>
                    <template v-else>
                      {{ formatGradeLabel(item.grade) }}
                    </template>
                  </td>

                  <!-- 성별 -->
                  <td class="td-gender">
                    <template v-if="studentEditingId === item.id">
                      <select v-model.number="studentEditFormData.gender" class="form-select form-select--sm">
                        <option :value="0">미선택</option>
                        <option :value="1">남성</option>
                        <option :value="2">여성</option>
                      </select>
                    </template>
                    <template v-else>
                      {{ formatGenderLabel(item.gender) }}
                    </template>
                  </td>

                  <!-- 등록시간 -->
                  <td class="td-time">
                    {{ formatDateTime(item.createdAt || item.enteredAt) }}
                  </td>

                  <!-- 관리 버튼 -->
                  <td class="td-actions">
                    <div class="types-table__actions">
                      <template v-if="studentEditingId === item.id">
                        <button
                          type="button"
                          class="btn btn-success btn-small"
                          :disabled="studentProcessingIds.has(item.id)"
                          @click="handleStudentSave(item.id)"
                        >
                          <span v-if="!studentProcessingIds.has(item.id)">저장</span>
                          <span v-else>저장 중...</span>
                        </button>
                        <button
                          type="button"
                          class="btn btn-secondary btn-small"
                          :disabled="studentProcessingIds.has(item.id)"
                          @click="cancelStudentEdit"
                        >
                          취소
                        </button>
                      </template>
                      <template v-else>
                        <button
                          type="button"
                          class="btn btn-primary btn-small"
                          :disabled="studentEditingId !== null"
                          @click="startStudentEdit(item)"
                        >
                          수정
                        </button>
                      </template>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- 하단 페이징 네비게이션 -->
            <div class="pagination-container">
              <div class="pagination-info-group">
                <div class="pagination-info">
                  {{ (studentCurrentPage - 1) * studentPageSize + 1 }} - {{ Math.min(studentCurrentPage * studentPageSize, filteredStudents.length) }}명 / 총 {{ filteredStudents.length }}명
                </div>
                <div class="page-size-selector">
                  <select
                    id="studentPageSizeSelectPagination"
                    v-model.number="studentPageSize"
                    class="form-select page-size-select"
                    @change="resetStudentPage"
                  >
                    <option v-for="size in studentPageSizeOptions" :key="size" :value="size">
                      {{ size }}개씩 보기
                    </option>
                  </select>
                </div>
              </div>

              <div class="pagination-nav">
                <button
                  type="button"
                  class="pagination-btn"
                  :disabled="studentCurrentPage === 1"
                  @click="studentCurrentPage--"
                >
                  &laquo; 이전
                </button>

                <button
                  v-for="page in studentVisiblePages"
                  :key="page"
                  type="button"
                  class="pagination-num"
                  :class="{ 'pagination-num--active': studentCurrentPage === page }"
                  @click="studentCurrentPage = page"
                >
                  {{ page }}
                </button>

                <button
                  type="button"
                  class="pagination-btn"
                  :disabled="studentCurrentPage === studentTotalPages"
                  @click="studentCurrentPage++"
                >
                  다음 &raquo;
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

    <!-- 상담 상세 내역 및 결과 모달 (관리자용) -->
    <div v-if="isDetailModalOpen && selectedConsultationDetail" class="modal-backdrop" @click.self="closeDetailModal">
      <div class="modal-card modal-card--lg">
        <div class="modal-header">
          <h3 class="modal-title">상담 상세 내역 & 상담 결과</h3>
          <button type="button" class="btn-close" @click="closeDetailModal">✕</button>
        </div>
        <div class="modal-body">
          <!-- 학생 정보 -->
          <div class="detail-section">
            <h4 class="detail-section__title">학생 기본 정보</h4>
            <div class="detail-grid">
              <div class="detail-item"><span class="label">학생 이름:</span> <strong>{{ selectedConsultationDetail.studentName }}</strong></div>
              <div class="detail-item"><span class="label">휴대폰 번호:</span> <span>{{ selectedConsultationDetail.studentPhone }}</span></div>
              <div class="detail-item"><span class="label">학교 구분:</span> <span>{{ formatSchoolTypeLabel(selectedConsultationDetail.schoolType) }}</span></div>
              <div class="detail-item"><span class="label">학년 / 성별:</span> <span>{{ formatGradeLabel(selectedConsultationDetail.grade) }} / {{ formatGenderLabel(selectedConsultationDetail.gender || 0) }}</span></div>
            </div>
          </div>

          <!-- 상담 정보 -->
          <div class="detail-section">
            <h4 class="detail-section__title">상담 분류 및 진행 상태</h4>
            <div class="detail-grid">
              <div class="detail-item"><span class="label">상담 테마:</span> <span>{{ selectedConsultationDetail.topicName || '-' }}</span></div>
              <div class="detail-item"><span class="label">상담 유형:</span> <span>{{ selectedConsultationDetail.typeName }}</span></div>
              <div class="detail-item"><span class="label">담당 상담사:</span> <span>{{ selectedConsultationDetail.counselorName || '미지정' }}</span></div>
              <div class="detail-item"><span class="label">진행 상태:</span> <span class="status-badge" :class="getStatusBadgeClass(selectedConsultationDetail.status)">{{ getStatusLabel(selectedConsultationDetail.status) }}</span></div>
              <div class="detail-item"><span class="label">접수 일시:</span> <span>{{ formatDateTime(selectedConsultationDetail.createdAt) }}</span></div>
              <div class="detail-item"><span class="label">최종 갱신:</span> <span>{{ formatDateTime(selectedConsultationDetail.updatedAt) }}</span></div>
            </div>
          </div>

          <!-- 상담 결과 및 만족도 -->
          <div class="detail-section detail-section--result">
            <h4 class="detail-section__title">상담 결과 및 만족도 평가</h4>
            <template v-if="selectedConsultationDetail.resultContent || selectedConsultationDetail.satisfactionScore">
              <div class="result-card">
                <div class="result-row">
                  <span class="result-label">상담 결과 요약:</span>
                  <div class="result-text-box">{{ selectedConsultationDetail.resultContent || '등록된 결과 내용이 없습니다.' }}</div>
                </div>
                <div class="result-row-flex">
                  <div class="result-meta-item">
                    <span class="result-label">재상담 필요 여부:</span>
                    <span class="badge-tag" :class="selectedConsultationDetail.reConsultationNeeded === 1 ? 'badge-warn' : 'badge-info'">
                      {{ selectedConsultationDetail.reConsultationNeeded === 1 ? '1. 재상담 필요' : '2. 재상담 불필요' }}
                    </span>
                  </div>
                  <div class="result-meta-item">
                    <span class="result-label">만족도 점수:</span>
                    <span class="star-rating-display">
                      <span class="stars-yellow">{{ '★'.repeat(selectedConsultationDetail.satisfactionScore || 0) }}{{ '☆'.repeat(5 - (selectedConsultationDetail.satisfactionScore || 0)) }}</span>
                      <strong class="score-text">{{ selectedConsultationDetail.satisfactionScore }}점</strong>
                    </span>
                  </div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="empty-result-box">
                <p>아직 등록된 상담 결과 내용 및 만족도 평가가 없습니다.</p>
              </div>
            </template>
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeDetailModal">닫기</button>
        </div>
      </div>
    </div>
    </div>
  </article>
</template>

<style scoped>
.admin-view {
  width: 100%;
}

.admin-view__container {
  padding: 1.5rem 1rem;
}

.admin-view__header {
  margin-bottom: 2rem;
}

.admin-view__title {
  margin: 0.75rem 0 0.5rem;
  font-size: 1.875rem;
  line-height: 1.2;
  color: #0f172a;
}

.admin-view__subtitle {
  margin: 0.5rem 0 0;
  color: #475569;
  font-size: 0.9375rem;
}

.admin-view__success {
  padding: 1rem;
  margin-bottom: 1.5rem;
  border-radius: 0.75rem;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
  color: #166534;
  font-size: 0.9375rem;
  font-weight: 500;
}

.admin-view__error {
  padding: 1rem;
  margin-bottom: 1.5rem;
  border-radius: 0.75rem;
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.3);
  color: #991b1b;
  font-size: 0.9375rem;
  font-weight: 500;
}

.admin-view__loading {
  padding: 2rem 1rem;
  text-align: center;
  color: #475569;
}

.type-create-panel {
  padding: 1.25rem;
  border: 1px solid var(--border);
  border-radius: 1rem;
  background: #f8fafc;
}

.type-create-panel__header {
  margin-bottom: 1rem;
}

.type-create-panel__title {
  margin: 0;
  font-size: 1.125rem;
  color: #0f172a;
}

.type-create-panel__subtitle {
  margin: 0.375rem 0 0;
  color: #475569;
  font-size: 0.875rem;
}

.type-create-panel__form {
  display: grid;
  gap: 1rem;
}

.type-create-panel__field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.type-create-panel__label {
  font-weight: 600;
  color: #0f172a;
  font-size: 0.875rem;
}

/* 컨테이너 */
.types-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.types-table-container {
  width: 100%;
  overflow-x: auto;
}

/* 테이블 */
.types-table {
  width: 100%;
  min-width: 650px;
  border-collapse: collapse;
  display: table;
  background: white;
  border-radius: 0.75rem;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--border);
}

.types-table__th {
  padding: 0.4rem 0.5rem;
  text-align: center;
  font-weight: 700;
  font-size: 0.875rem;
  color: #334155;
  background: #f8fafc;
  border-bottom: 2px solid var(--border);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  line-height: 1.2;
}

.types-table__th--id {
  width: 10%;
  white-space: nowrap;
}

.types-table__th--topic {
  width: 14%;
  white-space: nowrap;
}

.types-table__th--name {
  width: 20%;
  white-space: nowrap;
}

.types-table__th--description {
  width: 40%;
}

.types-table__th--actions {
  width: 16%;
  white-space: nowrap;
}

.types-table__row {
  border-bottom: 1px solid var(--border);
  transition: all 0.2s ease;
}

.types-table__row:hover {
  background: #f8fafc;
}

.types-table__row.editing {
  background: var(--primary-soft);
}

.types-table__cell {
  padding: 0.4rem 0.5rem;
  vertical-align: middle;
  text-align: center;
  font-size: 0.875rem;
  line-height: 1.2;
  color: #1e293b;
}

.types-table__cell--id {
  font-weight: 600;
  color: #0f172a;
}

/* 필드 스타일 */
.type-id {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.15rem 0.5rem;
  border-radius: 0.375rem;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 700;
  font-size: 0.8125rem;
  line-height: 1.2;
  width: auto;
  height: auto;
}

.type-field {
  color: #0f172a;
  font-size: 0.875rem;
  line-height: 1.3;
}

.type-field--description {
  color: #475569;
  font-size: 0.8125rem;
  max-height: 3.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-field-edit {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-input,
.form-textarea {
  padding: 0.5rem;
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  background: white;
  font-size: 0.875rem;
  font-family: inherit;
  transition: all 0.2s ease;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.form-input.error,
.form-textarea.error {
  border-color: #dc2626;
  background: rgba(220, 38, 38, 0.05);
}

.form-textarea {
  resize: vertical;
  min-height: 2.5rem;
}

.form-error {
  font-size: 0.75rem;
  color: #991b1b;
  font-weight: 500;
}

/* 액션 버튼 */
.type-actions,
.type-actions-edit {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  gap: 0.375rem;
  justify-content: center;
  align-items: center;
  white-space: nowrap;
}

.type-actions .btn,
.type-actions-edit .btn {
  white-space: nowrap;
  flex-shrink: 0;
}

/* 버튼 */
.btn {
  padding: 0.75rem 1rem;
  border: none;
  border-radius: 0.5rem;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-small {
  padding: 0.25rem 0.625rem;
  font-size: 0.775rem;
  line-height: 1.2;
  border-radius: 0.375rem;
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
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.3);
}

.btn-success {
  background: #22c55e;
  color: white;
}

.btn-success:not(:disabled):hover {
  background: #16a34a;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
}

.btn-secondary {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-secondary:not(:disabled):hover {
  background: #cbd5e1;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
}

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:not(:disabled):hover {
  background: #b91c1c;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
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

/* 반응형 - 데스크톱에서 테이블 표시 */
@media (min-width: 1024px) {
  .admin-view__container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
  }

  .types-table {
    display: table;
  }

  .types-cards {
    display: none;
  }
}

/* 반응형 - 태블릿 이상 */
@media (min-width: 768px) {
  .admin-view__container {
    max-width: 900px;
    margin: 0 auto;
    padding: 2rem;
  }

  .types-table {
    display: table;
  }

  .types-cards {
    display: none;
  }

  .type-create-panel__form {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1.5fr) auto;
    align-items: start;
  }

  .type-create-panel__field--wide {
    min-width: 0;
  }

  .types-table__th--id {
    width: 10%;
  }

  .types-table__th--topic {
    width: 14%;
  }

  .types-table__th--name {
    width: 20%;
  }

  .types-table__th--description {
    width: 40%;
  }

  .types-table__th--actions {
    width: 16%;
  }

  .consultations-table {
    display: table;
  }

  .consultations-cards {
    display: none;
  }
}

/* 탭 메뉴 스타일 */
.admin-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.75rem;
  border-bottom: 2px solid #e2e8f0;
}

.admin-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1.25rem;
  border: none;
  background: none;
  font-size: 1rem;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s ease;
}

.admin-tab:hover {
  color: #0f172a;
}

.admin-tab--active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.tab-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.125rem 0.5rem;
  border-radius: 9999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 700;
}

.admin-tab--active .tab-count-badge {
  background: var(--primary-soft);
  color: var(--primary);
}

/* 전체 학생 상담 목록 스타일 */
.empty-state {
  padding: 3rem 1.5rem;
  text-align: center;
  background: #f8fafc;
  border-radius: 0.75rem;
  border: 1px dashed #cbd5e1;
  color: #64748b;
  font-size: 0.9375rem;
}

.consultations-table-container {
  width: 100%;
  overflow-x: auto;
}

.consultations-table {
  width: 100%;
  min-width: 650px;
  border-collapse: collapse;
  display: table;
  background: white;
  border-radius: 0.75rem;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--border);
}

.consultations-table th {
  padding: 0.4rem 0.5rem;
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
  font-size: 0.875rem;
  text-align: center;
  border-bottom: 2px solid var(--border);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  line-height: 1.2;
}

.consultations-table td {
  padding: 0.4rem 0.5rem;
  border-bottom: 1px solid var(--border);
  font-size: 0.875rem;
  color: #1e293b;
  vertical-align: middle;
  text-align: center;
  line-height: 1.2;
}

.consultations-table tr:hover {
  background: #f8fafc;
}

.th-time { width: 13%; white-space: nowrap; }
.th-name { width: 9%; white-space: nowrap; }
.th-phone { width: 12%; white-space: nowrap; }
.th-school { width: 14%; white-space: nowrap; }
.th-topic { width: 14%; white-space: nowrap; }
.th-type { width: 18%; white-space: nowrap; }
.th-status { width: 10%; white-space: nowrap; }
.th-counselor { width: 10%; white-space: nowrap; }

.td-time, .td-name, .td-phone, .td-school, .td-topic, .td-type, .td-status, .td-counselor {
  white-space: nowrap;
}

.topic-badge {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: 0.375rem;
  background: #f1f5f9;
  color: #334155;
  font-size: 0.8125rem;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
}

.type-tag {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: 0.375rem;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 0.8125rem;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
}

/* 상태 배지 */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.8125rem;
  font-weight: 700;
  line-height: 1.2;
}

.status-badge--received {
  background: #fef3c7;
  color: #92400e;
}

.status-badge--accepted {
  background: #dbeafe;
  color: #1e40af;
}

.status-badge--progress {
  background: #e0e7ff;
  color: #3730a3;
}

.status-badge--completed {
  background: #dcfce7;
  color: #166534;
}

.status-badge--cancelled {
  background: #fee2e2;
  color: #991b1b;
}

/* 검색 필터 패널 스타일 */
.search-filter-panel {
  padding: 1rem 1.25rem;
  background: #f8fafc;
  border: 1px solid var(--border);
  border-radius: 0.875rem;
  margin-bottom: 1.25rem;
}

.search-filter-panel__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .search-filter-panel__grid {
    grid-template-columns: minmax(100px, 0.75fr) minmax(130px, 1.2fr) minmax(130px, 1.2fr) minmax(110px, 1fr) auto;
    align-items: end;
    gap: 0.625rem;
  }
}

.search-filter-panel .form-input,
.search-filter-panel .form-select {
  height: 2.375rem;
  padding: 0.4rem 0.65rem;
  font-size: 0.875rem;
  border-radius: 0.375rem;
  box-sizing: border-box;
  line-height: 1.3;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.filter-group--actions {
  justify-content: flex-end;
}

.filter-label {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #334155;
  white-space: nowrap;
}

.form-select {
  border: 1px solid var(--border);
  background: white;
  color: #0f172a;
  font-family: inherit;
  transition: all 0.2s ease;
  cursor: pointer;
}

.form-select:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.btn-reset {
  height: 2.375rem;
  padding: 0.4rem 0.75rem;
  font-size: 0.8125rem;
  white-space: nowrap;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.filter-summary {
  margin-bottom: 1rem;
  font-size: 0.875rem;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.filter-summary__left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-summary__right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.page-size-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #475569;
  white-space: nowrap;
}

.page-size-select {
  height: 2.125rem !important;
  padding: 0.2rem 0.5rem !important;
  font-size: 0.8125rem !important;
  border-radius: 0.375rem !important;
  background-color: white;
}

.filter-summary strong {
  color: var(--primary);
  font-weight: 700;
}

.summary-total {
  color: #94a3b8;
  margin-left: 0.25rem;
}

.btn-refresh {
  flex-shrink: 0;
}

/* 페이징 네비게이션 스타일 */
.pagination-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--border);
}

@media (min-width: 640px) {
  .pagination-container {
    flex-direction: row;
    justify-content: space-between;
  }
}

.pagination-info-group {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.pagination-info {
  font-size: 0.875rem;
  color: #64748b;
}

.pagination-nav {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.pagination-btn,
.pagination-num {
  padding: 0.5rem 0.875rem;
  border: 1px solid var(--border);
  border-radius: 0.5rem;
  background: white;
  color: #334155;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: #f1f5f9;
}

.pagination-btn:not(:disabled):hover,
.pagination-num:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-soft);
}

.pagination-num--active:hover {
  background: #1e40af;
  color: white;
}

.student-filter-grid {
  grid-template-columns: repeat(6, 1fr) !important;
  align-items: end !important;
}

@media (min-width: 768px) {
  .student-filter-grid {
    grid-template-columns: minmax(90px, 0.8fr) minmax(110px, 1.1fr) minmax(110px, 1fr) minmax(80px, 0.8fr) minmax(80px, 0.8fr) auto !important;
    align-items: end !important;
    gap: 0.5rem !important;
  }
}

.form-input--sm,
.form-select--sm {
  height: 2.125rem !important;
  padding: 0.2rem 0.4rem !important;
  font-size: 0.8125rem !important;
  border-radius: 0.375rem !important;
  width: 100%;
}

.students-table .td-name input,
.students-table .td-phone input {
  box-sizing: border-box;
}

.students-table .td-actions {
  text-align: center;
}

/* 클릭 가능한 테이블 행 */
.clickable-table-row {
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.clickable-table-row:hover {
  background-color: #f1f5f9 !important;
}

.star-rating-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  background: #fef3c7;
  color: #b45309;
  border-radius: 0.375rem;
  font-weight: 700;
  font-size: 0.8125rem;
}

/* 관리자 모달 스타일 */
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

.modal-card--lg {
  max-width: 44rem;
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
  max-height: 75vh;
  overflow-y: auto;
}

.detail-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 0.75rem;
  padding: 1rem 1.25rem;
}

.detail-section--result {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.detail-section__title {
  margin: 0 0 0.75rem 0;
  font-size: 0.9375rem;
  font-weight: 700;
  color: #1e293b;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 0.375rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.75rem;
  font-size: 0.875rem;
}

.detail-item .label {
  color: #64748b;
  margin-right: 0.375rem;
}

.result-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.result-row {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.result-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #334155;
}

.result-text-box {
  background: #f1f5f9;
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

.result-meta-item {
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

.empty-result-box {
  padding: 1.5rem;
  text-align: center;
  color: #94a3b8;
  font-size: 0.875rem;
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
