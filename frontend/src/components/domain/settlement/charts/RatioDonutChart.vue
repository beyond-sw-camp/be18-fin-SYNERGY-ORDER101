<template>
    <div class="donut-chart-container">
        <canvas ref="chartCanvas"></canvas>
    </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const props = defineProps({
    // SettlementReport.vue에서 ratioChart 데이터를 받습니다.
    data: {
        type: Array,
        default: () => []
    }
});

const chartCanvas = ref(null);
let chartInstance = null;

const colors = [
    '#6b72f9', '#f97316', '#10b981', '#f59e0b', '#8b5cf6',
    '#ec4899', '#14b8a6', '#f43f5e', '#06b6d4', '#84cc16'
];

function renderChart() {
    if (!chartCanvas.value) return;

    if (chartInstance) {
        chartInstance.destroy();
    }

    const ctx = chartCanvas.value.getContext('2d');

    chartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: props.data.map(item => item.name),
            datasets: [{
                data: props.data.map(item => item.value),
                backgroundColor: colors,
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 16,
                        font: {
                            size: 12
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: (context) => {
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((context.parsed / total) * 100).toFixed(1);
                            return `${context.label}: ₩${context.parsed.toLocaleString('ko-KR')} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

onMounted(() => {
    renderChart();
});

watch(() => props.data, () => {
    renderChart();
}, { deep: true });
</script>

<style scoped>
.donut-chart-container {
    position: relative;
    height: 300px;
}
</style>