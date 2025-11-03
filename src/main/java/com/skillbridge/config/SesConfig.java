package com.skillbridge.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS SES Configuration
 * Configures AWS Simple Email Service for sending emails
 */
@Configuration
public class SesConfig {

    @Value("${aws.ses.region:us-east-1}")
    private String region;

    @Value("${aws.ses.access-key:}")
    private String accessKey;

    @Value("${aws.ses.secret-key:}")
    private String secretKey;

    @Bean
    public AmazonSimpleEmailService amazonSES() {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            // Return null if credentials are not configured
            // This allows the application to run without SES configured
            return null;
        }

        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonSimpleEmailServiceClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(region)
                .build();
    }
}

