<template>
  <BaseItemModal
    title="창고 품목 추가"
    :fetchProductsFn="fetchWarehouseProducts"
    :showPrice="true"
    priceLabel="가격"
    :showStock="true"
    :showLeadTime="false"
    @close="emit('close')"
    @add="(items) => emit('add', items)"
  />
</template>

<script setup>
import BaseItemModal from './BaseItemModal.vue'
import { getInventoryList } from '@/components/api/warehouse/WarehouseService.js'


const emit = defineEmits(['close', 'add'])

/**
 * 창고 재고 기반 상품 목록 조회 함수
 * 창고 재고 API를 호출하여 품목 목록을 반환
 */
async function fetchWarehouseProducts(params) {
  const { largeCategoryId, mediumCategoryId, smallCategoryId, keyword } = params

  try {
    // 창고 재고 목록 조회
    const result = await getInventoryList(1, 100)
    let inventories = result.inventories || []

    // 클라이언트 사이드 필터링 (카테고리, 키워드)
    if (largeCategoryId || mediumCategoryId || smallCategoryId || keyword) {
      inventories = inventories.filter(item => {
        // 키워드 필터링 (SKU 또는 상품명)
        if (keyword) {
          const lowerKeyword = keyword.toLowerCase()
          const matchSku = item.productCode?.toLowerCase().includes(lowerKeyword)
          const matchName = item.productName?.toLowerCase().includes(lowerKeyword)
          if (!matchSku && !matchName) return false
        }

        // 카테고리 필터링 (productCategory 문자열 기반)
        // 참고: 백엔드에서 카테고리 ID 필터링이 지원되면 서버사이드로 변경 권장
        
        return true
      })
    }
    // BaseItemModal에서 사용하는 형식으로 변환
    return inventories.map(item => ({
      productId: item.productId,
      productCode: item.productCode,
      sku: item.productCode,
      productName: item.productName,
      name: item.productName,
      productCategory: item.productCategory,
      stock: item.onHandQty,
      stockQuantity: item.onHandQty,
      safetyQty: item.safetyQty,
      price: item.price, // 창고 재고에는 가격 정보가 없으므로 0 또는 별도 조회 필요
      leadTimeDays: 1
    }))
  } catch (error) {
    console.error('창고 재고 로드 실패:', error)
    throw error
  }
}
</script>
