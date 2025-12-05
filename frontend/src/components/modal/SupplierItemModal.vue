<template>
  <BaseItemModal
    title="품목 추가"
    :fetchProductsFn="fetchSupplierProducts"
    :additionalFilters="{ supplierId: props.initialSupplierId }"
    :showPrice="true"
    priceLabel="단가"
    :showStock="true"
    :showLeadTime="true"
    @close="emit('close')"
    @add="(items) => emit('add', items)"
  />
</template>

<script setup>
import apiClient from '../api'
import BaseItemModal from './BaseItemModal.vue'
import { getSupplierDetail } from '@/components/api/supplier/supplierService.js'

const emit = defineEmits(['close', 'add'])

const props = defineProps({
  initialSupplierId: {
    type: [String, Number],
    default: null,
  },
})

/**
 * 공급사 기반 상품 목록 조회 함수
 * 수정 전 OrderItemModal의 동작과 동일
 */
async function fetchSupplierProducts(params) {
  const { supplierId, largeCategoryId, mediumCategoryId, smallCategoryId, keyword } = params

  if (supplierId) {
    // 공급사 상세 조회로 품목 가져오기
    const detail = await getSupplierDetail(supplierId)
    let products = detail.products || []

    // 클라이언트 사이드 필터링 (카테고리, 키워드)
    products = products.filter((item) => {
      // 키워드 필터링 (SKU 또는 상품명)
      if (keyword) {
        const lowerKeyword = keyword.toLowerCase()
        const matchSku = (item.productCode || item.sku || '')?.toLowerCase().includes(lowerKeyword)
        const matchName = (item.productName || item.name || '')
          ?.toLowerCase()
          .includes(lowerKeyword)
        if (!matchSku && !matchName) return false
      }

      // 카테고리 필터링
      if (largeCategoryId && item.largeCategoryId !== largeCategoryId) return false
      if (mediumCategoryId && item.mediumCategoryId !== mediumCategoryId) return false
      if (smallCategoryId && item.smallCategoryId !== smallCategoryId) return false

      return true
    })

    return products
  } else {
    // 공급사 미지정 시 기존 제품 목록 API 사용
    const res = await apiClient
      .get('/api/v1/products', {
        params: {
          page: 1,
          numOfRows: 100,
          largeCategoryId,
          mediumCategoryId,
          smallCategoryId,
          keyword: keyword || undefined,
        },
      })
      .then((r) => r.data)

    return res.items?.[0]?.products || res.products || res.items || []
  }
}
</script>
