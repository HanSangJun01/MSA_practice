<template>
  <div class="page-wrapper">
    <AppHeader />

    <div class="detail-layout" v-if="course">
      <div class="detail-hero">
        <div class="detail-hero-inner">
          <!-- 좌측 상세 정보 -->
          <div class="detail-info fade-in-up">
            <div class="badge-row">
              <span class="badge badge-accent">{{ displayCategory }}</span>
              <span v-if="course.status && course.status !== 'APPROVED'" class="badge" :class="statusBadgeClass">
                {{ statusLabel }}
              </span>
            </div>
            <h1 class="detail-title">{{ course.title }}</h1>
            <p class="detail-desc">
              {{ course.description || '제3자 품질 검증을 거친 산업 부산물 원료입니다. 성분표와 함량을 확인하세요.' }}
            </p>

            <p v-if="course.status === 'REJECTED' && course.rejectionReason" class="rejection-reason">
              거절 사유: {{ course.rejectionReason }}
            </p>

            <div class="detail-meta">
              <span>공급기업: {{ displayInstructorName }}</span>
              <span>계약: {{ displayEnrollmentCount }}건</span>
              <span v-if="course.quantity">수량: {{ course.quantity.toLocaleString() }}</span>
              <span v-if="course.region">지역: {{ course.region }}</span>
              <span v-if="displayCreatedAt">등록일: {{ displayCreatedAt }}</span>
            </div>

            <div v-if="components.length" class="component-list">
              <span v-for="c in components" :key="c.name" class="component-chip">
                {{ getComponentLabel(c.name) }} {{ c.percentage }}%
              </span>
            </div>
          </div>

          <!-- 우측 결제/수강 카드 -->
          <div class="enroll-card fade-in">
            <div class="enroll-thumb thumb-industrial">
              <MaterialIcon :category="course.category" class="thumb-icon" />
            </div>

            <div class="enroll-body">
              <div class="enroll-price">₩{{ displayPrice }}</div>

              <button
                class="btn btn-primary btn-full"
                @click="handlePrimaryAction"
                :disabled="buttonDisabled"
                :class="{ 'btn-disabled': buttonDisabled }"
              >
                <span v-if="enrolling || deciding">처리 중...</span>
                <span v-else>{{ buttonLabel }}</span>
              </button>

              <div v-if="isIntermediary && course.status === 'PENDING'" class="reject-block">
                <button v-if="!showRejectForm" class="text-btn-reject" @click="showRejectForm = true">
                  거절하기
                </button>
                <div v-else class="reject-form">
                  <textarea
                    v-model="rejectReason"
                    class="reject-textarea"
                    rows="3"
                    placeholder="거절 사유를 입력하세요"
                  ></textarea>
                  <div class="reject-actions">
                    <button class="btn btn-ghost btn-sm" @click="showRejectForm = false">취소</button>
                    <button class="btn btn-sm reject-confirm-btn" :disabled="deciding" @click="handleReject">
                      거절 확정
                    </button>
                  </div>
                </div>
              </div>

              <div v-if="enrollError || decisionError" class="error-msg">{{ enrollError || decisionError }}</div>

              <p class="helper-text" v-if="helperText">
                {{ helperText }}
              </p>

              <ul class="enroll-info-list">
                <li>✅ 검증된 성분 분석표 제공</li>
                <li>✅ 계약 후 인수 일정 안내</li>
                <li>✅ 품질 시험성적서 제공</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="loading-center">
      <div class="spinner"></div>
    </div>

    <div v-else class="loading-center">
      <p class="empty-text">원료 정보를 불러오지 못했습니다.</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import MaterialIcon from '@/components/MaterialIcon.vue'
import { useCourseStore } from '@/store/course.js'
import { enrollmentApi } from '@/api/enrollment.js'
import { courseApi } from '@/api/course.js'
import { useAuthStore } from '@/store/auth.js'
import { getCategoryLabel } from '@/constants/category.js'
import { getComponentLabel } from '@/constants/materialComponent.js'
import { getLotStatusLabel, getLotStatusBadge } from '@/constants/lotStatus.js'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()
const auth = useAuthStore()

const enrolling = ref(false)
const enrollError = ref('')
const enrollmentStatus = ref('NONE') // NONE | PENDING | ACTIVE

const deciding = ref(false)
const decisionError = ref('')
const showRejectForm = ref(false)
const rejectReason = ref('')

const course = computed(() => courseStore.selectedCourse)
const loading = computed(() => courseStore.loading)
// role(INSTRUCTOR)은 공급기업/중간기업이 공유하므로 companyType으로 구분한다
const isSupplier = computed(() => auth.user?.companyType === 'SUPPLIER')
const isIntermediary = computed(() => auth.user?.companyType === 'INTERMEDIARY')

const displayCategory = computed(() => getCategoryLabel(course.value?.category) || '-')

const displayInstructorName = computed(() => {
  return (
    course.value?.supplierName ||
    course.value?.instructorName ||
    course.value?.teacherName ||
    course.value?.instructor?.name ||
    course.value?.instructor_name ||
    course.value?.ownerName ||
    '공급기업 정보 없음'
  )
})

const displayEnrollmentCount = computed(() => {
  const value = Number(
    course.value?.contractCount ??
    course.value?.enrollmentCount ??
    course.value?.enrollment_count ??
    0
  )
  return Number.isNaN(value) ? 0 : value.toLocaleString()
})

const components = computed(() => course.value?.components ?? [])

const displayCreatedAt = computed(() => {
  if (!course.value?.createdAt) return ''
  return new Date(course.value.createdAt).toLocaleDateString('ko-KR')
})

const statusLabel = computed(() => getLotStatusLabel(course.value?.status))
const statusBadgeClass = computed(() => getLotStatusBadge(course.value?.status))

const displayPrice = computed(() => {
  const value = Number(course.value?.price ?? 0)
  return Number.isNaN(value) ? '0' : value.toLocaleString()
})

const buttonLabel = computed(() => {
  if (isSupplier.value) return '공급기업 계정은 구매 신청 불가'
  if (isIntermediary.value) {
    return course.value?.status === 'PENDING' ? '승인하기' : '검토 완료된 원료'
  }
  if (enrollmentStatus.value === 'ACTIVE') return '내 구매 목록으로 이동'
  if (enrollmentStatus.value === 'PENDING') return '신청 완료 · 계약 처리 중'
  return '계약하고 구매하기'
})

const buttonDisabled = computed(() => {
  if (enrolling.value || deciding.value) return true
  if (isSupplier.value) return true
  if (isIntermediary.value) return course.value?.status !== 'PENDING'
  if (enrollmentStatus.value === 'PENDING') return true
  return false
})

const helperText = computed(() => {
  if (isSupplier.value) {
    return '공급기업 계정은 본인이 등록한 원료를 구매 신청할 수 없습니다.'
  }

  if (isIntermediary.value) {
    return course.value?.status === 'PENDING'
      ? '성분표와 함량을 확인한 뒤 승인 또는 거절을 결정해 주세요.'
      : '이미 검토가 완료된 원료입니다.'
  }

  if (enrollmentStatus.value === 'ACTIVE') {
    return '이미 계약이 완료된 원료입니다. 내 구매 목록에서 바로 확인할 수 있습니다.'
  }

  if (enrollmentStatus.value === 'PENDING') {
    return '구매 신청이 접수되었습니다. 계약/처리 상태가 반영되면 내 구매 목록에서 확인할 수 있습니다.'
  }

  return '계약을 진행하면 구매 신청이 함께 처리됩니다.'
})

async function loadEnrollmentStatus() {
  if (!auth.user?.id || !course.value?.id || isSupplier.value || isIntermediary.value) {
    enrollmentStatus.value = 'NONE'
    return
  }

  try {
    const res = await enrollmentApi.getMyEnrollments()
    console.log('[CourseDetail] my enrollments response =', res.data)

    const enrollments = Array.isArray(res.data?.data)
      ? res.data.data
      : Array.isArray(res.data)
        ? res.data
        : []

    const matched = enrollments.find(
      item => Number(item.materialLotId ?? item.courseId) === Number(course.value.id)
    )

    if (!matched) {
      enrollmentStatus.value = 'NONE'
      return
    }

    enrollmentStatus.value = matched.status === 'ACTIVE' ? 'ACTIVE' : 'PENDING'
  } catch (e) {
    console.error('[CourseDetail] failed to load enrollment status:', e)
    enrollmentStatus.value = 'NONE'
  }
}

async function handlePrimaryAction() {
  enrollError.value = ''
  decisionError.value = ''

  if (!course.value?.id) {
    enrollError.value = '원료 정보가 올바르지 않습니다.'
    return
  }

  if (isSupplier.value) {
    enrollError.value = '공급기업 계정은 본인이 등록한 원료를 구매 신청할 수 없습니다.'
    return
  }

  if (isIntermediary.value) {
    if (course.value.status !== 'PENDING') return

    deciding.value = true
    try {
      await courseApi.decideApproval(course.value.id, { decision: 'APPROVED' })
      router.push('/courses')
    } catch (e) {
      console.error('[CourseDetail] approval failed:', e)
      decisionError.value = e.response?.data?.message || '승인 처리에 실패했습니다.'
    } finally {
      deciding.value = false
    }
    return
  }

  if (enrollmentStatus.value === 'ACTIVE') {
    router.push('/enrollments')
    return
  }

  if (enrollmentStatus.value === 'PENDING') {
    return
  }

  enrolling.value = true

  try {
    await enrollmentApi.enroll(course.value.id)
    enrollmentStatus.value = 'PENDING'
  } catch (e) {
    console.error('[CourseDetail] enroll failed:', e)
    enrollError.value = e.response?.data?.message || '계약/구매 신청에 실패했습니다.'
  } finally {
    enrolling.value = false
  }
}

async function handleReject() {
  if (!course.value?.id) return

  decisionError.value = ''
  if (!rejectReason.value.trim()) {
    decisionError.value = '거절 사유를 입력해 주세요.'
    return
  }

  deciding.value = true
  try {
    await courseApi.decideApproval(course.value.id, {
      decision: 'REJECTED',
      rejectionReason: rejectReason.value.trim()
    })
    router.push('/courses')
  } catch (e) {
    console.error('[CourseDetail] rejection failed:', e)
    decisionError.value = e.response?.data?.message || '거절 처리에 실패했습니다.'
  } finally {
    deciding.value = false
  }
}

// 중간기업은 소유자가 아니어도 승인 대기 목록을 통해서만 타사 로트를 조회할 수 있다
// (GET /courses/{id} 는 본인 소유 로트가 아니면 APPROVED 상태만 노출한다)
async function loadCourseForReview(id) {
  courseStore.loading = true
  try {
    const res = await courseApi.getPendingApprovals()
    const list = Array.isArray(res.data?.data) ? res.data.data : Array.isArray(res.data) ? res.data : []
    const found = list.find(c => Number(c.id) === Number(id))
    courseStore.selectedCourse = found ? courseStore.normalizeCourse(found) : null
  } catch (e) {
    console.error('[CourseDetail] failed to load course for review:', e)
    courseStore.selectedCourse = null
  } finally {
    courseStore.loading = false
  }
}

onMounted(async () => {
  if (isIntermediary.value) {
    await loadCourseForReview(route.params.id)
  } else {
    await courseStore.fetchCourse(route.params.id)
  }
  console.log('[CourseDetail] selectedCourse =', courseStore.selectedCourse)
  await loadEnrollmentStatus()
})

watch(
  () => courseStore.selectedCourse,
  async (value) => {
    console.log('[CourseDetail] selectedCourse changed =', value)
    if (value?.id) {
      await loadEnrollmentStatus()
    }
  },
  { deep: true }
)
</script>

<style scoped>
.page-wrapper {
  min-height: 100vh;
  background: var(--color-bg-secondary);
}

.detail-hero {
  background: linear-gradient(135deg, var(--color-bg-tertiary) 0%, var(--color-bg-secondary) 100%);
  border-bottom: 1px solid var(--color-border);
  padding: 48px 0;
}

.detail-hero-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 48px;
  align-items: start;
}

.detail-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.badge-row {
  display: flex;
  gap: 8px;
}

.rejection-reason {
  font-size: 13px;
  color: var(--color-brand-error);
  background: rgba(220, 38, 38, 0.08);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  width: fit-content;
}

.component-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.component-chip {
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-full);
  padding: 4px 12px;
}

.detail-title {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.3;
}

.detail-desc {
  font-size: 15px;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.detail-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
  flex-wrap: wrap;
}

.enroll-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.enroll-thumb {
  height: 190px;
}

.thumb-icon {
  width: 64px;
  height: 64px;
}

.enroll-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.enroll-price {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-brand-green-deep);
}

.btn-full {
  width: 100%;
  padding: 13px;
  font-size: 15px;
  justify-content: center;
}

.btn-disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.enroll-info-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.enroll-info-list li {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.error-msg {
  font-size: 13px;
  color: var(--color-brand-error);
  padding: 8px 12px;
  background: rgba(220, 38, 38, 0.08);
  border-radius: var(--radius-sm);
}

.reject-block {
  display: flex;
  flex-direction: column;
}

.text-btn-reject {
  align-self: center;
  background: none;
  border: none;
  color: var(--color-brand-error);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 4px;
  text-decoration: underline;
}

.reject-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reject-textarea {
  padding: 10px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-family: var(--font-sans);
  color: var(--color-text-primary);
  background: var(--color-bg-primary);
  resize: vertical;
  outline: none;
}

.reject-textarea:focus {
  border-color: var(--color-brand-error);
}

.reject-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.reject-confirm-btn {
  background: var(--color-brand-error);
  color: #fff;
}

.reject-confirm-btn:hover {
  opacity: 0.9;
}

.reject-confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.helper-text {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.empty-text {
  font-size: 14px;
  color: var(--color-text-muted);
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 100px 0;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-brand-green);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .detail-hero-inner {
    grid-template-columns: 1fr;
  }
}
</style>