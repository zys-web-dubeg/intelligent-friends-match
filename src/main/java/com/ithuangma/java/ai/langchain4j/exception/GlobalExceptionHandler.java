package com.ithuangma.java.ai.langchain4j.exception;

import com.ithuangma.java.ai.langchain4j.common.BaseResponse;
import com.ithuangma.java.ai.langchain4j.common.ErrorCode;
import com.ithuangma.java.ai.langchain4j.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.net.SocketException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理客户端断开连接异常
     * 这种异常通常发生在流式响应时,用户刷新页面或关闭浏览器
     */
    @ExceptionHandler({
        IOException.class,
        SocketException.class
    })
    public ResponseEntity<Void> handleClientAbortException(Exception e) {
        // 只记录为debug级别,避免日志污染
        log.debug("客户端连接中断: {}", e.getMessage());
        // 返回空响应,因为客户端已经断开连接
        return ResponseEntity.status(HttpStatus.OK).build();
    }
    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("服务器内部错误");
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExceptionHandler(BusinessException e) {
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExceptionHandler(RuntimeException e) {
        log.error("runtimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage(), "");
    }

}
