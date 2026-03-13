package net.map591.service.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.map591.entity.*;
import net.map591.mapper.WnDataMapper;
import net.map591.utils.RedisGpsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WnDataAndImageImpl2 {

    @Autowired
    private WnDataMapper wnDataMapper;

    @Autowired
    private RedisGpsUtil redisGpsUtil;

    /**
     * 生成联单编号 行政区划+年月日+流水号
     * LD3411812025103000001
     */
    private String generateLDWithLock() {
        LocalDate today = LocalDate.now();
        String datePrefix = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 获取行政区划代码
        String areaCode = "LD341181";

        String ldDatePrefix = areaCode + datePrefix;
        String maxSerialNoStr = wnDataMapper.selectLdbhMaxForUpdate(ldDatePrefix);
        long nextSerialNumber = 1;
        if (maxSerialNoStr != null && !maxSerialNoStr.isEmpty()) {
            try {
                String serialPart = maxSerialNoStr.substring(ldDatePrefix.length());
                long currentSerial = Long.parseLong(serialPart);
                nextSerialNumber = currentSerial + 1;

                if (nextSerialNumber > 999999999) {
                    throw new RuntimeException("当日流水号已超过最大值99999");
                }
            } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                throw new RuntimeException("流水号解析失败，无法生成唯一编号", e);
            }
        }

        return ldDatePrefix + String.format("%05d", nextSerialNumber);
    }

    @Transactional
    public void saveWnData(WnTransportData data) {
        if (Objects.isNull(data)) {
            log.warn("接收到的数据为空");
            return;
        }
        String siteCode=data.getSSiteCode();
        String productName=data.getProductName();
        String sPlateName = data.getSPlateName();
        BigDecimal nTotalWeight =new BigDecimal(data.getNTotalWeight());
        BigDecimal suttle = new BigDecimal(data.getSuttle());
        BigDecimal pz=nTotalWeight.subtract(suttle);
        ZyJzljys zyJzljys = new ZyJzljys();

        // 接收称重的数据
        // 根据站点编号和车牌号分类
        // 皖M8A011 皖M4B961 皖M8A862 皖M9A017 豫QC2010 皖N43777
        /*
         *
         * 污泥处置厂接收的固定车辆： 天长市污水厂处理厂车辆皖M8A011 东市区污水处理厂车辆皖N43777
         * 目前为止就这两车是处理污泥车辆
         * 其余车辆是乡镇运输到天长市污水厂处理厂的车
            水厂进厂(空车)  → 水厂出厂（重车） → 运输途中 → 砖厂进厂（重车） → 完成联单
                 ↓            ↓                    ↓              ↓
               空车称重           开始运输             轨迹追踪         结束运输
         */
        //天长市污水厂处理厂
        if ("112661".equals(siteCode)){
            if ("皖M8A011".equals(sPlateName)){
                //天长市污水处理厂运输污泥处置厂的称重数据
            }else if ("皖M4B961".equals(sPlateName)){
                //乡镇水务运输天长市污水处理厂的称重数据
            }else if ("皖M8A862".equals(sPlateName)){
                //乡镇水务运输天长市污水处理厂的称重数据
            }else if ("皖M9A017".equals(sPlateName)){
                //乡镇水务运输天长市污水处理厂的称重数据
            }else if ("豫QC2010".equals(sPlateName)){
                //乡镇水务运输天长市污水处理厂的称重数据
            }else {
                //天长市污水处理厂未知车辆的称重数据
            }

        }else if ("112662".equals(siteCode)){
            if ("皖M8A011".equals(sPlateName)){
                //天长市污水处理厂运输污泥处置厂的称重数据
            }else if ("皖N43777".equals(sPlateName)){
                //东市区污水处理厂运输污泥处置厂的称重数据
            }
            else {
                //东市区污水处理厂未知车辆的称重数据
            }

        }else if ("112663".equals(siteCode)){
            if ("皖N43777".equals(sPlateName)){
                //东市区污水处理厂运输污泥处置厂的称重数据
            }else {
                //未知车辆的称重数据
            }

        }else {
            //未知站点的称重数据
        }
        //天长市污泥处置场
        //东市区污水处理厂




        //天长市污水厂
        if ("112661".equals(siteCode)){
            log.info("天长市污水厂接收数据");
            boolean isM8A011 = "皖M8A011".equals(sPlateName);
            if (!isM8A011){
                log.info("当前是天长市污水厂乡镇水务的称重数据，车牌号：{}", sPlateName);
                ZyClczjl zyClczjl = new ZyClczjl();
                zyClczjl.setCphm(sPlateName);
                zyClczjl.setCzlx("2");
                zyClczjl.setJzljlx("2");
                if (productName != null && productName.contains("含水率")){
                    Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
                    Matcher matcher = pattern.matcher(productName);
                    if (matcher.find()){
                        String gr = matcher.group();
                        zyClczjl.setWnhsl(convertToBigDecimal(gr));
                    }
                }
                zyClczjl.setCzsj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                zyClczjl.setJcczzl(nTotalWeight);
                zyClczjl.setMz(nTotalWeight);
                zyClczjl.setPz(pz);
                zyClczjl.setJz(suttle);
                zyClczjl.setCzdd("中冶水务有限公司");
                zyClczjl.setCzcbm("112661");
                zyClczjl.setSjly("1");
                zyClczjl.setZczp(data.getSBase64BodyData());
                zyClczjl.setBz("乡镇水务进厂称重记录");
                zyClczjl.setLdbh("L/N");//不是运输任务不联单
                log.info("中冶水务接收乡镇称重记录{}", zyClczjl);
                wnDataMapper.insertClczjl(zyClczjl);

                ZyJzljjs zyJzljjs = new ZyJzljjs();
                zyJzljjs.setLdbh("L/N");
                zyJzljjs.setCzcbm("112661");
                zyJzljjs.setCzcmc("中冶水务有限公司");
                zyJzljjs.setCphm(sPlateName);
                zyJzljjs.setSnrq(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                zyJzljjs.setSnsj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                zyJzljjs.setJzljlx("2");
                zyJzljjs.setCzjz(suttle);
                zyJzljjs.setLdzt("3");
                zyJzljjs.setCzjlid(zyClczjl.getCzjlid());
                zyJzljjs.setBz("乡镇水务进厂接收记录");
                log.info("中冶水务乡镇水务接收记录{}", zyJzljjs);
                wnDataMapper.insertJzljjs(zyJzljjs);
                log.info("保存结束天长市污水厂乡镇水务的称重数据");
            }else {
                log.info("当前车辆称重是天长市污水厂运输污泥处置场的数据，车牌号：{}", sPlateName);
                ZyClczjl zyClczjl = new ZyClczjl();
                zyClczjl.setCzcbm("112661");
                zyClczjl.setCphm(sPlateName);
                zyClczjl.setCzbbh("");
                zyClczjl.setCzlx("2");
                zyClczjl.setJzljlx("3");
                zyClczjl.setCzsj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                zyClczjl.setCcczsj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                zyClczjl.setMz(nTotalWeight);
                zyClczjl.setPz(pz);
                zyClczjl.setJz(suttle);
                zyClczjl.setCzdd("中冶水务有限公司");
                String ldbh = generateLDWithLock();
                zyClczjl.setLdbh(ldbh);
                zyClczjl.setSjly("3");
                zyClczjl.setZczp(data.getSBase64BodyData());
                zyClczjl.setBz("中冶水务移出称重记录");
                log.info("中冶水务移出称重记录{}", zyClczjl);
                wnDataMapper.insertClczjl(zyClczjl);

                ZyJzljyc zyJzljyc = new ZyJzljyc();
                zyJzljyc.setLdbh(ldbh);
                zyJzljyc.setYcrq(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                zyJzljyc.setYcsj(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                zyJzljyc.setSfkqys(false);
                zyJzljyc.setJsgdbh("112661");
                zyJzljyc.setYsqybh("");
                zyJzljyc.setCzcbm("112662");
                zyJzljyc.setJzljlx("");
                zyJzljyc.setJzljzl(suttle);
                zyJzljyc.setYscph(sPlateName);
                zyJzljyc.setJsyxm("");
                zyJzljyc.setLdzt("2");
                zyJzljyc.setXczp(data.getSBase64BodyData());
                zyJzljyc.setCzjlid(zyClczjl.getCzjlid());
                zyJzljyc.setBz("中冶水务移出记录");
                log.info("中冶水务移出记录{}", zyJzljyc);
                wnDataMapper.insertJzljyc(zyJzljyc);


                zyJzljys.setLdbh(ldbh);
                zyJzljys.setYsrq(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                zyJzljys.setYskssj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                zyJzljys.setYsgj("1");
                zyJzljys.setCphm(sPlateName);
                zyJzljys.setYsqd("112661");
                zyJzljys.setYszd("112662");
                zyJzljys.setYszl(suttle);
                List<Map> clYs = getClYs(sPlateName);//获取车辆预设路线
                if (clYs!=null&&!clYs.isEmpty()) {
                    for (Map map : clYs) {
                        String geom = (String) map.get("geom");
                        zyJzljys.setYslx(geom);
                    }
                }else {
                    log.error("车辆未设预定路线{}", sPlateName);
                    zyJzljys.setYslx("");
                }
//                zyJzljys.setSjlx("");//数据库geometry 开始持续记录点位形成线
                zyJzljys.setLdzt("2");
                zyJzljys.setBz("车辆离场开始记录运输路线");
                log.info("中冶水务移出运输记录{}", zyJzljys);
                wnDataMapper.insertJzljys(zyJzljys);
                log.info("运输开始 - 联单号: {}, 车牌: {}", ldbh, sPlateName);

                redisGpsUtil.startTracking(ldbh, sPlateName);
            }
        }
        //天道制砖厂
        if ("112662".equals(siteCode)){
            String plate = data.getSPlateName();
            // 添加空值检查
            if (plate == null || plate.trim().isEmpty()) {
                log.warn("接收到的车牌号为空，无法处理制砖厂数据");
                return;
            }
            //根据当前的车牌号判断是否是指定车辆到达制砖厂
            String brickTime  = data.getSDateTime();

            // 1. 查找车牌的联单记录
            String ldbh = null;
            if (!redisGpsUtil.isVehicleTracking( plate)){
                log.warn("车牌 {} 到达砖厂，但找不到对应的联单记录：{}", plate,ldbh);
                return;
            }
            ldbh=redisGpsUtil.getCurrentLdbhByPlate( plate);

            ZyClczjl zyClczjl = new ZyClczjl();
            zyClczjl.setCzcbm("112662");
            zyClczjl.setCphm(sPlateName);
            zyClczjl.setCzbbh("");
            zyClczjl.setCzlx("4");
            zyClczjl.setJzljlx("3");
            zyClczjl.setCzsj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            zyClczjl.setCcczsj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            zyClczjl.setMz(nTotalWeight);
            zyClczjl.setPz(pz);
            zyClczjl.setJz(suttle);
            zyClczjl.setCzdd("天道新型建材有限公司");
            zyClczjl.setLdbh(ldbh);
            zyClczjl.setSjly("3");
            zyClczjl.setZczp(data.getSBase64BodyData());
            zyClczjl.setBz("天道移入称重记录");
            log.info("天道接收称重记录{}", zyClczjl);
            wnDataMapper.insertClczjl(zyClczjl);

            ZyJzljjs zyJzljjs = new ZyJzljjs();
            zyJzljjs.setLdbh(ldbh);
            zyJzljjs.setCzcbm("112662");
            zyJzljjs.setCzcmc("天道新型建材有限公司");
            zyJzljjs.setCphm(sPlateName);
            zyJzljjs.setSnrq(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            zyJzljjs.setSnsj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            zyJzljjs.setJzljlx("2");
            zyJzljjs.setCzjz(suttle);
            zyJzljjs.setLdzt("3");
            zyJzljjs.setCzjlid(zyClczjl.getCzjlid());
            zyJzljjs.setBz("天道砖厂接收记录");
            log.info("天道接收记录{}", zyJzljjs);
            wnDataMapper.insertJzljjs(zyJzljjs);

            updateAllLianDanStatus(ldbh, "3");

            log.info("运输完成 - 联单号: {}, 车牌: {}", ldbh, sPlateName);

            // Redis标记该车辆需要停止记录（关联LDBH）
            redisGpsUtil.stopTracking(ldbh,plate);
        }
        //东市区污水厂
        if ("112663".equals(siteCode)){
            ZyClczjl zyClczjl = new ZyClczjl();

            zyClczjl.setCzcbm("112663");
            zyClczjl.setCphm(sPlateName);
            zyClczjl.setCzbbh("");
            zyClczjl.setCzlx("3");
            zyClczjl.setJzljlx("3");
            zyClczjl.setCzsj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            zyClczjl.setCcczsj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            zyClczjl.setMz(nTotalWeight);
            zyClczjl.setPz(pz);
            zyClczjl.setJz(suttle);
            zyClczjl.setCzdd("东市区污水有限公司");
            String ldbh = generateLDWithLock();
            zyClczjl.setLdbh(ldbh);
            zyClczjl.setSjly("3");
            zyClczjl.setZczp(data.getSBase64BodyData());
            zyClczjl.setBz("东市区污水称重记录");
            log.info("东市区污水称重记录{}", zyClczjl);
            wnDataMapper.insertClczjl(zyClczjl);


            ZyJzljyc zyJzljyc = new ZyJzljyc();
            zyJzljyc.setLdbh(ldbh);
            zyJzljyc.setYcrq(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            zyJzljyc.setYcsj(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            zyJzljyc.setSfkqys(false);
            zyJzljyc.setJsgdbh("112663");
            zyJzljyc.setYsqybh("");
            zyJzljyc.setCzcbm("112662");
            zyJzljyc.setJzljlx("");
            zyJzljyc.setJzljzl(suttle);
            zyJzljyc.setYscph(sPlateName);
            zyJzljyc.setJsyxm("");
            zyJzljyc.setLdzt("2");
            zyJzljyc.setXczp(data.getSBase64BodyData());
            zyJzljyc.setCzjlid(zyClczjl.getCzjlid());
            zyJzljyc.setBz("东市区污水移出记录");
            log.info("东市区污水移出记录{}", zyJzljyc);
            wnDataMapper.insertJzljyc(zyJzljyc);

            zyJzljys.setLdbh(ldbh);
            zyJzljys.setYsrq(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            zyJzljys.setYskssj(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            zyJzljys.setYsgj("");
            zyJzljys.setCphm(sPlateName);
            zyJzljys.setYsqd("112663");
            zyJzljys.setYszd("112662");
            zyJzljys.setYszl(suttle);
            List<Map> clYs = getClYs(sPlateName);//获取车辆预设路线
            if (clYs!=null&&!clYs.isEmpty()) {
                for (Map map : clYs) {
                    String geom = (String) map.get("geom");
                    zyJzljys.setYslx(geom);
                }
            }else {
                log.error("车辆未设预定路线{}", sPlateName);
                zyJzljys.setYslx("");
            }
//            zyJzljys.setSjlx("");
            zyJzljys.setLdzt("2");
            zyJzljys.setBz("车辆离场开始记录运输路线");
            log.info("东市区污水运输记录{}", zyJzljys);
            wnDataMapper.insertJzljys(zyJzljys);
            log.info("运输开始 - 联单号: {}, 车牌: {}", ldbh, sPlateName);

            redisGpsUtil.startTracking(ldbh,sPlateName);
        }

    }

    /**
     * 获取车辆的预设路线
     * @param sPlateName
     * @return
     */
    private List<Map> getClYs(String sPlateName) {
        return wnDataMapper.selectYxGzByCph(sPlateName);
    }

    /**
     * 更新所有表中的联单状态
     */
    private void updateAllLianDanStatus(String ldbh, String status) {
        // 更新移出表状态
        wnDataMapper.updateLianDanStatus(ldbh, status);
        // 更新接收表状态
        wnDataMapper.updateJsLianDanStatus(ldbh, status);
        // 更新运输表状态
        wnDataMapper.updateYsLianDanStatus(ldbh, status);
    }



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