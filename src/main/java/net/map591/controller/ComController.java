package net.map591.controller;

import net.map591.entity.LocationData;
import net.map591.model.response.ResponseResult;
import net.map591.service.impl.VehicleWebSocketService;
import net.map591.utils.RedisGpsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ComController {
    @Autowired
    private RedisGpsUtil redisGpsUtil;
    @Autowired
    private VehicleWebSocketService vehicleWebSocketService;

    /**
     * 首次进入页面前端获取车辆最新位置
     * WebSocket接收消息（如果前端需要主动发送消息）
     * 前端可以发送消息到这个端点：/app/vehicle.position
     */
    @RequestMapping("/vehicle/position")
    public ResponseResult<Map<String, Object>> handleVehiclePosition() {
        Map<String, LocationData> allVehicleLatest = redisGpsUtil.getAllVehicleLatest();
        // 创建 FeatureCollection
        List<Map<String, Object>> features = new ArrayList<>();
        allVehicleLatest.forEach((platePhone, locationData) -> {
            // 使用已有的 convertToGeoJson 方法创建单个 feature
            Map<String, Object> feature = vehicleWebSocketService.createPointFeature(locationData);
            features.add(feature);
        });
        // 构建最终的 GeoJSON 结构
        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);
        return new ResponseResult<>(featureCollection);
    }
}
