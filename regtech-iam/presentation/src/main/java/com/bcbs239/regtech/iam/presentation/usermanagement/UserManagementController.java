package com.bcbs239.regtech.iam.presentation.usermanagement;

import com.bcbs239.regtech.iam.application.usermanagement.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.*;

/**
 * User Management Routes
 * Uses EXISTING User domain model
 */
@Configuration
@RequiredArgsConstructor
public class UserManagementController {
    
    private final GetUsersByBankHandler getUsersByBankHandler;
    private final AddNewUserHandler addNewUserHandler;
    private final InviteUserHandler inviteUserHandler;
    private final UpdateUserRoleHandler updateUserRoleHandler;
    private final SuspendUserHandler suspendUserHandler;
    private final RevokeInvitationHandler revokeInvitationHandler;
    
    @Bean
    public RouterFunction<ServerResponse> userManagementRoutes() {
        return route()
            .GET("/api/banks/{bankId}/users", accept(MediaType.APPLICATION_JSON), this::getUsers)
            .POST("/api/banks/{bankId}/users", accept(MediaType.APPLICATION_JSON), this::addUser)
            .POST("/api/banks/{bankId}/users/invite", accept(MediaType.APPLICATION_JSON), this::inviteUser)
            .PUT("/api/users/{userId}/role", accept(MediaType.APPLICATION_JSON), this::updateUserRole)
            .PUT("/api/users/{userId}/suspend", accept(MediaType.APPLICATION_JSON), this::suspendUser)
            .DELETE("/api/users/{userId}/invitation", this::revokeInvitation)
            .build();
    }
    
    private ServerResponse getUsers(ServerRequest request) {
        try {
            Long bankId = Long.parseLong(request.pathVariable("bankId"));
            String filter = request.param("filter").orElse("all");
            
            var query = new GetUsersByBankHandler.Query(bankId, filter);
            var result = getUsersByBankHandler.handle(query);
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", result.getError().get()));
            }
            
            List<UserResponse> userResponses = result.getValue().get().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userResponses);
                
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "Invalid bank ID"));
        }
    }
    
    private ServerResponse addUser(ServerRequest request) {
        try {
            Long bankId = Long.parseLong(request.pathVariable("bankId"));
            var addRequest = request.body(AddUserRequest.class);
            
            // Get createdBy from authentication context (placeholder)
            String createdBy = "admin"; // TODO: Get from SecurityContext
            
            var command = new AddNewUserHandler.Command(
                bankId,
                addRequest.email(),
                addRequest.firstName(),
                addRequest.lastName(),
                addRequest.password(),
                addRequest.role(),
                createdBy
            );
            
            var result = addNewUserHandler.handle(command);
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", result.getError().get()));
            }
            
            UserResponse userResponse = UserResponse.from(result.getValue().get());
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userResponse);
                
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    private ServerResponse inviteUser(ServerRequest request) {
        try {
            Long bankId = Long.parseLong(request.pathVariable("bankId"));
            var inviteRequest = request.body(InviteUserRequest.class);
            
            // Get invitedBy from authentication context (placeholder)
            String invitedBy = "admin"; // TODO: Get from SecurityContext
            
            var command = new InviteUserHandler.Command(
                bankId,
                inviteRequest.email(),
                inviteRequest.firstName(),
                inviteRequest.lastName(),
                invitedBy
            );
            
            var result = inviteUserHandler.handle(command);
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", result.getError().get()));
            }
            
            UserResponse userResponse = UserResponse.from(result.getValue().get());
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userResponse);
                
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    private ServerResponse updateUserRole(ServerRequest request) {
        try {
            String userId = request.pathVariable("userId");
            var body = request.body(Map.class);
            
            String newRoleName = (String) body.get("roleName");
            String organizationId = (String) body.get("organizationId");
            String modifiedBy = "admin"; // TODO: Get from SecurityContext
            
            var command = new UpdateUserRoleHandler.Command(userId, newRoleName, organizationId, modifiedBy);
            var result = updateUserRoleHandler.handle(command);
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", result.getError().get()));
            }
            
            return ServerResponse.noContent().build();
            
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    private ServerResponse suspendUser(ServerRequest request) {
        try {
            String userId = request.pathVariable("userId");
            String suspendedBy = "admin"; // TODO: Get from SecurityContext
            
            var command = new SuspendUserHandler.Command(userId, suspendedBy);
            var result = suspendUserHandler.handle(command);
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", result.getError().get()));
            }
            
            UserResponse userResponse = UserResponse.from(result.getValue().get());
            
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userResponse);
                
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    private ServerResponse revokeInvitation(ServerRequest request) {
        try {
            String userId = request.pathVariable("userId");
            
            var command = new RevokeInvitationHandler.Command(userId);
            var result = revokeInvitationHandler.handle(command);
            
            if (result.isFailure()) {
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", result.getError().get()));
            }
            
            return ServerResponse.noContent().build();
            
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
}
