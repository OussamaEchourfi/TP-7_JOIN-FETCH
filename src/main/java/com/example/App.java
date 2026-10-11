package com.example;

import com.example.service.DataInitService;
import com.example.service.PerformanceTestService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) {
        System.out.println("=== INITIALISATION DE L'APPLICATION JEE / HIBERNATE ===");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);

        EntityManager em = emf.createEntityManager();

        DataInitService dataInitService = new DataInitService(em);
        PerformanceTestService performanceTestService = new PerformanceTestService(em);

        System.out.println("\n=== 1. INITIALISATION DES DONNEES (10 auteurs x 5 livres) ===");
        dataInitService.initData();

        System.out.println("\n=== 2. TEST PERFORMANCES SANS CACHE (N+1) ===");
        performanceTestService.testPerformanceSansCache();

        System.out.println("\n=== 3. TEST PERFORMANCES AVEC JOIN FETCH ===");
        performanceTestService.testPerformanceAvecJoinFetch();

        System.out.println("\n=== 4. TEST PERFORMANCES AVEC ENTITY GRAPH ===");
        performanceTestService.testPerformanceAvecEntityGraph();

        System.out.println("\n=== 5. TEST PERFORMANCES AVEC CACHE DE SECOND NIVEAU ===");
        performanceTestService.testPerformanceAvecCache();

        System.out.println("\n=== 6. TEST CACHE DE SECOND NIVEAU AVEC FIND ===");
        performanceTestService.testCacheSecondNiveauFind();

        // Statistiques Hibernate Cache
        System.out.println("\n=============================================");
        System.out.println("=== STATISTIQUES GLOBALES DU CACHE HIBERNATE ===");
        System.out.println("=============================================");
        System.out.println("L2 Cache Hits (succès)     : " + stats.getSecondLevelCacheHitCount());
        System.out.println("L2 Cache Misses (échecs)   : " + stats.getSecondLevelCacheMissCount());
        System.out.println("L2 Cache Puts (insérés)    : " + stats.getSecondLevelCachePutCount());
        System.out.println("Query Cache Hits           : " + stats.getQueryCacheHitCount());
        System.out.println("Query Cache Misses         : " + stats.getQueryCacheMissCount());
        System.out.println("Query Cache Puts           : " + stats.getQueryCachePutCount());
        System.out.println("Requêtes SQL exécutées     : " + stats.getQueryExecutionCount());
        System.out.println("Entités chargées           : " + stats.getEntityLoadCount());

        em.close();
        emf.close();
        System.out.println("\n=== FIN DE L'EXECUTION DU TP ===");
    }
}
