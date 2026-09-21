#!/usr/bin/env bash
set -e

# Automatically include gcloud in PATH if installed in user directory
export PATH="$HOME/google-cloud-sdk/bin:$PATH"

PROJECT_ID="expense-tracker-gcp-481004"
REGION="us-central1"
SERVICE_NAME="expensetracker-backend"

TARGET="${1:-all}"

echo "========================================================"
echo " ⚠️  Expense Tracker - Teardown / Undeploy"
echo " Project: ${PROJECT_ID}"
echo " Target:  ${TARGET}"
echo "========================================================"
echo "Note: This will remove compute/hosting endpoints. Your Firestore data is NOT deleted."
read -p "Are you sure you want to proceed? (y/N): " confirm

if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Teardown aborted."
    exit 0
fi

undeploy_backend() {
    echo ""
    echo "🗑️  [1/2] Deleting Cloud Run service '${SERVICE_NAME}'..."
    gcloud run services delete "${SERVICE_NAME}" \
        --region "${REGION}" \
        --project "${PROJECT_ID}" \
        --quiet || echo "Backend service already deleted or not found."
    echo "✅ Backend service successfully removed."
}

undeploy_frontend() {
    echo ""
    echo "🚫 [2/2] Disabling Firebase Hosting site..."
    npx -y firebase-tools hosting:disable --project "${PROJECT_ID}" --force || echo "Hosting already disabled."
    echo "✅ Firebase Hosting site is now offline."
}

case "${TARGET}" in
    backend)
        undeploy_backend
        ;;
    frontend)
        undeploy_frontend
        ;;
    all)
        undeploy_backend
        undeploy_frontend
        ;;
    *)
        echo "Usage: ./undeploy.sh [backend|frontend|all]"
        exit 1
        ;;
esac

echo ""
echo "========================================================"
echo " ✅ Undeploy completed successfully."
echo "========================================================"
