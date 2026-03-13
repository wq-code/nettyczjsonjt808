package net.map591.ai;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
@Data
public class ZyClwz {
    private Long id;
    private String ldbh;            // 联单编号
    private String cphm;            // 车牌号码
    private BigDecimal longitude;    // 经度
    private BigDecimal latitude;     // 纬度
    private BigDecimal speed;        // 速度
    private Double direction;       // 方向
    private LocalDateTime gpsTime;           // GPS时间
    private Date createTime;
}