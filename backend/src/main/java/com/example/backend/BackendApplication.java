package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {

        // Configure the application timezone to Vietnam time.
        // Required because the PostgreSQL Docker container uses Etc/UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        System.out.println("Timezone = " + TimeZone.getDefault().getID());
        System.out.println("ZoneId = " + java.time.ZoneId.systemDefault());

        SpringApplication.run(BackendApplication.class, args);
	}

}
