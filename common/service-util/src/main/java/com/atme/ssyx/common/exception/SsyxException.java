package com.atme.ssyx.common.exception;

import com.atme.ssyx.common.result.ResultCodeEnum;
import lombok.Data;

@Data
public class SsyxException extends RuntimeException{
    //异常状态码
    private Integer code;

    /**
     * 通过状态码和信息创建对象（使用基本参数）
     * @param message
     * @param code
     */
    public SsyxException(String message,Integer code) {
        super(message);
        this.code = code;
    }

    /**
     * 接收枚举类型的对象
     * @param resultCodeEnum
     */
    public SsyxException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }


}
