<template>
    <div class="filter-date-range">
        <label v-if="label">{{ label }}</label>
        <div class="date-inputs">
            <input class="input" type="date" :value="startDate"
                @change="$emit('update:startDate', $event.target.value)" />
            <span class="separator">~</span>
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
.filter-date-range {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.filter-date-range label {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
    margin-bottom: 0;
}

.date-inputs {
    display: flex;
    align-items: center;
    gap: 8px;
}

.separator {
    color: #64748b;
    font-weight: 500;
    font-size: 14px;
}

.input {
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e0e0e0;
    font-size: 14px;
    color: #333;
    outline: none;
    background-color: white;
    cursor: pointer;
    transition: border-color 0.2s;
    height: 38px;
}

.input:hover,
.input:focus {
    border-color: #6b72f9;
}
</style>