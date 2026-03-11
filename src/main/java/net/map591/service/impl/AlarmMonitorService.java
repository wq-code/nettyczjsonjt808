package net.map591.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.map591.entity.JgzcYcbj;
import net.map591.entity.LocationData;
import net.map591.mapper.WnDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class AlarmMonitorService {

    // 报警类型常量
    private static final String ALARM_TYPE_ROUTE_DEVIATION = "1"; // 路线偏离
    private static final String ALARM_TYPE_WEIGHT_ABNORMAL = "2"; // 重量异常
    private static final String ALARM_TYPE_TRANSPORT_TIMEOUT = "3"; // 运输超时
    private static final String ALARM_TYPE_OVERSTAY = "4"; // 超时停留
    private static final String ALARM_TYPE_OUTSIDE_FENCE = "5"; // 超出电子围栏
    @Autowired
    private WnDataMapper wnDataMapper;

    /**
     * 处理车辆位置数据，进行实时报警监测
     */
    public void processVehicleAlarm(String activeLdbh,LocationData locationData,String newTrackLine) {
        try {
            String platePhone = locationData.getPlatePhone();
            log.debug("开始对车辆{}进行报警监测，联单号：{}", platePhone, activeLdbh);

            // 2. 执行各种报警监测

            checkOutsideFence(activeLdbh,  locationData,newTrackLine);

//            checkRouteDeviation(activeLdbh, platePhone, locationData,newTrackLine);

            checkTransportTimeout(activeLdbh, platePhone, locationData,newTrackLine);

            checkOverstay(activeLdbh, platePhone, locationData,newTrackLine);

//            checkWeightAbnormal(activeLdbh,locationData);


        } catch (Exception e) {
            log.error("处理车辆位置报警监测失败", e);
        }
    }


    /**
     * 1. 检查路线偏离
     * 获取预设路线 polyline（LineString）
     * 计算当前 GPS 点到该线的 最短距离
     * 若 > yz_max → 违规
     */
    private void checkRouteDeviation(String ldbh, String platePhone, LocationData locationData,String newTrackLine) {

        log.info("检查车辆{}是否发生路线偏离", platePhone);
        // 检查当前位置是否偏离路线
        Map<String,Object> deviationDistance = wnDataMapper.checkRouteDeviation(platePhone, locationData.getLongitude(), locationData.getLatitude());
        if (deviationDistance == null||deviationDistance.isEmpty()){
            log.debug("联单编号{}未检测出偏离路线信息", ldbh);
            return;
        }
        String isDeviation = (String) deviationDistance.get("is_deviation");
        switch (isDeviation){
            case "DEVIATION":
                String czyf = (String) deviationDistance.get("czyf");
                log.debug("联单编号{}的车辆{}存在路线偏离, czyf:{}", ldbh, platePhone, czyf);
                saveAlarmRecord(ldbh, ALARM_TYPE_ROUTE_DEVIATION, newTrackLine, platePhone, czyf, "路线偏离");
                break;
            case "NORMAL":
                break;
            case "NO_ROUTE":
                break;
            default:
                break;
        }
    }
    /**
     * 3. 检查运输超时
     * 没有在规定的时间内到达制砖厂
     * 从移出时间到现在 > 阈值（分钟）
     */
    private void checkTransportTimeout(String activeLdbh, String platePhone, LocationData locationData,String newTrackLine) {
        log.info("检查车辆{}是否发生运输超时", platePhone);
        // 1. 获取超时规则（最大允许分钟数）
        Map<String, Object> routeRule3=wnDataMapper.getRouteRule3ByPlatePhone(platePhone);
        if (routeRule3 == null || routeRule3.isEmpty()){
            log.debug("车辆{}未检测出运输超时规则信息", platePhone);
            return;
        }
        Object value  = routeRule3.get("yz_value_max");
        Double yzMaxMinutes = null;
        if (value instanceof BigDecimal) {
            yzMaxMinutes = ((BigDecimal) value).doubleValue();
        } else if (value instanceof Number) {
            yzMaxMinutes = ((Number) value).doubleValue();
        } else if (value != null) {
            try {
                yzMaxMinutes = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                log.error("阈值转换失败: {}", value, e);
            }
        }
        if (yzMaxMinutes == null || yzMaxMinutes <= 0) {
            log.warn("车辆{}的超时阈值无效: {}", platePhone, yzMaxMinutes);
            return;
        }
        // 2. 获取移出时间（ycrq + ycsj）
        Map<String,Object> rqsj = null;
        try {
            rqsj = wnDataMapper.getYcsjByLdbh(activeLdbh, platePhone);
        } catch (Exception e) {
            log.error("获取车辆{}的移出时间失败", platePhone, e);
        }
        if (rqsj == null || rqsj.isEmpty()){
            log.debug("未找到联单{}的移出时间", activeLdbh);
            return;
        }
        String sjStr  = (String) rqsj.get("sj");
        LocalDateTime ycsj;
        try {
            ycsj = LocalDateTime.parse(sjStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.error("移出时间格式非法，无法解析: {}", sjStr, e);
            return;
        }

        // 3. 计算已用时间（分钟）
        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = java.time.Duration.between(ycsj, now).toMinutes();

        // 4. 判断是否超时
        if (minutesElapsed > yzMaxMinutes) {
            String czyf = String.valueOf(minutesElapsed); // 超时分钟数
            saveAlarmRecord(activeLdbh, ALARM_TYPE_TRANSPORT_TIMEOUT, newTrackLine, platePhone, czyf, "运输超时");
            log.warn("车辆{}运输超时！已用 {} 分钟，阈值 {} 分钟", platePhone, minutesElapsed, yzMaxMinutes);
        }


    }
    /**
     * 4. 检查超时停留
     * 根据最新的点位的gps_time和车牌号，查询规则范围内的点位是否存在变化
     * 车辆在运输途中在某个点停留时间超过规定阈值
     */
    private void checkOverstay(String activeLdbh, String platePhone, LocationData locationData,String newTrackLine) {
        log.info("开始检查车辆{}的停留时间", platePhone);
        try {
        // 1. 获取规则：最大允许停留时间（分钟）
        Map<String, Object> routeRule4 = wnDataMapper.getRouteRule4ByPlatePhone(platePhone);
        if (routeRule4 == null || routeRule4.isEmpty()){
            log.debug("车辆{}未检测出超时停留规则信息", platePhone);
            return;
        }
        Object value  = routeRule4.get("yz_value_max");
        Double maxStayMinutes = null;
        if (value instanceof BigDecimal) {
            maxStayMinutes = ((BigDecimal) value).doubleValue();
        } else if (value instanceof Number) {
            maxStayMinutes = ((Number) value).doubleValue();
        } else if (value != null) {
            try {
                maxStayMinutes = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                log.error("阈值转换失败: {}", value, e);
            }
        }
        if (maxStayMinutes == null || maxStayMinutes <= 0) {
            log.warn("车辆{}停留阈值无效: {}", platePhone, maxStayMinutes);
            return;
        }
        LocalDateTime currentTime = locationData.getGpsTime();
        int windowMinutes  = maxStayMinutes.intValue();

        log.info("检查车辆{}停留时间，阈值={}分钟，当前时间={}",
                platePhone, maxStayMinutes, currentTime);

        // 2. 查询去重后的轨迹点（时间窗口 = maxStayMinutes）
        List<LocationData> points = wnDataMapper.selectDistinctTrackPointsInWindow(platePhone, currentTime, windowMinutes);
        if (points == null || points.size() < 2) {
            log.info("车辆{}在最近{}分钟内点位太少({}个)，无法判断停留",
                    platePhone, windowMinutes, points == null ? 0 : points.size());
            return;
        }
        log.info("车辆{}查询到{}个轨迹点", platePhone, points.size());

        // 3. 计算实际持续时间
        LocationData earliestPoint = points.get(points.size() - 1); // 最早
        LocationData latestPoint = points.get(0);            // 最晚
        // 检查是否为null
        if (earliestPoint == null || earliestPoint.getGpsTime() == null ||
                latestPoint == null || latestPoint.getGpsTime() == null) {
            log.warn("车辆{}轨迹点时间数据为空", platePhone);
            return;
        }
        LocalDateTime earliestTime = earliestPoint.getGpsTime();
        LocalDateTime latestTime = latestPoint.getGpsTime();
        long actualStayMinutes = Duration.between(earliestTime, latestTime).toMinutes();
        log.info("车辆{}时间范围：{} 到 {}，持续 {} 分钟",
                platePhone, earliestTime, latestTime, actualStayMinutes);

        // 必须实际停留时间 ≥ 阈值
        if (actualStayMinutes < maxStayMinutes) {
            log.debug("车辆{}停留时间太短，实际 {} 分钟，阈值 {} 分钟", platePhone, actualStayMinutes, maxStayMinutes);
            return;
        }
        // 4. 计算位移（首尾点距离）
        double distance = computeGeoDistance(
                  points.get(0).getLongitude(), points.get(0).getLatitude(),
                  points.get(points.size() - 1).getLongitude(), points.get(points.size() - 1).getLatitude()
        );
        // 5. 检查位移和速度
        if (distance <= 50.0) {
            // 计算轨迹点集合 points 中每个点的速度平均值
            double avgSpeed = points.stream()
                    .mapToDouble(p -> Optional.ofNullable(p.getSpeed()).orElse(0.0))
                    .average().orElse(0.0);

            if (avgSpeed < 2.0) { // 静止
                String czyf = String.format("%.1f米/%d分钟", distance, actualStayMinutes);
                saveAlarmRecord(activeLdbh, ALARM_TYPE_OVERSTAY, newTrackLine, platePhone, czyf, "运输途中超时停留");
                log.debug("【超时停留】车辆{}在运输中停留{}分钟，位移{}米", platePhone, actualStayMinutes, distance);
            }
        }
    } catch (Exception e) {
        log.error("检查车辆{}停留报警失败", platePhone, e);
    }
    }

    /**
     * 5. 检查超出电子围栏
     */
    private void checkOutsideFence(String platePhone, LocationData locationData,String newTrackLine) {
        log.info("检查车辆{}是否超出电子围栏", platePhone);
        Boolean inFence=wnDataMapper.selectFenceOut(locationData);
        if (inFence){
            log.debug("车辆{}在电子围栏内", platePhone);
        }else {
            log.debug("车辆{}超出电子围栏", platePhone);
            saveAlarmRecord(null, ALARM_TYPE_OUTSIDE_FENCE, newTrackLine, platePhone, "超出电子围栏", "运输途中超出电子围栏");
        }
    }


    /**
     * 2. 检查重量异常（称重数据时触发）
     */
    public void checkWeightAbnormal(String activeLdbh, LocationData transportData) {

    }




    /**
     * 保存报警记录到数据库
     */
    private void saveAlarmRecord(String ldbh,String bjlx,String traceLine,String platePhone,String czyf,String bz) {
        try {
            // 构建报警记录对象
            JgzcYcbj alarmRecord = new JgzcYcbj();
            alarmRecord.setGlywdh(ldbh);
            alarmRecord.setYwlx("1"); // 运输环节
            alarmRecord.setBjlz(bjlx);
            alarmRecord.setBjsj(LocalDateTime.now());
            alarmRecord.setBjdjdz(traceLine);
            alarmRecord.setBjjb("1");
            alarmRecord.setCphm(platePhone);
            alarmRecord.setCzyf(czyf);
            alarmRecord.setCzzt("1"); // 待处理
            alarmRecord.setBz(bz);

            // 保存到数据库
            wnDataMapper.insertAlarmRecord(alarmRecord);
            log.info("报警记录保存成功：{}", alarmRecord);

            // 可以添加其他逻辑：发送通知、推送WebSocket等
            //更改联单状态
            updateLianDanStatusToAbnormal(ldbh);

        } catch (Exception e) {
            log.error("保存报警记录失败", e);
        }
    }

    /**
     * 将有报警的联单状态更新为异常（更新所有相关表）
     */
    private void updateLianDanStatusToAbnormal( String ldbh) {
        try {

                // 更新移出表
                int ycUpdated = wnDataMapper.updateLianDanStatusToAbnormal(ldbh);
                // 更新运输表
                int ysUpdated = wnDataMapper.updateYsLianDanStatusToAbnormal(ldbh);
                // 更新接收表
                int jsUpdated = wnDataMapper.updateJsLianDanStatusToAbnormal(ldbh);

                log.info("联单{}状态更新完成: 移出表{}条, 运输表{}条, 接收表{}条",
                        ldbh ,ycUpdated, ysUpdated, jsUpdated);
        } catch (Exception e) {
            log.error("更新联单状态为异常时发生错误", e);
        }
    }

    /**
     * 计算两个经纬度坐标之间的地理距离（单位：米），基于WGS84坐标系。 实现步骤如下：
     * 将纬度和经度差值转换为弧度；
     * 使用Haversine公式计算球面两点间的大圆距离；
     * 返回地球半径（6371000米）与中心角弧度乘积作为结果。
     * @param lon1
     * @param lat1
     * @param lon2
     * @param lat2
     * @return 距离（米）
     */
    private double computeGeoDistance(double lon1, double lat1, double lon2, double lat2) {
        final int R = 6371000; // 地球半径，单位：米
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

}
