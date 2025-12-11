<template>
  <div class="chat-backdrop">
    <div class="chat-window">

      <!-- Header -->
      <header class="chat-header">
        <span>ORDER101 챗봇</span>
        <button class="close-btn" @click="$emit('close')">×</button>
      </header>

      <!-- Category Pills -->
      <div class="chat-category-bar">
        <button
          v-for="cat in categories"
          :key="cat.key"
          class="category-pill"
          :class="{ active: cat.key === selectedCategoryKey }"
          @click="selectCategory(cat.key)"
        >
          {{ cat.label }}
        </button>
      </div>

      <!-- 예시 질문 -->
      <div v-if="selectedCategory" class="examples-panel">
        <button class="example-toggle" @click="showExamples = !showExamples">
          예시 질문 보기
        </button>

        <div v-if="showExamples" class="example-list">
          <div
            v-for="ex in selectedCategory.examples"
            :key="ex.id"
            class="example-item"
            @click="handleExampleClick(ex)"
          >
            {{ ex.question }}
          </div>
        </div>
      </div>

      <!-- Chat Body -->
      <div class="chat-body" ref="chatBody">

        <div v-for="(msg, index) in messages" :key="index">
          <div v-if="msg.role === 'user'" class="msg msg-user">
            {{ msg.content }}
          </div>

          <div v-else class="msg msg-bot">
            {{ msg.content }}
          </div>
        </div>

        <!-- Typing animation -->
        <div v-if="loading" class="typing-indicator">
          <span></span><span></span><span></span>
        </div>

      </div>

      <!-- Input -->
      <div class="chat-input">
        <input
          v-model="input"
          @keyup.enter="sendMessage"
          placeholder="메시지를 입력하세요…"
        />
        <button class="send-btn" @click="sendMessage">➤</button>
      </div>

    </div>
  </div>
</template>


<script setup>
import { ref, computed, nextTick, onMounted } from "vue";

const input = ref("");
const messages = ref([]);
const loading = ref(false);
const chatBody = ref(null);

const categories = ref([]);
const selectedCategoryKey = ref(null);
const showExamples = ref(false);

const selectedCategory = computed(() =>
  categories.value.find((c) => c.key === selectedCategoryKey.value) || null
);

function scrollToBottom() {
  nextTick(() => {
    if (chatBody.value) {
      chatBody.value.scrollTop = chatBody.value.scrollHeight;
    }
  });
}

function selectCategory(key) {
  selectedCategoryKey.value = key;
  showExamples.value = false;
}

async function loadMeta() {
  try {
    const res = await fetch("/api/v1/chat/meta");
    const data = await res.json();
    categories.value = data.categories;

    if (!selectedCategoryKey.value && categories.value.length > 0) {
      selectedCategoryKey.value = categories.value[0].key;
    }
  } catch (e) {
    console.error("Failed to load meta:", e);
  }
}

function handleExampleClick(example) {
  messages.value.push({ role: "user", content: example.question });
  messages.value.push({ role: "bot", content: example.answer });
  scrollToBottom();
}

onMounted(loadMeta);

async function sendMessage() {
  if (!input.value.trim()) return;

  if (!selectedCategoryKey.value) {
    messages.value.push({
      role: "bot",
      content: "먼저 상단 카테고리를 선택해 주세요.",
    });
    return;
  }

  const userMsg = input.value;
  messages.value.push({ role: "user", content: userMsg });
  input.value = "";
  
  loading.value = true;
  scrollToBottom();
  
  try {
    const response = await fetch("/api/v1/chat/stream", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        category: selectedCategoryKey.value,
        message: userMsg,
      }),
    });

    const botMsg = { role: "bot", content: "" };
    messages.value.push(botMsg);
    scrollToBottom();

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;

      botMsg.content += decoder.decode(value);
      scrollToBottom();
    }

  } catch (err) {
    messages.value.push({
      role: "bot",
      content: "챗봇 서버와 통신 중 오류가 발생했습니다.",
    });
  }

  setTimeout(() => {
    loading.value = false;
  }, 200);

  scrollToBottom();
}
</script>

<style scoped>
/* Backdrop */
.chat-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.35);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 3000;
}

/* Main Window */
.chat-window {
  width: 420px;
  height: 580px;
  background: white;
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 2px solid #8b5cf6;
}

/* Header */
.chat-header {
  background: #8b5cf6;
  color: white;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  font-weight: 600;
  font-size: 15px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 22px;
  cursor: pointer;
  color: white;
}

/* Category Pills */
.chat-category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 6px 10px;
  background: #f7f2ff;
  border-bottom: 1px solid #e5d5ff;
}

.category-pill {
  border: none;
  padding: 6px 12px;
  border-radius: 999px;
  background: #ede9fe;
  font-size: 13px;
  color: #4b0082;
  cursor: pointer;
  transition: 0.15s;
}

.category-pill.active {
  background: #8b5cf6;
  color: white;
}

/* Examples Area */
.examples-panel {
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
}

.example-toggle {
  border: none;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 500;
}

.example-list {
  margin-top: 8px;
  max-height: 110px;
  overflow-y: auto;
}

.example-item {
  font-size: 13px;
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 6px;
  background: #ffffff;
  border: 1px solid #dcdcdc;
  cursor: pointer;
  transition: 0.15s;
  color: #333;
}

.example-item:hover {
  background: #f3e8ff;
  border-color: #c084fc;
}

/* Chat Display */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  background: #f7f7fc;
}

.msg {
  max-width: 75%;
  padding: 10px 14px;
  margin-bottom: 12px;
  font-size: 14px;
  border-radius: 14px;
  line-height: 1.45;
}

.msg-user {
  margin-left: auto;
  background: #8b5cf6;
  color: white;
}

.msg-bot {
  background: #ececff;
  border: 1px solid #d4d4ff;
  color: #333;
}

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 5px;
  margin-left: 6px;
  margin-top: 4px;
}

.typing-indicator span {
  width: 9px;
  height: 9px;
  background: #8b5cf6;
  border-radius: 50%;
  animation: blink 1.3s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0% { opacity: .25; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-4px); }
  100% { opacity: .25; transform: translateY(0); }
}

/* Input */
.chat-input {
  display: flex;
  border-top: 1px solid #ddd;
}

.chat-input input {
  flex: 1;
  border: none;
  padding: 12px;
  font-size: 14px;
  outline: none;
  background: #fff;
}

.send-btn {
  width: 56px;
  background: #8b5cf6;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
}
</style>
