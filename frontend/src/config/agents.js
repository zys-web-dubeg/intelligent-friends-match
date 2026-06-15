import xiaozhiImg from '@/assets/0744b393451ea13efd3d5977b5d9786.jpg'
import xiaohuImg from '@/assets/a78b9097982bdb88efd3c8c45978c50c.jpg'
import loveAssistantImg from '@/assets/a285b709fabea7c35b76f685aa9f36a9.jpg'
import learningAssistantImg from '@/assets/b2869993aafbd1f96f44bc56c34b1fc2.jpg'
export const agents = [
  { id: 'zhangzhikang', name: '章志康', title: '医疗助手', api: '/api/xiaozhi/chat', image: xiaozhiImg },
  { id: 'yangnan', name: '杨楠', title: '购物助手', api: '/api/shopping/chat', image: xiaohuImg },
  { id: 'tuzhixing', name: '涂志兴', title: '恋爱助手', api: '/api/love/chat', image: loveAssistantImg },
  { id: 'learning', name: '陈俊辉', title: '学习助手', api: '/api/learning/chat', image: learningAssistantImg },
]

export const defaultAgentId = 'yangnan'
