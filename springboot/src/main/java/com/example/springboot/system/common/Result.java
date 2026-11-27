
package com.example.springboot.system.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

   private String code;

   private String msg;

   private T data;

   private Boolean success;

   /**
    * 成功（无数据）
    */
   public static Result<Void> success() {
      return new Result<>("200", "操作成功", null, true);
   }

   /**
    * 成功（带数据）
    */
   public static <T> Result<T> success(T data) {
      return new Result<>("200", "操作成功", data, true);
   }

   /**
    * 成功（带数据和消息）
    */
   public static <T> Result<T> success(T data, String msg) {
      return new Result<>("200", msg, data, true);
   }

   /**
    * 失败（无数据）
    */
   public static Result<Void> error() {
      return error(500, "服务器内部错误");
   }

   /**
    * 失败（带状态码和消息）
    */
   public static <T> Result<T> error(Integer code, String msg) {
      return new Result<>(
              String.valueOf(code),
              msg,
              null,
              false
      );
   }
}
