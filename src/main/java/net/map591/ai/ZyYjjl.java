package net.map591.ai;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

@Data
public class ZyYjjl {
    private String yjid;            // 预警ID
    private String ldbh;            // 联单编号
    private String cphm;            // 车牌号码
    private String yjlx;            // 预警类型
    private String yjms;            // 预警描述
    private Date yjsj;              // 预警时间
    private String clzt;            // 处理状态
    private Date clsj;              // 处理时间
    private String bz;              // 备注
    private String trackLine;
    private String alarmPoint;
    private Timestamp stayStartTime;
    private Timestamp stayEndTime;
    private Timestamp transportStartTime;
    private Timestamp transportCurrentTime;

}