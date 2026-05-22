import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/index.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('../views/SysUser.vue'),
        meta: { title: '运维账号管理' }
      },
      {
        path: 'faq',
        name: 'FaqManage',
        component: () => import('../views/OpsFaq.vue'),
        meta: { title: '知识库管理' }
      },
      {
        path: 'ticket',
        name: 'TicketManage',
        component: () => import('../views/OpsTicket.vue'),
        meta: { title: '工单管理' }
      },
      {
        path: 'chat',
        name: 'ChatPage',
        component: () => import('../views/Chat.vue'),
        meta: { title: '智能问答' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
