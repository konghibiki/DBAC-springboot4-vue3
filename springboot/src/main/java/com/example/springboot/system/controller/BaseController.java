package com.example.springboot.system.controller;

import com.example.springboot.system.common.Result;

public abstract class BaseController {

    /**
     * 成功结果（无数据）
     */
    protected Result<Void> success() {
        return Result.success();
    }

    /**
     * 成功结果（带数据）
     */
    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }

    /**
     * 成功结果（带数据和消息）
     */
    protected <T> Result<T> success(T data, String msg) {
        return Result.success(data, msg);
    }

    /**
     * 失败结果（无数据）
     */
    protected Result<Void> error() {
        return Result.error();
    }

    /**
     * 失败结果（带 code 和 msg）
     */
    protected <T> Result<T> error(Integer code, String msg) {
        return Result.error(code, msg);
    }

    /**
     * 根据数据库影响行数判断操作是否成功
     */
    protected <T> Result<T> toTable(
            int rows, Integer code, T data, String msg) {

        return rows > 0
                ? success(data, msg)
                : error(code, msg);
    }
}