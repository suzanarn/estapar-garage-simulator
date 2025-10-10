package com.estapar.parking_system;

import org.springframework.boot.SpringApplication;

public class TestParkingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(ParkingSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
