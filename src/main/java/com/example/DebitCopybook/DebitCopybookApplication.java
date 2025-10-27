package com.example.DebitCopybook;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DebitCopybookApplication {

	@PostConstruct
	public void init() {

		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Baku"));
	}
	public static void main(String[] args) {
		SpringApplication.run(DebitCopybookApplication.class, args);
	}

}
