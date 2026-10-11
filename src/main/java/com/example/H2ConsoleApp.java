package com.example;

import com.example.service.DataInitService;
import org.h2.tools.Server;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Scanner;

public class H2ConsoleApp {

    public static void main(String[] args) {
        System.out.println("=== DEMARRAGE DE LA CONSOLE WEB H2 ===");

        try {
            // Démarrage du serveur Web H2 sur le port 8082
            Server webServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
            System.out.println("\n==============================================================");
            System.out.println(">>> Console Web H2 active !");
            System.out.println(">>> URL du navigateur : http://localhost:8082");
            System.out.println(">>> Paramètres de connexion :");
            System.out.println("    - Driver Class : org.h2.Driver");
            System.out.println("    - JDBC URL     : jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            System.out.println("    - User Name    : sa");
            System.out.println("    - Password     : (laisser vide)");
            System.out.println("==============================================================\n");

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
            EntityManager em = emf.createEntityManager();

            // Initialiser les données de test
            DataInitService dataInitService = new DataInitService(em);
            dataInitService.initData();
            System.out.println("Données prêtes : 10 auteurs et 50 livres insérés dans la base H2.");

            System.out.println("\nVous pouvez exécuter vos requêtes SQL dans votre navigateur.");
            System.out.println("Appuyez sur ENTRÉE pour fermer la console H2...");

            Scanner scanner = new Scanner(System.in);
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }

            em.close();
            emf.close();
            webServer.stop();
            System.out.println("Serveur H2 arrêté proprement.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
