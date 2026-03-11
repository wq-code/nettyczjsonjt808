package net.map591.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 轨迹数据验证服务
 * 专门用于验证和清理轨迹相关的几何数据
 */
@Slf4j
@Service
public class TrackDataValidationService {

    /**
     * 验证坐标有效性
     * @param longitude 经度
     * @param latitude 纬度
     * @return 是否有效
     */
    public boolean isValidCoordinate(double longitude, double latitude) {
        // 经度范围：-180到180，纬度范围：-90到90
        return longitude >= -180 && longitude <= 180 && latitude >= -90 && latitude <= 90;
    }

    /**
     * 验证点字符串格式
     * @param point 点字符串，格式："经度 纬度"
     * @return 是否有效
     */
    public boolean isValidPointString(String point) {
        if (point == null || point.trim().isEmpty()) {
            return false;
        }


        try {
            String[] coords = point.trim().split("\\s+");
            if (coords.length != 2) {
                return false;
            }
            
            double lon = Double.parseDouble(coords[0]);
            double lat = Double.parseDouble(coords[1]);
            
            return isValidCoordinate(lon, lat);
        } catch (NumberFormatException e) {
            log.debug("点字符串格式错误: {}", point);
            return false;
        }
    }

    /**
     * 验证点集合字符串格式
     * @param points 点集合字符串，格式："经度1 纬度1, 经度2 纬度2, ..."
     * @return 是否有效
     */
    public boolean isValidPointsString(String points) {
        if (points == null || points.trim().isEmpty()) {
            return false;
        }
        
        String[] pointArray = points.split(",");
        if (pointArray.length == 0) {
            return false;
        }
        
        for (String point : pointArray) {
            if (!isValidPointString(point)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 验证LINESTRING格式 - 增强验证，确保至少2个点
     * @param lineString LINESTRING字符串
     * @return 是否有效
     */
    public boolean isValidLineString(String lineString) {
        if (lineString == null || lineString.trim().isEmpty()) {
            return false;
        }

        // 快速长度检查
        if (lineString.length() > 1000000) {
            log.warn("轨迹线过长，可能有问题: {} chars", lineString.length());
            return false;
        }

        if (!lineString.startsWith("LINESTRING(") || !lineString.endsWith(")")) {
            return false;
        }

        try {
            String points = lineString.substring(11, lineString.length() - 1);

            String[] pointArray = points.split(",");
            if (pointArray.length < 2) {
                return false;
            }

            // 4. 验证每个点
            for (String point : pointArray) {
                if (!isValidPointString(point.trim())) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.debug("验证LINESTRING失败: {}", e.getMessage());
            return false;
        }
    }
    /**
     * 清理和修复轨迹线数据 - 增强修复逻辑
     * @param lineString 原始轨迹线
     * @return 修复后的轨迹线，如果无法修复返回null
     */
    public String cleanAndFixLineString(String lineString) {
        if (lineString == null || lineString.trim().isEmpty()) {
            return null;
        }

        try {
            // 如果格式已经正确，直接返回
            if (isValidLineString(lineString)) {
                return lineString;
            }

            // 尝试修复格式
            String cleanString = lineString.trim();

            // 确保包含LINESTRING前缀和后缀
            if (!cleanString.startsWith("LINESTRING(")) {
                cleanString = "LINESTRING(" + cleanString;
            }

            if (!cleanString.endsWith(")")) {
                cleanString = cleanString + ")";
            }

            // 提取点集合
            String pointsPart = cleanString.substring(11, cleanString.length() - 1);
            String[] points = pointsPart.split(",");

            // 如果只有1个点，复制一个点使其有2个点
            if (points.length == 1) {
                String point = points[0].trim();
                cleanString = "LINESTRING(" + point + ", " + point + ")";
            }

            // 验证修复后的格式
            if (isValidLineString(cleanString)) {
                log.info("已修复轨迹线格式，包含{}个点", points.length);
                return cleanString;
            }

            log.warn("无法修复轨迹线格式: {}", lineString);
            return null;

        } catch (Exception e) {
            log.error("清理轨迹线失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 创建安全的轨迹线（确保至少包含2个相同的点）
     * @param longitude 经度
     * @param latitude 纬度
     * @return 安全的轨迹线字符串
     */
    public String createSafeLineString(double longitude, double latitude) {
        if (!isValidCoordinate(longitude, latitude)) {
            log.warn("无效坐标，无法创建轨迹线: 经度={}, 纬度={}", longitude, latitude);
            return null;
        }

        // PostGIS要求LINESTRING至少包含2个点，所以创建包含2个相同点的线
        String point = String.format("%.6f %.6f", longitude, latitude);
        return "LINESTRING(" + point + ", " + point + ")";
    }
    /**
     * 在现有轨迹线上添加新点
     * @param existingLineString 现有轨迹线
     * @param longitude 新点经度
     * @param latitude 新点纬度
     * @return 新的轨迹线
     */
    public String addPointToLineString(String existingLineString, double longitude, double latitude) {
        // 验证新坐标
        if (!isValidCoordinate(longitude, latitude)) {
            log.warn("无效坐标，无法添加到轨迹线: 经度={}, 纬度={}", longitude, latitude);
            return existingLineString;
        }

        // 如果现有轨迹线为空或无效，创建新的轨迹线
        if (existingLineString == null || existingLineString.trim().isEmpty()) {
            log.debug("现有轨迹线为空，创建新轨迹线");
            return createSafeLineString(longitude, latitude);
        }

        try {
            // 验证现有轨迹线格式
            if (!isValidLineString(existingLineString)) {
                log.warn("现有轨迹线格式无效，尝试修复或创建新轨迹线");
                String fixedLine = cleanAndFixLineString(existingLineString);
                if (fixedLine == null || !isValidLineString(fixedLine)) {
                    log.debug("无法修复现有轨迹线，创建新轨迹线");
                    return createSafeLineString(longitude, latitude);
                }
                existingLineString = fixedLine;
            }

            // 提取现有轨迹线的点集合
            String points = existingLineString.substring(11, existingLineString.length() - 1);

            // 检查点数量
            String[] existingPoints = points.split(",");

            // 添加新点
            String newPoint = String.format("%.6f %.6f", longitude, latitude);
            String newPoints;

            if (existingPoints.length == 0) {
                // 这种情况理论上不会发生，但做安全处理
                newPoints = newPoint + ", " + newPoint;
                log.debug("轨迹线无点，创建两个相同点");
            } else {
                newPoints = points + ", " + newPoint;
            }

            String newLineString = "LINESTRING(" + newPoints + ")";

            // 验证新轨迹线
            if (isValidLineString(newLineString)) {
                log.debug("成功添加新点，轨迹线现在包含{}个点", existingPoints.length + 1);
                return newLineString;
            } else {
                log.warn("添加新点后轨迹线格式无效，保留原轨迹");
                return existingLineString;
            }

        } catch (Exception e) {
            log.warn("处理轨迹线失败: {}，创建新轨迹线", e.getMessage());
            return createSafeLineString(longitude, latitude);
        }
    }
}