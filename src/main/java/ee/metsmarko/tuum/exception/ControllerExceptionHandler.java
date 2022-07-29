package ee.metsmarko.tuum.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import ee.metsmarko.tuum.account.TransactionDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ControllerExceptionHandler.class);

  @ExceptionHandler(value = {TuumInvalidInputException.class})
  protected ResponseEntity<Object> handleInvalidInput(TuumInvalidInputException ex,
                                                      WebRequest request) {
    log.error("Invalid input: {}", ex.getMessage());
    return handleExceptionInternal(
        ex, new ErrorResponse(ex.getMessage()), new HttpHeaders(), HttpStatus.BAD_REQUEST, request
    );
  }

  @Override
  @NonNull
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, @NonNull HttpHeaders headers, @NonNull HttpStatus status,
      @NonNull WebRequest request
  ) {
    // return a user-friendly error message when enum could not be mapped
    Throwable cause = ex.getCause();
    if (cause instanceof InvalidFormatException error) {
      if (error.getTargetType().isAssignableFrom(TransactionDirection.class)) {
        return handleExceptionInternal(ex, new ErrorResponse("invalid transaction direction"),
            headers, status, request);
      }
    }
    return super.handleHttpMessageNotReadable(ex, headers, status, request);
  }
}
