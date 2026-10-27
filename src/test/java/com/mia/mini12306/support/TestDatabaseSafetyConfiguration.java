package com.mia.mini12306.support;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.sql.SQLException;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseSafetyConfiguration {

    private static final String TEST_DATABASE = "mini_12306_test";
    private static final String TEST_USER = "mini12306_test";

    private static final String TEST_URL =
            "jdbc:mysql://localhost:3307/mini_12306_test"
                    + "?useSSL=false&serverTimezone=Asia/Shanghai"
                    + "&characterEncoding=UTF-8"
                    + "&allowPublicKeyRetrieval=true";

    @Bean
    static BeanFactoryPostProcessor verifyDatabaseSettings(
            Environment environment
    ) {
        return beanFactory -> {
            // Check effective settings before creating the DataSource.
            require(TEST_URL.equals(environment.getProperty("spring.datasource.url")));
            require(TEST_USER.equals(environment.getProperty("spring.datasource.username")));
            require(environment.getProperty("spring.flyway.enabled", Boolean.class, false));
            require(!environment.getProperty("spring.flyway.baseline-on-migrate", Boolean.class, false));
            require(environment.getProperty("spring.flyway.clean-disabled", Boolean.class, true));

            // Flyway must see the application's DataSource and default schema.
            for(String property : new String[] {
                    "spring.flyway.url",
                    "spring.flyway.user",
                    "spring.flyway.password",
                    "spring.flyway.schemas",
                    "spring.flyway.default-schema"
            }) {
                require(environment.getProperty(property) == null);
            }
        };
    }

    @Bean
    FlywayMigrationStrategy guardedTestMigration() {
        return flyway -> {
            // Verify the actual connection before executing any migration.
            try(var connection = flyway.getConfiguration().getDataSource().getConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("""
                    SELECT DATABASE(), SUBSTRING_INDEX(CURRENT_USER(), '@', 1)
                    """)) {

                require(result.next());
                require(TEST_DATABASE.equals(result.getString(1)));
                require(TEST_USER.equals(result.getString(2)));
            } catch(SQLException exception) {
                throw new IllegalStateException(
                        "Cannot verify the test database connection",
                        exception
                );
            }

            flyway.migrate();
        };
    }

    private static void require(boolean condition) {
        if(!condition) {
            throw new IllegalStateException(
                    "Unsafe test database configuration; startup blocked"
            );
        }
    }
}
