#!/usr/bin/env bash
set -e

# Automatically include gcloud in PATH if installed in user directory
export PATH="$HOME/google-cloud-sdk/bin:$PATH"

PROJECT_ID="expense-tracker-gcp-481004"
REGION="us-central1"
SERVICE_NAME="expensetracker-backend"

TARGET="${1:-all}"

echo "========================================================"
echo " Expense Tracker - Deployment to GCP & Firebase"
echo " Project: ${PROJECT_ID}"
echo " Region:  ${REGION}"
echo " Target:  ${TARGET}"
echo "========================================================"

deploy_backend() {
    echo ""
    echo "📦 [1/2] Deploying Spring Boot Backend to Google Cloud Run..."
    
    # Deploy source directly to Cloud Run using Google Cloud Build
    gcloud run deploy "${SERVICE_NAME}" \
        --source backend \
        --region "${REGION}" \
        --project "${PROJECT_ID}" \
        --set-env-vars GOOGLE_CLOUD_PROJECT="${PROJECT_ID}" \
        --allow-unauthenticated \
        --memory 512Mi \
        --cpu 1 \
        --min-instances 0 \
        --max-instances 2 \
        --quiet

    # Fetch deployed URL
    BACKEND_URL=$(gcloud run services describe "${SERVICE_NAME}" \
        --region "${REGION}" \
        --project "${PROJECT_ID}" \
        --format='value(status.url)')

    echo "✅ Backend successfully deployed at: ${BACKEND_URL}"
    echo "Updating frontend/src/environments/environment.prod.ts with API URL: ${BACKEND_URL}/api"
    
    # Update environment.prod.ts with the live backend URL
    sed -i '' -e "s|apiUrl: '.*'|apiUrl: '${BACKEND_URL}/api'|g" frontend/src/environments/environment.prod.ts 2>/dev/null || \
    sed -i -e "s|apiUrl: '.*'|apiUrl: '${BACKEND_URL}/api'|g" frontend/src/environments/environment.prod.ts
}

deploy_frontend() {
    echo ""
    echo "🌐 [2/2] Building and Deploying Angular Frontend to Firebase Hosting..."
    
    cd frontend
    echo "Building Angular production bundle..."
    npm run build -- --configuration production
    cd ..

    echo "Deploying static assets to Firebase Hosting..."
    npx -y firebase-tools deploy --only hosting --project "${PROJECT_ID}"

    echo ""
    echo "🎉 Frontend is LIVE at: https://${PROJECT_ID}.web.app"
    echo "   Alternative URL:     https://${PROJECT_ID}.firebaseapp.com"
}

case "${TARGET}" in
    backend)
        deploy_backend
        ;;
    frontend)
        deploy_frontend
        ;;
    all)
        deploy_backend
        deploy_frontend
        ;;
    *)
        echo "Usage: ./deploy.sh [backend|frontend|all]"
        exit 1
        ;;
esac

echo ""
echo "========================================================"
echo " ✨ Deployment Complete! Share with your friends in Peru!"
echo " Web App URL: https://${PROJECT_ID}.web.app"
echo "========================================================"
