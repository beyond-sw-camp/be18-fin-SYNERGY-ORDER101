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
/* 입력 필드/라벨 스타일은 부모에게 위임 */
/* .filter-search-input은 부모에서 flex-direction: column으로 제어됨 */
.input {

    padding: 8px 10px;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
}
</style>