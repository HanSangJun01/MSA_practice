<template>
  <div class="page-wrapper">
    <AppHeader />
    <div class="page-layout">
      <aside class="sidebar">
        <div class="sidebar-section">
          <div class="sidebar-label">메뉴</div>

          <router-link to="/courses" class="sidebar-item">
            <span class="si-icon">📚</span> 원료 목록
          </router-link>

          <router-link
            v-if="!isInstructor"
            to="/enrollments"
            class="sidebar-item active"
          >
            <span class="si-icon">✅</span> 내 구매 목록
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

      <main class="main-content">
        <h1 class="page-title">내 구매 목록</h1>

        <div v-if="loading" class="loading-center">
          <div class="spinner"></div>
        </div>

        <div v-else-if="enrollments.length" class="enrollment-list fade-in">
          <div v-for="item in enrollments" :key="item.id" class="enrollment-card">
            <div class="enroll-thumb" :class="getThumbBg(item.course?.category)">
              <img :src="getThumbSrc(item.course)" :alt="item.course?.title" />
            </div>

            <div class="enroll-info">
              <span class="badge" :class="getBadge(item.course?.category)">
                {{ item.course?.category }}
              </span>
              <h3 class="enroll-title">{{ item.course?.title }}</h3>
              <p class="enroll-instructor">공급기업: {{ item.course?.instructorName }}</p>
            </div>

            <div class="enroll-status">
              <span
                :class="[
                  'status-badge',
                  item.status === 'ACTIVE' ? 'status-active' : 'status-pending'
                ]"
              >
                {{ item.status === 'ACTIVE' ? '계약 완료' : '계약 대기' }}
              </span>
              <router-link :to="`/courses/${item.courseId}`" class="btn btn-ghost btn-sm">
                원료 보기
              </router-link>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <p class="empty-icon">📭</p>
          <p>구매한 원료가 없습니다.</p>
          <router-link to="/courses" class="btn btn-primary" style="margin-top:16px;">
            원료 둘러보기
          </router-link>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { enrollmentApi } from '@/api/enrollment.js'
import { useAuthStore } from '@/store/auth.js'

const router = useRouter()
const auth = useAuthStore()

const enrollments = ref([])
const loading = ref(true)

const isInstructor = computed(() => auth.user?.role === 'INSTRUCTOR')

const categoryConfig = {
  '폐건전지': { bg: 'thumb-blue', badge: 'badge-blue', thumb: 'kubernetes' },
  '폐수슬러지': { bg: 'thumb-gray', badge: 'badge-gray', thumb: 'docker' },
  '제철슬래그': { bg: 'thumb-orange', badge: 'badge-orange', thumb: 'spring_boot' },
  '폐합성수지': { bg: 'thumb-amber', badge: 'badge-amber', thumb: 'python' },
  '스크랩금속': { bg: 'thumb-blue', badge: 'badge-blue', thumb: 'vue_js' },
  '식품부산물': { bg: 'thumb-green', badge: 'badge-green', thumb: 'generative_ai' },
}

function getThumbBg(cat) {
  return categoryConfig[cat]?.bg || 'thumb-gray'
}

function getBadge(cat) {
  return categoryConfig[cat]?.badge || 'badge-gray'
}

function getThumbSrc(course) {
  const key = course?.thumbnail || categoryConfig[course?.category]?.thumb
  if (!key) return ''
  try {
    return new URL(`../assets/images/courses/${key}.png`, import.meta.url).href
  } catch {
    return ''
  }
}

function handleLogout() {
  auth.logout()
  router.push('/')
}

onMounted(async () => {
  // 공급기업은 이 페이지 접근 불가 → 마이페이지로 이동
  if (isInstructor.value) {
    console.warn('[EnrollmentView] instructor tried to access /enrollments, redirect to /mypage')
    router.replace('/mypage')
    return
  }

  try {
    const res = await enrollmentApi.getMyEnrollments()
    console.log('[EnrollmentView] my enrollments response:', res.data)

    if (Array.isArray(res.data?.data)) {
      enrollments.value = res.data.data
    } else if (Array.isArray(res.data)) {
      enrollments.value = res.data
    } else {
      enrollments.value = []
    }
  } catch (error) {
    console.error('[EnrollmentView] failed to load enrollments:', error)
    enrollments.value = []
  } finally {
    loading.value = false
  }
})
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

.main-content {
  min-width: 0;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 24px;
}

.enrollment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.enrollment-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px;
  transition: var(--transition);
}

.enrollment-card:hover {
  box-shadow: var(--shadow-sm);
}

.enroll-thumb {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.enroll-thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 8px;
}

.thumb-green {
  background: var(--color-brand-green-soft);
}

.thumb-blue {
  background: rgba(55, 114, 207, 0.12);
}

.thumb-orange {
  background: rgba(242, 104, 60, 0.12);
}

.thumb-amber {
  background: rgba(217, 119, 6, 0.12);
}

.thumb-gray {
  background: var(--color-bg-tertiary);
}

.enroll-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.enroll-title {
  font-size: 15px;
  font-weight: 600;
}

.enroll-instructor {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.enroll-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: var(--color-brand-green-soft);
  color: var(--color-brand-green-deep);
}

.status-pending {
  background: var(--color-bg-tertiary);
  color: var(--color-text-tertiary);
}

.btn-sm {
  padding: 7px 14px;
  font-size: 13px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.spinner {
  width: 36px;
  height: 36px;
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
</style>