package net.map591.ai;

/**
 * 联单状态枚举
 */
public class LianDanStatus {
    public static final String WAIT_OUT = "1";      // 待移出
    public static final String TRANSPORTING = "2";   // 运输中
    public static final String RECEIVED = "3";       // 已接收
    public static final String ABNORMAL = "4";       // 异常
    
    // 获取状态描述
    public static String getDesc(String status) {
        switch (status) {
            case WAIT_OUT: return "待移出";
            case TRANSPORTING: return "运输中";
            case RECEIVED: return "已接收";
            case ABNORMAL: return "异常";
            default: return "未知";
        }
    }
}