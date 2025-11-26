<template>
  <div class="modal-backdrop">
    <div class="modal">
      <header class="modal-header">
        <h3>{{ product.name }} ({{ product.sku }})</h3>
        <button class="close-btn" @click="$emit('close')">×</button>
      </header>

      <section class="modal-body">
        <canvas ref="chartRef"></canvas>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import axios from "axios";

import {
  Chart,
  LineController,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
} from "chart.js";

Chart.register(
  LineController,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend
);

const props = defineProps({
  product: Object,
  targetWeek: String,
});

const chartRef = ref(null);
let chartInstance = null;

async function loadChart() {
  const res = await axios.get("/api/v1/ai/demand-forecast/product-series", {
    params: {
      productId: props.product.productId,
      targetWeek: props.targetWeek,
    },
  });

  const list = res.data;

  const labels = list.map((x) => x.date);
  const fc = list.map((x) => x.forecast);
  const ac = list.map((x) => x.actual);

  if (chartInstance) chartInstance.destroy();

  chartInstance = new Chart(chartRef.value.getContext("2d"), {
    type: "line",
    data: {
      labels,
      datasets: [
        { label: "예측", data: fc, borderWidth: 2 },
        { label: "실제", data: ac, borderWidth: 2 },
      ],
    },
  });
}

onMounted(loadChart);
onBeforeUnmount(() => chartInstance?.destroy());
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal {
  width: 700px;
  background: white;
  padding: 16px;
  border-radius: 12px;
}
.close-btn {
  border: none;
  background: none;
  font-size: 24px;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.modal-body {
  margin-top: 16px;
}
</style>
