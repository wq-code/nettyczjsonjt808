package net.map591.entity;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 建筑垃圾运输信息表
 * </p>
 *
 * @author Allen
 * @since 2025-08-05
 */
@Data
public class ZyJzljys  implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 运输单号，主键，格式：YS+日期+流水号，如 YS20240624001，联单状态为“运输中”时自动生成
     */
      private String ysdh;

    /**
     * 关联联单编号，外键关联建筑垃圾移出信息表（jy_jzlj_yc.ldbh）
     */
    private String ldbh;

    /**
     * 运输日期，实际运输发生的日期（YYYYMMDD）
     */
    private String ysrq;

    /**
     * 运输开始时间，车辆离场时间，通常为最后一次过磅时间
     */
    private String yskssj;

    /**
     * 运输结束时间，车辆到达处置场并首次过磅的时间
     */
    private String ysjssj;

    /**
     * 运输方式：01-交通工具运输，02-非交通工具运输
     */
    private String ysfs;

    /**
     * 运输工具：01-汽车，02-船舶，03-管道，09-其他
     */
    private String ysgj;

    /**
     * 车牌号码，运输车辆的牌照号码，关联车辆信息表
     */
    private String cphm;

    /**
     * 驾驶员编号，关联驾驶员信息表
     */
    private String jsybh;

    /**
     * 运输起点，通常为工地或移出单位地址
     */
    private String ysqd;

    /**
     * 运输终点，通常为处置场或接收单位地址
     */
    private String yszd;

    /**
     * 运输方量，单位：立方米（m³），必须 > 0
     */
    private BigDecimal ysfl;

    /**
     * 运输重量，单位：千克（kg），由称重系统采集，必须 > 0
     */
    private BigDecimal yszl;

    /**
     * 建筑垃圾类型，引用字典 C016，如 01-工程渣土，04-工程泥浆等
     */
    private String jzljlx;

    /**
     * 运输路线，预设路线的GPS坐标串，系统自动关联
     */
    private String yslx;

    /**
     * 实际路线，运输过程中实时采集的GPS轨迹坐标串
     */
    private String sjlx;

    /**
     * 路线偏差，单位：米（m），实际轨迹偏离预设路线的平均或最大距离
     */
    private BigDecimal lxpc;

    /**
     * 运输时长，单位：分钟（min），从离场到到达的总耗时
     */
    private BigDecimal yssc;

    /**
     * 停留时长，单位：分钟（min），运输途中最长一次停留时间
     */
    private BigDecimal tlsc;

    /**
     * 联单状态，引用字典 C017：1-已生成，2-在途，3-已到达，4-已核销，5-异常
     */
    private String ldzt;

    /**
     * 备注，如绕行、堵车、设备故障等特殊情况说明
     */
    private String bz;
    private String lygdm;


}
