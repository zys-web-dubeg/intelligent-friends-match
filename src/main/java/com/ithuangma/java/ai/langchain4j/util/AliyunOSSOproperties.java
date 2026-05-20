package com.ithuangma.java.ai.langchain4j.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sky.aliyun.oss")
public class AliyunOSSOproperties {
    private String endpoint;
    private String bucketName;
    private String region;
}
