package net.map591;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("net.map591.mapper")
@EnableCaching //开启缓存的注解
@EnableScheduling
public class TcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcpServerApplication.class, args);
    }

}
