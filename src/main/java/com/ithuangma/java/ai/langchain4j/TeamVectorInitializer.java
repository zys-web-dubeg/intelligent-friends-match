package com.ithuangma.java.ai.langchain4j;

import com.ithuangma.java.ai.langchain4j.entity.TeamProfile;
import com.ithuangma.java.ai.langchain4j.mapper.TeamProfileMapper;
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
 * 应用启动时自动重新索引队伍向量数据到内存向量库
 */
@Component
public class TeamVectorInitializer implements CommandLineRunner {

    @Autowired
    private TeamProfileMapper teamProfileMapper;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public void run(String... args) {
        System.out.println("=== 开始初始化队伍向量数据 ===");
        
        try {
            List<TeamProfile> allTeams = teamProfileMapper.selectList(null);
            System.out.println("找到 " + allTeams.size() + " 个队伍");
            
            int successCount = 0;
            for (TeamProfile team : allTeams) {
                if (team.getPersonalityConstraint() != null && !team.getPersonalityConstraint().isEmpty()) {
                    try {
                        // 1. 检查是否已存在（通过 teamId 过滤）
                        String teamIdStr = team.getId().toString();
                        
                        // 先构建向量用于搜索，并增加 type 标记
                        Metadata metadata = new Metadata();
                        metadata.put("teamId", teamIdStr);
                        metadata.put("type", "team"); // 关键：增加类型标记
                        
                        TextSegment tempSegment = TextSegment.from(
                            team.getPersonalityConstraint(),
                            metadata
                        );
                        Embedding queryEmbedding = embeddingModel.embed(tempSegment).content();
                        
                        var searchRequest = EmbeddingSearchRequest.builder()
                                .queryEmbedding(queryEmbedding)
                                .minScore(0.9)
                                .maxResults(5)
                                .build();
                        
                        // 执行搜索并手动过滤
                        var result = embeddingStore.search(searchRequest);
                        boolean exists = false;
                        for (EmbeddingMatch<TextSegment> match : result.matches()) {
                            if (match.embedded() != null && match.embedded().metadata() != null) {
                                String existingTeamId = match.embedded().metadata().getString("teamId");
                                if (teamIdStr.equals(existingTeamId)) {
                                    exists = true;
                                    break;
                                }
                            }
                        }
                        
                        // 如果已经存在，则跳过
                        if (exists) {
                            System.out.println("- 队伍 [" + team.getName() + "] 已存在，跳过索引");
                            continue;
                        }

                        // 2. 如果不存在，则正式存入
                        embeddingStore.add(queryEmbedding, tempSegment);
                        
                        System.out.println("✓ 队伍 [" + team.getName() + "] 向量索引成功");
                        successCount++;
                    } catch (Exception e) {
                        System.err.println("✗ 队伍 [" + team.getName() + "] 向量索引失败: " + e.getMessage());
                        // 不打印完整堆栈，避免日志过多
                    }
                }
            }
            
            System.out.println("=== 向量索引完成: " + successCount + "/" + allTeams.size() + " ===");
        } catch (Exception e) {
            System.err.println("=== 队伍向量初始化失败: " + e.getMessage() + " ===");
            System.err.println("提示：如果使用的是 InMemoryEmbeddingStore，这是正常的，将在运行时动态添加向量");
            // 不阻止应用启动
        }
    }
}
