#!/usr/bin/env bash
set -e

# Automatically include gcloud in PATH if installed in user directory
export PATH="$HOME/google-cloud-sdk/bin:$PATH"

PROJECT_ID="expense-tracker-gcp-481004"

echo "=========================================================="
echo " 🔧 Initializing GCP Infrastructure & IAM Permissions"
echo " Project ID: ${PROJECT_ID}"
echo "=========================================================="

# 1. Verify gcloud authentication
echo "Checking gcloud authentication..."
gcloud config set project "${PROJECT_ID}" --quiet

# 2. Retrieve Project Number & Default Compute Service Account
PROJECT_NUMBER=$(gcloud projects describe "${PROJECT_ID}" --format='value(projectNumber)')
SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

echo "Project Number:  ${PROJECT_NUMBER}"
echo "Service Account: ${SA}"
echo ""

# 3. Enable Required Google Cloud APIs
echo "Enabling GCP Services (Cloud Run, Cloud Build, Artifact Registry, Firestore)..."
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  firestore.googleapis.com \
  --project "${PROJECT_ID}"

# 4. Grant IAM Roles to Default Compute Service Account
echo ""
echo "Granting required IAM roles to ${SA}..."
ROLES=(
  "roles/cloudbuild.builds.builder"
  "roles/storage.admin"
  "roles/datastore.user"
  "roles/artifactregistry.writer"
)

for ROLE in "${ROLES[@]}"; do
  echo "  -> Assigning ${ROLE}..."
  gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${SA}" \
    --role="${ROLE}" \
    --quiet > /dev/null
done

echo ""
echo "=========================================================="
echo " ✅ GCP Setup Complete! You can now deploy with ./deploy.sh"
echo "=========================================================="
