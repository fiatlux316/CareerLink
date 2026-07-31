<script setup lang="ts">
import { RouterView, RouterLink } from 'vue-router'
</script>

<template>
  <div class="app-container">
    <!-- 전역 상단 커리어링크 배너 -->
    <header class="global-banner">
      <div class="global-banner__bg"></div>
      <div class="global-banner__overlay"></div>
      <div class="global-banner__container">
        <RouterLink to="/" class="global-banner__brand">
          <span class="brand-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="brand-svg">
              <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
              <path d="M6 12v5c3 3 9 3 12 0v-5"/>
            </svg>
          </span>
          <span class="brand-text">CareerLink</span>
        </RouterLink>
        <span class="global-banner__tagline">청소년 맞춤 진로 & 학과 상담 플랫폼</span>
      </div>
    </header>

    <main class="app-main">
      <section class="app-main__content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 전역 상단 배너 */
.global-banner {
  position: relative;
  width: 100%;
  height: 3.5rem;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(15, 23, 42, 0.12);
  z-index: 100;
}

/* 학생 상담 배경 이미지 (희미하게 적용) */
.global-banner__bg {
  position: absolute;
  inset: 0;
  background-image: url('/banner-bg.png');
  background-size: cover;
  background-position: center 25%;
  filter: blur(1px) contrast(1.05);
  opacity: 0.38;
}

/* 희미하고 은은한 블루/다크 오버레이 */
.global-banner__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    rgba(15, 23, 42, 0.88) 0%,
    rgba(30, 58, 138, 0.78) 45%,
    rgba(15, 23, 42, 0.85) 100%
  );
}

.global-banner__container {
  position: relative;
  max-width: var(--page-max-width, 72rem);
  height: 100%;
  margin: 0 auto;
  padding: 0 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.global-banner__brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
  transition: transform 0.2s ease;
}

.global-banner__brand:hover {
  transform: translateY(-1px);
}

.brand-svg {
  width: 1.5rem;
  height: 1.5rem;
  color: #60a5fa;
}

.brand-text {
  font-size: 1.375rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #ffffff 0%, #93c5fd 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.global-banner__tagline {
  font-size: 0.8125rem;
  font-weight: 500;
  color: rgba(226, 232, 240, 0.85);
  letter-spacing: -0.01em;
}

@media (max-width: 640px) {
  .global-banner {
    height: 3rem;
  }
  .brand-text {
    font-size: 1.15rem;
  }
  .global-banner__tagline {
    font-size: 0.75rem;
  }
}

.app-main {
  flex: 1;
}
</style>
