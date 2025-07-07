package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.concurrent.TimeoutException;

/**
 * 全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理超时异常
     */
    @ExceptionHandler({TimeoutException.class, AsyncRequestTimeoutException.class})
    public ResponseEntity<ErrorResponse> handleTimeoutException(Exception e, WebRequest request) {
        logger.error("Request timeout occurred", e);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "TIMEOUT_ERROR",
            "Request timed out. The operation took too long to complete.",
            "Please try again with a simpler request or check your network connection."
        );
        
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
    }

    /**
     * 处理AI相关异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e, WebRequest request) {
        logger.error("Runtime exception occurred", e);
        
        // 检查是否是AI调用相关的异常
        String message = e.getMessage();
        if (message != null && (message.contains("tool") || message.contains("function") || message.contains("AI"))) {
            ErrorResponse errorResponse = new ErrorResponse(
                "AI_TOOL_ERROR",
                "An error occurred during AI tool execution: " + message,
                "The AI encountered an issue while processing your request. Please try rephrasing your request or try again."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            "RUNTIME_ERROR",
            "An unexpected error occurred: " + message,
            "Please try again. If the problem persists, contact support."
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e, WebRequest request) {
        logger.error("Unexpected exception occurred", e);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An internal server error occurred",
            "Something went wrong on our end. Please try again later."
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 错误响应类
     */
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private String suggestion;
        private long timestamp;

        public ErrorResponse(String errorCode, String message, String suggestion) {
            this.errorCode = errorCode;
            this.message = message;
            this.suggestion = suggestion;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and setters
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
