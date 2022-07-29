package ee.metsmarko.tuum.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import ee.metsmarko.tuum.account.TransactionDirection;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.WebRequest;

class ControllerExceptionHandlerTest {
  private final ControllerExceptionHandler exceptionHandler = new ControllerExceptionHandler();

  @Test
  void testHandleInvalidInput() {
    ResponseEntity<Object> response =
        exceptionHandler.handleInvalidInput(new TuumInvalidInputException("error occurred"),
            mock(WebRequest.class));

    assertEquals(400, response.getStatusCodeValue());
    assertEquals("error occurred", ((ErrorResponse) response.getBody()).error());
  }

  @Test
  void testInvalidEnumError() throws Exception {
    HttpMessageNotReadableException error =
        new HttpMessageNotReadableException(
            "error",
            new InvalidFormatException(
                mock(JsonParser.class), "error 2", new Object(), TransactionDirection.class
            ),
            mock(HttpInputMessage.class)
        );

    ResponseEntity<Object> response =
        exceptionHandler.handleException(error, mock(WebRequest.class));

    assertNotNull(response);
    assertEquals(400, response.getStatusCodeValue());
    assertEquals("invalid transaction direction", ((ErrorResponse) response.getBody()).error());
  }

  @Test
  void testHttpMessageNotReadable() throws Exception {
    HttpMessageNotReadableException error = new HttpMessageNotReadableException(
        "error", new Exception("error 2"), mock(HttpInputMessage.class)
    );

    ResponseEntity<Object> response =
        exceptionHandler.handleException(error, mock(WebRequest.class));

    assertNotNull(response);
    assertEquals(400, response.getStatusCodeValue());
  }
}