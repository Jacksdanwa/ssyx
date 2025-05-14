package com.atme.ssyx.common.result;

import lombok.Data;

@Data
public class Result<T> {
    //返回状态码
    private Integer code;
    //返回信息
    private String message;
    //返回数据
    private T data;

    //私有化构造器
    private Result() {
    }

    public static<T> Result<T> build(T data, Integer code, String message){
        //创建结果返回类
        Result<T> result = new Result<>();
        if (data != null) {
            //返回值不等于空的情况下，返回data的值
            result.setData(data);
        }
        //设置返回的状态码
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static<T> Result<T> build(T data, ResultCodeEnum resultCodeEnum){
        //创建结果返回类
        Result<T> result = new Result<>();
        if (data != null) {
            //返回值不等于空的情况下，返回data的值
            result.setData(data);
        }
        //设置返回的状态码
        result.setCode(resultCodeEnum.getCode());
        result.setMessage(resultCodeEnum.getMessage());
        return result;
    }

    //成功的方法
    public static<T> Result<T> ok(T data) {
        return build(data, ResultCodeEnum.SUCCESS);
    }

    //失败的方法
    public static<T> Result<T> fail(T data){
       return build(data, ResultCodeEnum.FAIL);
    }

}