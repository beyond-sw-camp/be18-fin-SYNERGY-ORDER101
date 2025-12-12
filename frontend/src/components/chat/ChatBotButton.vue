<template>
  <div class="chatbot-wrapper">
    <button class="chatbot-btn" @click="$emit('open')">
      <Bot class="icon" />
    </button>
  </div>
</template>

<script setup>
import { Bot } from 'lucide-vue-next'
</script>


<style scoped>
.chatbot-wrapper {
  position: fixed;
  bottom: 28px;
  right: 32px;
  z-index: 3000;
}

.icon {
  width: 28px;
  height: 28px;
}


.chatbot-btn {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  border: none;
  background: #8b5cf6;
  color: white;
  font-size: 26px;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 6px 12px rgba(0,0,0,0.25);
  transition: transform 0.15s ease-out;
}

.chatbot-btn:hover {
  transform: scale(1.08);
}
</style>
