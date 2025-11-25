<template>
    <div class="filter-search-input">
        <label v-if="label">{{ label }}</label>
        <input class="input" type="text" :placeholder="placeholder" :value="modelValue"
            @input="$emit('update:modelValue', $event.target.value)" @keyup.enter="$emit('search')" />
    </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';
const props = defineProps({ label: String, placeholder: String, modelValue: String });
const emit = defineEmits(['update:modelValue', 'search']);
</script>

<style scoped>
.filter-search-input {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.filter-search-input label {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
    margin-bottom: 0;
}

.input {
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e0e0e0;
    font-size: 14px;
    color: #333;
    outline: none;
    background-color: white;
    transition: border-color 0.2s;
    height: 38px;
    min-width: 200px;
}

.input:hover,
.input:focus {
    border-color: #6b72f9;
}

.input::placeholder {
    color: #9ca3af;
}
</style>