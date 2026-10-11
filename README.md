# TP 7 : Optimisation des Performances JPA / Hibernate
## Cache de Second Niveau, Résolution du Problème N+1 (JOIN FETCH & Entity Graphs)

Ce projet illustre et compare les différentes techniques d'optimisation des performances dans un environnement **JEE / Hibernate**, notamment l'élimination du problème des requêtes **N+1** et la mise en œuvre du **Cache de Second Niveau (L2)** avec **Ehcache 3 (JSR-107 JCache)** sur une base de données en mémoire **H2**.

---

## 🎯 Objectifs du TP

- Identifier et comprendre le problème de performance **N+1 queries** lié aux relations chargées en mode `LAZY`.
- Supprimer le problème N+1 à l'aide de la clause JPQL **`JOIN FETCH`**.
- Mettre en œuvre une alternative déclarative et réutilisable grâce aux **Entity Graphs** (`@NamedEntityGraph`).
- Configurer, activer et mesurer l'impact du **Cache de Second Niveau (L2)** et du **Cache de Requêtes** avec Hibernate et Ehcache 3.
- Valider le comportement via des **tests unitaires automatisés (JUnit 5)** et l'outil interactif **H2 Web Console**.

---

## 🛠 Environnement & Technologies

- **Langage :** Java OpenJDK 21
- **Gestionnaire de dépendances :** Apache Maven
- **ORM :** Hibernate ORM 5.4.32.Final (JPA 2.2)
- **Fournisseur de Cache L2 :** Ehcache 3.8.1 via `hibernate-jcache` (spécification JSR-107)
- **Base de données :** H2 Database 1.4.200 (embarquée en mémoire `jdbc:h2:mem:test`)
- **Tests unitaires :** JUnit 5 (Jupiter 5.10.0)

---

## 🏗 Architecture du Projet

```text
TP 7/
├── pom.xml                                   # Dépendances Maven (Hibernate, Ehcache, H2, JUnit 5)
├── README.md                                 # Documentation & captures de résultats
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── App.java                      # Point d'entrée principal (banc de test des performances)
│   │   │   ├── H2ConsoleApp.java             # Serveur web H2 interactif (http://localhost:8082)
│   │   │   ├── model/
│   │   │   │   ├── Auteur.java               # Entité Auteur (@Cacheable, @Cache, @NamedEntityGraph)
│   │   │   │   └── Livre.java                # Entité Livre (@Cacheable, @Cache)
│   │   │   └── service/
│   │   │       ├── DataInitService.java      # Peuplement initial (10 auteurs, 50 livres)
│   │   │       └── PerformanceTestService.java # Scénarios de tests comparatifs
│   │   └── resources/
│   │       ├── ehcache.xml                   # Configuration des régions de cache Ehcache 3
│   │       └── META-INF/
│   │           ├── ehcache.xml
│   │           └── persistence.xml           # Configuration JPA, Hibernate L2 et Query Cache
│   └── test/
│       └── java/com/example/
│           └── PerformanceTest.java          # Tests unitaires JUnit 5
```

---

## 💡 Concepts Clés & Solutions

### 1. Le Problème N+1
Par défaut, une association `@OneToMany(fetch = FetchType.LAZY)` ne charge pas la collection d'enfants lors du chargement de l'entité parente.
Lorsque l'application itère sur les $N$ auteurs pour accéder à leurs livres (`auteur.getLivres().size()`), Hibernate déclenche une requête SQL supplémentaire par auteur :
$$\text{Total Requêtes} = 1\text{ (Auteurs)} + N\text{ (Livres)} = 1 + 10 = 11\text{ requêtes}$$

### 2. Solution avec `JOIN FETCH`
La clause `JOIN FETCH` force Hibernate à ramener l'entité parente et sa collection associée en **une seule et unique requête SQL** avec jointure interne (`INNER JOIN`) :
```java
TypedQuery<Auteur> query = em.createQuery(
    "SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
List<Auteur> auteurs = query.getResultList();
```
> *Remarque :* L'usage du mot-clé `DISTINCT` évite la duplication des instances d'auteurs dans la liste résultat due au produit cartésien de la jointure.

### 3. Solution avec `Entity Graph`
L'Entity Graph permet de définir les associations à charger de façon déclarative sur l'entité, sans modifier la requête JPQL de base (`SELECT a FROM Auteur a`) :
```java
@Entity
@NamedEntityGraph(
    name = "Auteur.livres",
    attributeNodes = @NamedAttributeNode("livres")
)
public class Auteur { ... }
```
Utilisation dans la requête :
```java
EntityGraph<?> graph = em.getEntityGraph("Auteur.livres");
TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
query.setHint("javax.persistence.fetchgraph", graph);
List<Auteur> auteurs = query.getResultList();
```

### 4. Cache de Second Niveau (L2) & Cache de Requêtes
- **Cache L1 (Session / Persistence Context) :** Lié à l'instance de l'EntityManager, détruit à la fermeture de la session.
- **Cache L2 (SessionFactory) :** Partagé entre toutes les sessions et transactions de l'application.
- **Configuration :**
  - Entités annotées avec `@Cacheable` et `@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)`.
  - La collection `livres` est également annotée avec `@Cache` pour être conservée en mémoire.
  - La requête JPQL utilise le hint `query.setHint("org.hibernate.cacheable", true)` pour stocker la liste des identifiants dans le Query Results Cache.

---

## 📊 Tableau Comparatif des Performances

| Scénario de Test | Requêtes SQL exécutées | Temps Moyen Observé | Analyse Technique |
| :--- | :---: | :---: | :--- |
| **1. Sans Optimisation (N+1)** | **11 requêtes** ($1 + 10$) | **~1 439 ms** | ❌ Très lent. Surcharge d'allers-retours réseau avec la base de données. |
| **2. Avec `JOIN FETCH`** | **1 requête** (`INNER JOIN`) | **~175 ms** | ✅ Gain immédiat (~8x plus rapide). Résout le problème N+1. |
| **3. Avec `Entity Graph`** | **1 requête** (`LEFT OUTER JOIN`) | **~141 ms** | ✅ Très performant, déclaratif, découplé du code JPQL. |
| **4. Cache L2 (Lecture 1 - DB)** | **1 requête** | **~128 ms** | Chargement initial et mise en cache mémoire des entités. |
| **5. Cache L2 (Lecture 2 - RAM)** | **0 requête DB** | **~10 ms** | 🚀 **Accélération supérieure à 12x** grâce au cache L2 en mémoire vive. |

---

## 📷 Extraits des Résultats & Captures d'Écran


### 1. Consultation des Tables AUTEUR et LIVRES

#### Table `AUTEUR` (10 enregistrements) :
```sql
SELECT * FROM AUTEUR;
```
![Table AUTEUR](Screenshot%202026-10-11%20160115.png)

#### Table `LIVRES` (50 enregistrements) :
```sql
SELECT * FROM LIVRES;
```
![Table LIVRES](Screenshot%202026-10-11%20160216.png)

---

### 2. Vérification des Données Insérées (H2 Console)
Vérification des volumes de données via la console H2 :
- Chaque auteur possède exactement **5 livres**.
- Total global : **10 auteurs** et **50 livres**.

```sql
SELECT a.nom, COUNT(l.id) AS total_livres
FROM AUTEUR a
LEFT JOIN LIVRES l ON a.id = l.auteur_id
GROUP BY a.id, a.nom;

SELECT 
    (SELECT COUNT(*) FROM AUTEUR) AS nb_auteurs,
    (SELECT COUNT(*) FROM LIVRES) AS nb_livres;
```

![Comptage et vérification des données](Screenshot%202026-10-11%20160457.png)

---

### 3. Illustration du Problème N+1
Dans la console H2, on simule le comportement du mode `LAZY` non optimisé : pour chaque auteur, une requête individuelle est envoyée pour charger ses livres :

```sql
SELECT * FROM LIVRES WHERE auteur_id = 1;
SELECT * FROM LIVRES WHERE auteur_id = 2;
SELECT * FROM LIVRES WHERE auteur_id = 3;
```

![Simulation du problème N+1](Screenshot%202026-10-11%20160328.png)

---

### 4. Résolution avec JOIN FETCH
Une unique requête SQL rassemble l'ensemble des données des auteurs et de leurs livres via une jointure interne :

```sql
SELECT DISTINCT a.id AS auteur_id, a.nom, l.id AS livre_id, l.titre
FROM AUTEUR a
INNER JOIN LIVRES l ON a.id = l.auteur_id;
```

![Résolution avec JOIN FETCH](Screenshot%202026-10-11%20160407.png)

---

### 5. Résolution avec Entity Graph
Requête SQL générée par l'Entity Graph, effectuant une jointure externe gauche (`LEFT OUTER JOIN`) tout en préservant la structure du modèle :

```sql
SELECT a.id AS auteur_id, a.nom, l.id AS livre_id, l.titre
FROM AUTEUR a
LEFT OUTER JOIN LIVRES l ON a.id = l.auteur_id;
```

![Résolution avec Entity Graph](Screenshot%202026-10-11%20160428.png)

---

### 6. Validation des Tests Unitaires JUnit 5
Les 4 tests unitaires de la classe `PerformanceTest` s'exécutent avec succès (`4/4 validés`) :
- `testDataInit()` : Intégrité des données insérées.
- `testJoinFetch()` : Résolution du problème N+1.
- `testEntityGraph()` : Chargement déclaratif des livres associés.
- `testSecondLevelCache()` : Validation de la présence de l'entité en cache L2 après vidage du cache L1 (`em.clear()`).

![Tests Unitaires JUnit 5](Screenshot%202026-10-11%20161129.png)

---