import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const DashboardView = () => import('../views/DashboardView.vue')
const StoreDashboardView = () => import('../views/StoreDashboardView.vue')
const PagePlaceholder = () => import('../views/PagePlaceholder.vue')
const OrderCreateView = () => import('../views/hq/orders/OrderCreateView.vue')
const OrderStatusView = () => import('../views/hq/orders/OrderStatusView.vue')
const AutoOrderStatusView = () => import('../views/hq/orders/auto/AutoOrderStatusView.vue')
const AutoOrderDetailView = () => import('../views/hq/orders/auto/AutoOrderDetailView.vue')
const OrderApprovalView = () => import('../views/hq/orders/OrderApprovalView.vue')
const OrderApprovalListView = () => import('../views/hq/orders/OrderApprovalListView.vue')
const UserRegistrationView = () => import('../views/hq/user/UserRegistrationView.vue')
const UserListView = () => import('../views/hq/user/UsersListView.vue')
const StoreInventoryView = () => import('../views/store/inventory/InventoryView.vue')
const StoreInventoryDetailView = () => import('../views/store/inventory/InventoryDetailView.vue')
const LoginView = () => import('../views/LoginView.vue')
const MyView = () => import('../views/MyView.vue')
const MovementsView = () => import('../views/hq/inventory/MovementsView.vue')
const StockView = () => import('../views/hq/inventory/StockView.vue')
const FranchiseListView = () => import('../views/hq/franchise/FranchiseListView.vue')
const FranchiseStockView = () => import('../views/hq/franchise/FranchiseStockView.vue')
const FranchiseRegistrationView = () =>
  import('../views/hq/franchise/FranchiseRegistrationView.vue')
const SupplierListView = () => import('../views/hq/suppliers/SupplierListView.vue')
const SupplierDetailView = () => import('../views/hq/suppliers/SupplierDetailView.vue')
const SettlementListView = () => import('../views/hq/settlement/SettlementListView.vue')
const SettlementDetailView = () => import('../views/hq/settlement/SettlementDetailView.vue')
const SettlementReportView = () => import('../views/hq/settlement/SettlementReportView.vue')
const DailySettlementView = () => import('../views/hq/settlement/DailySettlementView.vue')
const FranchiseOrderApprovalView = () =>
  import('../views/hq/franchise/FranchiseOrderApprovalView.vue')
const FranchiseOrderApprovalDetailView = () =>
  import('../views/hq/franchise/FranchiseOrderApprovalDetailView.vue')
const FranchiseOrderListView = () => import('../views/hq/franchise/FranchiseOrderListView.vue')
const FranchiseOrderDetailView = () => import('../views/hq/franchise/FranchiseOrderDetailView.vue')
const FranchiseDeliveryView = () => import('../views/hq/franchise/FranchiseDeliveryView.vue')
const ProductListView = () => import('../views/hq/store/ProductListView.vue')
const ProductRegisterView = () => import('../views/hq/store/ProductRegisterView.vue')
const ProductDetailView = () => import('../views/hq/store/ProductDetailView.vue')
const SmartOrderListViewView = () => import('../views/hq/orders/SmartOrderListView.vue')
const SmartOrderDetailView = () => import('../views/hq/orders/SmartOrderDetailView.vue')
const DemandForecastView = () => import('../views/hq/dashboard/DemandForecastView.vue')
const StoreOrderCreateView = () => import('../views/store/purchase/StoreOrderCreateView.vue')
const StoreOrderListView = () => import('../views/store/purchase/StoreOrderListView.vue')
const StoreOrderDetailView = () => import('../views/store/purchase/StoreOrderDetailView.vue')
const StoreOrderDashboardView = () => import('../views/hq/dashboard/StoreOrderDashboardView.vue')
const SystemHomeView = () => import('../views/system/SystemHomeView.vue')

const hqRoutes = [
  {
    path: '/hq/dashboard',
    name: 'hq-dashboard',
    component: DashboardView,
    meta: { title: 'HQ 대시보드' },
  },
  {
    path: '/hq/orders/create',
    name: 'hq-orders-create',
    component: OrderCreateView,
    meta: { title: '발주서 생성' },
  },
  {
    path: '/hq/orders/status',
    name: 'hq-orders-status',
    component: OrderStatusView,
    meta: { title: '일반 발주 현황' },
  },
  {
    path: '/hq/orders/auto/status',
    name: 'hq-orders-auto-status',
    component: AutoOrderStatusView,
    meta: { title: '자동 발주 현황' },
  },
  {
    path: '/hq/orders/auto/status/:purchaseId',
    name: 'hq-orders-auto-detail',
    component: AutoOrderDetailView,
    meta: { title: '자동 발주 상세' },
  },
  {
    path: '/hq/orders/approval',
    name: 'hq-orders-approval',
    component: OrderApprovalListView,
    meta: { title: '발주 승인' },
  },
  {
    path: '/hq/orders/approval/:id',
    name: 'hq-orders-approval-detail',
    component: OrderApprovalView,
    meta: { title: '발주 상세' },
  },
  {
    path: '/hq/orders/vendors',
    name: 'hq-orders-vendors',
    component: SupplierListView,
    meta: { title: '공급사 관리' },
  },
  {
    path: '/hq/orders/vendors/:id',
    name: 'hq-supplier-detail',
    component: SupplierDetailView,
    meta: { title: '공급사 상세' },
  },
  {
    path: '/hq/inventory/stock',
    name: 'hq-inventory-stock',
    component: StockView,
    meta: { title: '재고 상태' },
  },
  {
    path: '/hq/inventory/movements',
    name: 'hq-inventory-movements',
    component: MovementsView,
    meta: { title: '입출고 조회' },
  },
  {
    path: '/hq/product/list',
    name: 'hq-products-list',
    component: ProductListView,
    meta: { title: '상품 목록' },
  },
  {
    path: '/hq/product/register',
    name: 'hq-product-register',
    component: ProductRegisterView,
    meta: { title: '상품 등록' },
  },
  {
    path: '/hq/product/:id',
    name: 'hq-product-detail',
    component: ProductDetailView,
    meta: { title: '상품 상세' },
  },
  // {
  //   path: '/hq/settlement/daily',
  //   name: 'hq-settlement-daily',
  //   component: DailySettlementView,
  //   meta: { title: '일일 정산' },
  // },
  {
    path: '/hq/settlement/list',
    name: 'SettlementList',
    component: SettlementListView,
    meta: { title: '정산 목록' },
  },
  {
    path: '/hq/settlement/:id',
    name: 'SettlementDetail',
    component: SettlementDetailView,
    meta: { title: '정산 상세' },
  },
  {
    path: '/hq/settlement/report',
    name: 'hq-settlement-report',
    component: SettlementReportView,
    meta: { title: '정산 리포트' },
  },
  {
    path: '/hq/users/',
    name: 'hq-users-list',
    component: UserListView,
    meta: { title: '사용자 목록' },
  },
  {
    path: '/hq/users/registration',
    name: 'hq-users-registration',
    component: UserRegistrationView,
    meta: { title: '사용자 등록' },
  },
  {
    path: '/hq/franchise/registration',
    name: 'hq-franchise-registration',
    component: FranchiseRegistrationView,
    meta: { title: '가맹점 등록' },
  },
  {
    path: '/hq/franchise/stock',
    name: 'hq-franchise-stock',
    component: FranchiseStockView,
    meta: { title: '가맹점 재고 현황' },
  },
  {
    path: '/hq/franchise/approval',
    name: 'hq-franchise-approval',
    component: FranchiseOrderApprovalView,
    meta: { title: '가맹점 주문 승인' },
  },
  {
    path: '/hq/franchise/approval/:id',
    name: 'hq-franchise-approval-detail',
    component: FranchiseOrderApprovalDetailView,
    meta: { title: '가맹점 주문 상세' },
  },
  {
    path: '/hq/franchise/orders',
    name: 'hq-franchise-orders',
    component: FranchiseOrderListView,
    meta: { title: '가맹점 주문 조회' },
  },
  {
    path: '/hq/franchise/orders/:id',
    name: 'hq-franchise-order-detail',
    component: FranchiseOrderDetailView,
    meta: { title: '가맹점 주문 상세' },
  },
  {
    path: '/hq/franchise/delivery',
    name: 'hq-franchise-delivery',
    component: FranchiseDeliveryView,
    meta: { title: '배송 목록 조회' },
  },
  {
    path: '/hq/smart-orders',
    name: 'hq-smart-orders',
    component: SmartOrderListViewView,
    meta: { title: '스마트 발주 현황' },
  },
  {
    path: '/hq/smart-orders/:supplierId/:targetWeek',
    name: 'hq-smart-order-detail',
    component: SmartOrderDetailView,
    meta: { title: '스마트 발주 상세' },
    props: true,
  },
  {
    path: '/hq/dashboard/forecast',
    name: 'hq-dashboard-forecast',
    component: DemandForecastView,
    meta: { title: '수요 예측 보고서' },
  },
  {
    path: '/hq/dashboard/forecast',
    name: 'hq-forecast',
    component: DemandForecastView,
    meta: { title: '수요 예측' }
  },
  {
    path: '/hq/dashboard/order',
    name: 'hq-dashboard-order',
    component: StoreOrderDashboardView,
    meta: { title: '주문 대시보드' },
  },
]

const storeRoutes = [
  {
    path: '/store/dashboard',
    name: 'store-dashboard',
    component: StoreDashboardView,
    meta: { title: '스토어 대시보드' },
  },
  {
    path: '/store/purchase/create',
    name: 'store-purchase-create',
    component: StoreOrderCreateView,
    meta: { title: '발주 생성' },
  },
  {
    path: '/store/purchase/list',
    name: 'store-purchase-list',
    component: StoreOrderListView,
    meta: { title: '발주 목록' },
  },
  {
    path: '/store/purchase/:id',
    name: 'store-purchase-detail',
    component: StoreOrderDetailView,
    meta: { title: '발주 상세' },
  },
  {
    path: '/store/inventory/stock',
    name: 'store-inventory-stock',
    component: StoreInventoryView,
    meta: { title: '재고 조회' },
  },
  {
    path: '/store/inventory/stock/:id',
    name: 'store-inventory-stock-detail',
    component: StoreInventoryDetailView,
    meta: { title: '재고 조회' },
  },
  {
    path: '/store/settlement/overview',
    name: 'store-settlement-overview',
    component: PagePlaceholder,
    meta: { title: '정산 관리' },
  },
  {
  path: '/system',
  name: 'system-home',
  component: SystemHomeView,
  meta: { title: '시스템 Test', requiresAuth: true }
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView, meta: { title: '대시보드' } },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: '로그인', public: true },
    },
    { path: '/mypage', name: 'mypage', component: MyView, meta: { title: '내 정보' } },
    ...hqRoutes,
    ...storeRoutes,
    // { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // derive token and expiry values
  const token = authStore.userInfo && authStore.userInfo.accessToken
  const expiresAtRaw = authStore.userInfo && authStore.userInfo.expiresAt
  const expiresAt = expiresAtRaw ? Number(expiresAtRaw) : 0

  let loggedIn = !!(token && expiresAt > Date.now())

  // try silent refresh when we have a token but it's expired
  if (token && !loggedIn && typeof authStore.refreshAccessToken === 'function') {
    try {
      const refreshed = await authStore.refreshAccessToken()
      if (refreshed) {
        const newExpires = authStore.userInfo && Number(authStore.userInfo.expiresAt)
        loggedIn = !!(authStore.userInfo.accessToken && newExpires > Date.now())
      }
    } catch (err) {
      // refresh failed - user needs to log in again
    }
  }

  // allow explicit public route (login)
  if (to.meta && to.meta.public) {
    if (loggedIn) {
      // already logged in -> redirect to role dashboard
      const role =
        authStore.userInfo &&
        (authStore.userInfo.role ||
          authStore.userInfo.type ||
          (authStore.userInfo.roles && authStore.userInfo.roles[0]))
      // return role === 'STORE_ADMIN'
      //   ? next({ name: 'store-dashboard' })
      //   : next({ name: 'hq-dashboard' })
      if (role === 'SYSTEM') return next({ name: 'system-home' })
      if (role === 'STORE_ADMIN') return next({ name: 'store-dashboard' })
      return next({ name: 'hq-dashboard' })

    }
    return next()
  }

  // require auth for non-public pages
  if (!loggedIn) {
    return next({ name: 'login' })
  }

  const role =
    authStore.userInfo &&
    (authStore.userInfo.role ||
      authStore.userInfo.type ||
      (authStore.userInfo.roles && authStore.userInfo.roles[0]))

  // If this is a store admin, ensure storeId exists; if not, force re-login
  const rawStoreId =
    authStore.userInfo && authStore.userInfo.storeId
      ? authStore.userInfo.storeId
      : localStorage.getItem('storeId')
  const storeId = rawStoreId == null ? '' : String(rawStoreId).trim()
  if (role === 'STORE_ADMIN' && (!storeId || storeId === 'null')) {
    // clear auth and redirect to login to recover a valid state
    try {
      authStore.logout()
    } catch (e) {
      // ignore
    }
    return next({ name: 'login' })
  }

  // redirect root/dashboard to role-specific dashboard
  if (to.path === '/' || to.name === 'dashboard') {
    if (role === 'SYSTEM') return next({ name: 'system-home' })
    if (role === 'STORE_ADMIN') return next({ name: 'store-dashboard' })
    return next({ name: 'hq-dashboard' })
  }

  if (role === 'SYSTEM' && (to.path.startsWith('/hq') || to.path.startsWith('/store'))) {
  return next({ name: 'system-home' })  
  }

  // enforce broad route namespace separation
  if (role === 'STORE_ADMIN' && to.path.startsWith('/hq')) {
    return next({ name: 'store-dashboard' })
  }

  if (role !== 'STORE_ADMIN' && to.path.startsWith('/store')) {
    return next({ name: 'hq-dashboard' })
  }

  return next()
})
export default router
