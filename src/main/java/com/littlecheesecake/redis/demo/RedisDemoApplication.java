package com.littlecheesecake.redis.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class RedisDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisDemoApplication.class, args);
	}

}
