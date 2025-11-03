package com.bcbs239.regtech.billing.infrastructure.configuration;

import java.util.Map;

/**
 * Type-safe configuration for notification settings.
 * Defines email, SMS, and push notification configurations.
 */
public record NotificationsConfiguration(
    EmailNotification email,
    SmsNotification sms,
    PushNotification push
) {
    
    /**
     * Email notification configuration
     */
    public record EmailNotification(
        boolean enabled,
        Map<String, String> templates
    ) {
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public Map<String, String> getTemplates() {
            return templates;
        }
        
        public String getTemplate(String templateName) {
            return templates.get(templateName);
        }
        
        public String getFirstReminderTemplate() {
            return templates.get("first-reminder");
        }
        
        public String getSecondReminderTemplate() {
            return templates.get("second-reminder");
        }
        
        public String getFinalNoticeTemplate() {
            return templates.get("final-notice");
        }
        
        public String getSuspensionNoticeTemplate() {
            return templates.get("suspension-notice");
        }
        
        public void validate() {
            if (enabled) {
                if (templates == null || templates.isEmpty()) {
                    throw new IllegalStateException("Email templates are required when email notifications are enabled");
                }
                
                // Check for required templates
                String[] requiredTemplates = {
                    "first-reminder", "second-reminder", "final-notice", "suspension-notice"
                };
                
                for (String template : requiredTemplates) {
                    if (!templates.containsKey(template) || templates.get(template) == null || templates.get(template).isBlank()) {
                        throw new IllegalStateException("Required email template missing: " + template);
                    }
                }
            }
        }
    }
    
    /**
     * SMS notification configuration
     */
    public record SmsNotification(
        boolean enabled
    ) {
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void validate() {
            // SMS validation can be added here when SMS functionality is implemented
        }
    }
    
    /**
     * Push notification configuration
     */
    public record PushNotification(
        boolean enabled
    ) {
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void validate() {
            // Push notification validation can be added here when push functionality is implemented
        }
    }
    
    /**
     * Gets email notification configuration
     */
    public EmailNotification getEmail() {
        return email;
    }
    
    /**
     * Gets SMS notification configuration
     */
    public SmsNotification getSms() {
        return sms;
    }
    
    /**
     * Gets push notification configuration
     */
    public PushNotification getPush() {
        return push;
    }
    
    /**
     * Validates notifications configuration
     */
    public void validate() {
        if (email != null) {
            email.validate();
        }
        if (sms != null) {
            sms.validate();
        }
        if (push != null) {
            push.validate();
        }
    }
}
