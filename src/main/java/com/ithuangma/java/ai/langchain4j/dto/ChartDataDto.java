package com.ithuangma.java.ai.langchain4j.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataDto {
    
    // 图表标题
    private String title;
    
    // X轴标签
    private List<String> xAxis;
    
    // Y轴数据
    private List<Integer> yAxis;
    
    // 图表类型 (line: 折线图, bar: 柱状图)
    private String chartType;
    
    // 图例名称
    private String legendName;
    
    // 额外数据，用于多系列图表
    private List<SeriesData> seriesList;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesData {
        private String name;
        private List<Integer> data;
        private String type; // line, bar
    }
}