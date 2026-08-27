import { defineStore } from 'pinia'
import { ref } from 'vue'
import { courseApi } from '@/api/course.js'
import { CATEGORY_LABELS, getCategoryLabel } from '@/constants/category.js'

export const useCourseStore = defineStore('course', () => {
  const courses = ref([])
  const selectedCourse = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const selectedCategory = ref('전체')

  const categories = ['전체', ...Object.values(CATEGORY_LABELS)]

  function normalizeCourse(course) {
    if (!course || typeof course !== 'object') return course

    return {
      ...course,
      category: getCategoryLabel(course.category)
    }
  }

  async function fetchCourses() {
    loading.value = true
    error.value = null

    try {
      const res = await courseApi.getAll()
      console.log('[CourseStore] fetchCourses response =', res.data)

      const rawCourses = Array.isArray(res.data?.data)
        ? res.data.data
        : Array.isArray(res.data)
          ? res.data
          : []

      courses.value = rawCourses.map(normalizeCourse)

      console.log('[CourseStore] normalized courses =', courses.value)
    } catch (e) {
      console.error('[CourseStore] fetchCourses failed:', e)
      error.value = e.message || '원료 목록을 불러오지 못했습니다.'
      courses.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchCourse(id) {
    loading.value = true
    error.value = null

    try {
      const res = await courseApi.getById(id)
      console.log('[CourseStore] fetchCourse response =', res.data)

      const rawCourse =
        res.data?.data && typeof res.data.data === 'object'
          ? res.data.data
          : res.data

      selectedCourse.value = normalizeCourse(rawCourse)

      console.log('[CourseStore] normalized selectedCourse =', selectedCourse.value)
    } catch (e) {
      console.error('[CourseStore] fetchCourse failed:', e)
      error.value = e.message || '원료 정보를 불러오지 못했습니다.'
      selectedCourse.value = null
    } finally {
      loading.value = false
    }
  }

  function setCategory(cat) {
    selectedCategory.value = cat
  }

  return {
    courses,
    selectedCourse,
    loading,
    error,
    categories,
    selectedCategory,
    normalizeCourse,
    fetchCourses,
    fetchCourse,
    setCategory
  }
})
