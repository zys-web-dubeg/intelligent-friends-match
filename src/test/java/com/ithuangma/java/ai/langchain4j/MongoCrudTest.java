package com.ithuangma.java.ai.langchain4j;

import com.ithuangma.java.ai.langchain4j.Bean.ChatMessages;
import com.jayway.jsonpath.Criteria;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

import javax.management.Query;

@SpringBootTest
public class MongoCrudTest {
    @Autowired
    private MongoTemplate mongoTemplate;
/**
 * 插入文档
 */
/* @Test
public void testInsert() {
mongoTemplate.insert(new ChatMessages(1L, "聊天记录"));
}*/
    /**
     * 插入文档
     */
    @Test
    public void testInsert2() {
        mongoTemplate.insert(new ChatMessages(1L, "聊天记录"));
    }
    /**
     * 根据id查询文档
     */
    @Test
    public void testFindById() {
        ChatMessages chatMessages = mongoTemplate.findById("6801ead733ba9c4a0d9b6c7b",
                ChatMessages.class);
        System.out.println(chatMessages);
    }
}
