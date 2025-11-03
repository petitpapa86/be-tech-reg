package com.bcbs239.regtech.modules.ingestion.presentation.batch.suggestion;

import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.ingestion.application.files.SuggestFileSplittingCommandHandler;
import com.bcbs239.regtech.modules.ingestion.application.files.SuggestFileSplittingCommand;
import com.bcbs239.regtech.modules.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.modules.ingestion.domain.performance.FileSplittingSuggestion;
import com.bcbs239.regtech.modules.ingestion.presentation.common.IEndpoint;
import jakarta.servlet.ServletException;
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

import java.io.IOException;

import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Component
public class FileSplittingController extends BaseController implements IEndpoint {

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

    private ServerResponse handle(ServerRequest request) throws ServletException, IOException {
        FileMetadataDto dto = request.body(FileMetadataDto.class);

        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        FileMetadata metadata = new FileMetadata(dto.fileName(), dto.contentType(), dto.fileSizeBytes(), dto.md5Checksum(), dto.sha256Checksum());
        SuggestFileSplittingCommand command = new SuggestFileSplittingCommand(metadata, null);
        Result<FileSplittingSuggestion> result = handler.handle(command);

        if (result.isSuccess()) {
            FileSplittingSuggestion suggestion = result.getValue().orElseThrow();
            FileSplittingSuggestionDto out = FileSplittingMapper.toDto(suggestion);
            // Build a Result for DTO and use BaseController to create consistent response
            ResponseEntity<? extends com.bcbs239.regtech.core.shared.ApiResponse<?>> resp =
                    handleResult(Result.success(out), "Splitting suggestion generated", "ingestion.splitting.generated");
            assert resp.getBody() != null;
            return ServerResponse.status(resp.getStatusCode()).body(resp.getBody());
        } else {
            // Delegate failure handling to BaseController
            ResponseEntity<? extends com.bcbs239.regtech.core.shared.ApiResponse<?>> resp =
                    handleResult(result, "Splitting suggestion generated", "ingestion.splitting.generated");
            assert resp.getBody() != null;
            return ServerResponse.status(resp.getStatusCode()).body(resp.getBody());
        }

    }

    public record FileMetadataDto(
            @NotBlank String fileName,
            @NotBlank String contentType,
            @NotNull @PositiveOrZero long fileSizeBytes,
            @NotBlank String md5Checksum,
            @NotBlank String sha256Checksum
    ) {
    }
}
