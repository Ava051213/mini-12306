package com.mia.mini12306.common.web;

import com.mia.mini12306.inventory.InsufficientTicketsException;
import com.mia.mini12306.inventory.TicketInventoryNotFoundException;
import com.mia.mini12306.order.IdempotencyKeyConflictException;
import com.mia.mini12306.order.OrderNotFoundException;
import com.mia.mini12306.order.OrderStateConflictException;
import com.mia.mini12306.passenger.PassengerAlreadyExistsException;
import com.mia.mini12306.passenger.PassengerInUseException;
import com.mia.mini12306.passenger.PassengerNotFoundException;
import com.mia.mini12306.train.InvalidTrainTripSearchException;
import com.mia.mini12306.user.InvalidCredentialsException;
import com.mia.mini12306.user.NotLoggedInException;
import com.mia.mini12306.user.UserAlreadyExistsException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExists(
            UserAlreadyExistsException exception
    ) {
        // Do not echo the submitted username.
        return new ErrorResponse("用户名已存在");
    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            NotLoggedInException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(RuntimeException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler({
            PassengerNotFoundException.class,
            TicketInventoryNotFoundException.class,
            OrderNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(RuntimeException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler({
            PassengerAlreadyExistsException.class,
            PassengerInUseException.class,
            InsufficientTicketsException.class,
            OrderStateConflictException.class,
            IdempotencyKeyConflictException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(RuntimeException exception) {
        // These business exceptions contain controlled messages.
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(InvalidTrainTripSearchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidTrainTripSearch(
            InvalidTrainTripSearchException exception
    ) {
        return new ErrorResponse(exception.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        // Select one error in a stable order.
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparing(FieldError::getField)
                        .thenComparing(error -> String.valueOf(error.getCode())))
                .findFirst()
                .map(this::safeValidationMessage)
                .orElse("请求参数不符合要求");

        // Preserve the framework status and headers.
        return handleExceptionInternal(
                exception,
                new ErrorResponse(message),
                headers,
                status,
                request
        );
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        // Keep return-value validation failures as safe server errors.
        if(status.is5xxServerError()) {
            return handleExceptionInternal(
                    exception, null, headers, status, request
            );
        }

        // Reuse safe field messages for validated request bodies.
        String message = exception.getBeanResults().stream()
                .flatMap(errors -> errors.getFieldErrors().stream())
                .sorted(Comparator.comparing(FieldError::getField)
                        .thenComparing(error -> String.valueOf(error.getCode())))
                .findFirst()
                .map(this::safeValidationMessage)
                .orElse("请求参数不符合要求");

        return handleExceptionInternal(
                exception,
                new ErrorResponse(message),
                headers,
                status,
                request
        );
    }

    private String safeValidationMessage(FieldError error) {
        String field = error.getField();

        // Map the internal validation property to the public input field.
        if("passwordWithinBcryptLimit".equals(field)) {
            return "password：UTF-8 编码不能超过 72 字节";
        }

        // Binding errors may contain raw input in their default messages.
        if(error.isBindingFailure()) {
            return field + "：格式不正确";
        }

        // Use controlled text instead of rejected values or default messages.
        String reason = switch(String.valueOf(error.getCode())) {
            case "NotBlank", "NotNull" -> "不能为空";
            case "Size" -> "长度不符合要求";
            case "Pattern" -> "格式不正确";
            default -> "不符合要求";
        };

        return field + "：" + reason;
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        // A committed response cannot be replaced.
        if(request instanceof ServletWebRequest servletRequest) {
            var response = servletRequest.getResponse();
            if(response != null && response.isCommitted()) {
                return null;
            }
        }

        // Keep our safe body; replace framework-generated error details.
        ErrorResponse safeBody = body instanceof ErrorResponse errorResponse
                ? errorResponse
                : new ErrorResponse(safeFrameworkMessage(exception, status));

        if(status.is5xxServerError()) {
            // Never log arbitrary exception messages or cause chains.
            logger.error(
                    "Server error; exceptionType=" + exception.getClass().getName()
            );
        }

        // Preserve protocol headers such as Allow, and return JSON errors.
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        return super.handleExceptionInternal(
                exception,
                safeBody,
                responseHeaders,
                status,
                request
        );
    }

    private String safeFrameworkMessage(
            Exception exception,
            HttpStatusCode status
    ) {
        // Server-side validation and conversion failures remain server errors.
        if(status.is5xxServerError()) {
            return "服务器内部错误";
        }

        if(exception instanceof HttpMessageNotReadableException) {
            return "请求体缺失或 JSON 格式不正确";
        }

        if(exception instanceof TypeMismatchException) {
            return "请求参数类型或格式不正确";
        }

        return switch(status.value()) {
            case 400 -> "请求参数不符合要求";
            case 404 -> "请求地址不存在";
            case 405 -> "请求方法不允许";
            case 406 -> "不支持请求的响应格式";
            case 413 -> "请求内容过大";
            case 415 -> "不支持请求的内容类型";
            default -> "请求处理失败";
        };
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        // Skip the parent implementation's raw exception-message warning.
        return handleExceptionInternal(
                exception,
                null,
                headers,
                status,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public @Nullable ResponseEntity<Object> handleUnexpectedException(
            Exception exception,
            WebRequest request
    ) {
        // Convert unexpected application failures to a controlled 500 response.
        return handleExceptionInternal(
                exception,
                new ErrorResponse("服务器内部错误"),
                new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }
}
