package com.house.expensetracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.house.expensetracker.config.TestFirebaseConfig;

@SpringBootTest
@Import(TestFirebaseConfig.class)
class ExpensetrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
