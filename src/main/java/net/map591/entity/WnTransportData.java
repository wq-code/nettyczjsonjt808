package net.map591.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WnTransportData {

    @JsonProperty("sDetectCode")
    private String sDetectCode; //检测单号
    @JsonProperty("sSiteCode")
    private String sSiteCode;   //站点标识
    @JsonProperty("sDateTime")
    private String sDateTime;   //检测时间
    @JsonProperty("sPlateName")
    private String sPlateName;  //车牌号码
    @JsonProperty("nTotalWeight")
    private Integer nTotalWeight;   //车货重量
    @JsonProperty("nEnterChannel")
    private Byte nEnterChannel; //检测通道
    @JsonProperty("nEnterAspect")
    private Byte nEnterAspect;  //进站方向
    @JsonProperty("sExitTime")
    private String sExitTime;   //出车时间
    @JsonProperty("sBase64HeadData")
    private String sBase64HeadData; //车头图片数据
    @JsonProperty("sBase64TailData")
    private String sBase64TailData; //车尾图片数据
    @JsonProperty("sBase64BodyData")
    private String sBase64BodyData; //车身图片数据
    @JsonProperty("sBase64PlateData")
    private String sBase64PlateData;    //车牌图片数据
    @JsonProperty("vehicleType")
    private Integer vehicleType;    //车辆类型（补充分类）
    @JsonProperty("driverName")
    private String driverName;  //司机姓名
    @JsonProperty("carid")
    private String carid;   //司机身份证号
    @JsonProperty("phone")
    private String phone;   //司机手机号
    @JsonProperty("driversLicense")
    private String driversLicense;  //司机驾驶证号
    @JsonProperty("drivingLicense")
    private String drivingLicense;  //车辆行驶证号
    @JsonProperty("weighMode")
    private Integer weighMode;  //过磅模式
    @JsonProperty("operatorOne")
    private String operatorOne; //一次过磅操作员
    @JsonProperty("operatorTwo")
    private String operatorTwo; //二次过磅操作员
    @JsonProperty("operationOneTime")
    private String operationOneTime;    //一次过磅操作日期
    @JsonProperty("operationTwoTime")
    private String operationTwoTime;    //二次过磅操作日期
    @JsonProperty("operatingUnit")
    private String operatingUnit;   //操作单位
    @JsonProperty("specs")
    private Integer specs;  //货物规格
    @JsonProperty("consignee")
    private String consignee;   //收货单位
    @JsonProperty("consigneeMan")
    private String consigneeMan;   //收货单位经办人
    @JsonProperty("dispatcher")
    private String dispatcher;  //发货单位
    @JsonProperty("dispatcherMan")
    private String dispatcherMan;  //发货单位经办人
    @JsonProperty("productName")
    private String productName; //货名
    @JsonProperty("weighType")
    private Integer weighType;  //过磅类型
    @JsonProperty("suttle")
    private String suttle;  //货物净重
    @JsonProperty("timeStampOne")
    private String timeStampOne;    //一次过磅重量（毛重）
    @JsonProperty("timeStampTwo")
    private String timeStampTwo;    //二次过磅重量（皮重）
    @JsonProperty("unit")
    private String unit;    //重量单位
    @JsonProperty("timeStampOneTime")
    private String timeStampOneTime;    //一次过磅时间
    @JsonProperty("timeStampTwoTime")
    private String timeStampTwoTime;    //二次过磅时间
    @JsonProperty("timeStampOnePhoto")
    private String timeStampOnePhoto;   //一次过磅车辆 / 货物照片 URL
    @JsonProperty("timeStampTwoPhoto")
    private String timeStampTwoPhoto;   //二次过磅车辆 / 货物照片 URL
    @JsonProperty("modifications")
    private String modifications;   //数据修改次数
    @JsonProperty("lastModificationTime")
    private String lastModificationTime;    //数据上次修改时间
    @JsonProperty("responsibleEntity")
    private String responsibleEntity;  //责任主体
    @JsonProperty("erpId")
    private String erpId;   //工厂系统编号（ERP 标识）
    @JsonProperty("notes")
    private String notes;   //备注
    @JsonIgnore
    private String ldbh;
    @JsonIgnore
    private String ldzt;
    @JsonIgnore
    private String czjlid;
}
