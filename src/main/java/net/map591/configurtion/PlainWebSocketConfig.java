package net.map591.configurtion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class PlainWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private VehicleWebSocketHandler vehicleWebSocketHandler;

    @Autowired
    private TrackWebSocketHandler trackWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry  registry) {
        // 1. 车辆实时位置端点（低频率，关注所有车辆）
        registry.addHandler(vehicleWebSocketHandler, "/ws-vehicle")
                .setAllowedOrigins("*");

        // 2. 运输轨迹端点（关注特定联单轨迹）
        registry.addHandler(trackWebSocketHandler, "/ws-track")
                .setAllowedOrigins("*");

    }
    /**
     * 配置WebSocket容器
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(600000L); // 10分钟
        return container;
    }
}


