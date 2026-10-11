package com.example;

import com.example.model.Auteur;
import com.example.model.Livre;
import com.example.service.DataInitService;
import org.junit.jupiter.api.*;

import javax.persistence.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceTest {

    private static EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    public static void setUpFactory() {
        emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        EntityManager em = emf.createEntityManager();
        DataInitService initService = new DataInitService(em);
        initService.initData();
        em.close();
    }

    @AfterAll
    public static void tearDownFactory() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    public void setUp() {
        em = emf.createEntityManager();
    }

    @AfterEach
    public void tearDown() {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Vérifier l'initialisation des données (10 auteurs, 50 livres)")
    public void testDataInit() {
        Long nbAuteurs = em.createQuery("SELECT COUNT(a) FROM Auteur a", Long.class).getSingleResult();
        Long nbLivres = em.createQuery("SELECT COUNT(l) FROM Livre l", Long.class).getSingleResult();

        assertEquals(10L, nbAuteurs, "Il doit y avoir 10 auteurs");
        assertEquals(50L, nbLivres, "Il doit y avoir 50 livres");
    }

    @Test
    @Order(2)
    @DisplayName("Test JOIN FETCH : résout le problème N+1 en chargeant auteurs et livres")
    public void testJoinFetch() {
        TypedQuery<Auteur> query = em.createQuery(
                "SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        assertEquals(10, auteurs.size(), "La liste doit contenir 10 auteurs distincts");
        for (Auteur auteur : auteurs) {
            assertNotNull(auteur.getLivres(), "La liste des livres ne doit pas être nulle");
            assertEquals(5, auteur.getLivres().size(), "Chaque auteur doit avoir 5 livres");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test Entity Graph : chargement déclaratif des livres associés")
    public void testEntityGraph() {
        EntityGraph<?> graph = em.getEntityGraph("Auteur.livres");

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
        query.setHint("javax.persistence.fetchgraph", graph);
        List<Auteur> auteurs = query.getResultList();

        assertEquals(10, auteurs.size(), "Il doit y avoir 10 auteurs");
        for (Auteur auteur : auteurs) {
            assertEquals(5, auteur.getLivres().size(), "Chaque auteur doit avoir 5 livres chargés");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test Cache de second niveau L2 : présence des entités dans le cache partagé")
    public void testSecondLevelCache() {
        Long id = em.createQuery("SELECT MIN(a.id) FROM Auteur a", Long.class).getSingleResult();

        Auteur auteur = em.find(Auteur.class, id);
        assertNotNull(auteur);

        // Vider le cache de premier niveau (Session)
        em.clear();

        // Vérification de la présence dans le cache L2 via l'API JPA Cache
        Cache cache = emf.getCache();
        assertTrue(cache.contains(Auteur.class, id),
                "L'entité Auteur doit être présente dans le cache de second niveau (L2)");

        // Lecture depuis le cache
        Auteur auteurFromCache = em.find(Auteur.class, id);
        assertNotNull(auteurFromCache);
        assertEquals(auteur.getNom(), auteurFromCache.getNom());
    }
}
