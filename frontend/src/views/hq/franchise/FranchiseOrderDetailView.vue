<template>
  <div class="page-shell">
    <header class="page-header">
      <h1>주문 상세 내역</h1>
    </header>

    <section class="info-cards">
      <div class="card info">
        <label>주문 ID</label>
        <div class="value">{{ detail.orderNo }}</div>
      </div>
      <div class="card info">
        <label>가맹점</label>
        <div class="value">{{ detail.storeName }}</div>
      </div>
      <div class="card info">
        <label>생성 시간</label>
        <div class="value">{{ detail.createdAt }}</div>
      </div>
      <div class="card info">
        <label>상태</label>
        <div class="value status-chip" :class="{ rejected: isRejected }">
          {{ statusLabel(displayStatus) }}
        </div>

      </div>
    </section>

    <section class="card items">
      <h3 class="card-title">주문 아이템</h3>
      <table class="items-table">
        <thead>
          <tr>
            <th>SKU</th>
            <th>상품 이름</th>
            <th class="numeric">현재 재고</th>
            <th class="numeric">주문 수량</th>
            <th class="numeric">단가</th>
            <th class="numeric">총액</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in detail.items" :key="it.productCode">
            <td>{{ it.productCode }}</td>
            <td>{{ it.productName }}</td>
            <td class="numeric">{{ it.stock }}</td>
            <td class="numeric">{{ it.orderQty }}</td>
            <td class="numeric"><Money :value="it.unitPrice" /></td>
            <td class="numeric"><Money :value="it.unitPrice * it.orderQty" /></td>
          </tr>

          <tr v-if="detail.items.length === 0">
            <td colspan="6" class="no-data">주문 아이템이 없습니다.</td>
          </tr>
        </tbody>
      </table>

      <div class="items-summary">
        총 수량: {{ totalQty }} | 총 금액:
        <strong>{{ formatMoney(totalAmount) }}</strong>
      </div>
    </section>

    <section class="card progress">
      <h3 class="card-title">주문 진행 상황</h3>

      <div class="timeline">
        <div class="steps-icons">
          <div
            v-for="(s, idx) in progressSteps"
            :key="s.key + '-icon'"
            class="step-icon"
            :class="{
              active: idx <= currentStepIndex,
              rejected: isRejected
            }"
          >
            <div class="icon">
              <i :class="idx <= currentStepIndex ? 'pi pi-check' : 'pi pi-circle'" />
            </div>
          </div>
        </div>
        <div class="track" ref="trackRef">
          <div
            class="track-fill"
            :class="{ rejected: isRejected }"
            :style="{ width: filledPercent + '%' }"
          ></div>
        </div>

        <div class="steps-labels">
          <div v-for="s in progressSteps" :key="s.key + '-label'" class="step-label">
            <div class="label">
              {{ s.label }}
              <div class="sub">{{ s.time }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="statuses">
        <div v-for="s in progressSteps" :key="s.key" class="status-row">
          <div class="status-title">{{ s.label }}</div>
          <div class="status-time">
            <template v-if="s.key === 'REJECTED'">
              본사에서 반려하였습니다.
            </template>

            <template v-else>
              {{ s.time }}
            </template>
          </div>

          <div v-if="s.key === 'SHIPPED'">
            <div class="status-desc">{{ trackingNumber }}</div>
          </div>

          <div v-else-if="s.note" class="status-desc">
            {{ s.note }}
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, computed, ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import apiClient from "@/components/api";
import Money from "@/components/global/Money.vue";
import { formatDateTimeMinute } from "@/components/global/Date.js";

const isRejected = computed(() => detail.status === "REJECTED");



const route = useRoute();
const orderId = route.params.id;

const detail = reactive({
  orderNo: "",
  storeName: "",
  createdAt: "",
  status: "",
  shipmentStatus: "",
  items: [],
  logs: [],
});

const hasStatus = (st) => detail.logs.some((l) => l.status === st);

const findTime = (st) => {
  const log = detail.logs.find(
    (l) => l.status === st || l.status.startsWith(st)
  );
  return log ? formatDateTimeMinute(log.changedAt) : "";
};


const findNote = (st) => {
  const log = detail.logs.find((l) => l.status === st);
  return log ? log.note : "";
};

const displayStatus = computed(() => {
  const ship = detail.shipmentStatus;

  if (ship === "DELIVERED") return "DELIVERED";
  if (ship === "IN_TRANSIT" || ship === "SHIPPED") return "SHIPPED";
  if (ship === "WAITING") return "WAITING";
  return "-";
});

const progressSteps = computed(() => {
  if (isRejected.value) {
    return [
      {
        key: "SUBMITTED",
        label: "제출됨",
        time: findTime("SUBMITTED"),
        note: findNote("SUBMITTED"),
      },
      {
        key: "REJECTED",
        label: "반려됨",
        time: findTime("."),
        note: findNote("."),
      },
    ];
  }

  return [
    { key: "SUBMITTED", label: "제출됨", time: findTime("SUBMITTED") },
    { key: "WAITING", label: "배송대기", time: findTime("WAITING") },
    { key: "SHIPPED", label: "배송중", time: findTime("SHIPPED") },
    { key: "DELIVERED", label: "배송완료", time: findTime("DELIVERED") },
  ];
});



const currentStepIndex = computed(() => {
  if (isRejected.value) return 1; 
  if (displayStatus.value === "DELIVERED") return 3;
  if (displayStatus.value === "SHIPPED") return 2;
  if (displayStatus.value === "WAITING") return 1;
  return 0;
});


const trackRef = ref(null);
const trackWidth = ref(0);

onMounted(() => {
  trackWidth.value = trackRef.value?.offsetWidth || 0;
});


const filledPercent = computed(() => {
  if (isRejected.value) return 37.5;

  switch (displayStatus.value) {
    case "WAITING":
      return 37.5; // 배송대기
    case "SHIPPED":
      return 62.5; // 배송중
    case "DELIVERED":
      return 100; // 배송완료
    default:
      return 15; 
  }
});


const trackingNumber = computed(() => {
  if (!(displayStatus.value === "SHIPPED" || displayStatus.value === "DELIVERED")) {
    return "아직 배송이 진행되기 전이라 송장번호가 없습니다";
  }

  const shipped = detail.logs.find((l) => l.status === "SHIPPED");
  return shipped?.note ? `송장번호 : ${shipped.note}` : "-";
});

async function fetchDetail() {
  const res = await apiClient.get(`/api/v1/store-orders/detail/${orderId}`);
  const data = res.data;

  detail.orderNo = data.orderNo;
  detail.storeName = data.storeName;

  detail.logs =
    data.progress?.map((p) => ({
      status: p.key.toUpperCase(),
      changedAt: p.time,
      note: p.note,
    })) || [];

  detail.createdAt = findTime("SUBMITTED");

  detail.status = data.orderStatus;
  detail.shipmentStatus = data.shipmentStatus;

  detail.items =
    data.items?.map((it) => ({
      productCode: it.sku,
      productName: it.name,
      stock: it.stock,
      orderQty: it.qty,
      unitPrice: it.price,
    })) || [];
}

onMounted(fetchDetail);

const totalQty = computed(() =>
  detail.items.reduce((s, it) => s + (it.orderQty || 0), 0)
);

const totalAmount = computed(() =>
  detail.items.reduce((s, it) => s + (it.orderQty || 0) * (it.unitPrice || 0), 0)
);

const formatMoney = (v) => Number(v).toLocaleString() + "원";

const statusLabel = (s) => {
  if (isRejected.value) return "반려됨";
  return {
    WAITING: "배송대기",
    SHIPPED: "배송중",
    DELIVERED: "배송완료",
  }[s] || "-";
};

</script>

<style scoped>
.page-shell {
  padding: 24px 32px;
}

.page-header {
  margin-bottom: 18px;
}

.info-cards {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.card.info {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #eef2f7;
  background: #fff;
  flex: 1;
}

.card.info .value {
  font-size: 1.2rem;
  font-weight: 700;
  margin-top: 8px;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.items-table th,
.items-table td {
  padding: 12px 8px;
  border-top: 1px solid #f3f4f6;
}

.numeric {
  text-align: right;
}
.status-chip {
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 700;
}

.status-chip.rejected {
  background: #ffe5e5;
  color: #d93025;
}


.timeline {
  display: grid;
  grid-template-rows: auto 6px auto;
  row-gap: 14px;
  padding: 28px 0;
}

.steps-icons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  text-align: center;
}

.step-icon .icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 2px solid #ddd;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
}

.step-icon.active .icon {
  background: #6b46ff;
  border-color: #6b46ff;
  color: white;
}

.track {
  position: relative;
  height: 6px;
  background: #eee;
  border-radius: 6px;
}

.track-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: #6b46ff;
  border-radius: 6px;
}

.steps-labels {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  text-align: center;
}

.steps-labels .sub {
  margin-top: 4px;
  font-size: 0.85rem;
  color: #666;
}

.status-row {
  padding: 12px 0;
  border-top: 1px solid #eee;
}

.step-icon.rejected .icon {
  background: #ff4d4d !important;
  border-color: #ff4d4d !important;
  color: white;
}

.track-fill.rejected {
  background: #ff4d4d !important;
}

</style>
