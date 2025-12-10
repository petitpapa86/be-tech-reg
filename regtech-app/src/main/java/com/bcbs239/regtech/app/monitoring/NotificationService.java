package com.bcbs239.regtech.app.monitoring;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notification service for delivering alerts through multiple channels.
 * Supports email, Slack, and webhook notifications with retry logic and failure handling.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 * - Add support for email, Slack, and webhook notifications
 * - Implement notification delivery retry logic and failure handling
 * - Add notification template system for different alert types
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);
    
    private final SendGrid sendGrid;
    private final RestTemplate restTemplate;
    
    @Value("${observability.notifications.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${observability.notifications.email.from:alerts@bcbs239.com}")
    private String emailFrom;
    
    @Value("${observability.notifications.email.to:}")
    private String emailTo;
    
    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;
    
    @Value("${observability.notifications.slack.enabled:false}")
    private boolean slackEnabled;
    
    @Value("${observability.notifications.slack.webhook-url:}")
    private String slackWebhookUrl;
    
    @Value("${observability.notifications.webhook.enabled:false}")
    private boolean webhookEnabled;
    
    @Value("${observability.notifications.webhook.url:}")
    private String webhookUrl;
    
    // Track notification failures for monitoring
    private final Map<String, NotificationFailure> recentFailures = new ConcurrentHashMap<>();
    
    public NotificationService(
            RestTemplate restTemplate) {
        this.sendGrid = new SendGrid(sendGridApiKey);
        this.restTemplate = restTemplate;
        
        logger.info("NotificationService initialized (email: {}, slack: {}, webhook: {})",
            emailEnabled, slackEnabled, webhookEnabled);
    }
    
    /**
     * Sends an alert through all configured notification channels.
     */
    public void sendAlert(AlertingService.Alert alert) {
        logger.info("Sending alert notification: {} (severity: {})", 
            alert.getRuleName(), alert.getSeverity());
        
        List<NotificationChannel> channels = getEnabledChannels();
        
        if (channels.isEmpty()) {
            logger.warn("No notification channels enabled, alert will not be sent: {}", 
                alert.getRuleName());
            return;
        }
        
        for (NotificationChannel channel : channels) {
            try {
                sendWithRetry(channel, alert);
            } catch (Exception e) {
                logger.error("Failed to send alert via {}: {}", 
                    channel.getChannelType(), alert.getRuleName(), e);
                recordFailure(channel.getChannelType(), alert, e);
            }
        }
    }
    
    /**
     * Sends a notification with retry logic.
     */
    private void sendWithRetry(NotificationChannel channel, AlertingService.Alert alert) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            attempts++;
            
            try {
                channel.sendAlert(alert);
                logger.debug("Alert sent successfully via {} (attempt {})", 
                    channel.getChannelType(), attempts);
                return;
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("Failed to send alert via {} (attempt {}/{}): {}", 
                    channel.getChannelType(), attempts, MAX_RETRY_ATTEMPTS, e.getMessage());
                
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry delay", ie);
                    }
                }
            }
        }
        
        // All retries failed
        throw new RuntimeException(
            String.format("Failed to send alert after %d attempts", MAX_RETRY_ATTEMPTS),
            lastException
        );
    }
    
    /**
     * Gets all enabled notification channels.
     */
    private List<NotificationChannel> getEnabledChannels() {
        List<NotificationChannel> channels = new ArrayList<>();
        
        if (emailEnabled && sendGrid != null && !emailTo.isEmpty() && !sendGridApiKey.isEmpty()) {
            channels.add(new EmailNotificationChannel());
        }
        
        if (slackEnabled && !slackWebhookUrl.isEmpty()) {
            channels.add(new SlackNotificationChannel());
        }
        
        if (webhookEnabled && !webhookUrl.isEmpty()) {
            channels.add(new WebhookNotificationChannel());
        }
        
        return channels;
    }
    
    /**
     * Records a notification failure for monitoring.
     */
    private void recordFailure(String channelType, AlertingService.Alert alert, Exception error) {
        String key = channelType + "_" + alert.getRuleId();
        recentFailures.put(key, new NotificationFailure(
            channelType,
            alert.getRuleName(),
            error.getMessage(),
            Instant.now()
        ));
        
        // Clean up old failures (keep last 100)
        if (recentFailures.size() > 100) {
            recentFailures.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(
                    Comparator.comparing(NotificationFailure::getTimestamp)))
                .limit(recentFailures.size() - 100)
                .map(Map.Entry::getKey)
                .forEach(recentFailures::remove);
        }
    }
    
    /**
     * Gets recent notification failures for monitoring.
     */
    public Collection<NotificationFailure> getRecentFailures() {
        return Collections.unmodifiableCollection(recentFailures.values());
    }
    
    /**
     * Gets notification statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("emailEnabled", emailEnabled);
        stats.put("sendGridConfigured", sendGrid != null && !sendGridApiKey.isEmpty());
        stats.put("slackEnabled", slackEnabled);
        stats.put("webhookEnabled", webhookEnabled);
        stats.put("recentFailures", recentFailures.size());
        return stats;
    }
    
    /**
     * Email notification channel implementation using SendGrid.
     */
    private class EmailNotificationChannel implements NotificationChannel {
        
        @Override
        public void sendAlert(AlertingService.Alert alert) {
            if (sendGrid == null || sendGridApiKey.isEmpty()) {
                throw new IllegalStateException("SendGrid not configured");
            }
            
            try {
                Email from = new Email(emailFrom);
                Email to = new Email(emailTo);
                Content content = new Content("text/plain", formatEmailBody(alert));
                Mail mail = new Mail(from, formatEmailSubject(alert), to, content);
                
                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());
                
                Response response = sendGrid.api(request);
                
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    logger.info("Email notification sent for alert: {}", alert.getRuleName());
                } else {
                    throw new RuntimeException("SendGrid API error: " + response.getStatusCode() + " - " + response.getBody());
                }
                
            } catch (IOException e) {
                throw new RuntimeException("Failed to send email via SendGrid", e);
            }
        }
        
        @Override
        public boolean isAvailable() {
            return emailEnabled && sendGrid != null && !emailTo.isEmpty() && !sendGridApiKey.isEmpty();
        }
        
        @Override
        public String getChannelType() {
            return "EMAIL";
        }
        
        private String formatEmailSubject(AlertingService.Alert alert) {
            return String.format("[%s] %s", alert.getSeverity(), alert.getRuleName());
        }
        
        private String formatEmailBody(AlertingService.Alert alert) {
            StringBuilder body = new StringBuilder();
            body.append("Alert Details:\n\n");
            body.append("Rule: ").append(alert.getRuleName()).append("\n");
            body.append("Severity: ").append(alert.getSeverity()).append("\n");
            body.append("Description: ").append(alert.getDescription()).append("\n");
            body.append("Timestamp: ").append(alert.getTimestamp()).append("\n\n");
            
            body.append("Metrics:\n");
            alert.getMetrics().forEach((key, value) -> 
                body.append("  ").append(key).append(": ").append(value).append("\n")
            );
            
            body.append("\n--\n");
            body.append("BCBS239 RegTech Platform Monitoring\n");
            
            return body.toString();
        }
    }
    
    /**
     * Slack notification channel implementation.
     */
    private class SlackNotificationChannel implements NotificationChannel {
        
        @Override
        public void sendAlert(AlertingService.Alert alert) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", formatSlackMessage(alert));
            payload.put("attachments", List.of(formatSlackAttachment(alert)));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(slackWebhookUrl, request, String.class);
            logger.info("Slack notification sent for alert: {}", alert.getRuleName());
        }
        
        @Override
        public boolean isAvailable() {
            return slackEnabled && !slackWebhookUrl.isEmpty();
        }
        
        @Override
        public String getChannelType() {
            return "SLACK";
        }
        
        private String formatSlackMessage(AlertingService.Alert alert) {
            return String.format("*%s Alert:* %s", alert.getSeverity(), alert.getRuleName());
        }
        
        private Map<String, Object> formatSlackAttachment(AlertingService.Alert alert) {
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", getColorForSeverity(alert.getSeverity()));
            attachment.put("title", alert.getRuleName());
            attachment.put("text", alert.getDescription());
            attachment.put("ts", alert.getTimestamp().getEpochSecond());
            
            List<Map<String, Object>> fields = new ArrayList<>();
            alert.getMetrics().forEach((key, value) -> {
                Map<String, Object> field = new HashMap<>();
                field.put("title", key);
                field.put("value", String.valueOf(value));
                field.put("short", true);
                fields.add(field);
            });
            attachment.put("fields", fields);
            
            return attachment;
        }
        
        private String getColorForSeverity(AlertingService.AlertSeverity severity) {
            return switch (severity) {
                case CRITICAL -> "danger";
                case WARNING -> "warning";
                case INFO -> "good";
            };
        }
    }
    
    /**
     * Generic webhook notification channel implementation.
     */
    private class WebhookNotificationChannel implements NotificationChannel {
        
        @Override
        public void sendAlert(AlertingService.Alert alert) {
            Map<String, Object> payload = alert.toMap();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(webhookUrl, request, String.class);
            logger.info("Webhook notification sent for alert: {}", alert.getRuleName());
        }
        
        @Override
        public boolean isAvailable() {
            return webhookEnabled && !webhookUrl.isEmpty();
        }
        
        @Override
        public String getChannelType() {
            return "WEBHOOK";
        }
    }
    
    /**
     * Interface for notification channels.
     */
    public interface NotificationChannel {
        void sendAlert(AlertingService.Alert alert);
        boolean isAvailable();
        String getChannelType();
    }
    
    /**
     * Represents a notification failure for monitoring.
     */
    public static class NotificationFailure {
        private final String channelType;
        private final String alertName;
        private final String errorMessage;
        private final Instant timestamp;
        
        public NotificationFailure(String channelType, String alertName, 
                                  String errorMessage, Instant timestamp) {
            this.channelType = channelType;
            this.alertName = alertName;
            this.errorMessage = errorMessage;
            this.timestamp = timestamp;
        }
        
        public String getChannelType() { return channelType; }
        public String getAlertName() { return alertName; }
        public String getErrorMessage() { return errorMessage; }
        public Instant getTimestamp() { return timestamp; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("channelType", channelType);
            map.put("alertName", alertName);
            map.put("errorMessage", errorMessage);
            map.put("timestamp", timestamp.toString());
            return map;
        }
    }
}
