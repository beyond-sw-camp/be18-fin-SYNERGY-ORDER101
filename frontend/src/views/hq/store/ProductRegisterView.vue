<template>
  <div class="product-registration">
    <h2 class="page-title">신규 상품 등록</h2>

    <div class="form-grid">
      <section class="card">
        <h3 class="card-title">기본 제품 정보</h3>

        <div class="form-row">
          <label>제품명</label>
          <input v-model="form.productName" placeholder="제품명을 입력하세요" class="input" />
        </div>

        <div class="form-row">
          <label>대분류</label>
          <select v-model.number="selectedLargeId" class="input" @change="onLargeChange">
            <option :value="null">선택</option>
            <option v-for="c in largeCategories" :key="c.id" :value="c.id">
              {{ c.name }}
            </option>
          </select>
        </div>

        <div class="form-row">
          <label>중분류</label>
          <select
            v-model.number="selectedMediumId"
            class="input"
            :disabled="!selectedLargeId"
            @change="onMediumChange"
          >
            <option :value="null">선택</option>
            <option v-for="c in mediumCategories" :key="c.id" :value="c.id">
              {{ c.name }}
            </option>
          </select>
        </div>

        <div class="form-row">
          <label>소분류</label>
          <select v-model.number="selectedSmallId" class="input" :disabled="!selectedMediumId">
            <option :value="null">선택</option>
            <option v-for="c in smallCategories" :key="c.id" :value="c.id">
              {{ c.name }}
            </option>
          </select>
        </div>

        <div class="form-row">
          <label>공급사</label>
          <select v-model="selectedSupplierId" class="input">
            <option :value="null">공급사 선택</option>
            <option v-for="s in suppliers" :key="s.supplierId" :value="s.supplierId">
              {{ s.supplierName }}
            </option>
          </select>
        </div>

        <div class="form-row">
          <label>공급가</label>
          <input
            v-model.number="form.price"
            placeholder="₩ 0"
            class="input"
            type="number"
            min="0"
          />
        </div>

        <div class="form-row">
          <label>납품가</label>
          <input
            v-model.number="form.deliveryPrice"
            placeholder="₩ 0"
            class="input"
            type="number"
            :disabled="!form.price"
            min="0"
          />
          <small v-if="form.price" class="hint-text">
            최소 {{ minDeliveryPrice.toLocaleString() }}원 이상 입력해야 합니다.
          </small>
        </div>

        <div class="form-row toggle-row">
          <label>활성 상태</label>
          <input type="checkbox" v-model="form.status" />
        </div>

        <div class="form-row">
          <label>제품 설명</label>
          <textarea
            v-model="form.description"
            class="textarea"
            placeholder="제품 설명을 입력하세요."
          ></textarea>
        </div>
      </section>

      <section class="card image-card">
        <h3 class="card-title">제품 이미지</h3>

        <div class="image-drop">
          <div class="image-placeholder">
            <div class="hint">이미지 파일을 여기에 끌어다 놓거나 클릭하여 업로드하세요.</div>
          </div>
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            @change="onFileChange"
            class="file-input"
          />
        </div>

        <div class="image-actions">
          <button class="btn-upload" @click="triggerFile">이미지 업로드</button>
          <div v-if="preview" class="preview-box">
            <img :src="preview" alt="preview" />
            <button class="btn-remove" @click="clearImage" type="button">삭제</button>
          </div>
        </div>
      </section>
    </div>

    <div class="actions">
      <button class="btn-cancel" @click="onCancel">취소</button>
      <button class="btn-save" :disabled="saving" @click="onSave">
        {{ saving ? '저장 중...' : '저장' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { createProduct } from '@/components/api/product/productService'
import apiClient from '@/components/api'

const router = useRouter()
const saving = ref(false)

const form = reactive({
  productName: '',
  price: 0,
  deliveryPrice: 0,
  status: true,
  description: '',
})

const minDeliveryPrice = computed(() => (form.price ? Math.ceil(form.price * 1.1) : 0))

const fileInput = ref(null)
const imageFile = ref(null)
const preview = ref(null)

const triggerFile = () => fileInput.value?.click()

const onFileChange = (e) => {
  const f = e.target.files?.[0]
  if (!f) return
  imageFile.value = f

  const reader = new FileReader()
  reader.onload = (ev) => (preview.value = ev.target.result)
  reader.readAsDataURL(f)
}

const clearImage = () => {
  imageFile.value = null
  preview.value = null
  if (fileInput.value) fileInput.value.value = ''
}

const largeCategories = ref([])
const mediumCategories = ref([])
const smallCategories = ref([])

const selectedLargeId = ref(null)
const selectedMediumId = ref(null)
const selectedSmallId = ref(null)

const fetchLarge = async () => {
  const res = await apiClient.get('/api/v1/categories/top')
  largeCategories.value = res.data
}

const fetchChildren = async (parentId) => {
  if (!parentId) return []
  const res = await apiClient.get(`/api/v1/categories/${parentId}/children`)
  return res.data
}

const onLargeChange = async () => {
  selectedMediumId.value = null
  selectedSmallId.value = null
  mediumCategories.value = await fetchChildren(selectedLargeId.value)
  smallCategories.value = []
}

const onMediumChange = async () => {
  selectedSmallId.value = null
  smallCategories.value = await fetchChildren(selectedMediumId.value)
}

const suppliers = ref([])
const selectedSupplierId = ref(null)

const fetchSuppliers = async () => {
  const res = await apiClient.get('/api/v1/suppliers', {
    params: { page: 1, numOfRows: 1000 },
  })
  suppliers.value = res.data.items
}

const onSave = async () => {
  if (!form.productName) {
    alert('제품명은 필수입니다.')
    return
  }
  if (!selectedSmallId.value) {
    alert('소분류를 선택해주세요.')
    return
  }
  if (!form.price || form.price <= 0) {
    alert('공급가를 입력해주세요.')
    return
  }

  if (!selectedSupplierId.value) {
    alert('공급사를 선택해주세요.')
    return
  }

  if (!form.deliveryPrice || form.deliveryPrice <= 0) {
    alert('납품가를 입력해주세요.')
    return
  }
  const minPrice = Math.ceil(form.price * 1.1)
  if (form.deliveryPrice < minPrice) {
    alert(
      `납품가는 공급가의 10% 이상이어야 합니다.\n` +
        `(공급가: ${form.price.toLocaleString()}원, ` +
        `최소 납품가: ${minPrice.toLocaleString()}원)`,
    )
    return
  }
  const req = {
    productName: form.productName,
    price: form.price,
    status: form.status,
    description: form.description,
    imageUrl: null, // 파일 올리면 백단이 덮어씀
    categoryLargeId: selectedLargeId.value,
    categoryMediumId: selectedMediumId.value,
    categorySmallId: selectedSmallId.value,
    supplierId: selectedSupplierId.value,
    deliveryPrice: form.deliveryPrice,
  }
  saving.value = true
  try {
    const created = await createProduct(req, imageFile.value)
    alert('상품이 등록되었습니다.')

    const newId = created?.productId
    if (newId) {
      router.push({ name: 'hq-product-detail', params: { id: newId } })
    } else {
      router.push({ name: 'hq-products-list' })
    }
  } catch (e) {
    alert(e?.response?.data?.message || '상품 등록에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

const onCancel = () => {
  router.back()
}

onMounted(() => {
  fetchLarge()
  fetchSuppliers()
})
</script>

<style scoped>
.page-title {
  font-size: 22px;
  margin-bottom: 18px;
}
.form-grid {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.card {
  flex: 1;
  background: #fff;
  border: 1px solid #eef2f7;
  padding: 20px;
  border-radius: 8px;
}
.image-card {
  width: 360px;
}
.card-title {
  font-size: 16px;
  margin-bottom: 12px;
}
.form-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.input,
.textarea,
select {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
}
.textarea {
  min-height: 120px;
  resize: vertical;
}
.toggle-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.image-drop {
  border: 1px dashed #e6e6ea;
  padding: 18px;
  border-radius: 8px;
  position: relative;
}
.image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 8px;
  color: #9ca3af;
}
.file-input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}
.image-actions {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
}
.btn-upload {
  background: #fff;
  border: 1px solid #e5e7eb;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}
.preview-box img {
  max-width: 120px;
  border-radius: 6px;
  border: 1px solid #eee;
}
.actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.btn-save {
  background: #6b46ff;
  color: #fff;
  border: none;
  padding: 10px 14px;
  border-radius: 8px;
}
.btn-cancel {
  background: #fff;
  border: 1px solid #e5e7eb;
  padding: 10px 14px;
  border-radius: 8px;
}

.btn-remove {
  margin-top: 6px;
  background: #fff;
  border: 1px solid #eee;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
}

@media (max-width: 1024px) {
  .form-grid {
    flex-direction: column;
  }
  .image-card {
    width: 100%;
  }
}
</style>
