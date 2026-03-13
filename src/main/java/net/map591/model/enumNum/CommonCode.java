package net.map591.model.enumNum;

import net.map591.model.response.ResultCode;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zl
 */
public enum CommonCode implements ResultCode {
    SUCCESS(true,10000,"操作成功！"),
    FAIL(false,40000,"操作失败！"),
    FAIL_ADMIN(false,40000,"添加失败！请联系开发人员"),
    INVALID_PARAM(false,10003,"非法参数！"),
    INVALID_PARAM_DATA(false,10003,"请检查参数是否符合规范，或文字长度是否过长！"),
    FAIL_ERROR(false,40002,"系统异常！"),
    FILE_TYPE_ERROE(false,40004,"文件类型不匹配！"),
    FAIL_MEERROR(false,40005,"自定义异常！"),
    FAIL_DATA_NOT_NULL(false,40002,"数据不可为空"),
    FAIL_GEO_NULL_ERROR(false,40002,"空间数据异常或数据为空"),
    ;

//    private static ImmutableMap<Integer, CommonCode> codes ;
    /**
     * 是否成功标识
     */
    boolean success;
    /**
     * 返回代码
     */
    int code;
    //提示信息
    /**
     * 返回信息
     */
    String message;
    private Map<String, Object> data = new HashMap<String, Object>();

    /**
     * 枚举构造函数
     * @param success
     * @param code
     * @param message
     */
    private CommonCode(boolean success, int code, String message){
        this.success = success;
        this.code = code;
        this.message = message;
    }

    @Override
    public boolean success() {
        return success;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    public CommonCode data(String key, Object value){
        this.data.put(key, value);
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public CommonCode data(Map<String, Object> map){
        this.setData(map);
        return this;
    }
}
