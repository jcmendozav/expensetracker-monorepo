package com.house.expensetracker.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

@TestConfiguration
public class TestFirebaseConfig {

    @Bean
    @Primary
    public FirebaseApp testFirebaseApp() {
        return Mockito.mock(FirebaseApp.class);
    }

    @Bean
    @Primary
    public FirebaseAuth testFirebaseAuth() {
        return Mockito.mock(FirebaseAuth.class);
    }

    @Bean
    @Primary
    public Firestore testFirestore() {
        return Mockito.mock(Firestore.class);
    }
}
