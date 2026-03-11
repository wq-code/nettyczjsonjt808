//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package net.map591.model.response;
import net.map591.model.enumNum.CommonCode;

public class ResponseResult<T> {
    private boolean success;
    private int code;
    private String message;
    private T resultData;

    public ResponseResult(ResultCode resultCode) {
        this.success = resultCode.success();
        this.code = resultCode.code();
        this.message = resultCode.message();
    }


    public ResponseResult(int a) {
        ResultCode resultCode = null;
        if (a <= 0) {
            resultCode = CommonCode.FAIL;
        } else {
            resultCode =  CommonCode.SUCCESS;
        }
        this.success = resultCode.success();
        this.code = resultCode.code();
        this.message = resultCode.message();
    }

    public ResponseResult() {
        ResultCode resultCode = CommonCode.SUCCESS;
        this.success = resultCode.success();
        this.code = resultCode.code();
        this.message = resultCode.message();
    }



    public ResponseResult(ResultCode resultCode, T resultData) {
        this.success = resultCode.success();
        this.code = resultCode.code();
        this.message = resultCode.message();
        this.resultData = resultData;
    }



    public ResponseResult(T resultData) {
        ResultCode resultCode = CommonCode.SUCCESS;
        this.success = resultCode.success();
        this.code = resultCode.code();
        this.message = resultCode.message();
        this.resultData = resultData;
    }

    public ResponseResult(String resultData) {
        ResultCode resultCode = CommonCode.FAIL;
        this.success = resultCode.success();
        this.code = resultCode.code();
        this.message =resultData ;
    }








    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }



    public T getResultData() {
        return this.resultData;
    }

    public void setResultData(T resultData) {
        this.resultData = resultData;
    }
}
