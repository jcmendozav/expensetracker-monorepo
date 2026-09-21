package com.house.expensetracker.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.house.expensetracker.dao.Transaction;
import com.house.expensetracker.dto.TransactionRequest;

@Service
public class TransactionService {

    private static final String COLLECTION_NAME = "transactions";

    // Inject the Firestore bean configured in FirebaseConfig
    private final Firestore firestore;

    @Autowired
    public TransactionService(Firestore firestore) {
        this.firestore = firestore;
    }

    private CollectionReference getCollection() {
        return firestore.collection(COLLECTION_NAME);
    }

    /**
     * Saves a new transaction document to Firestore.
     *
     * @param transaction The Transaction object to save.
     * @return The ID of the newly created document.
     */
    public String saveTransaction(Transaction transaction) throws ExecutionException, InterruptedException {
        // Use the add() method which generates an automatic Document ID
        ApiFuture<DocumentReference> future = getCollection().add(transaction);

        // Block and return the ID of the new document
        return future.get().getId();
    }

    /**
     * Retrieves a single transaction by its Firestore Document ID.
     *
     * @param documentId The unique Firestore document ID.
     * @return The Transaction object, or null if not found.
     */
    public Transaction getTransaction(String documentId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = getCollection().document(documentId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            // Firestore converts the document data directly into your POJO
            return document.toObject(Transaction.class);
        }
        return null;
    }

    /**
     * Retrieves all transactions for a specific household. NOTE: This is NOT
     * the optimized cash flow query yet.
     *
     * @param householdId The ID of the household to filter by.
     * @return A list of Transaction objects.
     */
    public List<Transaction> getTransactionsByHousehold(Integer householdId)
            throws ExecutionException, InterruptedException {

        ApiFuture<QuerySnapshot> future = getCollection()
                .whereEqualTo("householdId", householdId)
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        // Stream and map the DocumentSnapshots to Transaction POJOs
        return documents.stream()
                .map(document -> document.toObject(Transaction.class))
                .collect(Collectors.toList());
    }
// Inside TransactionService.java (Modification)

// You will need a CurrencyConversionService dependency here later!
// private final CurrencyConversionService conversionService; 
    public String saveTransactionFromRequest(TransactionRequest request, String userId)
            throws ExecutionException, InterruptedException {

        // **Future Step:** Call conversionService to calculate the final baseAmount here.
        Double calculatedBaseAmount = request.getOriginalAmount(); // Placeholder for now

        // 1. Map DTO to Entity (Transaction POJO)
        Transaction transaction = Transaction.builder()
                .householdId(request.getHouseholdId().longValue())
                .categoryId(request.getCategoryId().longValue())
                .originalAmount(request.getOriginalAmount())
                .originalCurrency(request.getOriginalCurrency())
                .baseAmount(calculatedBaseAmount)
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate() != null
                        ? Timestamp.of(new Date(request.getTransactionDate()))
                        : Timestamp.now())
                .userId(userId)
                // ... map other fields
                .build();

        // 2. Save the fully populated entity to Firestore
        return this.saveTransaction(transaction); // Calls the method defined previously
    }

    public List<Transaction> getTransactionsByUserId(String userId) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("userId", userId);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        return querySnapshot.get().getDocuments().stream()
                .map(doc -> {
                    Transaction t = doc.toObject(Transaction.class);
                    t.setId(doc.getId());
                    return t;
                })
                .collect(Collectors.toList());
    }

    public void deleteTransaction(String documentId, String currentUserId) throws Exception {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(documentId);

        System.out.println("Attempting to delete doc: " + documentId + " for user: " + currentUserId);

        DocumentSnapshot document = docRef.get().get();

        if (!document.exists()) {
            System.out.println("❌ Document not found in Firestore");
            throw new RuntimeException("Transaction not found");
        }

        String ownerId = document.getString("userId");
        System.out.println("Document owner in Firestore: " + ownerId);

        if (ownerId != null && ownerId.equals(currentUserId)) {
            docRef.delete().get();
            System.out.println("✅ Delete successful");
        } else {
            System.out.println("🚫 Ownership mismatch!");
            throw new RuntimeException("Unauthorized delete attempt");
        }
    }

    public Double getTotalSpent(String userId) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get();

        List<QueryDocumentSnapshot> documents = query.get().getDocuments();

        return documents.stream()
                .mapToDouble(doc -> {
                    Double amount = doc.getDouble("originalAmount");
                    return amount != null ? amount : 0.0;
                })
                .sum();
    }

    public void updateTransaction(String id, Transaction updatedTransaction, String currentUserId) throws Exception {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);

        DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            String ownerId = document.getString("userId");

            if (ownerId != null && ownerId.equals(currentUserId)) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("description", updatedTransaction.getDescription());
                updates.put("originalAmount", updatedTransaction.getOriginalAmount());
                updates.put("originalCurrency", updatedTransaction.getOriginalCurrency());
                updates.put("transactionDate", updatedTransaction.getTransactionDate());

                docRef.update(updates).get();
            } else {
                throw new RuntimeException("Unauthorized: You do not own this transaction.");
            }
        } else {
            throw new RuntimeException("Transaction not found");
        }
    }
}