package com.house.expensetracker.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

@Configuration
public class FirebaseConfig {

    // 1. Inject the file path from application.properties
    @Value("${firebase.service-account-path}")
    private Resource serviceAccount;

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {

        // 2. Load the credentials using the injected Resource
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                .build();

        // 3. Initialize the app globally (safe to call multiple times, but configured once)
        return FirebaseApp.initializeApp(options);
    }

    // 4. Expose FirebaseAuth as a Bean for security filters
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    // 5. Expose Firestore as a Bean for database operations
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        // FirestoreClient.getFirestore() returns the Firestore instance linked to the app
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
