package edu.esi.ds.esientradas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EsientradasApplication {

	public static void main(String[] args) {
		SpringApplication.run(EsientradasApplication.class, args);
	}

}
