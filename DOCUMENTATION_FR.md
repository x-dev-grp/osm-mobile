# Documentation OSM Mobile (FR)

## 1. Vue d’ensemble

OSM Mobile est l’application Android destinée aux opérations terrain et à la consultation des
entités métier (lots, palettes, colis, OF, unités de stockage). Elle couvre :

- Authentification OAuth2 et gestion de session.
- Scan QR (standard + format JSON sécurisé) et historique local.
- Consultation des entités et détails d’unités de stockage.
- Gestion des opérations de filtration.
- File d’attente hors‑ligne + synchronisation automatique au retour réseau.

Cette documentation décrit les fonctionnalités réellement présentes dans le module `osm-mobile`.

---

## 2. Périmètre technique

### 2.1 Plateforme et versions

- Module : `osm-mobile`
- Compile SDK : 36
- Min SDK : 29
- Target SDK : 36
- Kotlin/JVM : 11

### 2.2 Dépendances principales

- UI : AndroidX + Material Components
- Réseau : Retrofit + OkHttp + Gson
- Concurrence : Coroutines
- Scan : Quickie (QR code)

Fichier : `app/build.gradle.kts`

---

## 3. Configuration

### 3.1 Base API

La base API est définie dans `Constants.BASE_URL` :

- Fichier : `app/src/main/java/com/xdev/osm_mobile/utils/Constants.kt`
- Valeur par défaut : `http://10.0.2.2:8084/` (localhost de l’émulateur Android)

### 3.2 OAuth2 (client mobile)

Le client OAuth2 est défini dans `Constants` :

- `CLIENT_ID` : `osm-client`
- `CLIENT_SECRET` : `X7kP9mN2vQ8rT4wY6zA1bC3dE5fG8hJ9`

Le flux d’authentification se fait via `POST /oauth2/token` avec Basic Auth côté client.

### 3.3 Trafic en clair

`android:usesCleartextTraffic="true"` est activé dans `AndroidManifest.xml` pour permettre l’accès
HTTP en local.

---

## 4. Architecture (vue rapide)

### 4.1 Couches

- **UI** : activités + adaptateurs (`app/src/main/java/com/xdev/osm_mobile/ui/...`)
- **Réseau** : Retrofit + DTO (`app/src/main/java/com/xdev/osm_mobile/network/...`)
- **Session & utilitaires** : `SessionManager`, `NetworkUtils`, `Constants`
- **Offline** : `OfflineActionManager`, `OfflineOperationExecutor`, `OfflineSyncManager`

### 4.2 Démarrage

Point d’entrée : `MainActivity` (navigation principale + scan QR).

---

## 5. Authentification & session

### 5.1 Écran de connexion

**Fichiers clés**

- `ui/login/LoginActivity.kt`
- `ui/login/LoginViewModel.kt`

**Fonctionnalités**

- Validation locale des champs (username + password).
- Appel OAuth2 via `ApiService.login`.
- Sauvegarde des tokens et infos utilisateur dans `SessionManager`.
- Redirection automatique si session déjà active.

### 5.2 Gestion de session

**Fichier clé**

- `utils/SessionManager.kt`

**Données stockées**

- Access token, refresh token, userId, username, rôle, permissions, tenantId.
- Méthodes utilitaires pour permissions et modules (`hasPermission`, `hasModule`).

### 5.3 Refresh token automatique

**Fichier clé**

- `network/RetrofitClient.kt`

Un `Authenticator` OkHttp rafraîchit le token en cas de 401, puis rejoue la requête.

---

## 6. Réinitialisation du mot de passe

Flux en 3 étapes :

1. **ResetPasswordActivity** : saisie de l’identifiant → envoi du code.
2. **ValidateCodeActivity** : validation du code reçu.
3. **NewPasswordActivity** : définition du nouveau mot de passe.

**Fichiers clés**

- `ui/resetpassword/ResetPasswordActivity.kt`
- `ui/resetpassword/ValidateCodeActivity.kt`
- `ui/resetpassword/NewPasswordActivity.kt`
- `ui/resetpassword/ResetPasswordViewModel.kt`

---

## 7. Navigation principale

### 7.1 Accueil

**Fichier clé**

- `MainActivity.kt`

**Fonctionnalités**

- Cartes d’accès rapide : OF, unités de stockage, colis, palettes, lots, filtration.
- Menu FAB : lancer scan QR, ouvrir l’historique des scans.
- Menu toolbar : accès à la file de synchronisation et déconnexion.

### 7.2 Liste d’entités (écran unique)

**Fichiers clés**

- `ui/entity/EntityListActivity.kt`
- `ui/entity/EntityListType.kt`
- `ui/entity/adapters/EntityAdapter.kt`

**Fonctionnalités**

- Un seul écran liste pour tous les types d’entités.
- Résolution du type via `EntityListType` (LOT, COLIS, PALETTE, OF, STORAGE_UNIT).
- Support de scans : possibilité d’injecter `entity_id` ou `entity_json`.

### 7.3 Détail d’une unité de stockage

**Fichier clé**

- `ui/entity/StorageUnitDetailsActivity.kt`

**Fonctionnalités**

- Affiche statut, capacité, coûts, progression.
- Liste des transactions d’huile liées à l’unité.

---

## 8. QR Scan & Historique

### 8.1 Scan QR

**Fichiers clés**

- `ui/scan/qrcode/QrScannerManager.kt`
- `MainActivity.kt`

**Fonctionnalités**

- Scan QR via Quickie (caméra + overlay).
- Mapping du résultat vers `ScanUiResult`.
- Dialogue résumé après scan (`ScanResultDialogFragment`).

### 8.2 Décodage sécurisé

**Fichier clé**

- `ui/scan/qrcode/QrDecoder.kt`

**Fonctionnalités**

- Déchiffrement AES/GCM si QR chiffré.
- KDF PBKDF2 pour dériver la clé.
- Support d’un format JSON universel avec metadata.
- Fallback sur format legacy si le QR n’est pas JSON.

### 8.3 Historique des scans

**Fichiers clés**

- `ui/scan/ScanHistory.kt`
- `ui/scan/ScanHistoryViewModel.kt`
- `ui/scan/ScanHistoryManager.kt`
- `ui/scan/ScanHistoryAdapter.kt`

**Fonctionnalités**

- Liste locale des scans (SharedPreferences).
- Détails d’un scan (clic) + suppression (clic long).
- Synchronisation manuelle via FAB (marque localement comme synchronisé).

---

## 9. Filtration

**Fichier clé**

- `ui/filtration/FiltrationActivity.kt`

**Fonctionnalités**

- Liste des opérations de filtration.
- Création d’une filtration (source, cible, volume, note).
- Mise à jour de statut (IN_PROGRESS, COMPLETED, etc.).
- Gestion hors‑ligne via la file d’actions.

---

## 10. Offline & synchronisation

### 10.1 File d’actions hors‑ligne

**Fichiers clés**

- `ui/offline/OfflineActionManager.kt`
- `ui/offline/OfflineOperationExecutor.kt`
- `ui/offline/OfflineSyncManager.kt`

**Principe**

- `OfflineOperationExecutor.runOrQueue(...)` exécute l’action si en ligne, sinon l’enregistre.
- Les actions sont persistées en local via `OfflineActionManager` (SharedPreferences).
- Statuts : `PENDING`, `SYNCED`, `ERROR`.

### 10.2 Synchronisation automatique

**Fichiers clés**

- `OSMApplication.kt`
- `utils/NetworkChangeReceiver.kt`

**Fonctionnalités**

- Détection du retour réseau.
- Lancement de `OfflineSyncManager.syncPendingActions(...)`.

### 10.3 Actions réellement synchronisées

À ce stade, la synchronisation traite uniquement les actions de **filtration** :

- `FILTRATION_CREATE`
- `FILTRATION_START`
- `FILTRATION_COMPLETE`
- `FILTRATION_STATUS_UPDATE`

---

## 11. File de synchronisation (UI)

**Fichiers clés**

- `ui/offline/SyncQueueActivity.kt`
- `ui/offline/SyncQueueAdapter.kt`

**Fonctionnalités**

- Affichage des actions en attente.
- Synchronisation forcée manuelle.
- Nettoyage des actions synchronisées/erreurs.

---

## 12. Opérations hors‑ligne (démo)

**Fichier clé**

- `ui/offline/OperationOfflineActivity.kt`

Écran de démonstration pour simuler des opérations hors‑ligne via scan QR.

---

## 13. Réseau & API

**Fichiers clés**

- `network/RetrofitClient.kt`
- `network/ApiService.kt`

**Fonctionnalités**

- Interceptor d’authentification (Bearer token).
- Refresh token automatique en cas de 401.
- Ajout du tenant ID si disponible.

**Principaux endpoints**

- Auth (login, reset password, validate code, update password).
- Filtration (create, start, complete, update status, get all).
- Entités (lots, colis, palettes, OF, storage units).
- Transactions (transactions par unité de stockage).

---

## 14. Permissions Android

Déclarées dans `AndroidManifest.xml` :

- INTERNET
- ACCESS_NETWORK_STATE
- CAMERA

---

## 15. Stockage local

- **Session** : tokens + utilisateur dans `SessionManager`.
- **Historique des scans** : `ScanHistoryManager` (SharedPreferences).
- **Actions hors‑ligne** : `OfflineActionManager` (SharedPreferences).

---

## 16. Extensibilité

### Ajouter un nouveau type d’entité

1. Ajouter le DTO dans `network/models`.
2. Ajouter un `EntityItem` + mapping dans `EntityAdapter`.
3. Ajouter un nouveau `EntityListType`.
4. Ajouter l’endpoint dans `ApiService`.

### Ajouter une action hors‑ligne

1. Définir un type d’action (ex : `MY_NEW_ACTION`).
2. Appeler `OfflineOperationExecutor.runOrQueue(...)`.
3. Ajouter le traitement dans `OfflineSyncManager.processAction`.

---

## 17. Fonctionnement détaillé par module

### 17.1 Authentification & session

1. L’utilisateur saisit ses identifiants dans `LoginActivity`.
2. `LoginViewModel` appelle `ApiService.login` (OAuth2).
3. `RetrofitClient` ajoute l’en‑tête Basic Auth (client id/secret).
4. Les tokens et infos utilisateur sont stockés dans `SessionManager`.
5. L’application redirige vers `MainActivity`.
6. En cas de 401, l’`Authenticator` rafraîchit le token puis rejoue la requête.

### 17.2 Navigation principale

1. `MainActivity` affiche les cartes d’accès rapide.
2. Chaque carte ouvre `EntityListActivity` avec un `EntityListType`.
3. Le menu FAB déclenche le scan QR ou l’historique.

### 17.3 Liste d’entités (écran unique)

1. `EntityListActivity` lit le type d’entité depuis l’Intent.
2. Elle charge la liste via Retrofit (ou via JSON injecté par scan).
3. Les éléments sont convertis en `EntityItem` et rendus par `EntityAdapter`.
4. Un clic ouvre le détail (si unité de stockage) ou affiche un résumé (toast).

### 17.4 Détail d’une unité de stockage

1. L’unité est passée via l’Intent (`StorageUnitDto`).
2. L’écran affiche statut, capacité, coûts et progression.
3. Les transactions sont chargées par API et affichées avec `EntityAdapter`.

### 17.5 Scan QR

1. `QrScannerManager` configure la caméra via Quickie.
2. Le résultat est converti en `ScanUiResult`.
3. `MainActivity` enregistre le scan dans l’historique local.
4. `QrDecoder` déchiffre/parse les QR sécurisés (AES/GCM + PBKDF2).
5. Si le type d’entité est reconnu, navigation vers la liste correspondante.

### 17.6 Historique des scans

1. `ScanHistoryManager` lit/écrit la liste en SharedPreferences.
2. `ScanHistoryViewModel` expose la liste via LiveData.
3. `ScanHistory` affiche la liste, détails et suppression.
4. La synchronisation manuelle marque localement les scans comme “SYNCED”.

### 17.7 Filtration

1. `FiltrationActivity` charge les opérations via l’API.
2. Le dialogue de création envoie une requête (ou met en file hors‑ligne).
3. Le changement de statut passe par `OfflineOperationExecutor`.
4. En mode hors‑ligne, l’action est enregistrée puis synchronisée plus tard.

### 17.8 Mode hors‑ligne & synchronisation

1. `OfflineOperationExecutor` décide “exécuter ou mettre en file”.
2. `OfflineActionManager` persiste l’action avec un statut.
3. `SyncQueueActivity` affiche la file d’attente et permet la sync manuelle.
4. `OfflineSyncManager` traite les actions en PENDING/ERROR.
5. `OSMApplication` déclenche la sync dès que le réseau revient.

### 17.9 Réinitialisation du mot de passe

1. `ResetPasswordActivity` demande un code.
2. `ValidateCodeActivity` valide le code reçu.
3. `NewPasswordActivity` envoie le nouveau mot de passe.

### 17.10 Démo offline (opérations)

1. `OperationOfflineActivity` simule un scan QR.
2. L’opération est marquée SYNCED si réseau, sinon PENDING.
3. Cet écran est une démonstration locale (pas de persistance serveur).

---

## 18. Détails techniques des fonctionnalités

### 18.1 Authentification & session (technique)

- `ApiService.login` utilise un `POST /oauth2/token` avec `grant_type`, `username`, `password`.
- `RetrofitClient` ajoute l’en‑tête **Basic Auth** uniquement pour `oauth2/token`.
- Toutes les autres requêtes portent `Authorization: Bearer <token>` et `X-Tenant-Id` si disponible.
- Le refresh token est géré par un `Authenticator` OkHttp (appel `refreshTokenSync`).
- Les tokens/permissions sont stockés dans `SharedPreferences` via `SessionManager`.

### 18.2 Navigation & Intents

- `EntityListActivity.newIntent(...)` passe :
    - `extra_entity_type`
    - `extra_entity_id` (optionnel)
    - `extra_entity_json` (optionnel)
- Les scans legacy sont routés par préfixe (`OF`, `U`, `C`, `P`, `L`).

### 18.3 Liste d’entités & adapter

- `fetchEntityList` manipule `ApiResponse<T>` : la liste est dans `body.data`.
- `EntityAdapter` convertit chaque élément en `EntityDisplayModel` et applique une couleur de badge
  selon le statut.
- Les unités de stockage peuvent venir d’un JSON injecté par scan :
    - objet simple → `StorageUnitDto`
    - tableau → `List<StorageUnitDto>`

### 18.4 Détail unité de stockage

- L’objet est passé via `Intent` sous la clé `storage_unit` (Serializable).
- Les transactions sont chargées via `GET /api/production/oil_transaction/storage-unit/{id}`.
- La progression est calculée via `currentVolume / maxCapacity`.

### 18.5 Scan QR & décodage sécurisé

- `QrScannerManager` s’appuie sur Quickie (format QR uniquement).
- `QrDecoder` attend un JSON avec :
    - `metadata` (dont `entityType`, `encrypted`)
    - `payload`, et si chiffré : `salt` + `iv`
- Déchiffrement **AES/GCM**, clé dérivée via **PBKDF2WithHmacSHA256** (65536 itérations, 256 bits).
- Le payload déchiffré est re‑parsé pour extraire `ref.primaryId`.

### 18.6 Historique des scans (stockage local)

- `SharedPreferences` : `scan_history`
- Clé JSON : `history_list`
- Insertion en tête de liste (index 0).
- Statuts locaux : `PENDING`, `SYNCED` + drapeau `isSynced`.

### 18.7 Filtration (API + offline)

- Création : `POST /api/production/filtration` avec `FiltrationRequestDto`.
- Statut : `PUT /api/production/filtration/{id}/status`.
- Completion : `PUT /api/production/filtration/{id}/complete`.
- Hors‑ligne : `OfflineOperationExecutor` met en file si réseau absent.

### 18.8 File d’actions hors‑ligne

- Modèle : `OfflineOperation` (id UUID, type, endpoint, donnéeOperation, status, timestamp).
- `OfflineActionManager` stocke la liste dans `SharedPreferences` :
    - PREFS = `osm_offline_actions`
    - KEY = `offline_actions`
- `OfflineSyncManager` ne traite que les actions de filtration.
- Les statuts deviennent `SYNCED` ou `ERROR`.

### 18.9 File de synchronisation (UI)

- `SyncQueueActivity` affiche la liste brute des actions locales.
- “Nettoyer” supprime les éléments `SYNCED` et `ERROR` (reste `PENDING`).

### 18.10 Reset password

- `ResetPasswordActivity` → `POST /api/security/user/auth/resetPassword`.
- `ValidateCodeActivity` → `POST /api/security/user/auth/validateResetCode/{userId}`.
- `NewPasswordActivity` → `POST /api/security/user/auth/updatePassword/{userId}`.

### 18.11 Permissions & manifest

- Permissions déclarées dans `AndroidManifest.xml` : INTERNET, ACCESS_NETWORK_STATE, CAMERA.
- `usesCleartextTraffic=true` pour HTTP local.

### 18.12 Diagrammes de séquence (ASCII)

**Login + création de session**

```
Utilisateur -> LoginActivity : saisit username/password
LoginActivity -> LoginViewModel : login(username, password)
LoginViewModel -> ApiService : POST /oauth2/token
ApiService -> RetrofitClient : ajoute Basic Auth
RetrofitClient -> Backend : OAuth2 token
Backend --> RetrofitClient : AuthResponse
RetrofitClient --> LoginViewModel : AuthResponse
LoginViewModel -> SessionManager : saveAuthTokens + saveUser
LoginViewModel --> LoginActivity : succès
LoginActivity -> MainActivity : navigate
```

**Scan QR + routage vers entité**

```
Utilisateur -> MainActivity : lance scan
MainActivity -> QrScannerManager : buildScannerConfig
QrScannerManager -> Quickie : scan QR
Quickie --> MainActivity : QRResult
MainActivity -> QrDecoder : decode(rawText)
QrDecoder --> MainActivity : entityType + entityId + fullData
MainActivity -> EntityListActivity : newIntent(type, entityId, entity_json)
EntityListActivity -> RetrofitClient : fetch list (ou parse JSON)
RetrofitClient --> EntityListActivity : ApiResponse<T>
EntityListActivity -> EntityAdapter : render list
```

**Création de filtration hors‑ligne + sync**

```
Utilisateur -> FiltrationActivity : crée filtration
FiltrationActivity -> OfflineOperationExecutor : runOrQueue(...)
OfflineOperationExecutor -> NetworkUtils : isNetworkAvailable
alt en ligne
  OfflineOperationExecutor -> ApiService : POST /api/production/filtration
  ApiService --> OfflineOperationExecutor : succès
else hors-ligne
  OfflineOperationExecutor -> OfflineActionManager : saveAction(PENDING)
end

Réseau revient -> OSMApplication : callback réseau
OSMApplication -> OfflineSyncManager : syncPendingActions
OfflineSyncManager -> ApiService : POST/PUT selon type
ApiService --> OfflineSyncManager : succès/échec
OfflineSyncManager -> OfflineActionManager : status=SYNCED ou ERROR
```

### 18.13 Exemples de payloads / réponses

**OAuth2 – réponse token**

```json
{
  "access_token": "eyJhbGciOi...",
  "refresh_token": "dGhpc0lzQ...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "read write",
  "osmUser": {
    "id": "u-123",
    "username": "john.doe",
    "isNewUser": false,
    "role": "Admin",
    "tenantId": "t-001",
    "permissions": ["user:view", "user:edit"]
  }
}
```

**ApiResponse (liste d’entités)**

```json
{
  "data": [
    { "id": "l-001", "lotNumber": "LOT-42", "status": "CREATED" },
    { "id": "l-002", "lotNumber": "LOT-43", "status": "IN_PROGRESS" }
  ]
}
```

**StorageUnitDto**

```json
{
  "id": "su-001",
  "name": "Unite A",
  "location": "Zone 1",
  "description": "Cuve inox",
  "maxCapacity": 12000,
  "currentVolume": 4300,
  "avgCost": 8.120,
  "totalCost": 34800,
  "filteredOil": false,
  "lotNumber": "LOT-42",
  "status": "AVAILABLE"
}
```

**Filtration – création**

```json
{
  "source": "su-001",
  "target": "su-002",
  "volumeToFilter": 2500.0,
  "note": "Filtration batch A"
}
```

**Filtration – mise à jour statut**

```json
{
  "status": "IN_PROGRESS",
  "note": "Démarrée"
}
```

**Filtration – completion**

```json
{
  "volumeAfter": 2400.0,
  "note": "Perte 100L"
}
```

**FiltrationOperation (réponse)**

```json
{
  "operationId": "op-123",
  "source": { "id": "su-001", "name": "Unite A" },
  "target": { "id": "su-002", "name": "Unite B" },
  "volumeFiltered": 2500.0,
  "volumeAfter": 2400.0,
  "lossVolume": 100.0,
  "lossPercent": 4.0,
  "status": "COMPLETED",
  "timestamp": "2026-03-16T10:12:00Z",
  "note": "Perte 100L"
}
```

**Action hors‑ligne (stockée localement)**

```json
{
  "id": "uuid-123",
  "type": "FILTRATION_CREATE",
  "endpoint": "create",
  "donnéeOperation": "{\"source\":\"su-001\",\"target\":\"su-002\",\"volumeToFilter\":2500.0}",
  "status": "PENDING",
  "timestamp": 1710590000000,
  "errorMessage": null
}
```

**QR sécurisé (non chiffré)**

```json
{
  "metadata": { "encrypted": false, "entityType": { "name": "STORAGE_UNIT" }, "version": "1" },
  "payload": "{\"ref\":{\"primaryId\":\"su-001\"}}"
}
```

**QR sécurisé (chiffré)**

```json
{
  "metadata": { "encrypted": true, "entityType": { "name": "LOT" }, "version": "1" },
  "payload": "Base64(ciphertext)",
  "salt": "Base64(salt)",
  "iv": "Base64(iv)"
}
```

**Historique des scans (local)**

```json
[
  {
    "id": 1710590000000,
    "qrCode": "RAW_QR_TEXT",
    "status": "PENDING",
    "contentType": "TEXT",
    "timestamp": 1710590000000,
    "isSynced": false
  }
]
```

---

## 19. Lancer le projet

- Ouvrir `osm-mobile` dans Android Studio.
- Synchroniser Gradle.
- Vérifier `Constants.BASE_URL`.
- Lancer l’app sur un émulateur ou un device.

---

## 20. Tests

Tests unitaires et instrumentation configurés dans `build.gradle.kts`.

---

## 21. Récapitulatif des écrans (UI)

- Login : `activity_login.xml`
- Reset password : `activity_reset_password.xml`, `activity_validate_code.xml`,
  `activity_new_password.xml`
- Accueil : `activity_main.xml`
- Liste entités : `activity_entity_list.xml`
- Détails unité : `activity_storage_unit_details.xml`
- Filtration : `activity_filtration_main.xml`
- Historique scans : `activity_scan_history.xml`
- File synchro : `activity_sync_queue.xml`
- Démo offline : `activity_operation_offline.xml`

---

Fin de documentation.
