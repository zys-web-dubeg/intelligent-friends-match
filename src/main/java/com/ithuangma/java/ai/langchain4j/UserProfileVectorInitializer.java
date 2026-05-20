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
                
                // 构建用户画像描述（与正式存储时使用相同的描述）
                StringBuilder sb = new StringBuilder();
                sb.append("用户画像信息：\n");
                if (profile.getMbtiType() != null) sb.append("MBTI类型：").append(profile.getMbtiType()).append("\n");
                if (profile.getTags() != null) sb.append("标签：").append(profile.getTags()).append("\n");
                if (profile.getInterests() != null) sb.append("兴趣爱好：").append(profile.getInterests()).append("\n");
                if (profile.getPreferences() != null) sb.append("偏好设置：").append(profile.getPreferences()).append("\n");
                if (profile.getPersonalityTraits() != null) sb.append("性格特征：").append(profile.getPersonalityTraits()).append("\n");
                if (profile.getCommunicationStyle() != null) sb.append("沟通风格：").append(profile.getCommunicationStyle()).append("\n");
                
                String description = sb.toString();
                
                // 1. 先检查是否已经存在该用户的向量（使用实际的用户画像描述进行搜索）
                Embedding queryEmbedding = embeddingModel.embed(description).content();
                
                var searchRequest = EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .minScore(0.95)  // 提高阈值，确保找到完全相同的记录
                        .maxResults(10)  // 增加搜索数量
                        .build();
                
                // 执行搜索并手动过滤
                var result = embeddingStore.search(searchRequest);
                boolean exists = false;
                for (EmbeddingMatch<TextSegment> match : result.matches()) {
                    if (match.embedded() != null && match.embedded().metadata() != null) {
                        String existingUserId = match.embedded().metadata().getString("user_id");
                        if (userIdStr.equals(existingUserId)) {
                            exists = true;
                            break;
                        }
                    }
                }
                
                // 如果已经存在，则跳过
                if (exists) {
                    System.out.println("- 用户 [" + profile.getUserId() + "] 已存在，跳过索引");
                    continue;
                }
                
                // 创建元数据，必须包含 type 标记
                Metadata metadata = new Metadata();
                metadata.put("user_id", userIdStr);
                metadata.put("type", "user"); // 关键：添加 type 标记

                //创建 TextSegment（将文本和元数据绑定）
                TextSegment textSegment = TextSegment.from(description, metadata);
                Embedding embedding = embeddingModel.embed(textSegment).content();

                // 存储到向量数据库
                embeddingStore.add(embedding, textSegment);
                
                System.out.println("✓ 用户 [" + profile.getUserId() + "] 向量索引成功");
                successCount++;
            } catch (Exception e) {
                System.err.println("✗ 用户 [" + profile.getUserId() + "] 向量索引失败: " + e.getMessage());
                // 不打印完整堆栈，避免日志过多
            }
        }
        
        System.out.println("=== 用户向量索引完成: " + successCount + "/" + allProfiles.size() + " ===");
    }
}
