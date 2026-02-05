package com.api.bedhcd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BedhcdApplication {

	public static void main(String[] args) {
		// Set timezone to Vietnam
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(BedhcdApplication.class, args);
	}

}
