package net.map591.utils;

import lombok.extern.slf4j.Slf4j;
import net.map591.entity.LocationData;
import net.map591.mapper.WnDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisGpsUtil {

    private static final String VEHICLE_CACHE_KEY = "vehicle:latest";
    private static final int MAX_VEHICLES = 6;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @PostConstruct
    public void init() {
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        log.info("RedisGpsUtil 初始化完成，redisTemplate: {}", redisTemplate != null ? "已注入" : "未注入");
    }

    /**
     * 缓存车辆最新位置
     */
    public void cacheVehicleLatest(LocationData locationData) {
        try {
            // 使用车牌号作为field
            String platePhone = locationData.getPlatePhone();

            // 缓存数据
            redisTemplate.opsForHash().put(
                    VEHICLE_CACHE_KEY,
                    platePhone,
                    locationData
            );


        } catch (Exception e) {
            log.error("Redis缓存车辆数据失败", e);
        }
    }


    /**
     * 获取所有缓存车辆数据
     */
    public Map<String, LocationData> getAllVehicleLatest() {
        try {
            Map<Object, Object> map = redisTemplate.opsForHash().entries(VEHICLE_CACHE_KEY);
            Map<String, LocationData> result = new HashMap<>();

            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(
                            entry.getKey().toString(),
                            (LocationData) entry.getValue()
                    );
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取Redis缓存车辆数据失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取指定车辆的最新数据
     */
    public LocationData getVehicleLatest(String platePhone) {
        try {
            // 参数校验
            if (platePhone == null || platePhone.trim().isEmpty()) {
                log.warn("获取车辆最新位置失败: 车牌号为空");
                return null;
            }
            return (LocationData) redisTemplate.opsForHash().get(VEHICLE_CACHE_KEY, platePhone);
        } catch (Exception e) {
            log.error("获取指定车辆Redis缓存失败: platePhone={}", platePhone, e);
            return null;
        }
    }

    /**
     * 清除所有车辆缓存
     */
    public void clearAllVehicleCache() {
        redisTemplate.delete(VEHICLE_CACHE_KEY);
    }



    /**
     * 获取车辆当前运输的联单号
     */
    public String getCurrentLdbhByPlate(String plateNumber) {
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            log.warn("获取车辆最新位置失败: 车牌号为空");
            return null;
        }
        String vehicleMappingKey = "vehicle_ldbh_mapping_key" + plateNumber;
        try {
            return (String) redisTemplate.opsForValue().get(vehicleMappingKey);
        } catch (Exception e) {
            log.error("从Redis获取车辆联单号失败, 车牌号: {}", plateNumber, e);
            return null;
        }
    }

    /**
     * 检查车辆是否在运输中
     */
    public Boolean isVehicleTracking(String plateNumber) {
        // 1. 首先检查 redisTemplate 是否为空
        if (redisTemplate == null) {
            log.error("redisTemplate is null, cannot check vehicle tracking");
            return false;
        }
        // 2. 检查车牌号是否为空
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            log.warn("isVehicleTracking: 车牌号参数为空");
            return false;
        }
        try {
            String triggerKey = "track_trigger_key_prefix" + plateNumber;
            return redisTemplate.hasKey(triggerKey);
        } catch (Exception e) {
            log.error("检查车辆跟踪状态失败，车牌号: {}, 错误: {}", plateNumber, e.getMessage(), e);
            return false;
        }
    }




    /**
     * 开始记录轨迹 - Redis标记
     */
    public void startTracking(String ldbh, String plateNumber) {
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            log.error("车牌号为空，无法开始跟踪");
            return;
        }
        try {
            // 1. 触发键：标记车辆需要采集轨迹
            String triggerKey = "track_trigger_key_prefix" + plateNumber;
            redisTemplate.opsForValue().set(triggerKey, ldbh);

            // 2. 车辆-联单映射：便于根据车牌查找当前联单
            String vehicleMappingKey = "vehicle_ldbh_mapping_key" + plateNumber;
            redisTemplate.opsForValue().set(vehicleMappingKey, ldbh);

            // 3. 活跃联单集合：记录所有正在运输的联单
            redisTemplate.opsForSet().add("track_active_key", ldbh);

            // 4. 初始化轨迹临时缓存
            String tempTrackKey = "track_temp_key_prefix" + ldbh;
            redisTemplate.delete(tempTrackKey); // 清空之前的缓存

            log.info("开始轨迹记录 - 联单: {}, 车牌: {}", ldbh, plateNumber);

        }catch (Exception e) {
            log.error("Redis标记开始记录失败 - 联单: {}, 车牌: {}", ldbh, plateNumber, e);
        }
    }

    /**
     * 停止记录轨迹 - Redis标记
     */
    public void stopTracking(String ldbh, String plateNumber) {
        try {

            // 1. 移除触发标记
            String triggerKey = "track_trigger_key_prefix" + plateNumber;
            Object currentLdbh = redisTemplate.opsForValue().get(triggerKey);

            // 确保停止的是正确的联单
            if (currentLdbh != null && currentLdbh.equals(ldbh)) {
                redisTemplate.delete(triggerKey);

                // 2. 移除车辆-联单映射
                String vehicleMappingKey = "vehicle_ldbh_mapping_key" + plateNumber;
                redisTemplate.delete(vehicleMappingKey);

                // 3. 从活跃联单集合中移除
                redisTemplate.opsForSet().remove("track_active_key", ldbh);

                // 5. 标记轨迹为待处理状态（供轨迹处理服务消费）
                String pendingKey = "track:pending:" + ldbh;
                redisTemplate.opsForValue().set(pendingKey, "true");

                log.info("停止轨迹记录 - 联单: {}, 车牌: {}", ldbh, plateNumber);

            }

        } catch (Exception e) {
            log.error("Redis标记停止记录失败 - 联单: {}, 车牌: {}", ldbh, plateNumber, e);
        }
    }
    /**
     * 定时清理不再需要的Redis键
     * 可以每天凌晨执行一次
     */
//    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupRedisKeys() {
        log.info("开始清理Redis过期键...");
        try {
            // 1. 检查所有活跃联单，与数据库状态对比
            Map<String, LocationData> allVehicles = getAllVehicleLatest();
            for (Map.Entry<String, LocationData> entry   : allVehicles.entrySet()) {
                String platePhone = entry.getKey();
                String ldbh = getCurrentLdbhByPlate(platePhone);
                if (ldbh != null&&ldbh.trim().isEmpty()) {
                    log.info("定时清理联单编号失败，车辆联单编号是空");
                }
            }
            log.info("Redis键清理完成");
        } catch (Exception e) {
            log.error("清理Redis键失败", e);
        }
    }

}
