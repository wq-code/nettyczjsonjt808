package net.map591.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 车辆称重记录信息表
 * </p>
 *
 * @author Allen
 * @since 2025-08-05
 */
@Data
public class ZyClczjl implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 称重记录ID，主键，格式：CZ+日期+流水号，如 CZ20240624001
     */
      private String czjlid;

    /**
     * 车牌号码，运输车辆牌照，用于车辆识别
     */
    private String cphm;

    /**
     * 驾驶员姓名，实际操作人员，用于责任追溯
     */
    private String jsyxm;

    /**
     * 驾驶员身份证号，用于身份核验
     */
    private String jsysfzh;

    /**
     * 称重磅编号，对应物理称重磅设备编号
     */
    private String czbbh;

    /**
     * 称重类型：1-乡镇源头称重；2-污水厂进泥称重；3-污水厂出泥称重；4-处置场入厂称重
     */
    private String czlx;

    /**
     * 建筑垃圾类型，引用字典 C016：01-工程渣土，02-工程泥浆，03-工程垃圾，04-拆除垃圾
     */
    private String jzljlx;

    /**
     * 污泥含水量（%），≥0，用于泥浆类垃圾质量评估
     */
    private BigDecimal wnhsl;

    /**
     * 称重时间，本次称重操作的系统时间戳（进厂或出厂）
     */
    private String czsj;

    /**
     * 进厂称重时间，车辆首次进入厂区过磅的时间
     */
    private String jcczsj;

    /**
     * 进厂称重重量（kg），车辆+货物总重，即毛重
     */
    private BigDecimal jcczzl;

    /**
     * 出厂称重时间，车辆卸货后出厂过磅的时间
     */
    private String ccczsj;

    /**
     * 出厂称重重量（kg），空车重量，即皮重
     */
    private BigDecimal ccczzl;

    /**
     * 毛重（kg），车辆+货物总重，等于进厂称重重量
     */
    private BigDecimal mz;

    /**
     * 皮重（kg），空车重量，等于出厂称重重量
     */
    private BigDecimal pz;

    /**
     * 净重（kg），自动计算：毛重 - 皮重，必须 > 0
     */
    private BigDecimal jz;

    /**
     * 标准载重（kg），车辆核定最大载重，用于超载判断
     */
    private BigDecimal bzzz;

    /**
     * 超载率（%），自动计算：(净重 - 标准载重)/标准载重 * 100，≥0
     */
    private BigDecimal czl;

    /**
     * 称重地点，具体称重位置描述，如“浦东处置场1号磅”
     */
    private String czdd;

    /**
     * 经度，称重位置的GPS坐标（°E），可为空
     */
    private BigDecimal jd;

    /**
     * 纬度，称重位置的GPS坐标（°N），可为空
     */
    private BigDecimal wd;

    /**
     * 移出单位编号，如工地或污水处理厂编号
     */
    private String jsgdbh;

    /**
     * 接收单位编号，如处置场或污水厂编号
     */
    private String czcbm;

    /**
     * 关联联单编号，对应建筑垃圾移出信息表的 ldbh，用于全程追溯
     */
    private String ldbh;

    /**
     * 过磅员，现场称重操作人员姓名
     */
    private String gby;

    /**
     * 过磅员联系方式，联系电话或工号
     */
    private String gbylxfx;

    /**
     * 称重设备状态：1-正常；2-校准中；3-故障
     */
    private String sbzt;

    /**
     * 数据来源：1-自动采集（推荐）；2-人工录入；3-系统补录
     */
    private String sjly;

    /**
     * 重车称重照片，进厂过磅时抓拍的车辆图像（二进制）
     */
    private String zczp;

    /**
     * 空车称重照片，出厂过磅时抓拍的车辆图像（二进制）
     */
    private String kczp;

    /**
     * 备注
     */
    private String bz;

    private String sBase64HeadData; //车头图片数据
    private String sBase64TailData; //车尾图片数据
    private String sBase64BodyData; //车身图片数据
    private String sBase64PlateData;    //车牌图片数据
    private String createTime;
}
