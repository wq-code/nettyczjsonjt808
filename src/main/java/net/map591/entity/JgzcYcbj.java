package net.map591.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.map591.model.enumNum.PageEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 异常报警记录信息表
 * </p>
 *
 * @author Allen
 * @since 2025-08-05
 */
@Data
public class JgzcYcbj extends PageEntity implements Serializable {

    private static final long serialVersionUID=1L;

    private String geom;
    private String tingliuMinute;

    /**
     * 报警编号，主键，格式：BJ+日期+流水号，如 BJ20240624001
     */
      private String bjjlid;

    /**
     * 关联业务单号，可关联运输单、称重记录、联单编号等，用于追溯源头
     */
    private String glywdh;

    /**
     * 异常环节：1-运输；2-称重；3-进出场；4-处置；为空表示未知环节
     */
    private String ywlx;

    /**
     * 报警类型，引用字典 C018，如 01-超速报警，02-路线偏离，03-长时间停留等
     */
    private String bjlz;

    /**
     * 报警时间，事件触发的时间戳（YYYY-MM-DD HH24:MI:SS），精确到秒
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime bjsj;

    /**
     * 报警地址，GPS设备上报位置经解析后的地理描述，如“浦东新区XX路与XX路交叉口”
     */
    private String bjdjdz;

    /**
     * 报警级别，固定值范围：1-一般；2-严重；3-紧急，用于优先级处理
     */
    private String bjjb;

    /**
     * 车牌号码，涉事车辆牌照，便于责任追溯，可为空
     */
    private String cphm;

    /**
     * 触发阈值，触发报警的具体规则条件，如“速度>60km/h”、“偏离路线>500m”
     */
    private String czyf;

    /**
     * 处置状态：1-待处理；2-已处理；9-误报，控制流程推进
     */
    private String czzt;

    /**
     * 处置人，负责处理该报警的责任人姓名
     */
    private String czr;

    /**
     * 处置时间，报警被处理完成的时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime czsj;

    /**
     * 附件信息
     */
    private String fjxx;

    /**
     * 备注
     */
    private String bz;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;


    @Override
    public String toString() {
        return "JgzcYcbj{" +
        "bjjlid=" + bjjlid +
        ", glywdh=" + glywdh +
        ", ywlx=" + ywlx +
        ", bjlz=" + bjlz +
        ", bjsj=" + bjsj +
        ", bjdjdz=" + bjdjdz +
        ", bjjb=" + bjjb +
        ", cphm=" + cphm +
        ", czyf=" + czyf +
        ", czzt=" + czzt +
        ", czr=" + czr +
        ", czsj=" + czsj +
        ", fjxx=" + fjxx +
        ", bz=" + bz +
        "}";
    }
}
