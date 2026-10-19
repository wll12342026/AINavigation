<script setup lang="ts">
import { ref } from 'vue'
import { workspaceLogin } from '@/services/workspace'
const emit=defineEmits<{authenticated:[]}>()
const username=ref('admin'),password=ref(''),error=ref(''),busy=ref(false)
async function login(){busy.value=true;error.value='';try{await workspaceLogin(username.value,password.value);password.value='';emit('authenticated')}catch(e){error.value=e instanceof Error?e.message:'登录失败'}finally{busy.value=false}}
</script>
<template><section class="workspace-login panel"><div class="login-logo">◇</div><h2>登录知识与智能体工作台</h2><p>文档、提示词、模型连接及调试记录需要管理员权限。</p><form @submit.prevent="login"><label>管理员账号<input v-model="username" autocomplete="username" required></label><label>密码<input v-model="password" type="password" autocomplete="current-password" required placeholder="输入管理员密码"></label><p v-if="error" role="alert">{{ error }}</p><button class="btn primary" :disabled="busy">{{ busy?'正在登录…':'登录工作台' }}</button></form><small>沿用项目管理员账号。当前为本机开发模式，服务重启后需要重新登录。</small></section></template>
<style scoped>.workspace-login{max-width:480px;margin:65px auto;padding:38px}.login-logo{font-size:45px;color:#357bf3}.workspace-login h2{font-size:20px;margin:15px 0}.workspace-login p,.workspace-login small{color:#8496ae;font-size:12px;line-height:1.8}.workspace-login form,.workspace-login label{display:grid;gap:9px}.workspace-login form{gap:18px;margin:24px 0}.workspace-login input{border:1px solid #dbe7f5;border-radius:9px;padding:12px;background:#f8fbff}.workspace-login form p{color:#d25959;margin:0}.workspace-login label{color:#456584;font-size:12px}</style>
