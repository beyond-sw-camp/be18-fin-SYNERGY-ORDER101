<script setup>
const props = defineProps({
  warehouse: {
    type: Object,
    default: () => ({ warehouseId: null, name: '창고 선택' })
  }
});
const emit = defineEmits(['open-modal']);

const openModal = () => {
  emit('open-modal');
};
</script>

<template>
  <div class="warehouse-display-container" @click="openModal">
    <span :class="{ 'selected-name': props.warehouse.warehouseId, 'placeholder': !props.warehouse.warehouseId }">
      <template v-if="props.warehouse.warehouseId">
        {{ props.warehouse.warehouseName || props.warehouse.name }} ({{ props.warehouse.warehouseId }})
      </template>
      <template v-else>
        창고 선택
      </template>
    </span>
    <span class="dropdown-icon">▼</span>
  </div>
</template>

<style scoped>
.warehouse-display-container {
  padding: 8px 12px;
  border: 1px solid #c0c0c0;
  border-radius: 4px;
  background-color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  width: 100%;
  box-sizing: border-box;
}

.warehouse-display-container:hover {
  background-color: #f7f7f7;
}

.selected-name {
  color: #333;
}

.placeholder {
  color: #999;
}

.dropdown-icon {
  font-size: 10px;
  color: #666;
  margin-left: 10px;
}
</style>
