package net.map591.ai;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.map591.entity.LocationData;
import net.map591.entity.ZyClczjl;
import net.map591.mapper.WnDataMapper;
import net.map591.service.impl.TrackWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AlarmService {
    
    @Autowired
    private WnDataMapper wnDataMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private TrackWebSocketService trackWebSocketService;
    @Autowired
    private LianDanRedisManager lianDanRedisManager;
    @Autowired
    private AlarmService alarmService;
    @Autowired
    private ObjectMapper objectMapper;


    public void checkGeofence(String ldbh, String plateNumber, LocationData location,String currentTrackLine ){
        // 调用数据库函数判断点是否在电子围栏内
        Boolean isInFence = wnDataMapper.checkPointInFence(
                location.getLongitude(),
                location.getLatitude()
        );
        if (Boolean.FALSE.equals(isInFence)) {
            // 不在围栏内，触发预警
            alarmService.createAlarm(
                    AlarmType.GEOFENCE,
                    plateNumber,
                    String.format("车辆超出电子围栏，当前位置：(%f,%f)", location.getLongitude(), location.getLatitude()),
                    ldbh
            );
        }
    }

    public void checkRouteDeviation(String ldbh, String plateNumber, LocationData location, RoutePlanInfo routePlan,String currentTrackLine) {
        // 计算点到规划路线的距离
        Double distance = wnDataMapper.calculateDistanceToRoute(
                location.getLongitude(),
                location.getLatitude(),
                plateNumber
        );
        if (distance == null) return;
        // 获取路线允许的偏离阈值
        Double allowedDeviation = routePlan.getDeviation().getYzValueMax();

        if (distance > allowedDeviation) {
            alarmService.createRouteDeviationAlarm(
                    ldbh,
                    plateNumber,
                    distance,
                    allowedDeviation,
                    currentTrackLine,  // 记录当前轨迹线
                    location           // 记录当前位置
            );
        }
    }
    /**
     * 路线偏离预警 - 记录轨迹
     */
    public void createRouteDeviationAlarm(String ldbh, String plateNumber,
                                          Double distance, Double maxDeviation,
                                          String trackLine, LocationData location) {
        ZyYjjl alarm = new ZyYjjl();
        alarm.setYjid(generateYjid());
        alarm.setLdbh(ldbh);
        alarm.setCphm(plateNumber);
        alarm.setYjlx(AlarmType.ROUTE_DEVIATION);
        alarm.setYjms(String.format("车辆偏离路线%.2f米，超过允许值%.2f米", distance, maxDeviation));
        alarm.setYjsj(new Date());
        alarm.setClzt("未处理");

        // 记录预警时的轨迹线和位置
        alarm.setTrackLine(trackLine);
        alarm.setAlarmPoint(String.format("POINT(%f %f)",
                location.getLongitude(), location.getLatitude()));

        wnDataMapper.insertYjjl(alarm);

        // WebSocket推送
//        trackWebSocketService.pushAlarm(alarm);
    }

    public void checkStayTimeout(String ldbh, String plateNumber, LocationData location,RoutePlanInfo routePlan, String currentTrackLine) {
        if (routePlan.getStay() == null) return;

        String stayKey = "stay:" + ldbh;
        String lastLocationJson = redisTemplate.opsForValue().get(stayKey);

        if (lastLocationJson != null) {
            try {
                LocationData lastLocation = objectMapper.readValue(lastLocationJson, LocationData.class);

                double distance = wnDataMapper.calculateDistanceWithPostGIS(
                        lastLocation.getLatitude(), lastLocation.getLongitude(),
                        location.getLatitude(), location.getLongitude()
                );

                // 如果移动距离小于50米，认为是停留
                if (distance < 50) {
                    String stayStartKey = "stay:start:" + ldbh;
                    String stayStartStr = redisTemplate.opsForValue().get(stayStartKey);

                    if (stayStartStr == null) {
                        redisTemplate.opsForValue().set(stayStartKey,
                                LocalDateTime.now().toString(), 1, TimeUnit.DAYS);
                    } else {
                        LocalDateTime stayStart = LocalDateTime.parse(stayStartStr);
                        long minutes = ChronoUnit.MINUTES.between(stayStart, LocalDateTime.now());

                        Double maxStayTime = routePlan.getStay().getYzValueMax();

                        if (minutes >= maxStayTime) {
                            // 记录停留期间的轨迹
                            alarmService.createStayTimeoutAlarm(
                                    ldbh,
                                    plateNumber,
                                    minutes,
                                    maxStayTime,
                                    currentTrackLine,  // 记录当前轨迹线
                                    location,
                                    stayStart
                            );
                        }
                    }
                } else {
                    redisTemplate.delete("stay:start:" + ldbh);
                }

            } catch (Exception e) {
                log.error("检查超时停留失败", e);
            }
        }

        // 更新当前位置
        try {
            redisTemplate.opsForValue().set(stayKey,
                    objectMapper.writeValueAsString(location), 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("缓存当前位置失败", e);
        }
    }
    /**
     * 超时停留预警 - 记录停留轨迹
     */
    public void createStayTimeoutAlarm(String ldbh, String plateNumber,
                                       long minutes, Double maxStayTime,
                                       String trackLine, LocationData location,
                                       LocalDateTime stayStart) {
        ZyYjjl alarm = new ZyYjjl();

        alarm.setLdbh(ldbh);
        alarm.setCphm(plateNumber);
        alarm.setYjlx(AlarmType.STAY_TIMEOUT);
        alarm.setYjms(String.format("车辆停留%d分钟，超过允许值%.0f分钟", minutes, maxStayTime));
        alarm.setYjsj(new Date());
        alarm.setClzt("未处理");

        // 记录停留期间的轨迹和位置
        alarm.setTrackLine(trackLine);
        alarm.setAlarmPoint(String.format("POINT(%f %f)",
                location.getLongitude(), location.getLatitude()));
        alarm.setStayStartTime(Timestamp.valueOf(stayStart));
        alarm.setStayEndTime(new Timestamp(System.currentTimeMillis()));

        wnDataMapper.insertYjjl(alarm);

//        trackWebSocketService.pushAlarm(alarm);
    }

    /**
     * 运输超时检查 - 记录全程轨迹
     */
    public void checkTransportTimeout(String ldbh, String plateNumber,
                                       RoutePlanInfo routePlan,
                                       String currentTrackLine) {
        if (routePlan.getTransport() == null) return;

        String startTimeStr = getLianDanStartTime(ldbh);
        if (startTimeStr == null) return;

        LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        long minutes = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());

        Double maxTransportTime = routePlan.getTransport().getYzValueMax();

        if (minutes > maxTransportTime) {
            // 记录全程轨迹
            alarmService.createTransportTimeoutAlarm(
                    ldbh,
                    plateNumber,
                    minutes,
                    maxTransportTime,
                    currentTrackLine,  // 记录全程轨迹线
                    startTime,
                    LocalDateTime.now()
            );
        }
    }
    /**
     * 获取联单开始时间
     */
    private String getLianDanStartTime(String ldbh) {
        if (!StringUtils.hasText(ldbh)) {
            return null;
        }
        String key = RedisKeys.PREFIX_LIANDAN + ldbh;
        // 1. 尝试从Redis获取
        Object startTime = redisTemplate.opsForHash().get(key, "startTime");
        if (startTime != null) {
            return startTime.toString();
        }
        // 2. 尝试获取创建时间作为备选
        Object createTime = redisTemplate.opsForHash().get(key, "createTimeStr");
        if (createTime != null) {
            log.debug("使用createTime作为联单开始时间: {}", createTime);
            return createTime.toString();
        }
        // 3. 从数据库获取
        try {
            ZyLdjl ldjl = wnDataMapper.selectLdjlByLdbh(ldbh);
            if (ldjl != null && ldjl.getCreateTime() != null) {
                String dbTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(ldjl.getCreateTime());

                // 同步到Redis
                redisTemplate.opsForHash().put(key, "startTime", dbTime);
                redisTemplate.opsForHash().put(key, "createTimeStr", dbTime);

                log.debug("从数据库同步联单开始时间: {}", dbTime);
                return dbTime;
            }
        } catch (Exception e) {
            log.error("从数据库查询联单时间失败", e);
        }
        return null;
    }

    /**
     * 运输超时预警 - 记录全程轨迹
     */
    public void createTransportTimeoutAlarm(String ldbh, String plateNumber,
                                            long minutes, Double maxTransportTime,
                                            String trackLine,
                                            LocalDateTime startTime,
                                            LocalDateTime currentTime) {
        ZyYjjl alarm = new ZyYjjl();
        alarm.setYjid(generateYjid());
        alarm.setLdbh(ldbh);
        alarm.setCphm(plateNumber);
        alarm.setYjlx(AlarmType.TRANSPORT_TIMEOUT);
        alarm.setYjms(String.format("运输超时：已运行%d分钟，超过允许值%.0f分钟",
                minutes, maxTransportTime));
        alarm.setYjsj(new Date());
        alarm.setClzt("未处理");

        // 记录全程轨迹
        alarm.setTrackLine(trackLine);
        alarm.setTransportStartTime(Timestamp.valueOf(startTime));
        alarm.setTransportCurrentTime(Timestamp.valueOf(currentTime));

        wnDataMapper.insertYjjl(alarm);

//        trackWebSocketService.pushAlarm(alarm);
    }


    public RoutePlanInfo  getRouteInfoByPlate(String plateNumber) {
        return wnDataMapper.getRouteInfoByPlate(plateNumber);
    }

    /**
     * 创建预警（带联单号）
     */
    public void createAlarm(String alarmType, String plateNumber, String message, String ldbh) {
        try {
            ZyYjjl alarm = new ZyYjjl();
            alarm.setYjid(generateYjid());
            alarm.setYjlx(alarmType);
            alarm.setCphm(plateNumber);
            alarm.setLdbh(ldbh);
            alarm.setYjms(message);
            alarm.setYjsj(new Date());
            alarm.setClzt("未处理");

            // 保存到数据库
            wnDataMapper.insertYjjl(alarm);

            // 缓存到Redis（用于实时推送）
            String alarmKey = "alarm:latest:" + plateNumber;
            redisTemplate.opsForValue().set(alarmKey, JSON.toJSONString(alarm), 1, TimeUnit.DAYS);

            log.warn("创建预警：类型={}, 车牌={}, 消息={}", alarmType, plateNumber, message);

            // WebSocket推送预警
//            trackWebSocketService.pushAlarm(alarm);

        } catch (Exception e) {
            log.error("创建预警失败", e);
        }
    }

    /**
     * 检查断联预警 - 定时任务
     * 每30分钟执行一次
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void checkDisconnectAlarm() {
        log.info("开始检查断联预警");

        // 查询等待队列中超过24小时的移出记录
        Set<String> keys = redisTemplate.keys(RedisKeys.PREFIX_WAITING_OUT + "*");
        if (keys == null) return;

        for (String key : keys) {
            String outJson = redisTemplate.opsForValue().get(key);
            if (outJson == null) continue;

            try {
                ZyClczjl outRecord = JSON.parseObject(outJson, ZyClczjl.class);

                // 获取移出记录的创建时间（使用服务器时间）
                String createTimeStr = outRecord.getCreateTime();
                if (createTimeStr == null) continue;

                LocalDateTime createTime = LocalDateTime.parse(
                        createTimeStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );

                long hours = ChronoUnit.HOURS.between(createTime, LocalDateTime.now());

                // 超过48小时未匹配接收记录
                if (hours >= 48) {
                    String plateNumber = key.replace(RedisKeys.PREFIX_WAITING_OUT, "");

                    alarmService.createAlarm(
                            AlarmType.DISCONNECT,
                            plateNumber,
                            String.format("移出后%d小时未接收，称重ID=%s",
                                    hours, outRecord.getCzjlid()),
                            outRecord.getLdbh()
                    );

                    // 从等待队列删除，避免重复预警
                    redisTemplate.delete(key);

                    // 将联单状态更新为异常(4)
                    lianDanRedisManager.updateLianDanStatus(outRecord.getLdbh(), "4");
                }

            } catch (Exception e) {
                log.error("检查断联预警失败", e);
            }
        }
    }

    private String generateYjid() {
        return "YJ" + System.currentTimeMillis() +
               String.format("%03d", new Random().nextInt(1000));
    }

    public class AlarmType {
        // 称重相关预警
        public static final String WEIGHT_DEVIATION = "WEIGHT_DEVIATION";  // 重量偏差
        public static final String DUPLICATE_UPLOAD = "DUPLICATE_UPLOAD";  // 重复上传
        public static final String DISCONNECT = "DISCONNECT";              // 断联

        // 轨迹相关预警
        public static final String STAY_TIMEOUT = "STAY_TIMEOUT";          // 超时停留
        public static final String TRANSPORT_TIMEOUT = "TRANSPORT_TIMEOUT"; // 运输超时
        public static final String ROUTE_DEVIATION = "ROUTE_DEVIATION";    // 路线偏离
        public static final String GEOFENCE = "GEOFENCE";                  // 超出电子围栏

        public static final String INVALID_WEIGHT = "INVALID_WEIGHT";      // 无效净重（为0）
        public static final String UNAUTHORIZED_VEHICLE = "UNAUTHORIZED_VEHICLE"; // 车辆未授权
    }


}