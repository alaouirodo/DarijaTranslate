# DarijaTranslate

Application web JEE pour traduire l'anglais vers le Darija marocain. Le projet contient un backend REST en Java et une extension Chrome.

## Fonctionnalites

- Traduction anglais vers Darija avec l'API Google Gemini
- Affichage en ecriture arabe et en franco-arabe (transliteration latine)
- Synthese vocale (TTS) avec accent marocain naturel
- Saisie vocale (micro) avec reconnaissance de la parole
- Extension Chrome avec menu contextuel (clic droit)
- Bouton flottant "DT" pour traduire rapidement sur n'importe quelle page
- Authentification JWT

## Technologies utilisees

**Backend (JEE) :**
- Java 17
- Jersey 3.1.3 (JAX-RS)
- Google Vertex AI (Gemini 2.0 Flash)
- Google Cloud Speech-to-Text
- JWT (jjwt 0.12.6)
- Servlet API Jakarta 6.0
- Deploiement sur Tomcat (WAR)

**Extension Chrome :**
- Manifest V3
- JavaScript, HTML, CSS

**Client PHP :**
- Client PHP simple pour tester la traduction

## Installation

1. Creer un projet Google Cloud et activer l'API Vertex AI
2. Telecharger le fichier JSON de cle de compte de service
3. Modifier `src/main/webapp/WEB-INF/web.xml` avec le chemin vers votre fichier cle
4. Compiler avec Maven : `mvn clean package`
5. Deployer le fichier WAR sur Tomcat
6. Charger le dossier `chrome-extension/` comme extension non empaquetee dans Chrome

## Configuration

Dans `web.xml` :
- `vertex.key.path` — chemin vers la cle de compte de service Google Cloud
- `app.username` / `app.password` — identifiants de connexion

Ou bien definir la variable d'environnement `VERTEX_KEY_PATH`.

## Structure du projet

```
DarijaTranslator/
├── src/main/java/com/isga/translator/
│   ├── config/          → Configuration JEE (Jersey, CORS)
│   ├── model/           → Classes modeles (Request/Response)
│   ├── resource/        → Endpoints REST (translate, tts, transcribe)
│   ├── security/        → Authentification JWT
│   └── service/         → Services metier (Gemini, TTS, STT)
├── src/main/webapp/WEB-INF/
│   └── web.xml          → Descripteur de deploiement JEE
├── chrome-extension/    → Extension navigateur Chrome
├── php-client/          → Client PHP de test
└── pom.xml              → Configuration Maven
```
