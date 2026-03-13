package net.map591.ai;

import lombok.Data;


/**
 * 路线规划信息实体类
 */
@Data
public class RoutePlanInfo {
    private String cph;              // 车牌号
    private String routeId;           // 路线ID
    private String routeName;         // 路线名称
    private String geom;              // 规划路线几何（WKT格式）
    private Double routeHc;           // 路线长度/里程

    // 各类规则的阈值
    private DeviationRule deviation;   // 路线偏离规则
    private StayTimeoutRule stay;      // 超时停留规则
    private TransportTimeoutRule transport; // 运输超时规则

    @Data
    public static class DeviationRule {
        private Double yzValueMin;     // 最小阈值
        private Double yzValueMax;     // 最大阈值（偏离距离）
    }

    @Data
    public static class StayTimeoutRule {
        private Double yzValueMin;     // 最小停留时间
        private Double yzValueMax;     // 最大停留时间
    }

    @Data
    public static class TransportTimeoutRule {
        private Double yzValueMin;     // 最小运输时间
        private Double yzValueMax;     // 最大运输时间
    }
}