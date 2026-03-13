package net.map591.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 建筑垃圾移出信息表
 * </p>
 *
 * @author Allen
 * @since 2025-08-05
 */
@Data
public class ZyJzljyc implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 移出单号，主键，格式：YS+日期+流水号，如 YS20240624001
     */
      private String ycdh;

    /**
     * 联单编号，唯一，格式：行政区划+年月日+流水号，用于全程追溯
     */
    private String ldbh;

    /**
     * 移出日期，实际垃圾装车离场日期
     */
    private String  ycrq;

    /**
     * 移出时间，精确到秒，如 14:30:22
     */
    private String    ycsj;

    /**
     * 是否跨区转移：Y-是，N-否，默认 N
     */
    private Boolean sfkqys;

    /**
     * 移出单位编号，可为建设工地或污水处理厂编号
     */
    private String jsgdbh;

    /**
     * 运输企业编号，外键关联运输企业表
     */
    private String ysqybh;

    /**
     * 处置场编号，垃圾最终去向
     */
    private String czcbm;

    /**
     * 建筑垃圾类型，引用字典 C016：01-工程渣土，02-拆除垃圾，03-装修垃圾，04-工程泥浆等
     */
    private String jzljlx;

    /**
     * 污泥含水量（%），仅当垃圾类型为“工程泥浆”时必填
     */
    private BigDecimal wnhsl;

    /**
     * 移出方量，单位：立方米（m³），可为空
     */
    private BigDecimal ycfl;

    /**
     * 建筑垃圾重量，单位：千克（kg），必填 > 0
     */
    private BigDecimal jzljzl;

    /**
     * 运输车辆牌号，外键关联车辆信息表
     */
    private String yscph;

    /**
     * 驾驶员姓名，用于责任追溯
     */
    private String jsyxm;

    /**
     * 驾驶员身份证号，用于身份核验
     */
    private String jsysfzh;

    /**
     * 经办人，工地或单位现场操作人员
     */
    private String jbr;

    /**
     * 经办人联系方式，电话或手机
     */
    private String jbrlxfx;

    /**
     * 联单状态，引用字典 C017：1-已生成，2-在途，3-已到达，4-已核销，5-异常
     */
    private String ldzt;

    /**
     * 现场照片，装车过程图像，用于事后核查（建议存路径）
     */
    private String xczp;

    /**
     * 称重记录ID，外键关联称重记录表（如 jcss_czpjl）
     */
    private String czjlid;

    /**
     * 备注，如天气异常、临时变更路线等说明
     */
    private String bz;

}
