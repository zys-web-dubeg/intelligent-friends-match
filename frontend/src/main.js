import { createApp } from 'vue';
import App from './App.vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import router from './router';

// 导入 Font Awesome
import '@fortawesome/fontawesome-free/css/all.min.css';

const app = createApp(App);
app.use(ElementPlus);
app.use(router);

// 添加全局错误处理
app.config.errorHandler = (err, vm, info) => {
  console.error('Vue Error:', err);
  console.error('Component:', vm);
  console.error('Info:', info);
};

app.mount('#app');

