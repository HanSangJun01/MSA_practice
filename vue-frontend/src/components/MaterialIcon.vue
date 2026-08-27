<template>
  <svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <template v-if="code === 'BATTERY'">
      <rect x="6" y="14" width="30" height="20" rx="3" />
      <rect x="37" y="20" width="4" height="8" rx="1.2" fill="currentColor" stroke="none" />
      <line x1="14" y1="20" x2="14" y2="28" />
      <line x1="20" y1="20" x2="20" y2="28" />
      <line x1="26" y1="20" x2="26" y2="28" />
    </template>

    <template v-else-if="code === 'CHEMICAL'">
      <path d="M24 8c0 0-12 15.5-12 23.5a12 12 0 0 0 24 0C36 23.5 24 8 24 8Z" />
    </template>

    <template v-else-if="code === 'CONSTRUCTION'">
      <path d="M8 34 L18 16 L24 24 L30 12 L40 34 Z" />
      <line x1="8" y1="34" x2="40" y2="34" />
    </template>

    <template v-else-if="code === 'PLASTIC'">
      <rect x="21" y="7" width="6" height="7" rx="1" />
      <rect x="14" y="14" width="20" height="27" rx="4" />
      <line x1="14" y1="23" x2="34" y2="23" />
    </template>

    <template v-else-if="code === 'METAL'">
      <path d="M24 7l14 8v18l-14 8-14-8V15l14-8Z" />
      <circle cx="24" cy="24" r="5.5" />
    </template>

    <template v-else-if="code === 'TEXTILE'">
      <path d="M10 14 Q17 20 24 14 T38 14" />
      <path d="M10 24 Q17 30 24 24 T38 24" />
      <path d="M10 34 Q17 40 24 34 T38 34" />
    </template>

    <template v-else-if="code === 'ELECTRONIC'">
      <rect x="14" y="14" width="20" height="20" rx="2" />
      <line x1="18" y1="8" x2="18" y2="14" />
      <line x1="24" y1="8" x2="24" y2="14" />
      <line x1="30" y1="8" x2="30" y2="14" />
      <line x1="18" y1="34" x2="18" y2="40" />
      <line x1="24" y1="34" x2="24" y2="40" />
      <line x1="30" y1="34" x2="30" y2="40" />
      <line x1="8" y1="18" x2="14" y2="18" />
      <line x1="8" y1="24" x2="14" y2="24" />
      <line x1="8" y1="30" x2="14" y2="30" />
      <line x1="34" y1="18" x2="40" y2="18" />
      <line x1="34" y1="24" x2="40" y2="24" />
      <line x1="34" y1="30" x2="40" y2="30" />
    </template>

    <template v-else>
      <circle cx="24" cy="24" r="14" />
    </template>
  </svg>
</template>

<script setup>
import { computed } from 'vue'
import { getCategoryCode } from '@/constants/category.js'

const props = defineProps({
  category: { type: String, default: '' }
})

const code = computed(() => getCategoryCode(props.category))
</script>
