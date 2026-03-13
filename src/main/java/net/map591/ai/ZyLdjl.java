package net.map591.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ZyLdjl {
    private String ldid;           // 联单ID
    private String ldbh;           // 联单编号
    private String cphm;           // 车牌号码
    private String ldzt;           // 联单状态
    
    // 移出信息
    private String ycczjlid;       // 移出称重记录ID
    private String ycczdd;         // 移出站点
    private String ycczsj;         // 移出时间
    private BigDecimal ycjz;        // 移出净重
    private String ycjsgdbh;       // 移出单位
    private String ycczcbm;        // 移出接收单位
    
    // 接收信息
    private String jscczjlid;       // 接收称重记录ID
    private String jscczdd;         // 接收站点
    private String jscczsj;         // 接收时间
    private BigDecimal jscjz;       // 接收净重
    private String jscjsgdbh;      // 接收单位
    private String jscczcbm;       // 接收接收单位
    
    // 轨迹信息
    private String sjlx;            // 实际路线
    private Date createTime;
    private Date completeTime;
    private Date updateTime;
}