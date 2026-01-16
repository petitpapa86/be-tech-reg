package com.bcbs239.regtech.ingestion.presentation.batch.upload;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static com.bcbs239.regtech.core.presentation.routing.RouterAttributes.withAttributes;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Component
public class UploadAndProcessFileRoutes {

    private final UploadAndProcessFileController controller;

    public UploadAndProcessFileRoutes(UploadAndProcessFileController controller) {
        this.controller = controller;
    }

    @Bean
    public RouterFunction<ServerResponse> uploadAndProcessRoute() {
        return withAttributes(
            route(POST("/api/v1/ingestion/upload-and-process"), controller::handle),
            new String[]{"ingestion:upload-process"},
            new String[]{"File Upload and Processing", "Ingestion"},
            "Upload a file and immediately begin processing it asynchronously"
        );
    }
}
