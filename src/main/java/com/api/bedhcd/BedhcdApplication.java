package com.api.bedhcd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {
		"com.api.bedhcd.modules",
		"com.api.bedhcd.shared",
		"com.api.bedhcd.config",
		"com.api.bedhcd.util",
		"com.api.bedhcd.listener"
})
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = {
		"com.api.bedhcd.modules",
		"com.api.bedhcd.shared"
})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = {
		"com.api.bedhcd.modules",
		"com.api.bedhcd.shared"
})
public class BedhcdApplication {

	public static void main(String[] args) {
		// Set timezone to Vietnam
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(BedhcdApplication.class, args);
	}

}
