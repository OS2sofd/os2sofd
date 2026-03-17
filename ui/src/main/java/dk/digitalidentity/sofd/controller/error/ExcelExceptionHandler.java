package dk.digitalidentity.sofd.controller.error;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import dk.digitalidentity.sofd.controller.api.DownloadExcelApi;

// Scoped to only handle exceptions from the Excel download API
@RestControllerAdvice(assignableTypes = DownloadExcelApi.class)
public class ExcelExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        HttpStatus status;
        String message = ex.getMessage();

        if (message != null && message.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));

        return new ResponseEntity<>(problemDetail, status);
    }
}
