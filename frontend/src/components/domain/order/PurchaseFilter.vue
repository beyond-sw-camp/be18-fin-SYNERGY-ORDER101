<template>
    <div class="purchase-filter-container">
        <div class="filter-row">
            <FilterDropdown label="상태" :options="statusOptions" v-model="filters.status" />

            <FilterDropdown v-if="showTypeFilter" label="타입" :options="typeOptions" v-model="filters.orderType" />

            <FilterDropdown v-if="showVendorFilter" label="공급업체" :options="vendorOptions" v-model="filters.vendorId" :searchMode="true"
                placeholder="전체" @triggerSearchModal="openVendorSearchModal" />

            <FilterDateRange label="날짜 범위" v-model:startDate="filters.startDate" v-model:endDate="filters.endDate" />

            <div class="search-group">
                <FilterSearchInput label="검색" placeholder="PO 번호, 요청자 검색..." v-model="filters.keyword" />
                <div class="button-actions">
                    <button class="btn-search" @click="applyFilters">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <circle cx="11" cy="11" r="8" stroke-width="2" />
                            <path d="m21 21-4.35-4.35" stroke-width="2" stroke-linecap="round" />
                        </svg>
                        검색
                    </button>
                    <button class="btn-reset" @click="resetFilters">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                            <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"
                                stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                        </svg>
                        초기화
                    </button>
                </div>
            </div>
        </div>

        <VendorSearchModal v-if="isVendorModalOpen" :currentValue="filters.vendorId" currentType="SUPPLIER"
            @close="isVendorModalOpen = false" @select="handleVendorSelect" />
    </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import FilterDropdown from '../../base/FilterDropdown.vue';
import FilterDateRange from '../../base/FilterDateRange.vue';
import FilterSearchInput from '../../base/FilterSearchInput.vue';
import { getPastDateString } from '@/components/global/Date';
import VendorSearchModal from '@/components/modal/VenderSearchModal.vue';
import { purchaseStatusOptions, purchaseTypeOptions } from '@/components/api/purchase/purchaseService';

const props = defineProps({
    showTypeFilter: {
        type: Boolean,
        default: false
    },
    showVendorFilter: {
        type: Boolean,
        default: true
    }
});

const initialFilters = {
    status: 'ALL',
    orderType: 'ALL',
    vendorId: 'ALL',
    vendorName: '전체',
    startDate: getPastDateString(30),
    endDate: new Date().toISOString().slice(0, 10),
    keyword: '',
};

const filters = ref({ ...initialFilters });
const isVendorModalOpen = ref(false);

// 상태 옵션
const statusOptions = purchaseStatusOptions();

// 타입 옵션
const typeOptions = purchaseTypeOptions();

const vendorOptions = ref([
    { text: '전체', value: 'ALL' },
]);

const emit = defineEmits(['search']);

function openVendorSearchModal() {
    isVendorModalOpen.value = true;
}

/**
 * 모달에서 선택한 공급업체 처리
 */
function handleVendorSelect(vendor) {
    const { id, name } = vendor;

    filters.value.vendorId = id;
    filters.value.vendorName = name;

    // vendorOptions에 동적으로 추가 (중복 방지)
    const exists = vendorOptions.value.find(opt => opt.value === id);
    if (!exists && id !== 'ALL') {
        vendorOptions.value.push({
            text: `[공급업체] ${name}`,
            value: id
        });
    }

    isVendorModalOpen.value = false;
}

function applyFilters() {
    console.log('🔍 발주 필터 적용:', filters.value);
    emit('search', filters.value);
}

function resetFilters() {
    console.log('🔄 발주 필터 초기화');
    filters.value = { ...initialFilters };
    vendorOptions.value = [{ text: '전체', value: 'ALL' }];
    applyFilters();
}
</script>

<style scoped>
.purchase-filter-container {
    padding: 15px;
}

.filter-row {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: 20px;
}

.search-group {
    display: flex;
    align-items: flex-end;
    gap: 12px;
}

.button-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-bottom: 0;
}

:deep(select),
:deep(input[type="date"]),
:deep(input[type="text"]),
.btn-search,
.btn-reset {
    height: 38px;
}

.btn-search,
.btn-reset {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 8px 18px;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
}

.btn-search svg,
.btn-reset svg {
    width: 14px;
    height: 14px;
}

.btn-search {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    color: white;
    border: none;
    box-shadow: 0 2px 4px rgba(99, 102, 241, 0.2);
}

.btn-search:hover {
    background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
    box-shadow: 0 4px 8px rgba(99, 102, 241, 0.3);
    transform: translateY(-1px);
}

.btn-search:active {
    transform: translateY(0);
}

.btn-reset {
    background-color: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
}

.btn-reset:hover {
    background-color: #f8fafc;
    border-color: #cbd5e1;
    color: #475569;
}
</style>
