# TP1 – Modélisation XML et interrogation XPath avec Maven en Java

**Matière** : Architecture SOA et services web — **Filière** : LSI3 — Faculté des Sciences de Sfax (2025-2026)

## 1. Contenu du dépôt

| Fichier | Rôle |
|---|---|
| `pom.xml` | Projet Maven (Java 11, plugin `exec`, JUnit en test). JAXP est inclus dans le JDK : aucune dépendance externe nécessaire à l'exécution |
| `src/main/resources/cinema.xml` | Document XML (domaine : cinéma) |
| `src/main/resources/cinema.xsd` | Schéma XSD de validation |
| `src/main/resources/cinema-invalide.xml` | Copie volontairement erronée, pour démontrer la validation |
| `src/main/java/tn/fss/soa/Main.java` | Classe principale : validation XSD + requêtes XPath |

## 2. Exécution

```bash
mvn compile exec:java
```
ou depuis IntelliJ : clic droit sur `Main` → *Run*.

## 3. Modélisation

**Domaine** : un cinéma qui projette des films dans des salles lors de séances.

```
cinema (@nom)
├── films
│   └── film (@id, @genre)  : titre, realisateur, annee, dureeMinutes, note, enSalle
├── salles
│   └── salle (@id)         : nom, capacite
└── seances
    └── seance (@id, @filmRef, @salleRef) : date, heure, prix
├── spectateurs
│   └── spectateur (@id, @actif) : nom, email, age
└── reservations
    └── reservation (@id, @spectateurRef, @seanceRef) : nbPlaces
```

### Choix de conception
- **Cinq entités** (film, salle, séance, spectateur, réservation) regroupées dans des conteneurs `films/salles/seances/spectateurs/reservations`, ce qui rend les requêtes XPath lisibles et le document extensible.
- **Identifiants et références** : `xs:ID` pour `id`, `xs:IDREF` pour `filmRef` et `salleRef`. Le validateur vérifie donc l'unicité des identifiants et que chaque séance référence un film et une salle existants, et chaque réservation un spectateur et une séance existants (intégrité référentielle).
- **Attributs vs éléments** : les métadonnées (`id`, `genre`, références) sont des attributs ; les données descriptives sont des éléments.
- **Typage strict dans le XSD** :
  - `genre` : énumération (`ACTION`, `COMEDIE`, `DRAME`, `SCIENCE_FICTION`, `THRILLER`)
  - `annee` : entier entre 1888 et 2100 ; `note` : décimal entre 0 et 10 ; `prix` > 0
  - `email` : motif `x@y.z` ; `age` : entier entre 0 et 120 ; `actif` : `xs:boolean`
  - `enSalle` : `xs:boolean` ; `date` : `xs:date` ; `heure` : `xs:time` ; `capacite`, `dureeMinutes` : `xs:positiveInteger`
- Un type nommé par entité (`FilmType`, `SalleType`, `SeanceType`) pour la réutilisabilité.

## 4. Fonctionnalité 1 – Validation XML/XSD

`SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI)` → `newSchema(xsd)` → `schema.newValidator()` → `validator.validate(xml)`.
Un `ErrorHandler` collecte toutes les erreurs (avec le numéro de ligne) au lieu de s'arrêter à la première.

Résultat attendu :
```
[OK]  /cinema.xml est VALIDE par rapport a /cinema.xsd
[KO]  /cinema-invalide.xml est INVALIDE (4 erreur(s)) :
      - ligne 9 : ... Value '15' is not facet-valid with respect to maxInclusive '10.0' ...
      - ligne 12 : ... Value 'WESTERN' ... must be a value from the enumeration.
```

## 5. Fonctionnalité 2 – Interrogation XPath

`XPathFactory.newInstance().newXPath()` → `xpath.compile(expression)` (`XPathExpression`) → `evaluate(doc, XPathConstants.NODESET | NUMBER)`.

| Besoin | Expression XPath | Résultat |
|---|---|---|
| Tous les titres de films | `//film/titre` | Inception, Le Parrain, Intouchables, Dune : Deuxième partie, Parasite |
| Films actuellement en salle | `//film[enSalle='true']/titre` | Inception, Dune, Parasite |
| Films de science-fiction | `//film[@genre='SCIENCE_FICTION']/titre` | Inception, Dune |
| Films très bien notés | `//film[note >= 8.8]/titre` | Inception, Le Parrain |
| Réalisateurs des films après 2010 | `//film[annee > 2010]/realisateur` | Nakache, Villeneuve, Bong Joon-ho |
| Séances à moins de 13 DT | `//seance[prix < 13]/heure` | 18:00:00, 16:00:00 |
| Film d'une séance donnée (via IDREF) | `id(//seance[@id='se3']/@filmRef)/titre` | Parasite |
| Spectateurs actifs | `//spectateur[@actif='true']/nom` | Sarra, Youssef, Ahmed |
| Spectateurs de moins de 25 ans | `//spectateur[age < 25]/nom` | Sarra, Mariem |
| Spectateurs de la séance se1 | `id(//reservation[@seanceRef='se1']/@spectateurRef)/nom` | Sarra, Youssef |
| Audience de la séance se1 | `sum(//reservation[@seanceRef='se1']/nbPlaces)` | 6 |
| Audience totale | `sum(//reservation/nbPlaces)` | 12 |
| Capacité totale | `sum(//salle/capacite)` | 240 |
| Nombre de films | `count(//film)` | 5 |

> Note : la fonction `id()` ne fonctionne que si le document est parsé avec le schéma (les attributs `xs:ID` sont alors reconnus). C'est pourquoi `chargerDocument` appelle `dbf.setSchema(...)`.

## 6. Auteur
Med Hassan Ben Adi — LSI3
