package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.Bean.ChatForm;
import com.ithuangma.java.ai.langchain4j.Service.StatisticsService;
import com.ithuangma.java.ai.langchain4j.assistant.LearningAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@Tag(name = "学习智能助手陈俊辉")
@RequestMapping("/api/learning")
public class LearningController {
    @Autowired
    private LearningAgent learningAgent;
    
    @Autowired
    private StatisticsService statisticsService;

    @Operation(summary = "学习助手陈俊辉的聊天接口")
    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm) {
        // 记录API请求统计
        statisticsService.recordApiRequest("learning");
        
        return learningAgent.chat(chatForm.getMemoryId(), chatForm.getMessage());
    }
}