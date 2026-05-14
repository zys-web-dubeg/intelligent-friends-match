package com.ithuangma.java.ai.langchain4j;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {
        com.ithuangma.java.ai.langchain4j.Config.DashScopeConfig.class,
        com.ithuangma.java.ai.langchain4j.Config.EmbeddingStoreConfig.class
})
public class TeamVectorTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 上传队伍向量到Pinecone
     */
    @Test
    public void testUploadTeamVector() {
        // 模拟队伍特征描述（对应personalityConstraint字段）
        String teamFeature1 = "喜欢编程、技术交流、开源项目贡献，熟悉Java、Python等编程语言";
        dev.langchain4j.data.document.Metadata metadata1 = dev.langchain4j.data.document.Metadata.metadata("teamId", "1");
        TextSegment segment1 = TextSegment.from(teamFeature1, metadata1);
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        embeddingStore.add(embedding1, segment1);

        String teamFeature2 = "热爱户外运动、登山、骑行，追求健康生活方式，喜欢团队合作";
        dev.langchain4j.data.document.Metadata metadata2 = dev.langchain4j.data.document.Metadata.metadata("teamId", "2");
        TextSegment segment2 = TextSegment.from(teamFeature2, metadata2);
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);

        String teamFeature3 = "对AI、机器学习、深度学习感兴趣，正在学习相关技术";
        dev.langchain4j.data.document.Metadata metadata3 = dev.langchain4j.data.document.Metadata.metadata("teamId", "3");
        TextSegment segment3 = TextSegment.from(teamFeature3, metadata3);
        Embedding embedding3 = embeddingModel.embed(segment3).content();
        embeddingStore.add(embedding3, segment3);

        System.out.println("队伍向量上传完成！共 3 个队伍。");
    }

    /**
     * 搜索队伍向量
     */
    @Test
    public void testSearchTeamVector() {
        // 模拟用户特征描述
        Embedding queryEmbedding = embeddingModel.embed("我喜欢编程和技术开发，想找个技术团队").content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                .minScore(0.5)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        System.out.println("搜索结果数量：" + searchResult.matches().size());
        for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
            System.out.println("得分：" + match.score());
            if (match.embedded() != null) {
                System.out.println("队伍ID：" + match.embedded().metadata().getString("teamId"));
                System.out.println("内容：" + match.embedded().text());
            } else {
                System.out.println("内容为空");
            }
            System.out.println("---");
        }
    }

    /**
     * 测试用户匹配队伍（模拟matchTeamsForUser方法）
     */
    @Test
    public void testMatchUserToTeam() {
        // 先上传队伍向量（确保数据存在）
        testUploadTeamVector();

        // 模拟用户标签（对应UserProfile的tags字段）
        String userTags = "Java开发、后端编程、Spring框架、微服务架构";

        Embedding userEmbedding = embeddingModel.embed(userTags).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(userEmbedding)
                .maxResults(5)
                .minScore(0.6)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        System.out.println("为用户匹配的队伍数量：" + searchResult.matches().size());
        for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
            String teamId = match.embedded().metadata().getString("teamId");
            System.out.println("匹配队伍ID：" + teamId + "，相似度：" + match.score());
            System.out.println("队伍特征：" + match.embedded().text().substring(0, Math.min(50, match.embedded().text().length())) + "...");
            System.out.println("---");
        }
    }

    /**
     * 测试添加单个队伍向量（模拟createTeam时调用indexTeamVector）
     */
    @Test
    public void testUploadSingleTeamVector() {
        // 模拟TeamProfile对象的数据
        Long teamId = 100L;
        String personalityConstraint = "热爱前端开发，熟悉React、Vue等框架，对用户体验有追求";

        dev.langchain4j.data.document.Metadata metadata = dev.langchain4j.data.document.Metadata.metadata("teamId", teamId.toString());
        TextSegment textSegment = TextSegment.from(personalityConstraint, metadata);
        Embedding embedding = embeddingModel.embed(textSegment).content();

        embeddingStore.add(embedding, textSegment);

        System.out.println("队伍 " + teamId + " 的向量已上传到Pinecone");
    }

    /**
     * 测试搜索单个队伍向量
     */
    @Test
    public void testSearchSingleTeamVector() {
        Embedding queryEmbedding = embeddingModel.embed("前端开发React").content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        if (!searchResult.matches().isEmpty()) {
            EmbeddingMatch<TextSegment> match = searchResult.matches().get(0);
            System.out.println("得分：" + match.score());
            System.out.println("队伍ID：" + match.embedded().metadata().getString("teamId"));
            System.out.println("内容：" + match.embedded().text());
        } else {
            System.out.println("未找到匹配的队伍");
        }
    }
}
