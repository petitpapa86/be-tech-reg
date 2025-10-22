package com.bcbs239.regtech.iam.domain.users.events;

import com.bcbs239.regtech.core.application.IntegrationEvent;

import java.util.UUID;

public class UserRegisteredIntegrationEvent extends IntegrationEvent {

    private  UUID userId;
    private  String email;
    private  String firstName;
    private  String lastName;
    private  String bankId;

   // public UserRegisteredIntegrationEvent(){}

    public UserRegisteredIntegrationEvent(UUID userId, String email, String firstName, String lastName, String bankId) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bankId = bankId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getBankId() {
        return bankId;
    }
}