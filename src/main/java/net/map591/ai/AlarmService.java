package net.map591.ai;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.map591.entity.ZyClczjl;
import net.map591.mapper.WnDataMapper;
import net.map591.service.impl.TrackWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
     * 检查超时预警（定时任务）
     */
    @Scheduled(fixedDelay = 3600000) // 每小时
    public void checkTimeoutAlarm() {
        log.info("开始检查超时预警");
        
        Set<String> keys = redisTemplate.keys("waiting:out:*");
        if (keys == null) return;
        
        for (String key : keys) {
            String outJson = redisTemplate.opsForValue().get(key);
            if (outJson == null) continue;
            
            try {
                ZyClczjl outRecord = JSON.parseObject(outJson, ZyClczjl.class);
                String czsj = outRecord.getCzsj();
                
                if (czsj == null) continue;
                
                // 计算时间差
                LocalDateTime outTime = LocalDateTime.parse(czsj.replace(" ", "T"));
                long hours = ChronoUnit.HOURS.between(outTime, LocalDateTime.now());
                
                if (hours >= 24) {
                    String plateNumber = key.replace("waiting:out:", "");
                    createAlarm("TIMEOUT", plateNumber,
                        String.format("移出后%d小时未接收", hours),
                        outRecord.getLdbh());
                    
                    // 删除已预警的key，避免重复预警
                    redisTemplate.delete(key);
                }
            } catch (Exception e) {
                log.error("检查超时预警失败", e);
            }
        }
    }
    
    private String generateYjid() {
        return "YJ" + System.currentTimeMillis() + 
               String.format("%03d", new Random().nextInt(1000));
    }

}