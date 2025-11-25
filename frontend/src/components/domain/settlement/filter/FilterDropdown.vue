<template>
    <div class="filter-dropdown">
        <label v-if="label">{{ label }}</label>

        <!-- searchMode일 때: 클릭하면 모달을 띄우는 커스텀 셀렉트 -->
        <div v-if="searchMode" class="custom-select-trigger" @click="handleTriggerClick">
            <span>{{ selectedText || placeholder }}</span>
            <svg class="icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" stroke-width="2" stroke-linecap="round" />
            </svg>
        </div>

        <!-- searchMode가 아닐 때: 일반 select -->
        <select v-else :value="modelValue" @change="$emit('update:modelValue', $event.target.value)">
            <option v-for="option in options" :key="option.value" :value="option.value">
                {{ option.text }}
            </option>
        </select>
    </div>
</template>

<script setup>
import { defineProps, defineEmits, computed } from 'vue';

const props = defineProps({
    label: String,
    options: Array,
    modelValue: String,
    searchMode: {
        type: Boolean,
        default: false
    },
    placeholder: {
        type: String,
        default: '선택'
    }
});

const emit = defineEmits(['update:modelValue', 'triggerSearchModal']);

const selectedText = computed(() => {
    const selected = props.options.find(option => option.value === props.modelValue);
    return selected ? selected.text : props.placeholder;
});

// 모달 트리거 클릭 핸들러
function handleTriggerClick() {
    console.log('🔍 모달 열기 이벤트 발생');
    emit('triggerSearchModal');
}
</script>

<style scoped>
/* 기존 스타일 유지 */
.filter-dropdown {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.filter-dropdown label {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
    margin-bottom: 4px;
}

select {
    padding: 8px 12px;
    border: 1px solid #e0e0e0;
    border-radius: 6px;
    font-size: 14px;
    color: #333;
    outline: none;
    background-color: white;
    cursor: pointer;
    transition: border-color 0.2s;
}

select:hover,
select:focus {
    border-color: #6b72f9;
}

.custom-select-trigger {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    border: 1px solid #e0e0e0;
    border-radius: 6px;
    font-size: 14px;
    color: #333;
    outline: none;
    cursor: pointer;
    background-color: white;
    transition: all 0.2s;
    min-width: 200px;
    height: 38px;
}

.custom-select-trigger:hover {
    border-color: #6b72f9;
    background-color: #f8f9ff;
}

.custom-select-trigger span {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    flex: 1;
}

.icon {
    margin-left: 10px;
    flex-shrink: 0;
    color: #6b7280;
    transition: color 0.2s;
}

.custom-select-trigger:hover .icon {
    color: #6b72f9;
}
</style>