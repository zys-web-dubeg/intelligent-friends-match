package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.Bean.ChatForm;
import com.ithuangma.java.ai.langchain4j.Service.StatisticsService;
import com.ithuangma.java.ai.langchain4j.assistant.ShoppingAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@Tag(name = "购物助手杨楠")
@RequestMapping("/api/shopping")
public class ShoppingController {

    @Autowired
    private ShoppingAgent shoppingAgent;
    
    @Autowired
    private StatisticsService statisticsService;

    @Operation(summary = "购物助手杨楠的聊天接口")
    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm) {
        // 记录API请求统计
        statisticsService.recordApiRequest("shopping");
        
        return shoppingAgent.chat(chatForm.getMemoryId(), chatForm.getMessage());
    }
}