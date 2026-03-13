package net.map591.mapper;

import net.map591.ai.RoutePlanInfo;
import net.map591.ai.ZyClwz;
import net.map591.ai.ZyLdjl;
import net.map591.ai.ZyYjjl;
import net.map591.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface WnDataMapper {

    // 编号生成相关查询（加锁）
    String selectLdbhMaxForUpdate(@Param("datePrefix") String datePrefix);


    // 业务查询

    // 联单状态查询

    // 更新操作
    void updateZyClczjlPizhong(@Param("cphm") String cphm, @Param("pz") String pz, @Param("jcczsj") String jcczsj);
    void updateZyClczjlCccz(@Param("cphm") String cphm, @Param("ccczzl") BigDecimal ccczzl, @Param("ccczsj") LocalDateTime ccczsj);
    void updateZyJzljysStatusByLdbh(@Param("ldbh") String ldbh, @Param("ldzt") String ldzt, @Param("ysjssj") LocalTime ysjssj);
    void updateLianDanStatus(@Param("ldbh") String ldbh, @Param("status") String status);
    void updateYunShuEndTime(@Param("ldbh") String ldbh, @Param("endTime") String endTime);

    // 插入操作
    void insertClczjl(ZyClczjl czjl);
    void insertJzljyc(ZyJzljyc yc);
    void insertJzljys(ZyJzljys ys);
    void insertJzljjs(ZyJzljjs js);
    void insertWnData(WnTransportData data);

    // 在 WnDataMapper.java 中添加
    void updateJsLianDanStatus(@Param("ldbh") String ldbh, @Param("status") String status);
    void updateYsLianDanStatus(@Param("ldbh") String ldbh, @Param("status") String status);

    //插入原始数据 表wn_transport_data1 用于方便查验数据
    void insertWnData1(WnTransportData wnTransportData);


    //查询车辆的预设轨迹和规则
    List<Map> selectYxGzByCph(String sPlateName);


    // 更新轨迹
    void updateSjlxByLdbh(@Param("ldbh") String ldbh, @Param("sjlx") String sjlx);


    /**
     * 更新实时轨迹线（sjlx字段）
     */
    void updateSjlx(@Param("ldbh") String ldbh, @Param("trackLine") String trackLine);

    /**
     * 查询轨迹线
     */
    String selectTrackLineByLdbh(String ldbh);

    /**
     * 查询运输记录详情
     */
    Map<String, Object> selectTransportDetail(String ldbh);


    /**
     -- 1. 查询车辆正在运输的联单
     */
    String selectActiveLdbhByPlate(String platePhone);


    /**
     * 插入报警记录
     */
    int insertAlarmRecord(JgzcYcbj alarmRecord);

    /**
     * 查询车辆路线信息和规则
     */
    Map<String, Object> selectRouteInfoWithRule(@Param("platePhone") String platePhone,
                                                @Param("ruleType") String ruleType);

    /**
     * 检查路线偏离距离
     */
    Map<String,Object> checkRouteDeviation(@Param("platePhone") String platePhone,
                               @Param("longitude") double longitude,
                               @Param("latitude") double latitude);

    int updateLianDanStatusToAbnormal(String ldbh);

    int updateYsLianDanStatusToAbnormal(String ldbh);

    int updateJsLianDanStatusToAbnormal(String ldbh);

    Map<String, Object> getRouteRule3ByPlatePhone(String platePhone);

    Map<String, Object> getYcsjByLdbh(@Param("activeLdbh") String activeLdbh,@Param("platePhone") String platePhone);

    Map<String, Object> getRouteRule4ByPlatePhone(String platePhone);

    List<LocationData> selectDistinctTrackPointsInWindow(@Param("platePhone") String platePhone,@Param("currentTime")  LocalDateTime currentTime,@Param("maxStayMinutes") int maxStayMinutes);

    Boolean selectFenceOut(LocationData locationData);

    Integer existsBySDetectCode(String sDetectCode);

    void insertYjjl(ZyYjjl alarm);

    void insertLdjl(ZyLdjl ldjl);

    ZyLdjl selectLdjlByLdbh(@Param("ldbh") String ldbh);

    void updateLdjl(ZyLdjl ldjl);

    void insertClwz(ZyClwz zyClwz);

    List<ZyClwz> selectClwzByLdbh(@Param("ldbh") String ldbh);

    List<WnTransportData> selectRecentData(String sPlateName, String sSiteCode, String sDateTime, int i);

    Boolean checkPointInFence(double longitude, double latitude);

    Double calculateDistanceToRoute( double longitude, double latitude, String platePhone);

    RoutePlanInfo getRouteInfoByPlate(String plateNumber);

    double calculateDistanceWithPostGIS(double lat1, double lon1, double lat2, double lon2);
}
