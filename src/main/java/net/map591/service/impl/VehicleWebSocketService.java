package net.map591.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.map591.configurtion.VehicleWebSocketHandler;
import net.map591.entity.LocationData;
import net.map591.mapper.WnDataMapper;
import net.map591.utils.RedisGpsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class VehicleWebSocketService {


    @Autowired
    private VehicleWebSocketHandler vehicleWebSocketHandler;
    @Autowired
    private RedisGpsUtil redisGpsUtil;

    /**
     * 广播车辆位置给所有客户端
     */
    public void broadcastToPlainWebSocket(LocationData locationData) {
        Map<String, Object> message = convertToGeoJson(locationData);
        vehicleWebSocketHandler.broadcast(JSON.toJSONString(message));
    }

    /**
     * 转换为GeoJSON格式
     */
    public Map<String, Object> convertToGeoJson(LocationData data) {
        List<Map<String, Object>> features = new ArrayList<>();
        features.add(createPointFeature(data));

        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);

        return featureCollection;
    }
    /**
     * 创建GeoJSON Feature
     */
    public Map<String, Object> createPointFeature(LocationData data) {
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");

        // Geometry
        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "Point");
        geometry.put("coordinates", Arrays.asList(data.getLongitude(), data.getLatitude()));
        feature.put("geometry", geometry);

        // Properties
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("plate_phone", data.getPlatePhone());
        properties.put("terminal_phone", data.getTerminalPhone());
        properties.put("gps_time", data.getGpsTime());
        properties.put("speed", data.getSpeed());
        properties.put("direction", data.getDirection());

        feature.put("properties", properties);

        return feature;
    }
    /**
     * 获取所有车辆最新位置，组装为 GeoJSON FeatureCollection，并广播
     */
    public void broadcastAllVehicles() {
        try {
            Map<String, LocationData> allVehicles = redisGpsUtil.getAllVehicleLatest();
            List<Map<String, Object>> features = new ArrayList<>();

            for (LocationData data : allVehicles.values()) {
                features.add(createPointFeature(data));
            }

            Map<String, Object> featureCollection = new LinkedHashMap<>();
            featureCollection.put("type", "FeatureCollection");
            featureCollection.put("features", features);

            String jsonMessage = JSON.toJSONString(featureCollection);
            vehicleWebSocketHandler.broadcast(jsonMessage);

        } catch (Exception e) {
            log.error("广播所有车辆位置失败", e);
        }
    }
}