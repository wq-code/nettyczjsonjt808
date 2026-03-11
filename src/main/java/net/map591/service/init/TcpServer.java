package net.map591.service.init;

import net.map591.service.netty.NettyServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;


@Component
public class TcpServer implements ServletContextAware, ApplicationContextAware {

    @Autowired
    private  NettyServer nettyServer;

    //应用容器对象
    public static ApplicationContext applicationContext;

    @Override
    public void setServletContext(ServletContext servletContext) {
        Thread thread=new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    Thread.sleep(2000);  //5s后开启tcp服务
                    nettyServer.startServer();
                } catch (Exception e) {
                    // TODO 自动生成的 catch 块
                    e.printStackTrace();
                }
            }
        });
        thread.setName("TcpServer");
        thread.start();
    }

    @Override
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        applicationContext= arg0;
    }
}
