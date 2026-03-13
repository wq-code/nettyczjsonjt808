package net.map591.ai;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.map591.entity.WnTransportData;
import net.map591.entity.ZyClczjl;
import net.map591.mapper.WnDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class WeighingDataProcessor {

    @Autowired
    private WnDataMapper wnDataMapper;

    @Autowired
    private LianDanRedisManager lianDanRedisManager;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PROCESSED_KEY_PREFIX = "processed:weigh:";
    private static final String WAITING_OUT_PREFIX = "waiting:out:";

    /**
     * 检查数据是否已处理（幂等检查）
     */
    public boolean isProcessed(String sDetectCode) {
        if (sDetectCode == null || sDetectCode.isEmpty()) {
            return false;
        }

        // 1. 先查Redis缓存
        String key = PROCESSED_KEY_PREFIX + sDetectCode;
        Boolean hasKey = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(hasKey)) {
            log.info("Redis中已存在处理记录：{}", sDetectCode);
            return true;
        }

        // 2. Redis中没有，查数据库
        Integer count = wnDataMapper.existsBySDetectCode(sDetectCode);

        if (count != null && count > 0) {
            // 3. 同步到Redis，设置7天过期
            redisTemplate.opsForValue().set(key, "1", 7, TimeUnit.DAYS);
            log.info("数据库中存在记录，已同步到Redis：{}", sDetectCode);
            return true;
        }

        return false;
    }

    /**
     * 标记数据为已处理
     */
    private void markAsProcessed(String sDetectCode) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        String key = PROCESSED_KEY_PREFIX + sDetectCode;
                        redisTemplate.opsForValue().set(key, "1", 7, TimeUnit.DAYS);
                        log.info("事务提交后标记已处理：{}", sDetectCode);
                    }
                }
        );
    }

    /**
     * 处理称重数据
     */
    @Transactional
    public void processWeighingData(WnTransportData data) {
        String detectCode = data.getSDetectCode();

        // 1. 幂等检查
        if (isProcessed(detectCode)) {
            log.info("数据已处理，跳过：{}", detectCode);
            return;
        }

        try {
            // 2. 保存原始数据
            wnDataMapper.insertWnData1(data);

            // 3. 保存称重记录
            ZyClczjl record = convertToZyClczjl(data);
            wnDataMapper.insertClczjl(record);

            // 4. 联单匹配处理（此时record已经有ldbh）
            handleLianDanMatching(record);

            // 5. 标记为已处理（联单匹配成功后标记）
            markAsProcessed(detectCode);

            log.info("称重数据处理完成：{}, 联单号：{}", detectCode, record.getLdbh());

        } catch (Exception e) {
            log.error("处理称重数据失败：{}", detectCode, e);
            throw new RuntimeException("处理失败", e);
        }
    }

    /**
     * 转换为称重记录
     */
    private ZyClczjl convertToZyClczjl(WnTransportData data) {
        ZyClczjl record = new ZyClczjl();
        record.setCzjlid(data.getSDetectCode());
        record.setCphm(data.getSPlateName());
        record.setCzdd(data.getSSiteCode());
        record.setCzsj(data.getSDateTime());
        record.setMz(convertToBigDecimal(data.getTimeStampOne()));
        record.setPz(convertToBigDecimal(data.getTimeStampTwo()));
        record.setJz(convertToBigDecimal(data.getSuttle()));
        record.setJzljlx("3");

        // 根据站点和车牌设置移出/接收单位
        setTransferInfo(record, data);

        // 设置图片数据
        record.setSBase64PlateData(data.getSBase64PlateData());
        record.setSBase64HeadData(data.getSBase64HeadData());
        record.setSBase64BodyData(data.getSBase64BodyData());
        record.setSBase64TailData(data.getSBase64TailData());

        record.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return record;
    }

    /**
     * 设置移出/接收单位信息
     */
    private void setTransferInfo(ZyClczjl record, WnTransportData data) {
        switch (data.getSSiteCode()) {
            case "112661":
                switch (data.getSPlateName()) {
                    case "皖M8A011":
                        record.setJsgdbh("天长市污水处理厂");
                        record.setCzcbm("天长市污泥处置厂");
                        break;
                    case "皖M4B961":
                    case "皖M9A017":
                    case "皖M8A862":
                    case "豫QC2010":
                        record.setJsgdbh("乡镇水务");
                        record.setCzcbm("天长市污水处理厂");
                        break;
                    default:
                        record.setJsgdbh("");
                        record.setCzcbm("");
                }
                break;

            case "112662":
                switch (data.getSPlateName()) {
                    case "皖M8A011":
                        record.setJsgdbh("天长市污水处理厂");
                        record.setCzcbm("天长市污泥处置厂");
                        break;
                    case "皖N43777":
                        record.setJsgdbh("东市区污水处理厂");
                        record.setCzcbm("天长市污泥处置厂");
                        break;
                    default:
                        record.setJsgdbh("");
                        record.setCzcbm("");
                }
                break;

            case "112663":
                if ("皖N43777".equals(data.getSPlateName())) {
                    record.setJsgdbh("东市区污水处理厂");
                    record.setCzcbm("天长市污泥处置厂");
                } else {
                    record.setJsgdbh("");
                    record.setCzcbm("");
                }
                break;

            default:
                record.setJsgdbh("");
                record.setCzcbm("");
        }
    }

    /**
     * 联单匹配处理
     */
    private void handleLianDanMatching(ZyClczjl record) {
        String plateNumber = record.getCphm();
        String siteCode = record.getCzdd();

        if (isTransferOutSite(siteCode)) {
            // 移出站点处理
            handleOutRecord(record);

        } else if (isTransferInSite(siteCode)) {
            // 接收站点处理
            handleInRecord(record);
        }
    }

    /**
     * 处理移出记录
     * 接收到移出称重数据 → 立即开始运输，状态设为运输中(2)
     */
    private void handleOutRecord(ZyClczjl record) {
        String plateNumber = record.getCphm();

        log.info("移出记录生成联单号：{}，车辆开始运输", record.getLdbh());

        // 保存到Redis等待队列
        String waitingKey = WAITING_OUT_PREFIX + plateNumber;
        redisTemplate.opsForValue().set(waitingKey,
                JSON.toJSONString(record),
                72, TimeUnit.HOURS);

        // 创建联单，状态直接设为运输中(2)
        lianDanRedisManager.createLianDanWithTransport(record);

        log.info("移出记录处理完成：车牌={}, 联单号={}, 状态=运输中",
                plateNumber, record.getLdbh());
    }

    /**
     * 处理接收记录
     * 接收到接收称重数据 → 运输结束，状态设为已接收(3)
     */
    private void handleInRecord(ZyClczjl record) {
        String plateNumber = record.getCphm();

        // 查找匹配的移出记录
        String waitingKey = WAITING_OUT_PREFIX + plateNumber;
        String outJson = redisTemplate.opsForValue().get(waitingKey);

        if (outJson != null) {
            // 找到移出记录，完成联单
            ZyClczjl outRecord = JSON.parseObject(outJson, ZyClczjl.class);

            // 使用移出记录的联单号更新接收记录
            record.setLdbh(outRecord.getLdbh());  // 使用移出记录的联单号

            // 更新联单为已完成（状态3）
            lianDanRedisManager.completeLianDan(outRecord.getLdbh(), record);

            // 从等待队列删除
            redisTemplate.delete(waitingKey);

            log.info("联单匹配成功：联单号={}, 状态=已接收(3), 移出={}, 接收={}",
                    outRecord.getLdbh(), outRecord.getCzjlid(), record.getCzjlid());

            // 触发重量偏差检查
            checkWeightDeviation(outRecord, record);

        } else {
            // 没找到移出记录，可能是先接收后移出？记录异常
            log.warn("接收记录无匹配移出：车牌={}, 当前联单号={}", plateNumber, record.getLdbh());

            // 保存到Redis异常队列，等待人工处理或后续匹配
            String abnormalKey = "abnormal:in:" + plateNumber + ":" + record.getCzjlid();
            redisTemplate.opsForValue().set(abnormalKey, JSON.toJSONString(record), 7, TimeUnit.DAYS);

            // 触发断联预警
            alarmService.createAlarm(
                    "DISCONNECT",
                    plateNumber,
                    "接收记录无对应移出记录",
                    record.getLdbh()  // 这里可能为null，但预警服务会处理
            );
        }
    }

    /**
     * 重量偏差检查
     */
    private void checkWeightDeviation(ZyClczjl outRecord, ZyClczjl inRecord) {
        BigDecimal outJz = outRecord.getJz() != null ? outRecord.getJz() : BigDecimal.ZERO;
        BigDecimal inJz = inRecord.getJz() != null ? inRecord.getJz() : BigDecimal.ZERO;

        if (outJz.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("移出净重为0，跳过重量偏差检查");
            return;
        }

        // 计算偏差百分比
        BigDecimal diff = inJz.subtract(outJz).abs();
        BigDecimal percent = diff.divide(outJz, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        if (percent.compareTo(new BigDecimal("5")) > 0) {
            String message = String.format("重量偏差%.2f%%超过5%% (移出:%.2fkg, 接收:%.2fkg)",
                    percent, outJz, inJz);

            alarmService.createAlarm(
                    "WEIGHT_DEVIATION",
                    outRecord.getCphm(),
                    message,
                    outRecord.getLdbh()
            );
        }
    }

    /**
     * 判断是否是移出站点
     */
    private boolean isTransferOutSite(String siteCode) {
        return "112661".equals(siteCode) || "112663".equals(siteCode);
    }

    /**
     * 判断是否是接收站点
     */
    private boolean isTransferInSite(String siteCode) {
        return "112662".equals(siteCode);
    }

    /**
     * 转换BigDecimal
     */
    private BigDecimal convertToBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}