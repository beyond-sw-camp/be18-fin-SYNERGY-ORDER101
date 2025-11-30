<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-container">

      <div class="modal-header">
        <h3>입고 상세 품목 확인</h3>
        <p class="sub-text">{{ inboundNo }}</p>
      </div>

      <div class="modal-body">
        <table class="detail-table">
          <thead>
            <tr>
              <th>제품 코드</th>
              <th>제품명</th>
              <th>입고 수량</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="item in details" :key="item.inboundDetailId">
              <td>{{ item.productCode }}</td>
              <td>{{ item.productName }}</td>
              <td class="numeric">{{ item.receivedQty }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <button class="close-btn" @click="$emit('close')">닫기</button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  inboundNo: String,
  details: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 40px 0;
}
.modal-container {
  width: 900px;
  max-height: 90vh;
  background: #fff;
  border-radius: 20px;
  padding: 32px 40px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 25px rgba(0,0,0,0.15);
}
.modal-header {
  margin-bottom: 28px;
}
.modal-header h3 {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 10px;
}
.sub-text {
  font-size: 16px;
  color: #666;
}
.modal-body {
  background: #fff;
  padding: 0;
  border-radius: 14px;
  overflow-y: auto;
  max-height: 450px;
  border: 1px solid #e5e7eb;
}
.detail-table {
  width: 100%;
  border-collapse: collapse;
}
.detail-table thead th {
  background: #f9fafb;
  padding: 14px 20px;
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
  text-align: center;
}
.detail-table tbody td {
  padding: 16px 20px;
  font-size: 15px;
  border-bottom: 1px solid #f3f4f6;
  text-align: center;
}
.detail-table tbody tr:hover {
  background: #f7faff;
}
.numeric {
  text-align: right;
}
.close-btn {
  align-self: flex-end;
  margin-top: 24px;
  padding: 12px 32px;
  border-radius: 10px;
  font-size: 16px;
  background: #4f46e5;
  color: white;
  border: none;
  cursor: pointer;
  font-weight: 600;
}
.close-btn:hover {
  background: #4338ca;
}
</style>
