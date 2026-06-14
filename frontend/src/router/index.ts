import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/signup',
      name: 'signup',
      component: () => import('@/views/SignupView.vue')
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/MainLayout.vue'),
      redirect: '/main', // 루트 진입 시 메인 화면으로 자동 리다이렉션
      children: [
        {
          path: 'main',
          name: 'main',
          component: () => import('@/views/MainView.vue')
        },
        {
          path: 'board/:id?',
          name: 'board',
          component: () => import('@/views/BoardView.vue')
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue')
        },
        {
          path: 'members',
          name: 'members',
          component: () => import('@/views/MembersView.vue')
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue') // [NEW] 프로필 설정 뷰 추가
        },
        {
          path: 'admin',
          name: 'admin',
          component: () => import('@/views/AdminView.vue'), // [NEW] 어드민 관리 콘솔 뷰 추가
          beforeEnter: (to, from, next) => {
            const userRole = localStorage.getItem('userRole')
            if (userRole === 'admin' || userRole === 'moderator') {
              next()
            } else {
              next('/dashboard') // 관리자가 아니면 메인 대시보드로 격리 차단
            }
          }
        },
        {
          path: 'hr',
          redirect: '/hr/employees'
        },
        {
          path: 'hr/employees',
          name: 'hr-employees',
          component: () => import('@/views/erp/hr/EmployeeList.vue')
        },
        {
          path: 'hr/employees/:id',
          name: 'hr-employee-detail',
          component: () => import('@/views/erp/hr/EmployeeDetail.vue')
        },
        {
          path: 'hr/attendance',
          name: 'hr-attendance',
          component: () => import('@/views/erp/hr/AttendanceView.vue')
        },
        {
          path: 'hr/leaves',
          name: 'hr-leaves',
          component: () => import('@/views/erp/hr/LeaveList.vue')
        },
        {
          path: 'hr/payroll',
          name: 'hr-payroll',
          component: () => import('@/views/erp/hr/PayrollLedgerList.vue')
        },
        {
          path: 'hr/quotations',
          name: 'hr-quotations',
          component: () => import('@/views/erp/sales/QuotationList.vue')
        },
        {
          path: 'hr/quotations/:id',
          name: 'hr-quotation-detail',
          component: () => import('@/views/erp/sales/QuotationDetail.vue')
        },
        {
          path: 'hr/tax-invoices',
          name: 'hr-tax-invoices',
          component: () => import('@/views/erp/sales/TaxInvoiceList.vue')
        },
        {
          path: 'hr/tax-invoices/:id',
          name: 'hr-tax-invoice-detail',
          component: () => import('@/views/erp/sales/TaxInvoiceDetail.vue')
        },
        {
          path: 'erp/hr/payrolls/ledgers/:ledgerId',
          name: 'hr-payroll-ledger-items',
          component: () => import('@/views/erp/hr/PayrollEntryGrid.vue')
        },
        {
          path: 'erp/hr/payrolls/templates',
          name: 'hr-payroll-templates',
          component: () => import('@/views/erp/hr/SalaryTemplateManage.vue')
        },
        {
          path: 'erp/hr/payrolls/codes',
          name: 'hr-payroll-codes',
          component: () => import('@/views/erp/hr/PayrollCodeManage.vue')
        },
        {
          path: 'hr/reviews',
          name: 'hr-reviews',
          component: () => import('@/views/erp/hr/ReviewList.vue')
        },
        {
          path: 'hr/codes',
          name: 'hr-codes',
          component: () => import('@/views/erp/hr/CodeList.vue')
        },
        {
          path: 'hr/departments',
          name: 'hr-departments',
          component: () => import('@/views/erp/hr/DepartmentManage.vue')
        }
      ]
    }
  ]
})

// 10년 차 시니어 설계: 최초 앱 마운트 혹은 F5 새로고침 식별 리액티브 플래그
let isInitialLoad = true

// 10년 차 시니어 개발자 수준의 안전한 전역 네비게이션 가드 구현
router.beforeEach((to) => {
  const isAuthenticated = localStorage.getItem('isAuthenticated') === 'true' && !!localStorage.getItem('token')

  if (to.name !== 'login' && to.name !== 'signup' && !isAuthenticated) {
    // 인증되지 않은 유저가 접근할 경우 로그인으로 차단 및 우회
    return { name: 'login' }
  } else if ((to.name === 'login' || to.name === 'signup') && isAuthenticated) {
    // 이미 로그인한 상태로 로그인/회원가입 진입 시 메인 화면으로 유도
    return { name: 'main' }
  }
  
  // 🌟 [10년차 시니어 설계]: F5 키 입력에 의해 마우스 포인터를 잃고 앱이 재부팅(새로고침)되면, 기존 보드(/board/:id) 대신 메인(/main)으로 강제 이동 🌟
  if (isInitialLoad) {
    isInitialLoad = false
    if (to.name === 'board' || to.path.startsWith('/board')) {
      return { name: 'main' }
    }
  }
  
  return true
})

export default router
