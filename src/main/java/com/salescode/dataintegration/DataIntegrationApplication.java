package com.salescode.dataintegration;

import com.salescode.channelkart.services.SpringContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.util.Optional;

@SpringBootApplication
public class DataIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataIntegrationApplication.class, args);
	}

	public static String getEnv() {
		return SpringContext.getBeanSafely(Environment.class)
				.map(environment -> environment.getProperty("channelkart.environment", "dev"))
				.orElseGet(() -> Optional.ofNullable(System.getenv("channelkart.environment")).orElse("dev"));
	}

	public static String getLob() {
		return SpringContext.getBeanSafely(Environment.class)
				.map(environment -> environment.getProperty("channelkart.lobs", "none"))
				.orElseGet(() -> Optional.ofNullable(System.getenv("channelkart.lobs")).orElse("none"));
	}

}
