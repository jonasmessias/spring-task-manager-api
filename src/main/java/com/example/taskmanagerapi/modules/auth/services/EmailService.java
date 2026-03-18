package com.example.taskmanagerapi.modules.auth.services;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

/**
 * EmailService — sends HTML emails using Thymeleaf templates via AWS SES.
 * Falls back to plain text if no template is specified.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final SesClient sesClient;
    private final TemplateEngine templateEngine;

    @Value("${aws.ses.from}")
    private String from;

    /**
     * Send a plain-text email (backward compatibility).
     */
    public void sendEmail(String to, String subject, String text) {
        SendEmailRequest request = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .text(Content.builder().data(text).charset("UTF-8").build())
                                .build())
                        .build())
                .source(from)
                .build();

        sesClient.sendEmail(request);
    }

    /**
     * Send an HTML email rendered from a Thymeleaf template.
     *
     * @param to           recipient email
     * @param subject      email subject
     * @param templateName template file name (without .html), e.g. "email-verification"
     * @param variables    variables to inject into the template
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        String htmlBody = templateEngine.process(templateName, context);

        SendEmailRequest request = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                .build())
                        .build())
                .source(from)
                .build();

        sesClient.sendEmail(request);
        logger.info("[EMAIL] HTML email sent to {} | template={}", to, templateName);
    }
}

