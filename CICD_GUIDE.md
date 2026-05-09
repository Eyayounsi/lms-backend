# Guide CI/CD GitHub Actions — LMS Backend

## Ce qui sera mis en place

| Job | Déclencheur | Durée estimée |
|-----|------------|---------------|
| Build & Test | Chaque push / PR | ~3-5 min |
| SonarCloud (qualité) | PR + push main | ~4-6 min |
| Docker Build & Push | Création d'un tag `vX.X.X` | ~4-6 min |
| Dependabot (sécurité) | Automatique chaque lundi | Passif |

---

## ÉTAPE 1 — Accéder à SonarCloud (gratuit)

### 1.1 Créer un compte SonarCloud

1. Va sur **[sonarcloud.io](https://sonarcloud.io)**
2. Clique sur **"Log in"** → choisir **"Log in with GitHub"**
3. Autorise SonarCloud à accéder à ton compte GitHub
4. Tu arrives sur le tableau de bord SonarCloud

### 1.2 Créer une organisation SonarCloud

1. Clique sur le **"+"** en haut à droite → **"Create new organization"**
2. Clique sur **"Import from GitHub"**
3. Sélectionne ton compte GitHub `Eyayounsi`
4. Clique **"Continue"**
5. Choisis le **plan Free** → **"Create organization"**
6. Ton organization key sera : `eyayounsi` *(note-le, tu en auras besoin)*

### 1.3 Créer le projet SonarCloud

1. Dans SonarCloud, clique **"Analyze new project"**
2. Sélectionne le repo **`lms-backend`**
3. Clique **"Set Up"**
4. Choisis **"With GitHub Actions"**
5. SonarCloud va te montrer un **SONAR_TOKEN** → **copie-le maintenant** *(tu ne pourras plus le voir)*
6. Note aussi ton **Project Key** : `Eyayounsi_lms-backend`

---

## ÉTAPE 2 — Créer un compte Docker Hub (pour les images Docker)

### 2.1 Créer le compte

1. Va sur **[hub.docker.com](https://hub.docker.com)**
2. Clique **"Sign Up"** → crée un compte (note ton username)
3. Vérifie ton email

### 2.2 Créer un Access Token Docker Hub

1. Dans Docker Hub, clique sur ton **avatar** en haut à droite → **"Account Settings"**
2. Va dans **"Security"** → **"New Access Token"**
3. Description : `github-actions-lms`
4. Permissions : **Read, Write, Delete**
5. Clique **"Generate"** → **copie le token** *(ne se réaffiche pas)*

---

## ÉTAPE 3 — Configurer les Secrets GitHub

C'est ici qu'on stocke les mots de passe et tokens de façon sécurisée.

### 3.1 Accéder aux Secrets du repo backend

1. Va sur **[github.com/Eyayounsi/lms-backend](https://github.com/Eyayounsi/lms-backend)**
2. Clique sur **"Settings"** (onglet en haut du repo)
3. Dans le menu gauche : **"Secrets and variables"** → **"Actions"**
4. Tu vois la page **"Actions secrets and variables"**

### 3.2 Ajouter chaque secret (cliquer "New repository secret" pour chacun)

Clique **"New repository secret"** → entre le nom EXACT et la valeur :

| Nom du secret | Valeur à mettre | Où trouver |
|--------------|----------------|-----------|
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | Fichier `.env` → `JWT_SECRET` |
| `MAIL_USERNAME` | `eyayoounsi@gmail.com` | Fichier `.env` → `MAIL_USERNAME` |
| `MAIL_PASSWORD` | Mot de passe app Gmail (16 caractères) | Fichier `.env` → `MAIL_PASSWORD` |
| `STRIPE_SECRET_KEY` | `sk_test_51T3lMW0b...` (clé complète) | Fichier `.env` → `STRIPE_SECRET_KEY` |
| `STRIPE_WEBHOOK_SECRET` | `whsec_...` | Stripe Dashboard → Webhooks |
| `GOOGLE_CLIENT_ID` | `78802609982-i414nh1k...` | Fichier `.env` → `GOOGLE_CLIENT_ID` |
| `GOOGLE_CLIENT_SECRET` | `placeholder` *(non utilisé pour l'instant)* | Mettre `placeholder` si absent du `.env` |
| `FACE_SERVICE_SECRET` | `lms-face-id-secret-2024` | Fichier `.env` → `FACE_SERVICE_SECRET` |
| `SONAR_TOKEN` | Token généré sur sonarcloud.io | Étape 1.3 |
| `DOCKERHUB_USERNAME` | Ton username Docker Hub | Docker Hub |
| `DOCKERHUB_TOKEN` | Access token Docker Hub | Étape 2.2 |

> ⚠️ Le nom du secret doit être **exactement** comme indiqué (majuscules, underscores)
>
> ℹ️ Les clés `GROQ_API_KEY`, `GEMINI_API_KEY`, `HF_TOKEN` ne sont **pas nécessaires** dans GitHub Secrets — elles sont utilisées uniquement par le service chatbot Docker, pas par le pipeline CI.

### 3.3 Vérifier que tous les secrets sont là

Après avoir tout ajouté, la liste doit montrer ces **11 secrets** (les valeurs sont masquées, c'est normal).

---

## ÉTAPE 4 — Activer Dependabot (sécurité)

### 4.1 Activer dans les paramètres

1. Dans le repo **lms-backend** → **"Settings"**
2. Dans le menu gauche : **"Code security and analysis"**
3. Active ces 3 options :
   - **Dependabot alerts** → clic **"Enable"**
   - **Dependabot security updates** → clic **"Enable"**
   - **Dependabot version updates** → *(activé automatiquement par le fichier `.github/dependabot.yml` déjà créé)*

---

## ÉTAPE 5 — Pousser le code et déclencher le premier workflow

### 5.1 Vérifier que les fichiers sont bien présents en local

Les fichiers suivants ont été créés dans ton projet :
```
backend/lms-backend/
├── .github/
│   ├── workflows/
│   │   └── ci.yml          ← Le workflow CI/CD
│   └── dependabot.yml      ← Dependabot
```

### 5.2 Pusher vers GitHub

Ouvre un terminal dans `C:\Users\Lenovo\Desktop\LMS_eya\backend\lms-backend` :

```bash
git add .github/
git commit -m "ci: add GitHub Actions workflows and Dependabot"
git push origin main
```

### 5.3 Observer le premier run

1. Va sur **[github.com/Eyayounsi/lms-backend](https://github.com/Eyayounsi/lms-backend)**
2. Clique sur l'onglet **"Actions"**
3. Tu vois le workflow **"CI - Build & Test"** en cours d'exécution (icône jaune = en cours)
4. Clique dessus pour voir les logs en temps réel

---

## ÉTAPE 6 — Comprendre les résultats

### 6.1 Icônes de statut

| Icône | Signification |
|-------|--------------|
| 🟡 Jaune (spinning) | En cours d'exécution |
| ✅ Vert | Succès — tout est OK |
| ❌ Rouge | Échec — clique pour voir l'erreur |
| ⏭️ Gris | Ignoré (condition non remplie) |

### 6.2 Le job SonarCloud sera gris au premier push

C'est normal — il s'exécute uniquement sur `main` ou sur une PR. Il s'activera quand le build-and-test est vert.

### 6.3 Voir les résultats SonarCloud

1. Va sur **[sonarcloud.io](https://sonarcloud.io)**
2. Ton organisation → projet **lms-backend**
3. Tu verras : nombre de bugs, code smells, couverture de tests, duplications

---

## ÉTAPE 7 — Créer une release Docker (optionnel)

Pour déclencher le build Docker, il faut créer un tag Git :

```bash
git tag v1.0.0
git push origin v1.0.0
```

Cela va :
1. Compiler le JAR avec Maven
2. Builder l'image Docker
3. Pousser `ton-username/lms-backend:1.0.0` et `ton-username/lms-backend:latest` sur Docker Hub

---

## ÉTAPE 8 — Ajouter le badge de statut dans le README (optionnel)

Ajoute dans ton `README.md` du backend :

```markdown
[![CI](https://github.com/Eyayounsi/lms-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/Eyayounsi/lms-backend/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Eyayounsi_lms-backend&metric=alert_status)](https://sonarcloud.io/project/overview?id=Eyayounsi_lms-backend)
```

---

## Résumé des URLs importantes

| Service | URL |
|---------|-----|
| GitHub Actions | https://github.com/Eyayounsi/lms-backend/actions |
| SonarCloud | https://sonarcloud.io/project/overview?id=Eyayounsi_lms-backend |
| Docker Hub | https://hub.docker.com/r/TON_USERNAME/lms-backend |
| Dependabot alerts | https://github.com/Eyayounsi/lms-backend/security/dependabot |

---

## Problèmes fréquents

### ❌ "SONAR_TOKEN not found"
→ Vérifie que le secret s'appelle exactement `SONAR_TOKEN` (majuscules)

### ❌ "Connection refused" pour MySQL dans les tests
→ Le test Spring Boot essaie de se connecter à la vraie DB. Solution : ajouter `@MockBean` ou une config `application-test.properties` avec H2 en mémoire.

### ❌ "mvn: command not found"
→ Normal sur GitHub Actions — Maven est installé via `actions/setup-java@v4` automatiquement.

### ❌ Docker push échoue
→ Vérifie que `DOCKERHUB_USERNAME` et `DOCKERHUB_TOKEN` sont corrects. Le token doit avoir les droits **Write**.