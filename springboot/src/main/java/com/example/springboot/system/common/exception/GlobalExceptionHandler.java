package com.example.springboot.system.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.example.springboot.system.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 统一构建异常响应
     */
    private ResponseEntity<Result<Void>> response(
            HttpStatus status, String message) {

        Result<Void> result = Result.error(
                status.value(),
                message
        );

        return ResponseEntity.status(status).body(result);
    }

    /**
     * 400：参数错误
     */
    @ExceptionHandler({
            BindException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception e) {

        log.warn("请求参数异常：{}", e.getMessage());

        return response(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 401：未登录或登录状态失效
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(
            NotLoginException e) {

        return response(
                HttpStatus.UNAUTHORIZED,
                "未登录或登录状态已失效"
        );
    }

    /**
     * 402：需要支付
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Result<Void>> handleApiException(
            ApiException e) {

        return response(e.getStatus(), e.getMessage());
    }

    /**
     * 403：没有权限
     */
    @ExceptionHandler({
            NotPermissionException.class,
            NotRoleException.class
    })
    public ResponseEntity<Result<Void>> handleForbidden(Exception e) {

        log.warn("权限不足：{}", e.getMessage());

        return response(
                HttpStatus.FORBIDDEN,
                "没有权限访问该资源"
        );
    }

    /**
     * 404：资源不存在
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(
            NoResourceFoundException e) {

        return response(
                HttpStatus.NOT_FOUND,
                "请求的资源不存在"
        );
    }

    /**
     * 500：服务器内部异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {

        log.error("服务器内部异常：", e);

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "服务器内部错误，请稍后重试"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {

        // 获取第一个校验失败的字段提示
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");

        return Result.error(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(
            ConstraintViolationException e) {

        String message = e.getConstraintViolations()
                .stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("参数校验失败");

        return Result.error(400, message);
    }

}
