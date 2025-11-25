<template>
    <div class="filter-date-range">
        <label v-if="label">{{ label }}</label>
        <div> <input class="input" type="date" :value="startDate"
                @change="$emit('update:startDate', $event.target.value)" />
            <span>~</span>
            <input class="input" type="date" :value="endDate" @change="$emit('update:endDate', $event.target.value)" />
        </div>
    </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';
const props = defineProps({ label: String, startDate: String, endDate: String });
const emit = defineEmits(['update:startDate', 'update:endDate']);
</script>

<style scoped>
/* Flexbox 배치 관련 스타일만 남기고 입력 필드/라벨/span 스타일은 부모에게 위임 */
.filter-date-range>div {
    /* 날짜 입력 필드를 묶은 div에 flex 적용 */
    display: flex;
    align-items: center;
    gap: 5px;
}

.input {

    padding: 8px 10px;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
}
</style>