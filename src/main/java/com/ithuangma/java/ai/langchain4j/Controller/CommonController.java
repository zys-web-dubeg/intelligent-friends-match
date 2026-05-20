package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.Bean.Result;
import com.ithuangma.java.ai.langchain4j.util.AliyunOSSOperator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Operation(summary = "文件上传", description = "通用文件上传接口，支持图片等文件类型")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("上传文件：{}", file.getOriginalFilename());

        // 验证文件
        String validationMsg = validateImageFile(file);
        if (validationMsg != null) {
            return Result.fail(400, validationMsg);
        }

        String url = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
        log.info("文件上传成功，返回访问地址：{}", url);
        return Result.ok(url);
    }

    /**
     * 验证上传的文件是否符合要求
     *
     * @param file 上传的文件
     * @return 验证结果消息，如果验证通过返回null
     */
    private String validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "请选择要上传的文件";
        }

        // 文件类型校验
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return "只支持jpg、jpeg、png、gif、webp格式的图片";
        }

        // 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            return "文件大小不能超过5MB";
        }

        return null; // 验证通过
    }
}