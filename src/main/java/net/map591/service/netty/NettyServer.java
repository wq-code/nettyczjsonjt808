package net.map591.service.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.NoArgsConstructor;
import net.map591.utils.HybridDecoder;
import net.map591.utils.MyDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author Allen
 * @Date 2025/09/07
 * @Version V1.0.0
 * @Description netty框架端口绑定
 **/
@NoArgsConstructor
@Service
public class NettyServer {

    @Value("${tcp.server.port}")
    private int port;

    @Value("${tcp.server.port_earth}")
    private int port_earth;

    @Autowired
    private ServerHandler serverHandler;
    //连接信道集合
    public  static Map<String,Channel> channelMap=new ConcurrentHashMap<String,Channel>();

    /**
     * 开启netty框架tcp服务
     * @param
     * @return void
     */
    public void startServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);

        EventLoopGroup bossGroup1 = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup1 = new NioEventLoopGroup(1);

        try {
            /**
             * 创建并初始化 Netty 服务端 NIO 线程组
             * 创建自定义的解码器MyDecoder
             */
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("decoder", new MyDecoder());
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 4, 4, -8, 0));
                            pipeline.addLast("idleStateHandler",
                                    new IdleStateHandler(5, 0, 0, TimeUnit.MINUTES));
                            pipeline.addLast("serverChannelHandler", new ServerHandler());
                        }
                    });

            b.bind(port).addListener(future -> {
                if (future.isSuccess()) {
                    System.out.println("端口[" + port + "]绑定成功!");
                } else {
                    System.err.println("端口[" + port + "]绑定失败!");
                }
            });

            /**
             * 创建并初始化 Netty 服务端 NIO 线程组
             * 使用正规UTF-8编码
             */
            ServerBootstrap b2 = new ServerBootstrap();
            b2.group(bossGroup1, workerGroup1)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast(new HybridDecoder());

                            // 入站解码：将客户端发送的二进制数据（ByteBuf）按UTF-8解码为String
//                            pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                            // 出站编码：将服务端发送的String按UTF-8编码为二进制数据（ByteBuf）
//                            pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                            pipeline.addLast(new IdleStateHandler(0,0,300), serverHandler);
                        }
                    });

            b2.bind(port_earth).addListener(future -> {
                if (future.isSuccess()) {
                    System.out.println("端口[" + port_earth + "]绑定成功!");
                } else {
                    System.err.println("端口[" + port_earth + "]绑定失败!");
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } finally {

        }
    }
}
