package net.map591.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.map591.entity.LocationData;
import net.map591.mapper.LocationMapper;
import net.map591.utils.RedisGpsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class LocationServiceImpl {

    @Autowired
    private LocationMapper locationMapper;
    @Autowired
    private RedisGpsUtil redisCacheUtil;

    public void saveLocation(LocationData res) {
        CompletableFuture.runAsync(() -> {

        LocationData locationData = new LocationData();
        locationData.setTerminalPhone(res.getTerminalPhone());
        locationData.setPlatePhone(res.getPlatePhone());
        locationData.setMsgSn(res.getMsgSn());
        locationData.setGpsTime(res.getGpsTime());
        locationData.setLongitude(res.getLongitude());
        locationData.setLatitude(res.getLatitude());
        locationData.setGeom(res.getLongitude(), res.getLatitude());
        locationData.setSpeed(res.getSpeed());
        locationData.setDirection(res.getDirection());

        locationMapper.saveLocation(locationData);
        });
    }

    public List<Map<String, String>> getTerminalAndPlateMap() {
        return locationMapper.getTerminalAndPlateMap();
    }

    /**
     * 获取所有缓存车辆的最新位置
     */
    public Map<String, LocationData> getAllLatestVehicles() {
        return redisCacheUtil.getAllVehicleLatest();
    }

    /**
     * 获取指定车辆的最新位置
     */
    public LocationData getLatestVehiclePosition(String platePhone) {
        return redisCacheUtil.getVehicleLatest(platePhone);
    }
}
