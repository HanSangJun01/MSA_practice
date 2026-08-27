<template>
  <div class="page-wrapper">
    <AppHeader />

    <div class="page-layout">
      <!-- 사이드바 -->
      <aside class="sidebar">
        <div class="sidebar-section">
          <div class="sidebar-label">메뉴</div>

          <router-link
            to="/courses"
            class="sidebar-item"
            :class="{ active: $route.path === '/courses' }"
          >
            <span class="si-icon">🔩</span> 원료 목록
          </router-link>

          <router-link
            to="/courses/new"
            class="sidebar-item"
            :class="{ active: $route.path === '/courses/new' }"
          >
            <span class="si-icon">✍️</span> 원료 등록
          </router-link>

          <router-link to="/mypage" class="sidebar-item">
            <span class="si-icon">⭐</span> 마이페이지
          </router-link>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-label">계정</div>
          <router-link to="/mypage" class="sidebar-item">
            <span class="si-icon">👤</span> 마이페이지
          </router-link>
          <button class="sidebar-item sidebar-btn" @click="handleLogout">
            <span class="si-icon">🚪</span> 로그아웃
          </button>
        </div>
      </aside>

      <!-- 메인 -->
      <main class="main-content">
        <div class="content-header">
          <div>
            <h1 class="page-title">원료 등록</h1>
            <p class="page-subtitle">공급기업 계정으로 새로운 원료를 등록합니다.</p>
          </div>
        </div>

        <div class="form-card">
          <form class="course-form" @submit.prevent="handleSubmit">
            <div class="form-group">
              <label class="form-label" for="title">원료명</label>
              <input
                id="title"
                v-model.trim="form.title"
                type="text"
                class="form-input"
                placeholder="예: 리튬이온 배터리 블랙매스 (Ni·Co·Mn 함량 표기)"
                maxlength="100"
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="description">원료 설명</label>
              <textarea
                id="description"
                v-model.trim="form.description"
                class="form-textarea"
                rows="6"
                placeholder="부산물 성분, 월 발생량, 보관 상태, 활용 가능 용도 등을 입력해 주세요."
              ></textarea>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label" for="category">카테고리</label>
                <select id="category" v-model="form.category" class="form-select">
                  <option disabled value="">카테고리를 선택하세요</option>
                  <option
                    v-for="option in categoryOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label" for="price">로트 총가격</label>
                <input
                  id="price"
                  v-model.number="form.price"
                  type="number"
                  min="0"
                  step="1000"
                  class="form-input"
                  placeholder="예: 1200000"
                />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label" for="quantity">수량</label>
                <input
                  id="quantity"
                  v-model.number="form.quantity"
                  type="number"
                  min="0"
                  class="form-input"
                  placeholder="예: 5 (톤·개 등은 원료명에 표기)"
                />
              </div>

              <div class="form-group">
                <label class="form-label" for="region">공급 지역</label>
                <input
                  id="region"
                  v-model.trim="form.region"
                  type="text"
                  class="form-input"
                  placeholder="예: 경기 화성"
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">성분·함량</label>
              <div class="component-rows">
                <div v-for="(row, index) in form.components" :key="index" class="component-row">
                  <select v-model="row.name" class="form-select">
                    <option disabled value="">성분 선택</option>
                    <option v-for="opt in componentOptions" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                  <input
                    v-model.number="row.percentage"
                    type="number"
                    min="0"
                    max="100"
                    step="0.1"
                    class="form-input"
                    placeholder="함량 %"
                  />
                  <button type="button" class="component-remove" @click="removeComponentRow(index)">
                    삭제
                  </button>
                </div>
              </div>
              <button type="button" class="btn btn-ghost add-component-btn" @click="addComponentRow">
                + 성분 추가
              </button>
            </div>

            <div v-if="validationError" class="error-box">
              {{ validationError }}
            </div>

            <div v-if="submitError" class="error-box">
              {{ submitError }}
            </div>

            <div v-if="submitSuccess" class="success-box">
              {{ submitSuccess }}
            </div>

            <div class="form-actions">
              <router-link to="/courses" class="btn btn-ghost">
                취소
              </router-link>

              <button type="submit" class="btn btn-primary" :disabled="submitting">
                <span v-if="submitting">등록 중...</span>
                <span v-else>원료 등록</span>
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { courseApi } from '@/api/course.js'
import { useAuthStore } from '@/store/auth.js'
import { CATEGORY_OPTIONS } from '@/constants/category.js'
import { COMPONENT_OPTIONS } from '@/constants/materialComponent.js'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  title: '',
  description: '',
  category: '',
  price: null,
  quantity: null,
  region: '',
  components: []
})

const submitting = ref(false)
const validationError = ref('')
const submitError = ref('')
const submitSuccess = ref('')

const categoryOptions = CATEGORY_OPTIONS
const componentOptions = COMPONENT_OPTIONS

function addComponentRow() {
  form.components.push({ name: '', percentage: null })
}

function removeComponentRow(index) {
  form.components.splice(index, 1)
}

function handleLogout() {
  auth.logout()
  router.push('/')
}

function validateForm() {
  validationError.value = ''

  if (!auth.user || auth.user.role !== 'INSTRUCTOR') {
    validationError.value = '공급기업 계정만 원료를 등록할 수 있습니다.'
    return false
  }

  if (!form.title) {
    validationError.value = '원료명을 입력해 주세요.'
    return false
  }

  if (!form.description) {
    validationError.value = '원료 설명을 입력해 주세요.'
    return false
  }

  if (!form.category) {
    validationError.value = '카테고리를 선택해 주세요.'
    return false
  }

  if (form.price === null || form.price === undefined || form.price === '') {
    validationError.value = '가격을 입력해 주세요.'
    return false
  }

  const price = Number(form.price)
  if (Number.isNaN(price) || price < 0) {
    validationError.value = '가격은 0 이상의 숫자로 입력해 주세요.'
    return false
  }

  for (const row of form.components) {
    const hasName = !!row.name
    const hasPercentage = row.percentage !== null && row.percentage !== undefined && row.percentage !== ''
    if (hasName !== hasPercentage) {
      validationError.value = '성분과 함량을 모두 입력하거나, 비워서 삭제해 주세요.'
      return false
    }
    if (hasPercentage && (Number(row.percentage) < 0 || Number(row.percentage) > 100)) {
      validationError.value = '함량은 0~100 사이로 입력해 주세요.'
      return false
    }
  }

  return true
}

async function handleSubmit() {
  submitError.value = ''
  submitSuccess.value = ''

  if (!validateForm()) return

  submitting.value = true

  try {
    const payload = {
      title: form.title,
      description: form.description,
      category: form.category,
      price: Number(form.price),
      quantity: form.quantity === null || form.quantity === '' ? null : Number(form.quantity),
      region: form.region || null,
      components: form.components
        .filter(row => row.name && row.percentage !== null && row.percentage !== '')
        .map(row => ({ name: row.name, percentage: Number(row.percentage) }))
    }

    const res = await courseApi.create(payload)
    console.log('[CourseCreate] create response =', res.data)

    submitSuccess.value = '원료가 성공적으로 등록되었습니다.'

    const createdCourseId =
      res.data?.data?.id ??
      res.data?.id

    if (createdCourseId) {
      setTimeout(() => {
        router.push(`/courses/${createdCourseId}`)
      }, 500)
    } else {
      setTimeout(() => {
        router.push('/courses')
      }, 500)
    }
  } catch (error) {
    console.error('[CourseCreate] create failed:', error)
    submitError.value =
      error.response?.data?.message ||
      '원료 등록에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-wrapper {
  min-height: 100vh;
  background: var(--color-bg-secondary);
}

.page-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 28px;
}

/* 사이드바 */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 8px;
}

.sidebar-label {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
  padding: 8px 12px 4px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-secondary);
  transition: var(--transition);
  background: none;
  border: none;
  width: 100%;
  text-align: left;
  cursor: pointer;
  font-family: var(--font-sans);
  text-decoration: none;
}

.sidebar-item:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.sidebar-item.active {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
  font-weight: 500;
}

.si-icon {
  font-size: 15px;
}

.sidebar-btn {
  color: var(--color-text-secondary);
}

/* 메인 */
.main-content {
  min-width: 0;
}

.content-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.page-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.form-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);
}

.course-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.form-input,
.form-textarea,
.form-select {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-primary);
  padding: 12px 14px;
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  outline: none;
  transition: var(--transition);
  box-sizing: border-box;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  border-color: var(--color-brand-green);
  box-shadow: 0 0 0 3px var(--color-brand-green-soft);
}

.form-textarea {
  resize: vertical;
  min-height: 140px;
  line-height: 1.5;
}

.component-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.component-row {
  display: grid;
  grid-template-columns: 1fr 120px auto;
  gap: 8px;
  align-items: center;
}

.component-remove {
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  font-size: 13px;
  cursor: pointer;
  padding: 8px;
}

.component-remove:hover {
  color: var(--color-brand-error);
}

.add-component-btn {
  width: fit-content;
  margin-top: 8px;
}

.error-box {
  background: rgba(220, 38, 38, 0.08);
  color: var(--color-brand-error);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  font-size: 13px;
}

.success-box {
  background: var(--color-brand-green-soft);
  color: var(--color-brand-green-deep);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 6px;
}

@media (max-width: 992px) {
  .page-layout {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>