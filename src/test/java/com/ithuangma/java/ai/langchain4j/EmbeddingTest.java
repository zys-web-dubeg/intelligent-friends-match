package com.ithuangma.java.ai.langchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = {
        com.ithuangma.java.ai.langchain4j.Config.DashScopeConfig.class,
        com.ithuangma.java.ai.langchain4j.Config.EmbeddingStoreConfig.class
})
public class EmbeddingTest {
@Autowired
private EmbeddingModel embeddingModel;
@Autowired
private EmbeddingStore<TextSegment> embeddingStore;
@Test
public void testEmbeddingModel(){
    Response<Embedding> embed = embeddingModel.embed("你好");

    System.out.println("向量维度：" + embed.content().vector().length);
    System.out.println("向量输出：" + embed.toString());
    }
    @Test
    public void testEmbeddingModel2(){
        //将文本转换成向量
        TextSegment segment1 = TextSegment.from("我喜欢羽毛球");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        //存入数据库
        embeddingStore.add(String.valueOf(segment1), embedding1);

        //将文本转换成向量
        TextSegment segment2 = TextSegment.from("今天天气真好");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        //存入数据库
        embeddingStore.add(String.valueOf(segment2), embedding2);
    }
    /**
     * pinecone-相似度匹配
     */
    @Test
    public void embeddingSearch(){
        //提问，并将问题转成向量数据
        Embedding queryEmbedding = embeddingModel.embed("你最喜欢的运动是什么？").content();
        //创建搜索请求对象
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1) //匹配最相似的一条记录
                //.minScore(0.8)
                .build();
        //根据搜索请求 searchRequest 在向量存储中进行相似度搜索
        EmbeddingSearchResult<TextSegment> searchResult =
                embeddingStore.search(searchRequest);
        //searchResult.matches()：获取搜索结果中的匹配项列表。
        //.get(0)：从匹配项列表中获取第一个匹配项
        EmbeddingMatch<TextSegment> embeddingMatch = searchResult.matches().get(0);
        //获取匹配项的相似度得分
        System.out.println(embeddingMatch.score()); // 0.8144288515898701
        //返回文本结果
        System.out.println(embeddingMatch.embedded().text());
    }
    @Test
    public void testUploadKnowledgeLibrary() {
        //使用FileSystemDocumentLoader读取指定目录下的知识库文档
        //并使用默认的文档解析器对文档进行解析
        Document document1 = FileSystemDocumentLoader.loadDocument("E:/knowledge/医院信息.md");
        Document document2 = FileSystemDocumentLoader.loadDocument("E:/knowledge/科室信息.md");
        Document document3 = FileSystemDocumentLoader.loadDocument("E:/knowledge/神经内科.md");
        List<Document> documents = Arrays.asList(document1, document2, document3);
        //文本向量化并存入向量数据库：将每个片段进行向量化，得到一个嵌入向量
        EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build()
                .ingest(documents);
    }

    @Test
    public void testUploadAndSearchShoppingKnowledge() {
        // 先上传
        Document document1 = FileSystemDocumentLoader.loadDocument("E:/knowledge/shopping/商品分类信息.md");
        Document document2 = FileSystemDocumentLoader.loadDocument("E:/knowledge/shopping/热销商品信息.md");
        Document document3 = FileSystemDocumentLoader.loadDocument("E:/knowledge/shopping/购物指南.md");
        Document document4 = FileSystemDocumentLoader.loadDocument("E:/knowledge/shopping/退货政策.md");
        List<Document> documents = Arrays.asList(document1, document2, document3, document4);
        EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build()
                .ingest(documents);
        System.out.println("上传完成，共 " + documents.size() + " 个文档。");

        // 再搜索
        Embedding queryEmbedding = embeddingModel.embed("推荐一款笔记本电脑").content();
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                .minScore(0.0)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        System.out.println("搜索结果数量：" + searchResult.matches().size());
        for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
            System.out.println("得分：" + match.score());
            System.out.println("内容：" + match.embedded().text().substring(0, Math.min(100, match.embedded().text().length())) + "...");
            System.out.println("---");
        }
    }
    @Test
    public void testUploadLoveKnowledge() {
        Document document1 = FileSystemDocumentLoader.loadDocument("E:/knowledge/恋爱/恋爱心理学.md");
        List<Document> documents = Collections.singletonList(document1);

        EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build()
                .ingest(documents);

        System.out.println("恋爱知识库文档入库完成！共 " + documents.size() + " 个文档。");
    }
    @Test
    public void testSearchLoveKnowledge() {
        Embedding queryEmbedding = embeddingModel.embed("恋爱心理学").content();
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
            System.out.println("得分：" + match.score());
            System.out.println("内容：" + match.embedded().text());
            System.out.println("---");
        }
    }

    @Test
    public void testUploadLearningKnowledge() {
        Document document1 = FileSystemDocumentLoader.loadDocument("E:/knowledge/学习/学习助手.md");
        List<Document> documents = Collections.singletonList(document1);

        EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build()
                .ingest(documents);

        System.out.println("学习知识库文档入库完成！共 " + documents.size() + " 个文档。");
    }

    @Test
    public void testSearchLearningKnowledge() {
        Embedding queryEmbedding = embeddingModel.embed("高效学习方法").content();
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
            System.out.println("得分：" + match.score());
            System.out.println("内容：" + match.embedded().text());
            System.out.println("---");
        }
    }
}
