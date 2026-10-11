package com.example.service;

import com.example.model.Auteur;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class PerformanceTestService {

    @PersistenceContext
    private EntityManager em;

    public PerformanceTestService() {
    }

    public PerformanceTestService(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public void testPerformanceSansCache() {
        System.out.println("\n--- 1. Test Sans Cache (Problème N+1) ---");
        em.clear();
        long start = System.currentTimeMillis();

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        // Accès aux livres pour provoquer le chargement LAZY (problème N+1)
        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Durée sans cache : " + (end - start) + " ms");
    }

    public void testPerformanceAvecJoinFetch() {
        System.out.println("\n--- 2. Test Avec JOIN FETCH ---");
        em.clear();
        long start = System.currentTimeMillis();

        TypedQuery<Auteur> query = em.createQuery("SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        // L'accès aux livres ne déclenche plus de requêtes supplémentaires
        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Durée avec JOIN FETCH : " + (end - start) + " ms");
    }

    public void testPerformanceAvecEntityGraph() {
        System.out.println("\n--- 3. Test Avec Entity Graph ---");
        em.clear();
        long start = System.currentTimeMillis();

        EntityGraph<?> graph = em.getEntityGraph("Auteur.livres");

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
        query.setHint("javax.persistence.fetchgraph", graph);
        List<Auteur> auteurs = query.getResultList();

        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Durée avec Entity Graph : " + (end - start) + " ms");
    }

    public void testPerformanceAvecCache() {
        System.out.println("\n--- 4. Test Performance Avec Cache de second niveau ---");
        em.clear();

        // Première lecture - chargement depuis la base avec mise en cache
        System.out.println("[Lecture 1 - chargement depuis la base (requête SQL exécutée)]");
        long start1 = System.currentTimeMillis();
        TypedQuery<Auteur> query1 = em.createQuery("SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        query1.setHint("org.hibernate.cacheable", true);
        List<Auteur> auteurs1 = query1.getResultList();
        for (Auteur auteur : auteurs1) {
            auteur.getLivres().size();
        }
        long end1 = System.currentTimeMillis();
        System.out.println("Durée lecture 1 (base de données) : " + (end1 - start1) + " ms");

        // Deuxième lecture - chargement depuis le cache (Query Cache + L2 Cache)
        System.out.println("\n[Lecture 2 - chargement depuis le cache (aucune nouvelle requête SQL)]");
        long start2 = System.currentTimeMillis();
        TypedQuery<Auteur> query2 = em.createQuery("SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        query2.setHint("org.hibernate.cacheable", true);
        List<Auteur> auteurs2 = query2.getResultList();
        for (Auteur auteur : auteurs2) {
            auteur.getLivres().size();
        }
        long end2 = System.currentTimeMillis();
        System.out.println("Durée lecture 2 (depuis le cache) : " + (end2 - start2) + " ms");
    }

    public void testCacheSecondNiveauFind() {
        System.out.println("\n--- 5. Test Cache de second niveau avec em.find() ---");
        Long premierId = em.createQuery("SELECT min(a.id) FROM Auteur a", Long.class).getSingleResult();
        System.out.println("ID cible testé : " + premierId);

        // Vider le cache de premier niveau (Session / Persistence Context)
        em.clear();

        System.out.println("Recherche Auteur ID " + premierId + " (récupération optimisée via cache L2) :");
        Auteur auteur = em.find(Auteur.class, premierId);
        if (auteur != null) {
            System.out.println("Auteur récupéré : " + auteur.getNom() + " (livres associés: " + auteur.getLivres().size() + ")");
        }
    }
}
