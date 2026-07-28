<script setup lang="ts">
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()

const navigationItems = [
  { to: '/receive', label: '학생 접수' },
  { to: '/status/sample', label: '상담 현황' },
  { to: '/counselor', label: '상담사' },
  { to: '/admin', label: '관리자' },
]
</script>

<template>
  <div>
    <main class="app-main">
      <section class="app-main__content">
        <RouterView />
      </section>
    </main>
  </div>
</template>
