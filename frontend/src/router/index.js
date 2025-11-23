import { createRouter, createWebHistory } from 'vue-router'

const DashboardView = () => import('../views/DashboardView.vue')
const PagePlaceholder = () => import('../views/PagePlaceholder.vue')
const OrderCreateView = () => import('../views/hq/orders/OrderCreateView.vue')
const OrderStatusView = () => import('../views/hq/orders/OrderStatusView.vue')
const OrderApprovalView = () => import('../views/hq/orders/OrderApprovalView.vue')
const OrderApprovalListView = () => import('../views/hq/orders/OrderApprovalListView.vue')
const UserRegistrationView = () => import('../views/hq/user/UserRegistrationView.vue')
const UserListView = () => import('../views/hq/user/UsersListView.vue')
const StoreInventoryView = () => import('../views/store/inventory/InventoryView.vue')
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
const DailySettlementView = () => import('../views/hq/settlement/DailySettlementView.vue')
const FranchiseOrderApprovalView = () =>
  import('../views/hq/franchise/FranchiseOrderApprovalView.vue')
const FranchiseOrderListView = () => import('../views/hq/franchise/FranchiseOrderListView.vue')
const FranchiseOrderDetailView = () => import('../views/hq/franchise/FranchiseOrderDetailView.vue')
const FranchiseDeliveryView = () => import('../views/hq/franchise/FranchiseDeliveryView.vue')
const ProductListView = () => import('../views/hq/store/ProductListView.vue')
const ProductRegisterView = () => import('../views/hq/store/ProductRegisterView.vue')
const ProductDetailView = () => import('../views/hq/store/ProductDetailView.vue')

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
    meta: { title: '구매 주문 생성' },
  },
  {
    path: '/hq/orders/status',
    name: 'hq-orders-status',
    component: OrderStatusView,
    meta: { title: '주문 현황' },
  },
  {
    path: '/hq/orders/approval',
    name: 'hq-orders-approval',
    component: OrderApprovalListView,
    meta: { title: '주문 승인' },
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
  {
    path: '/hq/inventory/stock',
    name: 'hq-inventory-stock',
    component: PagePlaceholder,
    meta: { title: '상품 목록' },
  },
  {
    path: '/hq/settlement/daily',
    name: 'hq-settlement-daily',
    component: DailySettlementView,
    meta: { title: '일일 정산' },
  },
  {
    path: '/hq/settlement/list',
    name: 'hq-settlement-list',
    component: SettlementListView,
    meta: { title: '정산 목록' },
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
    path: '/hq/franchise/list',
    name: 'hq-franchise-list',
    component: FranchiseListView,
    meta: { title: '가맹점 목록' },
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
    component: OrderApprovalView,
    meta: { title: '가맹점 발주 상세' },
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
    meta: { title: '배송 관리' },
  },
]

const storeRoutes = [
  {
    path: '/store/dashboard',
    name: 'store-dashboard',
    component: PagePlaceholder,
    meta: { title: '스토어 대시보드' },
  },
  {
    path: '/store/purchase/create',
    name: 'store-purchase-create',
    component: PagePlaceholder,
    meta: { title: '발주 생성' },
  },
  {
    path: '/store/inventory/stock',
    name: 'store-inventory-stock',
    component: StoreInventoryView,
    meta: { title: '재고 조회' },
  },
  {
    path: '/store/settlement/overview',
    name: 'store-settlement-overview',
    component: PagePlaceholder,
    meta: { title: '정산 관리' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView, meta: { title: '대시보드' } },
    { path: '/login', name: 'login', component: LoginView, meta: { title: '로그인' } },
    { path: '/mypage', name: 'mypage', component: MyView, meta: { title: '내 정보' } },
    ...hqRoutes,
    ...storeRoutes,
    // { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

export default router
