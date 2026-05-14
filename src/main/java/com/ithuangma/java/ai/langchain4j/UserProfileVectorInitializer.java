package com.ithuangma.java.ai.langchain4j;

import com.ithuangma.java.ai.langchain4j.entity.UserProfile;
import com.ithuangma.java.ai.langchain4j.mapper.UserProfileMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时自动重新索引用户画像向量数据到内存向量库
 */
@Component
public class UserProfileVectorInitializer implements CommandLineRunner {

    @Autowired
    private UserProfileMapper userProfileMapper;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== 开始初始化用户画像向量数据 ===");
        
        List<UserProfile> allProfiles = userProfileMapper.selectList(null);
        System.out.println("找到 " + allProfiles.size() + " 个用户画像");
        
        int successCount = 0;
        for (UserProfile profile : allProfiles) {
            try {
                String userIdStr = profile.getUserId().toString();
                
                // 构建描述
                StringBuilder sb = new StringBuilder();
                sb.append("用户画像信息：\n");
                if (profile.getMbtiType() != null) sb.append("MBTI类型：").append(profile.getMbtiType()).append("\n");
                if (profile.getTags() != null) sb.append("标签：").append(profile.getTags()).append("\n");
                if (profile.getInterests() != null) sb.append("兴趣爱好：").append(profile.getInterests()).append("\n");
                if (profile.getPreferences() != null) sb.append("偏好设置：").append(profile.getPreferences()).append("\n");
                if (profile.getPersonalityTraits() != null) sb.append("性格特征：").append(profile.getPersonalityTraits()).append("\n");
                if (profile.getCommunicationStyle() != null) sb.append("沟通风格：").append(profile.getCommunicationStyle()).append("\n");
                
                String description = sb.toString();
                
                // 创建元数据，必须包含 type 标记
                Metadata metadata = new Metadata();
                metadata.put("user_id", userIdStr);
                metadata.put("type", "user"); // 关键：添加 type 标记
                
                TextSegment textSegment = TextSegment.from(description, metadata);
                Embedding embedding = embeddingModel.embed(textSegment).content();

                // 存储到向量数据库（使用 upsert 方式，如果存在则更新）
                embeddingStore.add(embedding, textSegment);
                
                System.out.println("✓ 用户 [" + profile.getUserId() + "] 向量索引成功");
                successCount++;
            } catch (Exception e) {
                System.err.println("✗ 用户 [" + profile.getUserId() + "] 向量索引失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("=== 用户向量索引完成: " + successCount + "/" + allProfiles.size() + " ===");
    }
}
