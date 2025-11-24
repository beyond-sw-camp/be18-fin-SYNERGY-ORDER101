<template>
    <div class="settlement-filter-container">
        <div class="filter-row">
            <FilterDropdown label="범위" :options="scopeOptions" v-model="filters.scope" />

            <FilterDropdown label="가맹점/공급업체" :options="vendorOptions" v-model="filters.vendorId" :searchMode="true"
                placeholder="전체" @triggerSearchModal="openVendorSearchModal" />

            <FilterDateRange label="날짜 범위" v-model:startDate="filters.startDate" v-model:endDate="filters.endDate" />

            <FilterSearchInput label="검색" placeholder="검색어 입력..." v-model="filters.keyword" />

            <div class="button-actions">
                <button class="btn-search" @click="applyFilters">검색</button>
                <button class="btn-reset" @click="resetFilters">필터 초기화</button>
            </div>
        </div>

        <!-- scope prop 제거 -->
        <VendorSearchModal v-if="isVendorModalOpen" :currentValue="filters.vendorId" :currentType="filters.vendorType"
            @close="isVendorModalOpen = false" @select="handleVendorSelect" />
    </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import FilterDropdown from './FilterDropdown.vue';
import FilterDateRange from './FilterDateRange.vue';
import FilterSearchInput from './FilterSearchInput.vue';
import { getPastDateString } from '@/components/global/Date';
import VendorSearchModal from '@/components/modal/VenderSearchModal.vue';

const initialFilters = {
    scope: 'ALL',
    vendorId: 'ALL',
    vendorType: 'FRANCHISE', // 'FRANCHISE' | 'SUPPLIER'
    vendorName: '전체',
    startDate: getPastDateString(30),
    endDate: new Date().toISOString().slice(0, 10),
    keyword: '',
};

const filters = ref({ ...initialFilters });
const isVendorModalOpen = ref(false);

const scopeOptions = [
    { text: '전체', value: 'ALL' },
    { text: '미수금(AR)', value: 'AR' },
    { text: '미지급금(AP)', value: 'AP' },
];

const vendorOptions = ref([
    { text: '전체', value: 'ALL' },
]);

const emit = defineEmits(['search']);

function openVendorSearchModal() {
    console.log('📋 모달 열기');
    isVendorModalOpen.value = true;
}

/**
 * 모달에서 선택한 가맹점/공급사 처리
 * @param {Object} vendor - { type: 'FRANCHISE' | 'SUPPLIER', id: string, name: string, code: string }
 */
function handleVendorSelect(vendor) {
    console.log('✅ 선택된 업체:', vendor);

    const { type, id, name, code } = vendor;

    // 필터 값 업데이트
    filters.value.scope = 'ALL'; // 선택 시 전체로 변경
    filters.value.vendorId = id;
    filters.value.vendorName = name;

    // vendorOptions에 동적으로 추가 (중복 방지)
    const exists = vendorOptions.value.find(opt => opt.value === id);
    if (!exists && id !== 'ALL') {
        const typeLabel = type === 'FRANCHISE' ? '[가맹점]' : '[공급사]';
        vendorOptions.value.push({
            text: `${typeLabel} ${name}`,
            value: id
        });
    }

    // scope 자동 조정 (선택사항)
    if (type === 'FRANCHISE') {
        filters.value.scope = 'AR'; // 가맹점 → 미수금
    } else if (type === 'SUPPLIER') {
        filters.value.scope = 'AP'; // 공급사 → 미지급금
    }

    isVendorModalOpen.value = false;
}

function applyFilters() {
    console.log('🔍 필터 적용:', {
        scope: filters.value.scope,
        vendorType: filters.value.vendorType,
        vendorId: filters.value.vendorId,
        vendorName: filters.value.vendorName,
        startDate: filters.value.startDate,
        endDate: filters.value.endDate,
        keyword: filters.value.keyword
    });

    emit('search', filters.value);
}

function resetFilters() {
    console.log('🔄 필터 초기화');
    filters.value = { ...initialFilters };
    vendorOptions.value = [{ text: '전체', value: 'ALL' }];
    applyFilters();
}

// scope 변경 시 vendorId 초기화 (선택사항)
watch(() => filters.value.scope, (newScope) => {
    console.log('📊 범위 변경:', newScope);
    // scope 변경 시 업체 선택 초기화 (선택사항)
    if (newScope === 'ALL') {
        // 전체 선택 시 업체도 전체로 초기화
        filters.value.vendorId = 'ALL';
        filters.value.vendorName = '전체';
        vendorOptions.value = [{ text: '전체', value: 'ALL' }];
    } else if (newScope === 'AR') {
        // AR 선택 시 가맹점만 표시되도록 vendorType 고정
        filters.value.vendorType = 'FRANCHISE';
    } else if (newScope === 'AP') {
        // AP 선택 시 공급사만 표시되도록 vendorType 고정
        filters.value.vendorType = 'SUPPLIER';
    }
});
</script>

<style scoped>
/* 기존 스타일 유지 */
.settlement-filter-container {
    padding: 15px;
}

.filter-row {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: 20px;
}

.button-actions {
    display: flex;
    align-items: flex-end;
    gap: 10px;
}

:deep(select),
:deep(input[type="date"]),
:deep(input[type="text"]),
.btn-search,
.btn-reset {
    height: 38px;
}

.btn-search {
    background-color: #6b72f9;
    color: white;
    border: none;
    padding: 8px 20px;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-search:hover {
    background-color: #5a61e0;
}

.btn-reset {
    background-color: #f0f2f5;
    color: #495057;
    border: 1px solid #dcdfe6;
    padding: 8px 20px;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-reset:hover {
    background-color: #e9ecef;
}
</style>