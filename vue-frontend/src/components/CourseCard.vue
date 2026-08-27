<template>
  <router-link :to="`/courses/${course.id}`" class="course-card">
    <!-- 썸네일 -->
    <div class="card-thumb thumb-industrial">
      <MaterialIcon :category="course.category" class="thumb-icon" />
    </div>

    <!-- 내용 -->
    <div class="card-body">
      <span class="badge badge-accent">{{ categoryLabel }}</span>
      <h3 class="card-title">{{ course.title }}</h3>
      <div class="card-meta">
        <span class="instructor">{{ course.supplierName ?? course.instructorName }}</span>
        <span class="price">₩{{ Number(course.price).toLocaleString() }}</span>
      </div>
      <div class="card-footer">
        <span class="enrolled">계약 {{ (course.contractCount ?? course.enrollmentCount)?.toLocaleString() }}건</span>
      </div>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import MaterialIcon from '@/components/MaterialIcon.vue'
import { getCategoryLabel } from '@/constants/category.js'

const props = defineProps({
  course: { type: Object, required: true }
})

const categoryLabel = computed(() => getCategoryLabel(props.course.category))
</script>

<style scoped>
.course-card {
  display: flex;
  flex-direction: column;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: var(--transition);
  cursor: pointer;
}
.course-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-border-hover);
}
.card-thumb {
  height: 168px;
}
.thumb-icon {
  width: 56px;
  height: 56px;
}
.card-body {
  padding: 18px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 9px;
  flex: 1;
}
.card-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.4;
}
.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.instructor {
  font-size: 14px;
  color: var(--color-text-secondary);
}
.price {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.card-footer {
  margin-top: 2px;
}
.enrolled {
  font-size: 13px;
  color: var(--color-text-muted);
}
</style>
