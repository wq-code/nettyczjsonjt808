package net.map591.service.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import net.map591.ai.TrajectoryProcessor;
import net.map591.ai.WeighingDataProcessor;
import net.map591.entity.LocationData;
import net.map591.entity.WnTransportData;
import net.map591.mapper.WnDataMapper;
import net.map591.service.impl.*;
import net.map591.utils.ByteUtils;
import net.map591.utils.JTT808Packet;
import net.map591.utils.RedisGpsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Author Allen
 * @Date 2025/08/08
 * @Version V1.0.0
 * @Description netty框架接收数据处理方法
 **/
@Service
@ChannelHandler.Sharable //允许被多个 Channel 共享
@Slf4j
public class ServerHandler extends ChannelInboundHandlerAdapter {

    private  final Logger logger= LoggerFactory.getLogger(ServerHandler.class);


    @Value("${tcp.server.port}")
    private int port;

    @Value("${tcp.server.port_earth}")
    private int port_earth;

    private static ServerHandler serverHandler;

    @Autowired
    private LocationServiceImpl locationService;
    @Autowired
    private  ObjectMapper objectMapper ;
    @Autowired
    private WnDataAndImageImpl2 wnDataAndImageImpl2;
    @Autowired
    private WnDataAndImageImpl wnDataAndImageImpl;
    @Autowired
    private RedisGpsUtil redisGpsUtil;
    @Autowired
    private VehicleWebSocketService vehicleWebSocketService;
    @Autowired
    private TrackWebSocketService trackWebSocketService;
    @Autowired
    private WnDataMapper wnDataMapper;
    @Autowired
    private TrackDataValidationService trackDataValidationService;
    @Autowired
    private AlarmMonitorService alarmMonitorService;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private WeighingDataProcessor weighingDataProcessor    ;
    @Autowired
    private TrajectoryProcessor trajectoryProcessor ;


    @PostConstruct
    public void init(){
        serverHandler = this;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.info("Client:"+getRemoteAddress(ctx)+"|设备接入");
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.info("Client:"+getRemoteAddress(ctx)+"|断开链接");
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {
        try {
            if (msg instanceof JTT808Packet) {
                handleJTT808(ctx, (JTT808Packet) msg);
            } else if (msg instanceof String) {
                handleString(ctx, (String) msg);
            } else {
                System.err.println("未知消息类型: " + msg.getClass());
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void handleString(ChannelHandlerContext ctx, String msg) {

        //数据解析业务处理
        String strPort = ctx.channel().localAddress().toString().split(":")[1];
        //根据strPort端口，判断是哪个TCP程序连接了
        log.info("收到端口{}的数据：{}", strPort, msg);
        System.err.println("判断端口后，要写具体的实现类，来实现接入数据后的处理");

        try {
            if (strPort.equals("8092") && !msg.isEmpty()) {

                    WnTransportData data = objectMapper.readValue(msg, WnTransportData.class);
                // 使用WeighingDataProcessor的isProcessed方法进行幂等检查
                if (!weighingDataProcessor.isProcessed(data.getSDetectCode())) {

                    log.info("**********************数据库数据单号：{}不存在，开始保存数据*********",
                            data.getSDetectCode());

                    // 处理称重数据（包含保存和联单匹配）
                    weighingDataProcessor.processWeighingData(data);

                    // 返回成功
                    ByteBuf responseBuf = Unpooled.copiedBuffer(
                            "{\"code\":0,\"message\": \"成功\"}", CharsetUtil.UTF_8);
                    ctx.writeAndFlush(responseBuf);

                } else {
                    log.info("数据已存在，幂等返回成功：{}", data.getSDetectCode());
                    // 仍然返回成功（幂等）
                    ByteBuf responseBuf = Unpooled.copiedBuffer(
                            "{\"code\":0,\"message\": \"成功\"}", CharsetUtil.UTF_8);
                    ctx.writeAndFlush(responseBuf);
                }
        } else if (!strPort.equals("8092")) {
            logger.error("端口错误：{}", strPort);
            ByteBuf errorBuf = Unpooled.copiedBuffer(
                    "{\"code\":1,\"message\": \"非8092端口\"}", CharsetUtil.UTF_8);
            ctx.writeAndFlush(errorBuf);
        }else {
                logger.error("数据是空：{}", msg);
                ByteBuf errorBuf = Unpooled.copiedBuffer(
                        "{\"code\":1,\"message\": \"数据是空\"}", CharsetUtil.UTF_8);
                ctx.writeAndFlush(errorBuf);
            }
        } catch (IOException e) {
            logger.error("接收原始称重JSON解析错误", e);
            ByteBuf errorBuf = Unpooled.copiedBuffer("{\"code\":1,\"message\": \"解析失败\"}", CharsetUtil.UTF_8);
            ctx.writeAndFlush(errorBuf);
        }
    }

    private void handleJTT808(ChannelHandlerContext ctx, JTT808Packet packet) {
        String strPort = ctx.channel().localAddress().toString().split(":")[1];
        //根据strPort端口，判断是哪个TCP程序连接了
        System.err.println(strPort);
        if (strPort.equals("8092")) {
            byte[] data = packet.toBytes();
            String hex = ByteUtils.bytesToHexWithSpace(data);
            System.out.println("【JT/T 808】收到: " + hex);

            // 解析消息
            LocationData res=parseJT808Message(data);
            System.out.println("【JT/T 808】解析结果: " + res);
            if (res!=null){
                try {
                    //  实时推送车的实时点
                    vehicleWebSocketService.broadcastAllVehicles();

                    // 保存到数据库
                    locationService.saveLocation(res);

                    //  缓存到Redis
                    redisGpsUtil.cacheVehicleLatest(res);

                    //  处理轨迹记录逻辑
                    trajectoryProcessor.processTrajectory(res);

                    log.info("车辆{}位置数据已处理并推送，时间：{}", res.getPlatePhone(), res.getGpsTime());

                } catch (Exception e) {
                    log.error("处理车辆数据失败: {}", res.getPlatePhone(), e);
                }
            }

            ctx.writeAndFlush(packet);
        }
    }

    /**
     * 处理轨迹记录逻辑
     * @param locationData GPS位置数据
     */
    private void processTrajectory(LocationData locationData) {
        try {
            String platePhone = locationData.getPlatePhone();
            if (platePhone == null || platePhone.trim().isEmpty()) {
                log.info("车牌号为空，无法处理轨迹");
                return;
            }

            // 1. 查询车辆是否有活跃的运输联单
//            String activeLdbh = wnDataMapper.selectActiveLdbhByPlate(platePhone);
            String activeLdbh ="";
            if (redisGpsUtil.isVehicleTracking(platePhone)){
                activeLdbh=redisGpsUtil.getCurrentLdbhByPlate(platePhone);
            }
            if (activeLdbh == null || activeLdbh.trim().isEmpty()) {
                log.info("车辆{}当前无活跃运输任务", platePhone);
                return;
            }

            log.info("车辆{}正在执行运输任务，联单号：{}", platePhone, activeLdbh);

            // 2. 获取当前轨迹线（使用安全的方法）
            String currentTrackLine = getSafeTrackLine(activeLdbh);
            log.info("当前轨迹线: {}", currentTrackLine);

            // 3. 获取坐标并验证
            double longitude = locationData.getLongitude();
            double latitude = locationData.getLatitude();

            // 使用专门的验证服务验证坐标
            if (!trackDataValidationService.isValidCoordinate(longitude, latitude)) {
                log.info("车辆{}的坐标无效：经度={}, 纬度={}", platePhone, longitude, latitude);
                return;
            }

            // 4. 使用验证服务添加新点到轨迹线
            String newTrackLine = trackDataValidationService.addPointToLineString(
                    currentTrackLine, longitude, latitude);

            if (newTrackLine == null) {
                log.error("无法生成有效的轨迹线");
                return;
            }
            log.info("新轨迹线: {}", newTrackLine);
            // 验证新轨迹线格式
            if (!trackDataValidationService.isValidLineString(newTrackLine)) {
                log.info("生成的新轨迹线格式无效: {}", newTrackLine);
                return;
            }


            // 5. 更新轨迹线到数据库
            wnDataMapper.updateSjlxByLdbh(activeLdbh, newTrackLine);
            log.info("车辆{}轨迹已更新，联单号：{}", platePhone, activeLdbh);

            // 6. 通过轨迹WebSocket服务推送轨迹更新
            trackWebSocketService.pushTrackUpdate(activeLdbh, locationData);

            // 7. 监控运输中的轨迹是否触发报警类型
            alarmMonitorService.processVehicleAlarm(activeLdbh, locationData,newTrackLine );

        } catch (Exception e) {
            log.error("处理车辆轨迹失败", e);
        }
    }

    /**
     * 安全获取轨迹线，处理可能的几何数据错误
     */
    private String getSafeTrackLine(String ldbh) {
        try {
            String trackLine = wnDataMapper.selectTrackLineByLdbh(ldbh);

            // 使用专门的验证服务验证轨迹线格式
            if (trackLine != null && !trackLine.isEmpty()) {
                // 尝试清理和修复轨迹线
                String cleanedLine = trackDataValidationService.cleanAndFixLineString(trackLine);
                if (cleanedLine != null) {
                    return cleanedLine;
                }
            }

            return null;
        } catch (Exception e) {
            if (e.getMessage().contains("invalid geometry")) {
                log.warn("获取轨迹线时发现无效几何数据，联单号：{}", ldbh);
                // 清除无效数据
                try {
                    wnDataMapper.updateSjlxByLdbh(ldbh, null);
                    log.info("已清除联单号{}的无效轨迹数据", ldbh);
                } catch (Exception ex) {
                    log.error("清除无效轨迹数据失败", ex);
                }
            } else {
                log.error("获取轨迹线失败", e);
            }
            return null;
        }
    }

    /**
     * 7E 02 00 00 61 01 45 42 76 37 90 15 AA 00 00 00 00 00 00 00 03 01 F3 0A 4E 07 18 13 15 00 08 00 C3 01 2D 25 09 18
     * 07 57 48 01 04 00 00 2E E1 30 01 0E 31 01 21 EB 37 00 0C 00 B2 89 86 08 50 16 24 C0 01 21 17 00 06 00 89 FF FF FF
     * FF 00 06 00 C5 FF FF FF EF 00 04 00 2D 6C 34 00 11 00 D5 38 36 38 33 38 37 30 37 31 36 30 38 30 38 39 A2 7E
     */
    private LocationData parseJT808Message(byte[] data) {
        data = unescape(data); //
        //检测是否是以7E首尾
        if (data[0] != 0x7E || data[data.length - 1] != 0x7E) {
            return null;
        }
        int index = 0;
        System.out.println("===============开始解析【JT/T 808】===================" );

        //跳过7E
        index++;

        //消息id
        int msgId = (data[index] & 0xFF) << 8 | (data[index + 1] & 0xFF);
        String hexId = Integer.toHexString(msgId);
        index += 2;
        if (msgId != 0x0200){
            System.out.println("非位置汇报消息，ID:  + hexId ;"+ hexId);
            return null;
        }

        // 消息体属性: 2字节
        int msgBody = (data[index] & 0xFF) << 8 | (data[index + 1] & 0xFF);
        index += 2;

        // 终端手机号: 6字节 BCD码
        byte[] phoneBytes = new byte[6];
        System.arraycopy(data, index, phoneBytes, 0, 6);
        String terminalPhone = bcdToStr(phoneBytes);

        List<Map<String, String>> terminalAndPlateMap = locationService.getTerminalAndPlateMap();
        String platePhone =null;
        for (Map<String, String> map : terminalAndPlateMap) {
            if (terminalPhone.equals(map.get("zdh"))){
                platePhone = map.get("cph");
                break;
            }
        }
        index += 6;

        // 消息流水号: 2字节
        int msgSn  = (data[index] & 0xFF) << 8 | (data[index + 1] & 0xFF);
        index += 2;

        // ================================ 开始解析消息体：位置信息汇报 ===============================

        // 报警标志: 4字节
        index += 4;

        // 状态标志: 4字节
        index += 4;

        // 纬度: 4字节（大端，单位：十万分之一度）
        int lat= (data[index] & 0xFF) << 24 | (data[index + 1] & 0xFF) << 16 | (data[index + 2] & 0xFF) << 8 | (data[index + 3] & 0xFF);
        double latitude = lat / 1e6;  // 转换为十进制度
        index += 4;

        // 经度: 4字节
        int lon= (data[index] & 0xFF) << 24 | (data[index + 1] & 0xFF) << 16 | (data[index + 2] & 0xFF) << 8 | (data[index + 3] & 0xFF);
        double longitude = lon / 1e6;
        index += 4;

        // 高程: 2字节（单位：米）
        index += 2;

        // 速度: 2字节（单位：0.1 km/h）
        int speedRaw  = (data[index] & 0xFF) << 8 | (data[index + 1] & 0xFF);
        double speed = speedRaw / 10.0;
        index += 2;

        // 方向: 2字节（单位：0.1 度）
        int directionRaw  = (data[index] & 0xFF) << 8 | (data[index + 1] & 0xFF);
        double direction = directionRaw;
        index += 2;

        // 时间: 6字节 BCD码 (YY MM DD HH MM SS)
        byte[] timeBytes = new byte[6];
        System.arraycopy(data, index, timeBytes, 0, 6);
        LocalDateTime time =bcdToGpsTime(timeBytes);
        index += 6;

        // === 解析附加信息 ===

//        return String.format("消息id=%s ,终端手机号=%s -> 对应车牌号=%s ,流水号=%s, 时间=%s, 经度=%.6f°E, 纬度=%.6f°N, 速度=%.1fkm/h, 方向=%.1f°",
//                hexId,terminalPhone,platePhone,msgSn,time, longitude, latitude, speed, direction);
        return new LocationData(terminalPhone, platePhone, msgSn, time, longitude, latitude, speed, direction, null);
    }
    public byte[] unescape(byte[] data) {
        if (data.length < 3) return data;

        byte[] result = new byte[data.length];
        int resultIndex = 0;

        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0x7D && i + 1 < data.length) {
                if (data[i + 1] == 0x01) {
                    result[resultIndex++] = 0x7E;
                    i++; // 跳过下一个
                } else if (data[i + 1] == 0x02) {
                    result[resultIndex++] = 0x7D;
                    i++;
                } else {
                    result[resultIndex++] = data[i];
                }
            } else {
                result[resultIndex++] = data[i];
            }
        }

        return Arrays.copyOf(result, resultIndex);
    }
    private String bcdToStr(byte[] phoneBytes) {
        StringBuilder sb = new StringBuilder();
        for (byte phoneByte : phoneBytes) {
            sb.append(String.format("%02X", phoneByte));
        }
        return sb.toString().replaceAll("F", "");
    }

    private LocalDateTime bcdToGpsTime(byte[] timeBytes) {
        if (timeBytes.length < 6) throw new IllegalArgumentException("Time bytes too short");
        StringBuilder sb = new StringBuilder();
        for (byte b : timeBytes) {
            sb.append(String.format("%02X", b));
        }
        String timeStr = sb.toString(); // YY MM DD HH MM SS
        int year = Integer.parseInt(timeStr.substring(0, 2)) + 2000;
        int month = Integer.parseInt(timeStr.substring(2, 4));
        int day = Integer.parseInt(timeStr.substring(4, 6));
        int hour = Integer.parseInt(timeStr.substring(6, 8));
        int minute = Integer.parseInt(timeStr.substring(8, 10));
        int second = Integer.parseInt(timeStr.substring(10, 12));
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx){
        System.out.println("TCP-Server服务端接收数据完毕......");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String remoteAddr = getRemoteAddress(ctx);

        if (cause instanceof IOException) {
            String msg = cause.getMessage();
            if (msg != null && (msg.contains("Connection reset by peer")
                    || msg.contains("Broken pipe"))) {
                logger.debug("Client {} disconnected abruptly", remoteAddr);
            } else {
                logger.warn("IO Exception from {}", remoteAddr, cause);
            }
        } else {
            logger.error("Unexpected exception from {}", remoteAddr, cause);
        }

        // 关闭 channel，释放资源
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt){
        String socketString = getRemoteAddress(ctx);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                logger.info("Client: " + socketString + " READER_IDLE 读超时");
                ctx.disconnect();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                logger.info("Client: " + socketString + " WRITER_IDLE 写超时");
                ctx.disconnect();
            } else if (event.state() == IdleState.ALL_IDLE) {
                logger.info("Client: " + socketString + " ALL_IDLE 总超时");
                ctx.disconnect();
            }
        }
    }

    //获取客户端远程地址(含有端口)
    private   String getRemoteAddress(ChannelHandlerContext ctx) {
        String socketString = "";
        socketString = ctx.channel().remoteAddress().toString();
        return socketString;
    }
}
