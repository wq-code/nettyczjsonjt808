package net.map591.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.map591.entity.LocationData;
import net.map591.ai.ZyClwz;
import net.map591.ai.ZyLdjl;
import net.map591.mapper.WnDataMapper;
import net.map591.service.impl.TrackWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 轨迹采集处理器
 * 负责处理JT/T808协议上传的车辆轨迹点
 */
@Slf4j
@Service
public class TrajectoryProcessor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WnDataMapper wnDataMapper;

    @Autowired
    private LianDanRedisManager lianDanRedisManager;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private TrackWebSocketService trackWebSocketService;

    @Autowired
    private ObjectMapper objectMapper;

    // Redis key前缀
    private static final String PREFIX_LIANDAN = "ld:info:";
    private static final String PREFIX_VEHICLE_TRACKING = "tracking:vehicle:";
    public static final String PREFIX_TRACK = "track:line:";
    private static final String PREFIX_LAST_TIME = "last:time:";
    private static final String PREFIX_LATEST_GPS = "gps:latest:";

    // 日期格式化
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 处理轨迹点数据（由JT/T808协议调用）
     */
    public void processTrajectory(LocationData locationData) {
        if (locationData == null || !StringUtils.hasText(locationData.getPlatePhone())) {
            log.warn("轨迹数据无效：locationData为空或车牌为空");
            return;
        }

        String plateNumber = locationData.getPlatePhone();
        log.debug("收到车辆{}轨迹点：经度={}, 纬度={}, 时间={}",
                plateNumber, locationData.getLongitude(), locationData.getLatitude(),
                locationData.getGpsTime());

        try {
            // 1. 检查车辆是否有活跃联单（运输中的联单）
            String ldbh = getActiveLdbhForVehicle(plateNumber);
            if (ldbh == null) {
                log.debug("车辆{}当前无活跃联单，轨迹点不保存", plateNumber);
                return;
            }

            // 2. 验证联单状态是否为运输中
            String currentStatus = getLianDanStatus(ldbh);
            if (!"2".equals(currentStatus)) {
                log.debug("车辆{}联单{}状态不是运输中(当前:{})，轨迹点不保存",
                        plateNumber, ldbh, currentStatus);
                return;
            }

            // 3. 验证坐标有效性
            if (!isValidCoordinate(locationData.getLongitude(), locationData.getLatitude())) {
                log.warn("车辆{}坐标无效：经度={}, 纬度={}",
                        plateNumber, locationData.getLongitude(), locationData.getLatitude());
                return;
            }

            // 4. 保存轨迹点到数据库
            saveTrackPoint(ldbh, locationData);

            // 5. 更新Redis中的轨迹线
            updateTrackLine(ldbh, locationData);

            // 6. 增加轨迹点计数
            incrementTrackPointCount(ldbh);

            // 7. 缓存最新位置（用于实时显示）
            cacheLatestLocation(plateNumber, locationData);

            // 8. 更新最后活动时间（用于离线判断）
            updateLastActiveTime(ldbh, plateNumber);

            // 9. 通过WebSocket推送实时位置
            trackWebSocketService.pushTrackUpdate(ldbh, locationData);

            log.info("车辆{}轨迹点已保存，联单号：{}，经度：{}，纬度：{}",
                    plateNumber, ldbh, locationData.getLongitude(), locationData.getLatitude());

        } catch (Exception e) {
            log.error("处理车辆{}轨迹点失败", plateNumber, e);
        }
    }

    /**
     * 获取车辆当前活跃联单号
     * 优先从Redis获取，没有则查数据库
     */
    private String getActiveLdbhForVehicle(String plateNumber) {
        // 先从Redis获取
        String key = PREFIX_VEHICLE_TRACKING + plateNumber;
        String ldbh = redisTemplate.opsForValue().get(key);

        if (StringUtils.hasText(ldbh)) {
            return ldbh;
        }

        // Redis中没有，从数据库查询运输中的联单
        ldbh = wnDataMapper.selectActiveLdbhByPlate(plateNumber);
        if (StringUtils.hasText(ldbh)) {
            // 同步到Redis
            redisTemplate.opsForValue().set(key, ldbh, 7, TimeUnit.DAYS);
            log.info("从数据库同步车辆{}活跃联单到Redis：{}", plateNumber, ldbh);
        }

        return ldbh;
    }

    /**
     * 获取联单状态
     */
    private String getLianDanStatus(String ldbh) {
        String key = PREFIX_LIANDAN + ldbh;
        Object status = redisTemplate.opsForHash().get(key, "ldzt");
        if (status != null) {
            return status.toString();
        }

        // Redis中没有，从数据库查询
        ZyLdjl ldjl = wnDataMapper.selectLdjlByLdbh(ldbh);
        return ldjl != null ? ldjl.getLdzt() : null;
    }

    /**
     * 保存轨迹点到数据库
     */
    private void saveTrackPoint(String ldbh, LocationData location) {
        ZyClwz trackPoint = new ZyClwz();
        trackPoint.setLdbh(ldbh);
        trackPoint.setCphm(location.getPlatePhone());
        trackPoint.setLongitude(BigDecimal.valueOf(location.getLongitude()));
        trackPoint.setLatitude(BigDecimal.valueOf(location.getLatitude()));
        trackPoint.setSpeed(BigDecimal.valueOf(location.getSpeed()));
        trackPoint.setDirection(location.getDirection());
        trackPoint.setGpsTime(location.getGpsTime());
        trackPoint.setCreateTime(new Date());

        wnDataMapper.insertClwz(trackPoint);
    }

    /**
     * 更新Redis中的轨迹线（WKT格式：LINESTRING(x1 y1, x2 y2, ...)）
     */
    private void updateTrackLine(String ldbh, LocationData location) {
        String trackKey = PREFIX_TRACK + ldbh;
        String existingTrack = redisTemplate.opsForValue().get(trackKey);

        String newPoint = String.format("%f %f",
                location.getLongitude(), location.getLatitude());

        String newTrack;
        if (!StringUtils.hasText(existingTrack)) {
            newTrack = "LINESTRING(" + newPoint + ")";
        } else {
            // 提取已有坐标
            String coords = existingTrack
                    .replace("LINESTRING(", "")
                    .replace(")", "");
            newTrack = "LINESTRING(" + coords + ", " + newPoint + ")";
        }

        // 保存到Redis，设置7天过期
        redisTemplate.opsForValue().set(trackKey, newTrack, 7, TimeUnit.DAYS);

        // 异步更新数据库中的轨迹线（每10个点更新一次，减少数据库压力）
        String trackCountStr = (String) redisTemplate.opsForHash().get(
                PREFIX_LIANDAN + ldbh, "trackPoints");
        int trackCount = trackCountStr != null ? Integer.parseInt(trackCountStr) : 0;

        if (trackCount % 10 == 0) {
            CompletableFuture.runAsync(() -> {
                wnDataMapper.updateSjlxByLdbh(ldbh, newTrack);
                log.debug("异步更新联单{}轨迹线到数据库，当前点数：{}", ldbh, trackCount);
            });
        }
    }

    /**
     * 增加轨迹点计数
     */
    private void incrementTrackPointCount(String ldbh) {
        String key = PREFIX_LIANDAN + ldbh;
        Long count = redisTemplate.opsForHash().increment(key, "trackPoints", 1);
        log.debug("联单{}当前轨迹点数量：{}", ldbh, count);
    }

    /**
     * 缓存车辆最新位置
     */
    private void cacheLatestLocation(String plateNumber, LocationData location) {
        try {
            String key = PREFIX_LATEST_GPS + plateNumber;
            String value = objectMapper.writeValueAsString(location);
            redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("缓存车辆最新位置失败", e);
        }
    }

    /**
     * 更新车辆最后活动时间
     */
    private void updateLastActiveTime(String ldbh, String plateNumber) {
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        // 按联单记录
        String key1 = PREFIX_LAST_TIME + ldbh;
        redisTemplate.opsForValue().set(key1, now, 1, TimeUnit.DAYS);

        // 按车牌记录
        String key2 = PREFIX_LAST_TIME + plateNumber;
        redisTemplate.opsForValue().set(key2, now, 1, TimeUnit.DAYS);
    }

    /**
     * 验证坐标有效性
     */
    private boolean isValidCoordinate(double longitude, double latitude) {
        // 中国境内大致范围
        return longitude >= 70 && longitude <= 140
                && latitude >= 0 && latitude <= 55;
    }

    /**
     * 解析GPS时间
     */
    private Date parseGpsTime(String gpsTime) {
        try {
            if (StringUtils.hasText(gpsTime)) {
                LocalDateTime dateTime = LocalDateTime.parse(
                        gpsTime.replace(" ", "T"));
                return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
            }
        } catch (Exception e) {
            log.warn("解析GPS时间失败：{}", gpsTime);
        }
        return new Date();
    }

    /**
     * 检查离线预警（定时任务调用）
     */
    public void checkOfflineAlarms() {
        log.info("开始检查车辆离线状态");

        // 获取所有活跃联单
        // 这里需要根据实际情况实现，可以从数据库查询所有状态为2的联单
        // 此处省略具体实现
    }

    /**
     * 获取联单完整轨迹（用于前端展示）
     */
    public Map<String, Object> getLianDanTrack(String ldbh) {
        return lianDanRedisManager.getLianDanDetail(ldbh);
    }
}