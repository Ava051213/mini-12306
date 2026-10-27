package com.mia.mini12306.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabaseSafetyConfiguration.class)
public abstract class DatabaseIntegrationTest {
}
