package com.ithuangma.java.ai.langchain4j.Bean;

import lombok.Data;

@Data
public class ChatForm {
    private Long memoryId;//对话id
    private String message;//用户问题
    private Long userId;//当前登录用户ID，用于智能匹配助手识别用户身份
}
