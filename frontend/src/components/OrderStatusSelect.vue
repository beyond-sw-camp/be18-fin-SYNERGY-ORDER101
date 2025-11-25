<template>
    <div class="order-status-select">
        <button class="status-toggle-btn" @click="toggleDropdown">
            {{ selectedStatusDisplay }}
        </button>

        <div v-if="isOpen" class="status-dropdown">
            <ul class="status-list">
                <li v-for="status in statuses" :key="status.value" :class="{ 'is-active': status.value === modelValue }"
                    @click="selectStatus(status.value)">
                    {{ status.label }}
                </li>
            </ul>
        </div>

        <div v-if="isOpen" class="dropdown-overlay" @click="closeDropdown"></div>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue';

// 1. Props 정의 (v-model 지원)
const props = defineProps({
    // v-model의 기본 props 이름
    modelValue: {
        type: String,
        default: '전체' // 기본값 설정
    }
});

// 2. Emits 정의
const emit = defineEmits(['update:modelValue', 'change']);

// 3. 상태 목록 정의
const statuses = ref([
    { label: '모든 상태', value: '전체' },
    { label: '승인됨 (CONFIRMED)', value: 'CONFIRMED' },
    { label: '제출됨 (SUBMITTED)', value: 'SUBMITTED' },
    { label: '거절됨 (REJECTED)', value: 'REJECTED' },
    { label: '처리 중 (PROCESSING)', value: 'PROCESSING' }
]);

// 4. 드롭다운 상태
const isOpen = ref(false);

// 5. 현재 선택된 상태의 표시 레이블 계산
const selectedStatusDisplay = computed(() => {
    const current = statuses.value.find(s => s.value === props.modelValue);
    return current ? current.label : '모든 상태';
});

// 6. 메서드
function toggleDropdown() {
    isOpen.value = !isOpen.value;
}

function closeDropdown() {
    isOpen.value = false;
}

function selectStatus(statusValue) {
    // 상태 변경 및 부모 컴포넌트로 전달 (v-model 업데이트)
    emit('update:modelValue', statusValue);
    // 추가적으로 change 이벤트도 발생시킬 수 있습니다.
    emit('change', statusValue);

    // 드롭다운 닫기
    closeDropdown();
}
</script>

<style scoped>
/* 2. 스타일 */
.order-status-select {
    position: relative;
    display: inline-block;
}

.status-toggle-btn {
    /* 이미지에 나온 '모든 상태' 버튼 디자인을 모방 */
    padding: 8px 12px;
    border-radius: 8px;
    border: 1px solid #e6e6e9;
    /* Light grey border */
    background: white;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
}

.status-toggle-btn:hover {
    border-color: #ccc;
}

.icon {
    font-size: 16px;
}

.status-dropdown {
    position: absolute;
    top: 100%;
    /* 버튼 아래에 위치 */
    right: 0;
    margin-top: 5px;
    min-width: 180px;
    background: white;
    border: 1px solid #f0f0f3;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    z-index: 1000;
    /* 다른 요소 위에 표시 */
}

.status-list {
    list-style: none;
    padding: 8px 0;
    margin: 0;
}

.status-list li {
    padding: 10px 15px;
    cursor: pointer;
    font-size: 14px;
}

.status-list li:hover {
    background: #f4f7fa;
    /* 호버 시 배경색 변경 */
}

.status-list li.is-active {
    background: #e6e6e9;
    /* 현재 선택된 상태 강조 */
    font-weight: bold;
}

.dropdown-overlay {
    /* 드롭다운이 열렸을 때 전체 화면을 덮어 외부 클릭을 처리 */
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 999;
}
</style>