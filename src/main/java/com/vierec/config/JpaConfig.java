package com.vierec.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Enables {@code @CreatedDate} / {@code @LastModifiedDate}. The tables have no created_by / updated_by
 * columns, so no {@code AuditorAware} is needed.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
public class JpaConfig {
}
