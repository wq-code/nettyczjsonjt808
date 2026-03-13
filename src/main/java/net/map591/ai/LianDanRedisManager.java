package net.map591.ai;

import lombok.extern.slf4j.Slf4j;
import net.map591.entity.ZyClczjl;
import net.map591.mapper.WnDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.map591.ai.TrajectoryProcessor.PREFIX_TRACK;

@Slf4j
@Component
public class LianDanRedisManager {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private WnDataMapper wnDataMapper;
    @Autowired
    private AlarmService alarmService;

    private static final String PREFIX_LIANDAN = "ld:";
    private static final String PREFIX_WAITING_OUT = "waiting:out:";
    private static final String PREFIX_VEHICLE_TRACKING = "tracking:";
    private static final String PREFIX_VEHICLE_LATEST = "gps:latest:";


    // 联单状态枚举
    public enum LianDanStatus {
        WAITING_RECEIVE("待接收"),      // 只有移出记录
        COMPLETED("已完成"),             // 两条记录齐全
        ABNORMAL("异常");                // 异常状态

        private String desc;
        LianDanStatus(String desc) { this.desc = desc; }
    }

    /**
     * 创建或更新联单
     */
    public void createLianDanWithTransport(ZyClczjl record) {
        String ldbh = record.getLdbh();
        if (ldbh == null || ldbh.isEmpty()) {
            log.error("联单号为空，无法创建联单：{}", record.getCzjlid());
            return;
        }

        String key = PREFIX_LIANDAN + ldbh;
        Map<String, String> map = new HashMap<>();

        // 设置基本信息 - 状态直接设为运输中(2)
        map.put("ldbh", ldbh);
        map.put("cphm", record.getCphm());
        map.put("ldzt", net.map591.ai.LianDanStatus.TRANSPORTING);  // 直接设为运输中
        map.put("createTime", String.valueOf(System.currentTimeMillis()));

        // 设置移出信息
        map.put("ycczjlid", record.getCzjlid());
        map.put("ycczdd", record.getCzdd());
        map.put("ycjz", record.getJz() != null ? record.getJz().toString() : "0");
        map.put("ycczsj", record.getCzsj());
        map.put("ycjsgdbh", record.getJsgdbh() != null ? record.getJsgdbh() : "");
        map.put("ycczcbm", record.getCzcbm() != null ? record.getCzcbm() : "");

        // 保存到Redis
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, 7, TimeUnit.DAYS);

        // 维护车辆与联单的关系
        redisTemplate.opsForValue().set(
                PREFIX_VEHICLE_TRACKING + record.getCphm(),
                ldbh,
                7,
                TimeUnit.DAYS
        );

        log.info("联单创建成功：{}, 状态：运输中(2), 开始时间：{}",
                ldbh, record.getCzsj());

        // 异步保存到数据库
        saveLianDanToDatabase(key, net.map591.ai.LianDanStatus.TRANSPORTING);
    }

    /**
     * 完成联单（接收记录调用）
     * 状态流转: 运输中(2) → 已接收(3)
     */
    public void completeLianDan(String ldbh, ZyClczjl inRecord) {
        String key = PREFIX_LIANDAN + ldbh;

        // 获取移出信息
        Map<Object, Object> outInfo = redisTemplate.opsForHash().entries(key);
        if (outInfo.isEmpty()) {
            log.error("联单不存在：{}", ldbh);
            return;
        }

        // 获取当前状态，验证是否是运输中
        String currentStatus = (String) outInfo.get("ldzt");
        if (!net.map591.ai.LianDanStatus.TRANSPORTING.equals(currentStatus)) {
            log.warn("联单状态异常，当前状态：{}，期望状态：运输中(2)", currentStatus);
        }

        // 更新接收信息
        Map<String, String> updateMap = new HashMap<>();
        updateMap.put("jscczjlid", inRecord.getCzjlid());
        updateMap.put("jscczdd", inRecord.getCzdd());
        updateMap.put("jscjz", inRecord.getJz() != null ? inRecord.getJz().toString() : "0");
        updateMap.put("jscczsj", inRecord.getCzsj());
        updateMap.put("jscjsgdbh", inRecord.getJsgdbh() != null ? inRecord.getJsgdbh() : "");
        updateMap.put("jscczcbm", inRecord.getCzcbm() != null ? inRecord.getCzcbm() : "");

        // 状态更新为已接收(3)
        updateMap.put("ldzt", net.map591.ai.LianDanStatus.RECEIVED);
        updateMap.put("completeTime", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForHash().putAll(key, updateMap);

        // 计算运输时长

        log.info("联单已完成：{}, 状态：已接收(3), 结束时间：{}", ldbh, inRecord.getCreateTime());

        // 获取轨迹点数量
        String trackCount = (String) redisTemplate.opsForHash().get(key, "trackPoints");
        log.info("联单{}共采集{}个轨迹点", ldbh, trackCount);

        // 保存到数据库联单表
        saveCompletedLianDanToDatabase(key, inRecord);
    }
    /**
     * 保存联单到数据库（待移出状态）
     */
    private void saveLianDanToDatabase(String redisKey, String ldStatus) {
        try {
            Map<Object, Object> ldInfo = redisTemplate.opsForHash().entries(redisKey);
            if (ldInfo.isEmpty()) return;

            ZyLdjl ldjl = new ZyLdjl();
            ldjl.setLdbh((String) ldInfo.get("ldbh"));
            ldjl.setCphm((String) ldInfo.get("cphm"));
            ldjl.setLdzt(ldStatus);  // 待移出

            // 移出信息
            ldjl.setYcczjlid((String) ldInfo.get("ycczjlid"));
            ldjl.setYcczdd((String) ldInfo.get("ycczdd"));
            ldjl.setYcczsj((String) ldInfo.get("ycczsj"));
            ldjl.setYcjz(parseBigDecimal((String) ldInfo.get("ycjz")));
            ldjl.setYcjsgdbh((String) ldInfo.get("ycjsgdbh"));
            ldjl.setYcczcbm((String) ldInfo.get("ycczcbm"));

            ldjl.setCreateTime(new Date());
            ldjl.setUpdateTime(new Date());

            wnDataMapper.insertLdjl(ldjl);

            log.info("联单已保存到数据库：{}, 状态：待移出", ldjl.getLdbh());

        } catch (Exception e) {
            log.error("保存联单到数据库失败", e);
        }
    }

    /**
     * 保存完成的联单到数据库（已接收状态）
     */
    private void saveCompletedLianDanToDatabase(String redisKey, ZyClczjl inRecord) {
        try {
            Map<Object, Object> ldInfo = redisTemplate.opsForHash().entries(redisKey);
            if (ldInfo.isEmpty()) return;

            ZyLdjl ldjl = new ZyLdjl();
            ldjl.setLdbh((String) ldInfo.get("ldbh"));
            ldjl.setCphm((String) ldInfo.get("cphm"));
            ldjl.setLdzt(net.map591.ai.LianDanStatus.RECEIVED);  // 已接收

            // 移出信息
            ldjl.setYcczjlid((String) ldInfo.get("ycczjlid"));
            ldjl.setYcczdd((String) ldInfo.get("ycczdd"));
            ldjl.setYcczsj((String) ldInfo.get("ycczsj"));
            ldjl.setYcjz(parseBigDecimal((String) ldInfo.get("ycjz")));
            ldjl.setYcjsgdbh((String) ldInfo.get("ycjsgdbh"));
            ldjl.setYcczcbm((String) ldInfo.get("ycczcbm"));

            // 接收信息
            ldjl.setJscczjlid(inRecord.getCzjlid());
            ldjl.setJscczdd(inRecord.getCzdd());
            ldjl.setJscczsj(inRecord.getCzsj());
            ldjl.setJscjz(inRecord.getJz());
            ldjl.setJscjsgdbh(inRecord.getJsgdbh());
            ldjl.setJscczcbm(inRecord.getCzcbm());

            ldjl.setCompleteTime(new Date());
            ldjl.setUpdateTime(new Date());

            // 如果已存在则更新，否则插入
            ZyLdjl existing = wnDataMapper.selectLdjlByLdbh(ldjl.getLdbh());
            if (existing != null) {
                wnDataMapper.updateLdjl(ldjl);
            } else {
                wnDataMapper.insertLdjl(ldjl);
            }

            log.info("联单已完成并保存到数据库：{}, 状态：已接收", ldjl.getLdbh());

        } catch (Exception e) {
            log.error("保存已完成联单到数据库失败", e);
        }
    }

    /**
     * 获取联单状态
     */
    public String getLianDanStatus(String ldbh) {
        String key = PREFIX_LIANDAN + ldbh;
        Object status = redisTemplate.opsForHash().get(key, "ldzt");
        return status != null ? status.toString() : null;
    }


    /**
     * 获取车辆当前联单及状态
     */
    public Map<String, String> getVehicleLianDanInfo(String plateNumber) {
        String ldbh = getActiveLdbhByPlate(plateNumber);
        if (ldbh == null) {
            return null;
        }

        String key = PREFIX_LIANDAN + ldbh;
        Map<Object, Object> ldInfo = redisTemplate.opsForHash().entries(key);

        Map<String, String> result = new HashMap<>();
        result.put("ldbh", ldbh);
        result.put("ldzt", (String) ldInfo.get("ldzt"));
        result.put("cphm", (String) ldInfo.get("cphm"));
        result.put("ycczdd", (String) ldInfo.get("ycczdd"));
        result.put("ycczsj", (String) ldInfo.get("ycczsj"));

        return result;
    }

    /**
     * 获取联单完整信息（包括轨迹）
     */
    public Map<String, Object> getLianDanDetail(String ldbh) {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取Redis中的联单信息
        String key = PREFIX_LIANDAN + ldbh;
        Map<Object, Object> ldInfo = redisTemplate.opsForHash().entries(key);

        if (!ldInfo.isEmpty()) {
            // 从Redis获取基本信息
            result.put("ldbh", ldInfo.get("ldbh"));
            result.put("cphm", ldInfo.get("cphm"));
            result.put("ldzt", ldInfo.get("ldzt"));
            result.put("statusDesc", net.map591.ai.LianDanStatus.getDesc((String) ldInfo.get("ldzt")));
            result.put("outTime", ldInfo.get("ycczsj"));
            result.put("endTime", ldInfo.get("jscczsj"));

            String trackPoints = (String) ldInfo.get("trackPoints");
            result.put("trackCount", trackPoints != null ? Integer.parseInt(trackPoints) : 0);
        } else {
            // 从数据库查询
            ZyLdjl ldjl = wnDataMapper.selectLdjlByLdbh(ldbh);
            if (ldjl != null) {
                result.put("ldbh", ldjl.getLdbh());
                result.put("cphm", ldjl.getCphm());
                result.put("ldzt", ldjl.getLdzt());
                result.put("statusDesc", net.map591.ai.LianDanStatus.getDesc(ldjl.getLdzt()));
                result.put("outTime", ldjl.getYcczsj());
                result.put("endTime", ldjl.getJscczsj());
            }
        }

        // 2. 从数据库获取轨迹点列表
        List<ZyClwz> trackPoints = wnDataMapper.selectClwzByLdbh(ldbh);
        result.put("trackPoints", trackPoints);

        // 3. 获取轨迹线（优先从Redis获取，没有则从数据库获取）
        String trackLine = getTrackLine(ldbh);
        if (trackLine == null && !trackPoints.isEmpty()) {
            // 从轨迹点构建轨迹线
            trackLine = buildTrackLineFromPoints(trackPoints);
        }
        result.put("trackLine", trackLine);

        // 4. 计算运输时长
        if (result.get("outTime") != null && result.get("endTime") != null) {
            try {
                String outTimeStr = result.get("outTime").toString();
                String endTimeStr = result.get("endTime").toString();
                long minutes = calculateMinutesBetween(outTimeStr, endTimeStr);
                result.put("transportMinutes", minutes);
                result.put("transportHours", String.format("%.1f", minutes / 60.0));
            } catch (Exception e) {
                log.error("计算运输时长失败", e);
            }
        }

        return result;
    }

    /**
     * 从轨迹点构建轨迹线
     */
    private String buildTrackLineFromPoints(List<ZyClwz> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("LINESTRING(");
        for (int i = 0; i < points.size(); i++) {
            ZyClwz point = points.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(point.getLongitude().doubleValue())
                    .append(" ")
                    .append(point.getLatitude().doubleValue());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 计算两个时间之间的分钟数
     */
    private long calculateMinutesBetween(String time1, String time2) {
        try {
            LocalDateTime t1 = LocalDateTime.parse(time1.replace(" ", "T"));
            LocalDateTime t2 = LocalDateTime.parse(time2.replace(" ", "T"));
            return Math.abs(ChronoUnit.MINUTES.between(t1, t2));
        } catch (Exception e) {
            log.error("计算时间差失败：{} - {}", time1, time2);
            return 0;
        }
    }

    /**
     * 获取轨迹线
     */
    public String getTrackLine(String ldbh) {
        String key = PREFIX_TRACK + ldbh;
        return redisTemplate.opsForValue().get(key);
    }
    /**
     * 更新联单状态
     */
    public void updateLianDanStatus(String ldbh, String status) {
        String key = PREFIX_LIANDAN + ldbh;
        redisTemplate.opsForHash().put(key, "ldzt", status);

        // 更新数据库
        ZyLdjl ldjl = new ZyLdjl();
        ldjl.setLdbh(ldbh);
        ldjl.setLdzt(status);
        ldjl.setUpdateTime(new Date());

//        wnDataMapper.updateLdjlStatus(ldjl);

        log.info("联单状态已更新：{}, 状态：{}", ldbh, status);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取车辆当前活跃联单号
     * 优先从Redis获取，如果没有则查数据库
     */
    public String getActiveLdbhByPlate(String plateNumber) {
        if (plateNumber == null || plateNumber.isEmpty()) {
            return null;
        }

        // 1. 先从Redis获取
        String key = PREFIX_VEHICLE_TRACKING + plateNumber;
        String ldbh = redisTemplate.opsForValue().get(key);

        if (ldbh != null && !ldbh.isEmpty()) {
            log.debug("从Redis获取车辆{}活跃联单：{}", plateNumber, ldbh);
            return ldbh;
        }

        // 2. Redis中没有，查数据库
        ldbh = wnDataMapper.selectActiveLdbhByPlate(plateNumber);

        if (ldbh != null && !ldbh.isEmpty()) {
            // 同步到Redis
            redisTemplate.opsForValue().set(key, ldbh, 7, TimeUnit.DAYS);
            log.info("从数据库获取车辆{}活跃联单并同步到Redis：{}", plateNumber, ldbh);
        }

        return ldbh;
    }

    /**
     * 重量偏差检查（±5%）
     */
    private void checkWeightDeviation(Map<Object, Object> existing, Map<String, String> current) {
        try {
            BigDecimal outJz = new BigDecimal((String) existing.get("outJz"));
            BigDecimal inJz = new BigDecimal(current.get("inJz"));

            BigDecimal diff = inJz.subtract(outJz).abs();
            BigDecimal percent = diff.divide(outJz, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            if (percent.compareTo(new BigDecimal("5")) > 0) {
                // 触发重量偏差预警
                String ldbh = (String) existing.get("ldbh");
                alarmService.createAlarm("WEIGHT_DEVIATION",current.get("plateNumber"),
                        String.format("重量偏差%.2f%%超过5%%", percent),ldbh);
            }
        } catch (Exception e) {
            log.error("重量偏差检查失败", e);
        }
    }
}