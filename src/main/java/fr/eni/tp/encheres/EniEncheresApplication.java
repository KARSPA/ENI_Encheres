package fr.eni.tp.encheres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EniEncheresApplication {

	public static void main(String[] args) {
		SpringApplication.run(EniEncheresApplication.class, args);
	}

}
