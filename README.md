# Spring Authorization Server — Production Ready

Serveur d'authentification OAuth2 / OIDC complet, basé sur **Spring Authorization Server 1.x** avec :

- ✅ Gestion des utilisateurs (CRUD)
- ✅ Interface admin
- ✅ Reset de mot de passe par email
- ✅ MFA / 2FA TOTP (Google Authenticator, Authy)
- ✅ Social Login (Google, GitHub)
- ✅ Base DB2 (Flyway migrations)
- ✅ Nettoyage automatique des tokens expirés

---

## Prérequis

- **Java 21+**
- **Maven 3.9+**
- **DB2 LUW 11.5+** (ou IBM Db2 Community Edition)

---

## Démarrage rapide

### 1. Configuration

Copier et adapter les variables d'environnement :

```bash
# DB2
export DB_HOST=localhost
export DB_PORT=50000
export DB_NAME=AUTHDB
export DB_USERNAME=db2user
export DB_PASSWORD=yourpassword

# URL publique du serveur
export APP_BASE_URL=http://localhost:8080

# Email (Gmail exemple)
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your@gmail.com
export MAIL_PASSWORD=your-app-password
export MAIL_FROM=noreply@yourdomain.com

# Social login (optionnel)
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
export GITHUB_CLIENT_ID=...
export GITHUB_CLIENT_SECRET=...

# Admin initial password (changer à la première connexion !)
export ADMIN_INITIAL_PASSWORD=Admin@123!
```

### 2. Créer la base DB2

```sql
CREATE DATABASE AUTHDB USING CODESET UTF-8 TERRITORY FR PAGESIZE 32768;
```

### 3. Build & Run

```bash
mvn clean package -DskipTests
java -jar target/spring-auth-server-1.0.0-SNAPSHOT.jar
```

Les migrations Flyway créent automatiquement toutes les tables au démarrage.

---

## Accès

| URL | Description |
|-----|-------------|
| `http://localhost:8080/login` | Page de connexion |
| `http://localhost:8080/admin/users` | Gestion des utilisateurs |
| `http://localhost:8080/admin/clients` | Gestion des clients OAuth2 |
| `http://localhost:8080/account` | Mon compte (MFA, mot de passe) |
| `http://localhost:8080/.well-known/openid-configuration` | Discovery OIDC |
| `http://localhost:8080/oauth2/jwks` | Clés publiques JWT |

**Admin par défaut** : `admin` / `Admin@123!`  
⚠️ Changer le mot de passe immédiatement après la première connexion !

---

## Client OAuth2 par défaut

Un client `demo-client` est enregistré au démarrage :

| Paramètre | Valeur |
|-----------|--------|
| Client ID | `demo-client` |
| Client Secret | `demo-secret` |
| Grant types | `authorization_code`, `refresh_token`, `client_credentials` |
| Scopes | `openid`, `profile`, `email`, `read`, `write` |
| Redirect URIs | `http://localhost:8080/login/oauth2/code/demo-client`, `http://localhost:3000/callback` |

Ajouter d'autres clients via `/admin/clients/new`.

---

## Social Login

### Google

1. Créer un projet sur [Google Cloud Console](https://console.cloud.google.com/)
2. Activer "OAuth 2.0 Client IDs" (type: Web application)
3. Ajouter comme Authorized redirect URI : `http://votre-domaine/login/oauth2/code/google`
4. Renseigner `GOOGLE_CLIENT_ID` et `GOOGLE_CLIENT_SECRET`

### GitHub

1. Aller dans GitHub Settings → Developer settings → OAuth Apps
2. Callback URL : `http://votre-domaine/login/oauth2/code/github`
3. Renseigner `GITHUB_CLIENT_ID` et `GITHUB_CLIENT_SECRET`

---

## MFA (2FA TOTP)

Les utilisateurs activent le 2FA depuis leur page `/account` :
1. Clic sur **Enable 2FA**
2. Scanner le QR code avec Google Authenticator ou Authy
3. Saisir le code de vérification
4. À la prochaine connexion, un code à 6 chiffres sera demandé

Les admins peuvent réinitialiser le MFA d'un utilisateur (perte de téléphone) via `/admin/users/{id}/edit`.

---

## Clés JWT en production

Par défaut, une clé RSA éphémère est générée au démarrage (non persistée).  
**Pour la production**, utiliser un keystore JKS :

```bash
# Générer le keystore
keytool -genkeypair -alias authserver -keyalg RSA -keysize 2048 \
  -keystore authserver.jks -storepass changeit -validity 3650

# Configurer
export JWT_KEYSTORE_PATH=/path/to/authserver.jks
export JWT_KEYSTORE_PASSWORD=changeit
export JWT_KEY_ALIAS=authserver
```

> Note : le support keystore est préparé dans `SecurityConfig` — 
> implémenter `KeyStoreKeyFactory` pour l'activer.

---

## Structure du projet

```
src/main/java/com/example/authserver/
├── config/          # SecurityConfig, MFA handler, Client seeder
├── controller/      # Home, Account, MFA, Password reset
│   └── admin/       # AdminUserController, AdminClientController
├── dto/             # UserDto, UserForm
├── entity/          # User, Role, PasswordResetToken
├── repository/      # JPA repositories
├── security/        # UserDetailsImpl
└── service/         # User, MFA, Email, PasswordReset, OAuth2 social

src/main/resources/
├── db/migration/    # V1 (schema), V2 (OAuth2 tables), V3 (data)
├── static/css/      # main.css
└── templates/       # Thymeleaf (auth, mfa, account, admin)
```

---

## Sécurité — checklist production

- [ ] Changer le mot de passe admin
- [ ] Configurer un keystore JKS persistent
- [ ] Activer HTTPS (TLS) devant le serveur (Nginx, load balancer)
- [ ] Configurer des secrets forts pour les clients OAuth2
- [ ] Restreindre l'accès `/admin/**` par IP si possible
- [ ] Configurer `SESSION_TIMEOUT` adapté
- [ ] Activer l'audit logging
- [ ] Configurer les backups DB2
