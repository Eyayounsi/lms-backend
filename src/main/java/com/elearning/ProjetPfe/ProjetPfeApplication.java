package com.elearning.ProjetPfe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjetPfeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjetPfeApplication.class, args);
		System.out.println("==========================================");
		System.out.println("✅ APPLICATION DÉMARRÉE AVEC SUCCÈS");
		System.out.println("📍 http://localhost:8080/api/auth/test");
		System.out.println("==========================================");
	}

}
