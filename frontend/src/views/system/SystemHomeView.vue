<template>
  <div class="page-shell" style="padding: 24px">
    <h1>SYSTEM 테스트 도구</h1>
    <p>AI 관련 API를 스케줄러 대신 테스트.</p>
    <p>공용 DB 사용 시 남발하지 말아주세요</p>

    <section style="margin-top: 30px">
      <h2>수요예측 실행 (Java -> Python)</h2>
      <h4>매주 월요일만 수요예측이 가능합니다</h4>
      <h4>한번의 미래의 3개월치를 우선 예측합니다.</h4>
      <h4>현재 25년 12월이니, 26년 1월, 2월, 3월의 수요예측 실행이 가능.</h4>
      <h4>에러 발생시 -> 1. DB에 관련 값들이 잘 들어가있는지 확인. 2. 모델 재학습.</h4>
      <input
        v-model="forecastWeek"
        type="date"
        class="input"
        placeholder="YYYY-MM-DD"
      />
      <button @click="runForecast" class="btn">실행</button>
      <pre v-if="forecastResult">{{ forecastResult }}</pre>
    </section>


    <section style="margin-top: 30px">
      <h2>스마트 발주 초안 생성</h2>
      <h4>생성된 수요예측에 대해 스마트 발주를 생성</h4>
      <h4>해당일자에 수요예측이 존재해야함</h4>
      <input
        v-model="smartOrderWeek"
        type="date"
        class="input"
        placeholder="YYYY-MM-DD"
      />
      <button @click="createSmartOrderDraft" class="btn">실행</button>
      <pre v-if="smartOrderResult">{{ smartOrderResult }}</pre>
    </section>


    <section style="margin-top: 30px">
      <h2>모델 재학습</h2>
      <button @click="runRetrain" class="btn">실행</button>
      <pre v-if="retrainResult">{{ retrainResult }}</pre>
    </section>


    <section style="margin-top: 30px">
      <h2>예측 스냅샷 리스트 조회</h2>
      <button @click="loadSnapshots" class="btn">조회</button>
      <pre v-if="snapshots">{{ snapshots }}</pre>
    </section>
  </div>
</template>

<script>
import apiClient from "@/components/api"

export default {
  name: "SystemHomeView",
  data() {
    return {
      forecastWeek: "",       
      smartOrderWeek: "",     
      forecastResult: "",
      smartOrderResult: "",
      retrainResult: "",
      snapshots: ""
    }
  },
  methods: {

    async runForecast() {
      if (!this.forecastWeek) {
        alert("targetWeek 날짜를 입력하세요.")
        return
      }

      try {
        const res = await apiClient.post(
          `/api/v1/internal/ai/forecasts?target_week=${this.forecastWeek}`
        )
        this.forecastResult = JSON.stringify(res.data, null, 2)
      } catch (err) {
        this.forecastResult = "오류: " + err
      }
    },


    async createSmartOrderDraft() {
      if (!this.smartOrderWeek) {
        alert("targetWeek 날짜를 입력하세요.")
        return
      }

      try {
        const res = await apiClient.post(
          `/api/v1/smart-orders/generate?target_week=${this.smartOrderWeek}`
        )
        this.smartOrderResult = JSON.stringify(res.data, null, 2)
      } catch (err) {
        this.smartOrderResult = "오류: " + err
      }
    },


    async runRetrain() {
      try {
        const res = await apiClient.get("/api/v1/ai/train")
        this.retrainResult = JSON.stringify(res.data, null, 2)
      } catch (err) {
        this.retrainResult = "오류: " + err
      }
    },

  
    async loadSnapshots() {
      try {
        const res = await apiClient.get("/api/v1/forecasts/snapshots")
        this.snapshots = JSON.stringify(res.data, null, 2)
      } catch (err) {
        this.snapshots = "오류: " + err
      }
    }
  }
}
</script>

<style scoped>
.input {
  padding: 8px 12px;
  margin-right: 10px;
  border-radius: 6px;
  border: 1px solid #ccc;
  width: 180px;
}
.btn {
  padding: 8px 14px;
  border-radius: 8px;
  background: #6b63f6;
  color: white;
  cursor: pointer;
  border: none;
}
.btn:hover {
  background: #6b63f6;
}
pre {
  background: #222;
  color: #eee;
  padding: 12px;
  border-radius: 8px;
  margin-top: 10px;
  white-space: pre-wrap;
}
</style>
