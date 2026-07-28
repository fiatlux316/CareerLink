<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminTypes, createAdminType, updateAdminType, deleteAdminType } from '../api/admin'
import type { ConsultationType, ErrorResponse } from '../types/consultation'

const types = ref<ConsultationType[]>([])
const isLoadingTypes = ref(true)
const errorMessage = ref('')
const successMessage = ref('')
const editingId = ref<number | null>(null)
const processingIds = ref<Set<number>>(new Set())
const isCreating = ref(false)

const createFormData = ref({
  name: '',
  description: '',
})

const editFormData = ref({
  name: '',
  description: '',
})

const createFieldErrors = ref<Record<string, string>>({})
const editFieldErrors = ref<Record<string, string>>({})

const validateTypeForm = (payload: { name: string; description: string }) => {
  const errors: Record<string, string> = {}

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

onMounted(async () => {
  try {
    types.value = await getAdminTypes()
  } catch (error) {
    console.error('Failed to load admin types:', error)
    errorMessage.value = '상담 유형을 불러올 수 없습니다'
  } finally {
    isLoadingTypes.value = false
  }
})
</script>

<template>
  <article class="admin-view">
    <div class="admin-view__container">
      <!-- 헤더 -->
      <div class="admin-view__header">
        <span class="badge">관리자</span>
        <h1 class="admin-view__title">상담 유형 관리</h1>
        <p class="admin-view__subtitle">상담 유형의 이름과 설명을 관리합니다</p>
      </div>

      <!-- 성공 메시지 -->
      <div v-if="successMessage" class="admin-view__success">
        {{ successMessage }}
      </div>

      <!-- 에러 메시지 -->
      <div v-if="errorMessage" class="admin-view__error">
        {{ errorMessage }}
      </div>

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
              <label class="type-create-panel__label" for="newTypeName">상담 유형명</label>
              <input
                id="newTypeName"
                v-model="createFormData.name"
                type="text"
                class="form-input"
                :class="{ error: createFieldErrors.name }"
                placeholder="예: 진로 설계 상담"
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

        <!-- 데스크톱 테이블 -->
        <table class="types-table">
          <thead>
            <tr>
              <th class="types-table__th types-table__th--id">유형 ID</th>
              <th class="types-table__th types-table__th--name">상담 유형명</th>
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

        <!-- 모바일 카드 뷰 -->
        <div class="types-cards">
          <div
            v-for="type in types"
            :key="type.id"
            class="type-card"
            :class="{ editing: editingId === type.id }"
          >
            <div class="type-card__header">
              <div class="type-card__id-badge">ID: {{ type.id }}</div>
            </div>

            <div class="type-card__body">
              <!-- 이름 -->
              <div class="type-card__field">
                <label class="type-card__label">상담 유형명</label>
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
              </div>

              <!-- 설명 -->
              <div class="type-card__field">
                <label class="type-card__label">설명</label>
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
              </div>
            </div>

            <!-- 작업 버튼 -->
            <div class="type-card__footer">
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
            </div>
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

/* 테이블 (데스크톱) */
.types-table {
  width: 100%;
  border-collapse: collapse;
  display: none;
}

.types-table__th {
  padding: 1rem;
  text-align: left;
  font-weight: 700;
  font-size: 0.875rem;
  color: #0f172a;
  background: #f8fafc;
  border-bottom: 2px solid var(--border);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.types-table__th--id {
  width: 10%;
}

.types-table__th--name {
  width: 20%;
}

.types-table__th--description {
  width: 50%;
}

.types-table__th--actions {
  width: 20%;
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
  padding: 1rem;
  vertical-align: middle;
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
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 700;
  font-size: 0.875rem;
}

.type-field {
  color: #0f172a;
  font-size: 0.9375rem;
  line-height: 1.5;
}

.type-field--description {
  color: #475569;
  font-size: 0.875rem;
  max-height: 5rem;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-field-edit {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-input,
.form-textarea {
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: 0.5rem;
  background: white;
  font-size: 0.9375rem;
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
  min-height: 3rem;
}

.form-error {
  font-size: 0.8125rem;
  color: #991b1b;
  font-weight: 500;
}

/* 액션 버튼 */
.type-actions,
.type-actions-edit {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.type-actions-edit {
  flex-wrap: wrap;
}

/* 모바일 카드 뷰 */
.types-cards {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.type-card {
  border: 1px solid var(--border);
  border-radius: 0.875rem;
  background: white;
  overflow: hidden;
  transition: all 0.2s ease;
}

.type-card:hover {
  border-color: var(--primary);
  box-shadow: 0 2px 8px rgba(29, 78, 216, 0.1);
}

.type-card.editing {
  background: var(--primary-soft);
  border-color: var(--primary);
  box-shadow: 0 2px 12px rgba(29, 78, 216, 0.15);
}

.type-card__header {
  padding: 0.75rem 1rem;
  background: #f8fafc;
  border-bottom: 1px solid var(--border);
}

.type-card__id-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.375rem 0.75rem;
  border-radius: 0.5rem;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 700;
  font-size: 0.875rem;
}

.type-card__body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.type-card__field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.type-card__label {
  display: block;
  font-weight: 600;
  font-size: 0.875rem;
  color: #0f172a;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.type-card__footer {
  padding: 1rem;
  background: #f8fafc;
  border-top: 1px solid var(--border);
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
  padding: 0.625rem 0.875rem;
  font-size: 0.8125rem;
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
    width: 8%;
  }

  .types-table__th--name {
    width: 18%;
  }

  .types-table__th--description {
    width: 54%;
  }

  .types-table__th--actions {
    width: 20%;
  }
}
</style>
