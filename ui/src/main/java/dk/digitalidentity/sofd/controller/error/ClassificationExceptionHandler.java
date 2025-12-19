package dk.digitalidentity.sofd.controller.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

// Scoped to only handle exceptions from the classification package
@RestControllerAdvice(basePackages = "dk.digitalidentity.sofd.controller.api.classification")
public class ClassificationExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        // Determine status based on message content
        HttpStatus status;
        String message = ex.getMessage();

        if (message != null && message.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (message != null && message.contains("already exists")) {
            status = HttpStatus.BAD_REQUEST;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status.value(),
                ex.getMessage()
        );
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(problemDetail, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid input data"
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setInstance(request.getDescription(false).replace("uri=", ""));
        problemDetail.setProperty("errors", fieldErrors);

        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }
}