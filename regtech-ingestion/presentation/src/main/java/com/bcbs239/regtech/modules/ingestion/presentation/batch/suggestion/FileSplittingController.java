package com.bcbs239.regtech.modules.ingestion.presentation.batch.suggestion;

import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.ResponseUtils;
import com.bcbs239.regtech.modules.ingestion.application.files.SuggestFileSplittingCommandHandler;
import com.bcbs239.regtech.modules.ingestion.application.files.SuggestFileSplittingCommand;
import com.bcbs239.regtech.modules.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.modules.ingestion.domain.performance.FileSplittingSuggestion;
import com.bcbs239.regtech.modules.ingestion.presentation.common.IEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Component
public class FileSplittingController extends BaseController implements IEndpoint {

    private static final Logger log = LoggerFactory.getLogger(FileSplittingController.class);

    private final SuggestFileSplittingCommandHandler handler;
    private final Validator validator;

    public FileSplittingController(SuggestFileSplittingCommandHandler handler, Validator validator) {
        this.handler = handler;
        this.validator = validator;
    }

    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return route(POST("/api/v1/ingestion/suggestions/splitting"), this::handle)
            .withAttribute("tags", new String[]{"Ingestion", "Suggestions"})
            .withAttribute("permissions", new String[]{"ingestion:suggestions"});
    }

    private ServerResponse handle(ServerRequest request) {
        try {
            FileMetadataDto dto = request.body(FileMetadataDto.class);

            if (dto == null) {
                ResponseEntity<? extends com.bcbs239.regtech.core.shared.ApiResponse<?>> resp =
                    handleValidationError(java.util.List.of(new com.bcbs239.regtech.core.shared.FieldError("body", "REQUIRED", "Request body is required")), "Invalid request");
                return ServerResponse.status(resp.getStatusCode()).body(resp.getBody());
            }

            var violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            // Extract auth token from Authorization header (Bearer <token>)
            String authHeader = request.headers().firstHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else if (authHeader != null) {
                token = authHeader; // allow raw token header as fallback
            }

            FileMetadata metadata = new FileMetadata(dto.fileName(), dto.contentType(), dto.fileSizeBytes(), dto.md5Checksum(), dto.sha256Checksum());
            SuggestFileSplittingCommand command = new SuggestFileSplittingCommand(metadata, token);
            var result = handler.handle(command);
            if (result.isFailure()) {
                var err = result.getError().orElseThrow();
                ResponseEntity<? extends com.bcbs239.regtech.core.shared.ApiResponse<?>> resp = handleResult(Result.<Object>failure(err), "", "");
                return ServerResponse.status(resp.getStatusCode()).body(resp.getBody());
            }
            FileSplittingSuggestion suggestion = result.getValue().orElseThrow();
            FileSplittingSuggestionDto out = FileSplittingMapper.toDto(suggestion);

            return ServerResponse.ok().body(ResponseUtils.success(out, "Splitting suggestion generated"));
        } catch (ConstraintViolationException e) {
            // Let the global exception handler convert constraint violations into a structured response
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Validation error generating splitting suggestion: {}", e.getMessage());
            ResponseEntity<? extends com.bcbs239.regtech.core.shared.ApiResponse<?>> resp = handleSystemError(e);
            return ServerResponse.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            log.error("Unexpected error generating splitting suggestion", e);
            ResponseEntity<? extends com.bcbs239.regtech.core.shared.ApiResponse<?>> resp = handleSystemError(e);
            assert resp.getBody() != null;
            return ServerResponse.status(resp.getStatusCode()).body(resp.getBody());
        }
    }

    public static record FileMetadataDto(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull @PositiveOrZero long fileSizeBytes,
        @NotBlank String md5Checksum,
        @NotBlank String sha256Checksum
    ) {}
}
