package com.ithuangma.java.ai.langchain4j.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ithuangma.java.ai.langchain4j.Service.StatisticsService;
import com.ithuangma.java.ai.langchain4j.dto.ChartDataDto;
import com.ithuangma.java.ai.langchain4j.entity.Statistics;
import com.ithuangma.java.ai.langchain4j.entity.TeamProfile;
import com.ithuangma.java.ai.langchain4j.mapper.StatisticsMapper;
import com.ithuangma.java.ai.langchain4j.mapper.TeamProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private StatisticsMapper statisticsMapper;

    @Autowired
    private TeamProfileMapper teamProfileMapper;

    @Override
    public void recordUserRegistration() {
        Statistics statistics = Statistics.builder()
                .statisticsType("user_register")
                .apiType(null)
                .statisticsDate(LocalDateTime.now())
                .count(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        statisticsMapper.insert(statistics);
    }

    @Override
    public void recordApiRequest(String apiType) {
        Statistics statistics = Statistics.builder()
                .statisticsType("api_request")
                .apiType(apiType)
                .statisticsDate(LocalDateTime.now())
                .count(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        statisticsMapper.insert(statistics);
    }

    @Override
    public ChartDataDto getUserRegistrationStats(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> data = statisticsMapper.selectByDateGroup(
                "user_register",
                null,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        return buildChartData("用户注册统计", "注册数", "bar", data);
    }

    @Override
    public ChartDataDto getApiRequestStats(String apiType, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> data = statisticsMapper.selectByDateGroup(
                "api_request",
                apiType,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        String title = getApiTypeName(apiType) + "接口请求统计";
        return buildChartData(title, "请求次数", "line", data);
    }

    @Override
    public ChartDataDto getAllApiRequestsStats(LocalDate startDate, LocalDate endDate) {
        String title = "各智能体接口请求统计";

        List<String> xAxis = new ArrayList<>();
        List<ChartDataDto.SeriesData> seriesList = new ArrayList<>();

        // 获取各种API类型的统计数据
        String[] apiTypes = {"learning", "love", "shopping", "xiaozhi"};
        String[] apiNames = {"学习助手", "恋爱智能体", "购物助手", "硅谷小智"};

        for (int i = 0; i < apiTypes.length; i++) {
            List<Map<String, Object>> data = statisticsMapper.selectByDateGroup(
                    "api_request",
                    apiTypes[i],
                    startDate.atStartOfDay(),
                    endDate.atTime(LocalTime.MAX)
            );

            // 整理数据
            Map<String, Integer> dateCountMap = data.stream()
                    .collect(Collectors.toMap(
                            item -> (String) item.get("date"),
                            item -> ((Number) item.get("count")).intValue(),
                            Integer::sum
                    ));

            // 如果xAxis为空，填充日期轴
            if (xAxis.isEmpty()) {
                xAxis = generateDateList(startDate, endDate);
            }

            // 为每个日期填充对应的数据点
            List<Integer> yAxisData = xAxis.stream()
                    .map(date -> dateCountMap.getOrDefault(date, 0))
                    .collect(Collectors.toList());

            ChartDataDto.SeriesData seriesData = ChartDataDto.SeriesData.builder()
                    .name(apiNames[i])
                    .data(yAxisData)
                    .type("line")
                    .build();

            seriesList.add(seriesData);
        }

        return ChartDataDto.builder()
                .title(title)
                .xAxis(xAxis)
                .seriesList(seriesList)
                .chartType("line")
                .legendName("各智能体")
                .build();
    }

    @Override
    public ChartDataDto getUserAndApiComparisonStats(LocalDate startDate, LocalDate endDate) {
        String title = "用户注册与API请求对比";
        List<String> xAxis = generateDateList(startDate, endDate);
        List<ChartDataDto.SeriesData> seriesList = new ArrayList<>();

        // 用户注册数据
        List<Map<String, Object>> userRegisterData = statisticsMapper.selectByDateGroup(
                "user_register",
                null,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        Map<String, Integer> userRegisterMap = userRegisterData.stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("date"),
                        item -> ((Number) item.get("count")).intValue(),
                        Integer::sum
                ));

        List<Integer> userRegisterCounts = xAxis.stream()
                .map(date -> userRegisterMap.getOrDefault(date, 0))
                .collect(Collectors.toList());

        // API请求数据
        List<Map<String, Object>> apiRequestData = statisticsMapper.selectByDateGroup(
                "api_request",
                null,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        Map<String, Integer> apiRequestMap = apiRequestData.stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("date"),
                        item -> ((Number) item.get("count")).intValue(),
                        Integer::sum
                ));

        List<Integer> apiRequestCounts = xAxis.stream()
                .map(date -> apiRequestMap.getOrDefault(date, 0))
                .collect(Collectors.toList());

        // 添加系列数据
        seriesList.add(ChartDataDto.SeriesData.builder()
                .name("用户注册数")
                .data(userRegisterCounts)
                .type("bar")
                .build());

        seriesList.add(ChartDataDto.SeriesData.builder()
                .name("API请求次数")
                .data(apiRequestCounts)
                .type("line")
                .build());

        return ChartDataDto.builder()
                .title(title)
                .xAxis(xAxis)
                .seriesList(seriesList)
                .chartType("mixed")
                .build();
    }

    @Override
    public Map<String, Object> getRecentThreeDaysStats() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(2); // 最近3天

        return getStatsForPeriod(startDate, endDate, "近3天");
    }

    @Override
    public Map<String, Object> getRecentSevenDaysStats() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // 最近7天

        return getStatsForPeriod(startDate, endDate, "近7天");
    }

    @Override
    public Map<String, Object> getRecentThirtyDaysStats() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29); // 最近30天

        return getStatsForPeriod(startDate, endDate, "近30天");
    }

    private Map<String, Object> getStatsForPeriod(LocalDate startDate, LocalDate endDate, String periodName) {
        Map<String, Object> result = new HashMap<>();

        // 查询用户注册数
        QueryWrapper<Statistics> userRegWrapper = new QueryWrapper<>();
        userRegWrapper.eq("statistics_type", "user_register")
                .between("statistics_date", startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        int userRegisterCount = statisticsMapper.selectCount(userRegWrapper).intValue();

        // 查询队伍总数（不限制时间范围，显示所有队伍）
        QueryWrapper<TeamProfile> teamWrapper = new QueryWrapper<>();
        int teamCount = teamProfileMapper.selectCount(teamWrapper).intValue();

        // 查询各API类型请求总数
        Map<String, Integer> apiRequests = new HashMap<>();
        String[] apiTypes = {"learning", "love", "shopping", "xiaozhi"};
        String[] apiNames = {"学习助手", "恋爱智能体", "购物助手", "硅谷小智"};

        for (int i = 0; i < apiTypes.length; i++) {
            QueryWrapper<Statistics> apiWrapper = new QueryWrapper<>();
            apiWrapper.eq("statistics_type", "api_request")
                    .eq("api_type", apiTypes[i])
                    .between("statistics_date", startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
            int count = statisticsMapper.selectCount(apiWrapper).intValue();
            apiRequests.put(apiNames[i], count);
        }

        result.put("period", periodName);
        result.put("userRegisterCount", userRegisterCount);
        result.put("teamCount", teamCount);
        result.put("totalApiRequests", getTotalApiRequests(startDate, endDate));
        result.put("apiRequests", apiRequests);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());

        // 添加按天分组的详细数据
        result.put("userRegistrationData", buildDailyDataList("user_register", null, startDate, endDate));
        result.put("apiRequestData", buildDailyDataList("api_request", null, startDate, endDate));
        result.put("comparisonData", buildComparisonDataList(startDate, endDate));

        return result;
    }

    private int getTotalApiRequests(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<Statistics> wrapper = new QueryWrapper<>();
        wrapper.eq("statistics_type", "api_request")
                .between("statistics_date", startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        return statisticsMapper.selectCount(wrapper).intValue();
    }

    private ChartDataDto buildChartData(String title, String legendName, String chartType, List<Map<String, Object>> data) {
        List<String> xAxis = new ArrayList<>();
        List<Integer> yAxis = new ArrayList<>();

        for (Map<String, Object> item : data) {
            xAxis.add((String) item.get("date"));
            yAxis.add(((Number) item.get("count")).intValue());
        }

        return ChartDataDto.builder()
                .title(title)
                .xAxis(xAxis)
                .yAxis(yAxis)
                .chartType(chartType)
                .legendName(legendName)
                .build();
    }

    private List<String> generateDateList(LocalDate startDate, LocalDate endDate) {
        List<String> dates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate.format(formatter));
            currentDate = currentDate.plusDays(1);
        }

        return dates;
    }

    private String getApiTypeName(String apiType) {
        switch (apiType) {
            case "learning": return "学习助手";
            case "love": return "恋爱智能体";
            case "shopping": return "购物助手";
            case "xiaozhi": return "硅谷小智";
            default: return "未知智能体";
        }
    }

    /**
     * 构建按天分组的数据列表
     */
    private List<Map<String, Object>> buildDailyDataList(String statisticsType, String apiType, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> data = statisticsMapper.selectByDateGroup(
                statisticsType,
                apiType,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        // 将数据转换为Map以便快速查找
        Map<String, Integer> dateCountMap = data.stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("date"),
                        item -> ((Number) item.get("count")).intValue(),
                        Integer::sum
                ));

        // 生成完整的日期列表，确保没有数据的日期也有0值
        List<String> allDates = generateDateList(startDate, endDate);
        List<Map<String, Object>> result = new ArrayList<>();

        for (String date : allDates) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", dateCountMap.getOrDefault(date, 0));
            result.add(item);
        }

        return result;
    }

    /**
     * 构建对比数据列表（用户注册 + API请求）
     */
    private List<Map<String, Object>> buildComparisonDataList(LocalDate startDate, LocalDate endDate) {
        // 获取用户注册数据
        List<Map<String, Object>> userRegData = buildDailyDataList("user_register", null, startDate, endDate);
        // 获取API请求数据
        List<Map<String, Object>> apiReqData = buildDailyDataList("api_request", null, startDate, endDate);

        // 将API数据转换为Map以便快速查找
        Map<String, Integer> apiCountMap = apiReqData.stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("date"),
                        item -> ((Number) item.get("count")).intValue()
                ));

        // 合并数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> userItem : userRegData) {
            String date = (String) userItem.get("date");
            Map<String, Object> comparisonItem = new HashMap<>();
            comparisonItem.put("date", date);
            comparisonItem.put("userCount", userItem.get("count"));
            comparisonItem.put("apiCount", apiCountMap.getOrDefault(date, 0));
            result.add(comparisonItem);
        }

        return result;
    }
}