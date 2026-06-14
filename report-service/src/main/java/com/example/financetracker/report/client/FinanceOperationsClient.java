package com.example.financetracker.report.client;

import com.example.financetracker.report.api.dto.ReportRequest;
import com.example.financetracker.report.api.error.ExternalServiceException;
import com.example.financetracker.report.api.error.InvalidReportPeriodException;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

@Component
public class FinanceOperationsClient {

    private final RestClient restClient;

    public FinanceOperationsClient(RestClient financeServiceRestClient) {
        this.restClient = financeServiceRestClient;
    }

    public List<OperationReportItem> getOperations(ReportRequest request, String authorizationHeader) {
        try {
            OperationReportItem[] operations = restClient.get()
                    .uri(uriBuilder -> buildOperationsUri(uriBuilder, request))
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(OperationReportItem[].class);
            if (operations == null) {
                return List.of();
            }
            return Arrays.asList(operations);
        } catch (ResourceAccessException exception) {
            throw financeServiceUnavailable();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is5xxServerError()) {
                throw financeServiceUnavailable();
            }
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED
                    || exception.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new AccessDeniedException("Access denied");
            }
            if (exception.getStatusCode().is4xxClientError()) {
                throw new InvalidReportPeriodException("Invalid report filters");
            }
            throw exception;
        }
    }

    private java.net.URI buildOperationsUri(UriBuilder uriBuilder, ReportRequest request) {
        UriBuilder builder = uriBuilder
                .path("/internal/operations")
                .queryParam("from", request.from())
                .queryParam("to", request.to());

        if (request.groupId() != null) {
            builder.queryParam("groupId", request.groupId());
        }
        if (request.userIds() != null && !request.userIds().isEmpty()) {
            builder.queryParam("userIds", request.userIds().toArray());
        }
        if (request.type() != null) {
            builder.queryParam("type", request.type());
        }
        return builder.build();
    }

    private ExternalServiceException financeServiceUnavailable() {
        return new ExternalServiceException(
                "FINANCE_SERVICE_UNAVAILABLE",
                "Finance service is temporarily unavailable"
        );
    }
}
