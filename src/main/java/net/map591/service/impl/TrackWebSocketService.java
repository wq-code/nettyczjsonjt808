package net.map591.service.impl;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.map591.configurtion.TrackWebSocketHandler;
import net.map591.entity.LocationData;
import net.map591.mapper.WnDataMapper;
import net.map591.utils.RedisGpsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TrackWebSocketService extends TextWebSocketHandler {

    @Autowired
    private TrackWebSocketHandler trackWebSocketHandler;

    @Autowired
    private RedisGpsUtil redisGpsUtil;

    @Autowired
    private WnDataMapper wnDataMapper;

    @Autowired
    private ObjectMapper objectMapper;


    /**
     * 推送轨迹更新
     * @param ldbh 联单号
     * @param locationData 最新的位置数据
     */
    public void pushTrackUpdate(String ldbh, LocationData locationData) {
        try {
            if (ldbh == null || ldbh.trim().isEmpty()) {
                log.warn("联单号为空，无法推送轨迹更新");
                return;
            }

            // 1. 获取当前轨迹线
            String currentTrackLine = wnDataMapper.selectTrackLineByLdbh(ldbh);

            // 2. 获取运输记录详情
            Map<String, Object> transportDetail = wnDataMapper.selectTransportDetail(ldbh);
            if (transportDetail == null) {
                log.warn("未找到联单号 {} 的运输记录", ldbh);
                return;
            }
            Map<String, Object> geoJsonData = convertToGeoJson(ldbh, currentTrackLine, locationData, transportDetail);
            if (geoJsonData == null) {
                log.warn("无法构建 GeoJSON 数据，跳过推送");
                return;
            }


            // 4. 序列化为JSON
            String trackData = objectMapper.writeValueAsString(geoJsonData);

            // 5. 推送给订阅了该联单号的客户端
            trackWebSocketHandler.sendTrackUpdate(ldbh, trackData);

            log.debug("已推送联单号 {} 的轨迹更新", ldbh);
        } catch (Exception e) {
            log.error("推送轨迹更新失败", e);
        }
    }

    /**
     * 转换为GeoJSON格式
     */
    private Map<String, Object> convertToGeoJson(String ldbh, String lineString, LocationData latestPoint, Map<String, Object> transportDetail) {
        Map<String, Object> geoJson = new HashMap<>();
        try {
            // 1. 解析轨迹线坐标
            List<List<Double>> coordinates = parseLineStringToCoordinates(lineString);

            // 2. 构建GeoJSON Feature
            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");


            // 3. 构建geometry
            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "LineString");
            geometry.put("coordinates", coordinates);
            feature.put("geometry", geometry);

            // 4. 构建properties（合并运输详情和最新点信息）
            Map<String, Object> properties = new HashMap<>();
            properties.put("ldbh", ldbh);
            properties.put("type", "track_update");

            // 从transportDetail中获取信息
            if (transportDetail != null) {
                properties.put("cphm", transportDetail.get("cphm"));
                properties.put("ldzt", transportDetail.get("ldzt"));
                // 添加其他需要的运输详情字段
            }
            properties.put("timestamp", System.currentTimeMillis());

            feature.put("properties", properties);

            // 5. 构建FeatureCollection
            List<Map<String, Object>> features = new ArrayList<>();
            features.add(feature);

            geoJson.put("type", "FeatureCollection");
            geoJson.put("features", features);
        }catch (Exception e){
            log.error("解析轨迹线坐标失败", e);
            return null;
        }
        return geoJson;
    }


    /**
     * 解析LINESTRING字符串为坐标列表
     */
    private List<List<Double>> parseLineStringToCoordinates(String lineString) {
        List<List<Double>> coordinates = new ArrayList<>();

        if (lineString == null || lineString.trim().isEmpty()) {
            return coordinates;
        }

        try {
            // 检查是否包含LINESTRING前缀
            if (lineString.startsWith("LINESTRING(")) {
                // 去除LINESTRING(和)
                String pointsPart = lineString.substring(11, lineString.length() - 1);

                // 分割各个点
                String[] points = pointsPart.split(",");

                for (String point : points) {
                    point = point.trim();
                    String[] coords = point.split("\\s+");

                    if (coords.length >= 2) {
                        try {
                            double longitude = Double.parseDouble(coords[0]);
                            double latitude = Double.parseDouble(coords[1]);

                            List<Double> coord = new ArrayList<>();
                            coord.add(longitude);
                            coord.add(latitude);
                            coordinates.add(coord);
                        } catch (NumberFormatException e) {
                            log.warn("坐标格式错误: {}", point);
                        }
                    }
                }
            } else {
                // 尝试直接处理坐标字符串
                log.warn("轨迹线格式不是标准的LINESTRING: {}", lineString);
            }
        } catch (Exception e) {
            log.error("解析轨迹线坐标失败: {}", lineString, e);
        }

        return coordinates;
    }
    /**
     * 推送轨迹完成通知
     * @param ldbh 联单号
     */
    public void pushTrackComplete(String ldbh) {
        try {
            if (ldbh == null || ldbh.trim().isEmpty()) {
                log.warn("联单号为空，无法推送轨迹完成通知");
                return;
            }

            // 1. 获取最终轨迹线
            String finalTrackLine = wnDataMapper.selectTrackLineByLdbh(ldbh);

            // 2. 获取运输记录详情
            Map<String, Object> transportDetail = wnDataMapper.selectTransportDetail(ldbh);
            if (transportDetail == null) {
                log.warn("未找到联单号 {} 的运输记录", ldbh);
                return;
            }

            // 3. 构建轨迹完成数据
            Map<String, Object> trackCompleteData = new HashMap<>();
            trackCompleteData.put("type", "track_complete");
            trackCompleteData.put("ldbh", ldbh);
            trackCompleteData.put("sjlx", finalTrackLine); // 最终轨迹线
            trackCompleteData.put("transportDetail", transportDetail); // 运输详情
            trackCompleteData.put("timestamp", System.currentTimeMillis()); // 时间戳

            // 4. 序列化为JSON
            String trackData = objectMapper.writeValueAsString(trackCompleteData);

            // 5. 推送给订阅了该联单号的客户端
            trackWebSocketHandler.sendTrackUpdate(ldbh, trackData);

            log.info("已推送联单号 {} 的轨迹完成通知", ldbh);
        } catch (Exception e) {
            log.error("推送轨迹完成通知失败", e);
        }
    }

    /**
     * 推送轨迹错误通知
     * @param ldbh 联单号
     * @param errorMessage 错误信息
     */
    public void pushTrackError(String ldbh, String errorMessage) {
        try {
            if (ldbh == null || ldbh.trim().isEmpty()) {
                log.warn("联单号为空，无法推送轨迹错误通知");
                return;
            }

            // 1. 构建轨迹错误数据
            Map<String, Object> trackErrorData = new HashMap<>();
            trackErrorData.put("type", "track_error");
            trackErrorData.put("ldbh", ldbh);
            trackErrorData.put("errorMessage", errorMessage);
            trackErrorData.put("timestamp", System.currentTimeMillis()); // 时间戳

            // 2. 序列化为JSON
            String trackData = objectMapper.writeValueAsString(trackErrorData);

            // 3. 推送给订阅了该联单号的客户端
            trackWebSocketHandler.sendTrackUpdate(ldbh, trackData);

            log.warn("已推送联单号 {} 的轨迹错误通知: {}", ldbh, errorMessage);
        } catch (Exception e) {
            log.error("推送轨迹错误通知失败", e);
        }
    }

    /**
     * 获取当前订阅的联单号数量
     * @return 订阅数量
     */
    public int getSubscriptionCount() {
        return trackWebSocketHandler.getTrackSessions().size();
    }

    /**
     * 检查联单号是否有订阅者
     * @param ldbh 联单号
     * @return 是否有订阅者
     */
    public boolean hasSubscribers(String ldbh) {
        if (ldbh == null || ldbh.trim().isEmpty()) {
            return false;
        }
        return trackWebSocketHandler.getTrackSessions().containsKey(ldbh);
    }


}
