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
            <span class="si-icon">🔩</span> 산업 부산물 목록
          </router-link>

          <router-link
            v-if="!isSupplier"
            to="/enrollments"
            class="sidebar-item"
          >
            <span class="si-icon">✅</span> 내 구매 목록
          </router-link>

          <router-link
            to="/mypage"
            class="sidebar-item"
          >
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
            <h1 class="page-title">산업 부산물 목록</h1>
            <p class="page-subtitle" v-if="isSupplier">
              공급기업 계정으로 등록한 원료를 확인하고 새 원료를 추가할 수 있습니다.
            </p>
            <p class="page-subtitle" v-else-if="isIntermediary">
              검토 대기 중인 원료를 확인하고 승인 여부를 결정할 수 있습니다.
            </p>
          </div>

          <router-link
            v-if="isSupplier"
            to="/courses/new"
            class="btn btn-primary create-course-btn"
          >
            원료 등록
          </router-link>
        </div>

        <!-- 필터 -->
        <div class="filter-bar">
          <button
            v-for="cat in categories"
            :key="cat"
            :class="['filter-chip', { active: selectedCategory === cat }]"
            @click="selectCategory(cat)"
          >
            {{ cat }}
          </button>
        </div>

        <!-- 로딩 -->
        <div v-if="loading" class="loading-grid">
          <div v-for="i in 6" :key="i" class="skeleton-card">
            <div class="skeleton-thumb"></div>
            <div class="skeleton-body">
              <div class="skeleton-line short"></div>
              <div class="skeleton-line"></div>
              <div class="skeleton-line medium"></div>
            </div>
          </div>
        </div>

        <!-- 원료 그리드 -->
        <div v-else-if="filteredCourses.length" class="course-grid fade-in">
          <CourseCard
            v-for="course in filteredCourses"
            :key="course.id"
            :course="course"
          />
        </div>

        <!-- 빈 상태 -->
        <div v-else class="empty-state">
          <p v-if="isIntermediary">검토 대기 중인 원료가 없습니다.</p>
          <p v-else>해당 카테고리의 원료가 없습니다.</p>

          <router-link
            v-if="isSupplier"
            to="/courses/new"
            class="btn btn-primary empty-action-btn"
          >
            첫 원료 등록하기
          </router-link>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import CourseCard from '@/components/CourseCard.vue'
import { useCourseStore } from '@/store/course.js'
import { useAuthStore } from '@/store/auth.js'
import { courseApi } from '@/api/course.js'

const router = useRouter()
const courseStore = useCourseStore()
const auth = useAuthStore()

const { categories, loading } = courseStore

const selectedCategory = computed(() => courseStore.selectedCategory)
// role(INSTRUCTOR)은 공급기업/중간기업이 공유하므로 companyType으로 구분한다
const isSupplier = computed(() => auth.user?.companyType === 'SUPPLIER')
const isIntermediary = computed(() => auth.user?.companyType === 'INTERMEDIARY')

const filteredCourses = computed(() => {
  if (!Array.isArray(courseStore.courses)) return []
  if (selectedCategory.value === '전체') return courseStore.courses
  return courseStore.courses.filter(c => c.category === selectedCategory.value)
})

function selectCategory(cat) {
  courseStore.setCategory(cat)
}

function handleLogout() {
  auth.logout()
  router.push('/')
}

// 중간기업은 승인 대기 로트만, 공급기업은 공개 목록 + 본인 로트(상태 무관)를 함께 본다
async function loadForRole() {
  if (isIntermediary.value) {
    courseStore.loading = true
    try {
      const res = await courseApi.getPendingApprovals()
      const list = Array.isArray(res.data?.data) ? res.data.data : Array.isArray(res.data) ? res.data : []
      courseStore.courses = list.map(courseStore.normalizeCourse)
    } catch (e) {
      console.error('[CourseList] failed to load pending approvals:', e)
      courseStore.courses = []
    } finally {
      courseStore.loading = false
    }
    return
  }

  await courseStore.fetchCourses()

  if (isSupplier.value) {
    try {
      const res = await courseApi.getMyCourses()
      const mine = Array.isArray(res.data?.data) ? res.data.data : Array.isArray(res.data) ? res.data : []
      const existingIds = new Set(courseStore.courses.map(c => c.id))
      const onlyNew = mine.map(courseStore.normalizeCourse).filter(c => !existingIds.has(c.id))
      courseStore.courses = [...courseStore.courses, ...onlyNew]
    } catch (e) {
      console.error('[CourseList] failed to merge my courses:', e)
    }
  }
}

onMounted(() => {
  loadForRole()
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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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

.create-course-btn {
  white-space: nowrap;
  text-decoration: none;
}

/* 필터 */
.filter-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.filter-chip {
  padding: 7px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  border: 1.5px solid var(--color-border);
  background: var(--color-bg-primary);
  color: var(--color-text-secondary);
  transition: var(--transition);
  cursor: pointer;
}

.filter-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.filter-chip.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

/* 강의 그리드 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

/* 스켈레톤 */
.loading-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.skeleton-card {
  background: var(--color-bg-primary);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.skeleton-thumb {
  height: 120px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-body {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-line {
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line.short {
  width: 40%;
}

.skeleton-line.medium {
  width: 70%;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}

/* 빈 상태 */
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--color-text-muted);
  font-size: 15px;
}

.empty-action-btn {
  display: inline-flex;
  margin-top: 16px;
  text-decoration: none;
}

@media (max-width: 992px) {
  .page-layout {
    grid-template-columns: 1fr;
  }

  .course-grid,
  .loading-grid {
    grid-template-columns: 1fr;
  }

  .content-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>