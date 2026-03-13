package net.map591.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 建筑垃圾接收信息表
 * </p>
 *
 * @author Allen
 * @since 2025-08-05
 */
@Data
public class ZyJzljjs implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 接收单号，主键，格式：SN+日期+流水号，如 SN20240624001，由接收单位反馈生成
     */
      private String snjlid;

    /**
     * 关联联单编号，对应移出信息表的联单编号（jy_jzlj_yc.ldbh），用于全程追溯
     */
    private String ldbh;

    /**
     * 处置场编号，外键关联处置场信息表（jcss_cnfxx.cnfxxbh）
     */
    private String czcbm;

    /**
     * 处置场名称，冗余字段，便于查询展示
     */
    private String czcmc;

    /**
     * 移出单位编号，如工地或污水处理厂编号，关联 jcss_jsbgd 或相关单位表
     */
    private String lygd;

    /**
     * 移出单位名称，冗余字段，便于展示
     */
    private String lygdm;

    /**
     * 车牌号码，运输车辆牌照，关联车辆信息表（jcss_clxx）
     */
    private String cphm;

    /**
     * 接收日期，垃圾实际到达并完成卸料的日期（YYYYMMDD）
     */
    private String snrq;

    /**
     * 接收时间，精确到秒，如 16:45:10
     */
    private String snsj;

    /**
     * 建筑垃圾类型，引用字典 C016：01-工程渣土，02-工程泥浆，03-工程垃圾，04-拆除垃圾等
     */
    private String jzljlx;

    /**
     * 接收方量，单位：立方米（m³），可为空，必须 > 0 若填写
     */
    private BigDecimal snfl;

    /**
     * 称重净重，单位：千克（kg），必须 > 0，来自称重系统
     */
    private BigDecimal czjz;

    /**
     * 处置方式，固定值：3-资源化利用，引用字典 C005
     */
    private String czfs;

    /**
     * 再生利用类型，固定值：2-材料再生利用，引用字典 C006
     */
    private String zslylx;

    /**
     * 质检结果：1-合格，2-不合格，3-待检
     */
    private String zjjg;

    /**
     * 不合格原因：01-含杂率超标，02-含水率超标，03-重金属超标，仅当 zjjg='2' 时有效
     */
    private String bhgyy;

    /**
     * 经办人，处置场现场接收操作人员
     */
    private String jbr;

    /**
     * 运输确认人，运输方签字确认人员（如驾驶员或押运员）
     */
    private String ysqr;

    /**
     * 联单状态，引用字典 C017：1-已生成，2-在途，3-已到达，4-已核销，5-异常
     */
    private String ldzt;

    /**
     * 称重记录ID，关联称重记录表（如 jcss_czpjl.czjlid）
     */
    private String czjlid;

    /**
     * 备注
     */
    private String bz;

}
