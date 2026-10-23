package io.parcelpilot.web;

import io.parcelpilot.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Every error is an RFC 9457 problem document. Errors raised by this API carry a stable {@code code} the client can
 * switch on; framework errors (404, 405, ...) keep Spring's standard problem documents.
 */
@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<Object> domain(DomainException e) {
        HttpStatus status = switch (e.kind()) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case RULE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case CONFLICT -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return problem(status, e.code(), e.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Object> unavailable(IllegalStateException e) {
        log.warn("Service unavailable: {}", e.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "TEMPORARILY_UNAVAILABLE", "Please try again shortly.", null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message", String.valueOf(f.getDefaultMessage())))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Some fields are missing or invalid.", fields);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body is not valid JSON for this endpoint.", null);
    }

    private static ResponseEntity<Object> problem(HttpStatus status, String code, String detail, Object errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        if (errors != null) pd.setProperty("errors", errors);
        return ResponseEntity.status(status).body(pd);
    }
}
