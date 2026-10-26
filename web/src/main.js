import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/main.scss'

const app = createApp(App)

// 全局注册 Element Plus 图标，模板里直接用 <el-icon><Edit /></el-icon>
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
