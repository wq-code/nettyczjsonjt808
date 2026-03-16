package net.map591.ai;

/**
 * Redis Key常量配置类
 * 统一管理所有key前缀，避免不一致问题
 */
public class RedisKeys {
    
    // 联单信息
    public static final String PREFIX_LIANDAN = "ld:";
    
    // 车辆-联单映射 (统一使用这个)
    public static final String PREFIX_VEHICLE_TRACKING = "tracking:vehicle:";
    
    // 等待队列
    public static final String PREFIX_WAITING_OUT = "waiting:out:";
    
    // 轨迹线
    public static final String PREFIX_TRACK = "track:line:";
    
    // 最后活动时间
    public static final String PREFIX_LAST_TIME = "last:time:";
    
    // 最新GPS位置
    public static final String PREFIX_LATEST_GPS = "gps:latest:";
    
    // 已处理称重数据（幂等）
    public static final String PROCESSED_WEIGH = "processed:weigh:";

    // 停留相关
    public static final String PREFIX_STAY = "stay:";
    public static final String PREFIX_STAY_START = "stay:start:";

}