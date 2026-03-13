package net.map591.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.map591.entity.WnTransportData;
import net.map591.entity.ZyClczjl;
import net.map591.entity.ZyJzljys;
import net.map591.mapper.WnDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Slf4j
@Service
public class WnDataAndImageImpl {
    @Autowired
    private WnDataMapper wnDataMapper;

    public void saveWnData(WnTransportData wnTransportData) {
        log.info("========================开始保存污泥原始数据=================================");
        WnTransportData data = new WnTransportData();
        String sDetectCode = wnTransportData.getSDetectCode();
        data.setSDetectCode(sDetectCode);
        log.info("检测单号：{}",sDetectCode);
        String sSiteCode = wnTransportData.getSSiteCode();
        data.setSSiteCode(sSiteCode);
        log.info("站点：{}",sSiteCode);
        data.setSDateTime(wnTransportData.getSDateTime());
        log.info("称重时间：{}",data.getSDateTime());
        String sPlateName = wnTransportData.getSPlateName();
        data.setSPlateName(sPlateName);
        log.info("当前车牌：{}",sPlateName);
        data.setNTotalWeight(wnTransportData.getNTotalWeight());
        data.setNEnterChannel(wnTransportData.getNEnterChannel());
        data.setNEnterAspect(wnTransportData.getNEnterAspect());
        data.setSExitTime(wnTransportData.getSExitTime());
        data.setSBase64HeadData(wnTransportData.getSBase64HeadData());
        data.setSBase64TailData(wnTransportData.getSBase64TailData());
        data.setSBase64BodyData(wnTransportData.getSBase64BodyData());
        data.setSBase64PlateData(wnTransportData.getSBase64PlateData());
        data.setVehicleType(wnTransportData.getVehicleType());
        data.setDriverName(wnTransportData.getDriverName());
        data.setCarid(wnTransportData.getCarid());
        data.setPhone(wnTransportData.getPhone());
        data.setDriversLicense(wnTransportData.getDriversLicense());
        data.setDrivingLicense(wnTransportData.getDrivingLicense());
        data.setWeighMode(wnTransportData.getWeighMode());
        data.setOperatorOne(wnTransportData.getOperatorTwo());
        data.setOperationOneTime(wnTransportData.getOperationOneTime());
        data.setOperationTwoTime(wnTransportData.getOperationTwoTime());
        data.setOperatingUnit(wnTransportData.getOperatingUnit());
        data.setSpecs(wnTransportData.getSpecs());
        data.setConsignee(wnTransportData.getConsignee());
        data.setDispatcher(wnTransportData.getDispatcher());
        data.setProductName(wnTransportData.getProductName());
        data.setWeighType(wnTransportData.getWeighType());
        data.setSuttle(wnTransportData.getSuttle());
        data.setTimeStampOne(wnTransportData.getTimeStampOne());
        data.setTimeStampTwo(wnTransportData.getTimeStampTwo());
        data.setUnit(wnTransportData.getUnit());
        data.setTimeStampOneTime(wnTransportData.getTimeStampOneTime());
        data.setTimeStampTwoTime(wnTransportData.getTimeStampTwoTime());
        data.setTimeStampOnePhoto(wnTransportData.getTimeStampOnePhoto());
        data.setTimeStampTwoPhoto(wnTransportData.getTimeStampTwoPhoto());
        data.setModifications(wnTransportData.getModifications());
        data.setLastModificationTime(wnTransportData.getLastModificationTime());
        data.setErpId(wnTransportData.getErpId());
        data.setNotes(wnTransportData.getNotes());
        data.setConsigneeMan(wnTransportData.getConsigneeMan());
        data.setDispatcherMan(wnTransportData.getDispatcherMan());
        data.setResponsibleEntity(wnTransportData.getResponsibleEntity());
        wnDataMapper.insertWnData1(data);
        log.info("======================保存污泥原始数据成功===================================");



    }

    public void savezyClczjl(WnTransportData data) {
        log.info("====================开始保存数据到zy_clczjl==============================");
        ZyClczjl zyClczjl = new ZyClczjl();
        zyClczjl.setCzjlid(data.getSDetectCode());                      //     称重记录编号
        log.info("称重记录编号：{}",zyClczjl.getCzjlid());
//      zyClczjl.setCzbbh("");                                          //     称重磅编号
        zyClczjl.setCphm(data.getSPlateName());                         //     车牌号码
        zyClczjl.setCzlx("");                                           //     称重类型
        zyClczjl.setJzljlx("3");                                        //     建筑垃圾类型
        zyClczjl.setMz(convertToBigDecimal(data.getTimeStampOne()));    //     毛重（kg）
        zyClczjl.setPz(convertToBigDecimal(data.getTimeStampTwo()));    //     皮重（kg）
        zyClczjl.setJz(convertToBigDecimal(data.getSuttle()));          //     净重（kg）
//        zyClczjl.setBzzz(convertToBigDecimal(""));                    //     标准载重（kg）
//        zyClczjl.setCzl(convertToBigDecimal(""));                     //     超载率
        zyClczjl.setCzdd(data.getSSiteCode());                          //     称重地点
        switch (data.getSSiteCode()){
            case "112661":
                switch (data.getSPlateName()){
                    case "皖M8A011":
                        //天长市污水处理厂运输污泥处置厂的称重数据
                        zyClczjl.setJsgdbh("天长市污水处理厂");             //     移出单位名称
                        zyClczjl.setCzcbm("天长市污泥处置厂");              //     接收单位名称
                        break;
                    case "皖M4B961":
                    case "皖M9A017":
                    case "皖M8A862":
                    case "豫QC2010":
                        //乡镇水务运输天长市污水处理厂的称重数据
                        zyClczjl.setJsgdbh("乡镇水务");
                        zyClczjl.setCzcbm("天长市污水处理厂");
                        break;
                    default:
                        //天长市污水处理厂未知车辆的称重数据
                        zyClczjl.setJsgdbh("");
                        zyClczjl.setCzcbm("");
                        break;
                }
                break;
            case "112662":
                switch (data.getSPlateName()) {
                    case "皖M8A011":
                        //天长市污水处理厂运输污泥处置厂的称重数据
                        zyClczjl.setJsgdbh("天长市污水处理厂");
                        zyClczjl.setCzcbm("天长市污泥处置厂");
                        break;
                    case "皖N43777":
                        //东市区污水处理厂运输污泥处置厂的称重数据
                        zyClczjl.setJsgdbh("东市区污水处理厂");
                        zyClczjl.setCzcbm("天长市污泥处置厂");
                        break;
                    default:
                        //东市区污水处理厂未知车辆的称重数据
                        zyClczjl.setJsgdbh("");
                        zyClczjl.setCzcbm("");
                        break;
                }
                break;
            case "112663":
                if ("皖N43777".equals(data.getSPlateName())){
                    //东市区污水处理厂运输污泥处置厂的称重数据
                    zyClczjl.setJsgdbh("东市区污水处理厂");
                    zyClczjl.setCzcbm("天长市污泥处置厂");
                }else {
                    //未知车辆的称重数据
                    zyClczjl.setJsgdbh("");
                    zyClczjl.setCzcbm("");
                }
                break;
            default:
                //未知站点的称重数据
                zyClczjl.setJsgdbh("");
                zyClczjl.setCzcbm("");
                break;
        }

//      zyClczjl.setGby("");                                            //     操作员

        zyClczjl.setCzsj(data.getSDateTime());                          //     称重磅称重时间
        zyClczjl.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        zyClczjl.setSBase64PlateData(data.getSBase64PlateData());       //
        zyClczjl.setSBase64HeadData(data.getSBase64HeadData());         //
        zyClczjl.setSBase64BodyData(data.getSBase64BodyData());         //
        zyClczjl.setSBase64TailData(data.getSBase64TailData());         //

        wnDataMapper.insertClczjl(zyClczjl);
        log.info("===============保存数据到zy_clczjl结束===========================================");
    }

    public BigDecimal convertToBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public boolean existsBySDetectCode(String sDetectCode) {
        Integer count =wnDataMapper.existsBySDetectCode(sDetectCode);
        return count > 0;
    }
}
