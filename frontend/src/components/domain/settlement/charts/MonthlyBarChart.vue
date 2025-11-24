<template>
    <div class="bar-chart-container">
        <canvas ref="chartCanvas"></canvas>
    </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const props = defineProps({
    // SettlementReport.vue에서 monthlyChart 데이터를 받습니다.
    data: {
        type: Array,
        default: () => []
    }
});

const chartCanvas = ref(null);
let chartInstance = null;

function renderChart() {
    if (!chartCanvas.value) return;

    // 기존 차트 제거
    if (chartInstance) {
        chartInstance.destroy();
    }

    const ctx = chartCanvas.value.getContext('2d');

    chartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: props.data.map(item => `${item.month}월`),
            datasets: [{
                label: '금액',
                data: props.data.map(item => item.amount),
                backgroundColor: '#6b72f9',
                borderRadius: 6,
                barThickness: 32
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: (context) => {
                            return `₩${context.parsed.y.toLocaleString('ko-KR')}`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: (value) => {
                            return `₩${(value / 10000).toFixed(0)}만`;
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
.bar-chart-container {
    position: relative;
    height: 300px;
}
</style>