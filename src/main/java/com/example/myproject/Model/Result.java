package com.example.myproject.Model;

//创造一个Result类，用于返回结果，包含code，message，data,其中data为泛型
@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static Result success() {
        return new Result(200, "操作成功", null);
    }

    public static <E> Result<E> success(E data) {
        return new Result<>(200, "操作成功", data);
    }

    public static Result failure(String message) {
        return new Result(400, message, null);
    }
}
