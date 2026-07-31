import { createRouter, createWebHistory } from 'vue-router'

import AdminView from '../views/AdminView.vue'
import CounselorView from '../views/CounselorView.vue'
import CounselorDetailView from '../views/CounselorDetailView.vue'
import StudentReceiveView from '../views/StudentReceiveView.vue'
import StudentHomeView from '../views/StudentHomeView.vue'
import StudentStatusView from '../views/StudentStatusView.vue'
import StudentConsultationListView from '../views/StudentConsultationListView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/receive',
    },
    {
      path: '/receive',
      name: 'student-receive',
      component: StudentReceiveView,
    },
    {
      path: '/home',
      name: 'student-home',
      component: StudentHomeView,
    },
    {
      path: '/status',
      name: 'student-consultation-list',
      component: StudentConsultationListView,
    },
    {
      path: '/status/:id',
      name: 'student-status',
      component: StudentStatusView,
      props: true,
    },
    {
      path: '/counselor',
      name: 'counselor',
      component: CounselorView,
    },
    {
      path: '/counselor/status/:id',
      name: 'counselor-status',
      component: CounselorDetailView,
      props: true,
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminView,
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
