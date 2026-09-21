# Expense Tracker Monorepo

A modern, full-stack expense tracking monorepo deployed on Google Cloud Platform and Firebase.

- **Backend**: Java 21, Spring Boot 4.x, Google Cloud Firestore, Firebase Admin SDK, Google Cloud Run.
- **Frontend**: Angular (latest), Angular Material, RxJS, AngularFire, Firebase Hosting.

---

## 🛠️ Local Development

### Prerequisites
- **Java 21+** (JDK)
- **Node.js 20+** & **npm**
- **Google Cloud SDK (`gcloud`)** & **Firebase CLI**

### 1. Run the Spring Boot Backend
Place your Firebase Admin service account key at `backend/src/main/resources/house-expense-tracker-admin.json`, then:

```bash
cd backend
./gradlew bootRun
```
*Backend runs on `http://localhost:8080`.*

### 2. Run the Angular Frontend
```bash
cd frontend
npm install
npm start
```
*Frontend runs on `http://localhost:4200`.*

### 3. Run Automated Tests
```bash
# Backend unit tests
cd backend && ./gradlew test

# Frontend unit tests (headless)
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless
```

---

## ☁️ Google Cloud Deployment (100% Free Tier)

### 1. One-Time GCP Setup & Permissions
Before your first deployment, initialize the required Google Cloud APIs and grant IAM roles to Cloud Build:

```bash
./setup-gcp.sh
```

*(Alternatively, run manually)*:
```bash
# Enable APIs
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com firestore.googleapis.com --project expense-tracker-gcp-481004

# Grant Cloud Build & Storage permissions to default compute service account
PROJECT_NUMBER=$(gcloud projects describe expense-tracker-gcp-481004 --format='value(projectNumber)')
SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

gcloud projects add-iam-policy-binding expense-tracker-gcp-481004 --member="serviceAccount:${SA}" --role="roles/cloudbuild.builds.builder"
gcloud projects add-iam-policy-binding expense-tracker-gcp-481004 --member="serviceAccount:${SA}" --role="roles/storage.admin"
gcloud projects add-iam-policy-binding expense-tracker-gcp-481004 --member="serviceAccount:${SA}" --role="roles/datastore.user"
gcloud projects add-iam-policy-binding expense-tracker-gcp-481004 --member="serviceAccount:${SA}" --role="roles/artifactregistry.writer"
```

---

### 2. Deploy to Cloud Run & Firebase Hosting

Deploy everything with a single command:

```bash
./deploy.sh all
```

Or deploy components individually:
```bash
./deploy.sh backend   # Deploys Spring Boot to Cloud Run in us-central1
./deploy.sh frontend  # Builds Angular and deploys to Firebase Hosting
```

**Live Production URLs**:
- **Web App**: `https://expense-tracker-gcp-481004.web.app`
- **Alternative**: `https://expense-tracker-gcp-481004.firebaseapp.com`

---

### 3. Teardown / Undeploy
To take the app offline and delete Cloud Run compute resources (while preserving your Firestore data):

```bash
./undeploy.sh all
```

---

## 📂 Project Architecture

```
expensetracker-monorepo/
├── backend/                  # Spring Boot 4 REST API
│   ├── src/                  # Controllers, Services, Security, DAOs
│   ├── Dockerfile            # Multi-stage container definition
│   ├── build.gradle          # Gradle dependencies & plugins
│   └── gradlew               # Gradle wrapper
├── frontend/                 # Angular SPA
│   ├── src/                  # Components, Services, Guards, Environments
│   └── angular.json          # Build configurations
├── docs/
│   ├── designs/              # Active design docs
│   └── archive/              # Completed feature designs
├── setup-gcp.sh              # One-time GCP bootstrap script
├── deploy.sh                 # Cloud Run & Firebase Hosting deployment script
├── undeploy.sh               # Teardown script (leaves database safe)
├── firebase.json             # Firebase Hosting SPA routing config
└── AGENTS.md                 # Developer & AI Agent workflow guidelines
```
