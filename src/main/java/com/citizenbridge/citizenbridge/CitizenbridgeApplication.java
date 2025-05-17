package com.citizenbridge.citizenbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CitizenbridgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CitizenbridgeApplication.class, args);
	}

}
