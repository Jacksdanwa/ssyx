package com.atme.ssyx.common.exception;

import com.atme.ssyx.common.result.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

//AOP面向切面编程（不改变代码而增加一个功能）
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class) //异常处理器
    @ResponseBody //返回json数据
    public Result error(Exception e){
        e.printStackTrace();
        return Result.fail(null);
    }

    @ExceptionHandler(SsyxException.class)
    @ResponseBody
    public Result error(SsyxException e) {
        e.printStackTrace();
        return Result.build(null,e.getCode(),e.getMessage());
    }
}
