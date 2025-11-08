package com.skillbridge.repository.common;

import com.skillbridge.entity.common.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Email Template Repository
 * Handles database operations for email templates
 */
@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Integer> {

    /**
     * Find email template by template name
     * @param templateName Template name
     * @return Optional EmailTemplate
     */
    Optional<EmailTemplate> findByTemplateName(String templateName);

    /**
     * Find active email template by template name
     * @param templateName Template name
     * @return Optional EmailTemplate
     */
    Optional<EmailTemplate> findByTemplateNameAndIsActiveTrue(String templateName);
}

