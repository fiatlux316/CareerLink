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
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header__inner">
        <div>
          <p class="app-header__eyebrow">CareerLink</p>
          <h1 class="app-header__title">상담 연결 운영 화면</h1>
          <p class="app-header__description">
            학생 접수, 학생 현황 조회, 상담사 처리, 관리자 유형 관리 화면을 한 번에 사용할 수 있습니다.
          </p>
        </div>

        <nav class="app-nav" aria-label="주요 화면">
          <RouterLink
            v-for="item in navigationItems"
            :key="item.to"
            :to="item.to"
            class="app-nav__link"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
      </div>
    </header>

    <main class="app-main">
      <section class="app-main__intro card">
        <span class="badge">현재 경로</span>
        <strong>{{ route.fullPath }}</strong>
        <p>현재 경로 기준으로 실제 상담 플로우 화면이 즉시 렌더링됩니다.</p>
      </section>

      <section class="app-main__content">
        <RouterView />
      </section>
    </main>
  </div>
</template>
