package com.correai.api.adapter.in.web.error;

import com.correai.api.domain.model.activity.ActivityNotFoundException;
import com.correai.api.domain.model.auth.InvalidCredentialsException;
import com.correai.api.domain.model.auth.InvalidTokenException;
import com.correai.api.domain.model.user.AccountAlreadyRegisteredException;
import com.correai.api.domain.model.user.EmailAlreadyRegisteredException;
import com.correai.api.domain.model.goal.GoalNotFoundException;
import com.correai.api.domain.model.goal.GoalHasBadgeException;
import com.correai.api.domain.model.user.UserNotFoundException;
import com.correai.api.domain.model.user.ConcurrentProfileUpdateException;
import com.correai.api.domain.model.media.MediaNotFoundException;
import com.correai.api.domain.model.group.GroupNotFoundException;
import com.correai.api.domain.model.friendship.FriendshipNotFoundException;
import com.correai.api.domain.model.friendship.FriendshipConflictException;
import com.correai.api.domain.model.admin.AdminAccessDeniedException;
import com.correai.api.domain.model.admin.AdminConflictException;
import com.correai.api.domain.model.badge.BadgeTemplateNotFoundException;
import com.correai.api.domain.model.challenge.ChallengeConflictException;
import com.correai.api.domain.model.challenge.ChallengeNotFoundException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<ProblemDetail> handleInvalidToken(InvalidTokenException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problem);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problem);
    }

    @ExceptionHandler({EmailAlreadyRegisteredException.class, AccountAlreadyRegisteredException.class,
            ConcurrentProfileUpdateException.class, GoalHasBadgeException.class, FriendshipConflictException.class,
            AdminConflictException.class, ChallengeConflictException.class})
    ProblemDetail handleEmailAlreadyRegistered(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler({GoalNotFoundException.class, ActivityNotFoundException.class, UserNotFoundException.class,
            MediaNotFoundException.class, GroupNotFoundException.class, FriendshipNotFoundException.class,
            BadgeTemplateNotFoundException.class, ChallengeNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Not Found");
        return problem;
    }

    @ExceptionHandler(AdminAccessDeniedException.class)
    ProblemDetail handleAdminAccessDenied(AdminAccessDeniedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Forbidden");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Bad Request");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
        problem.setTitle("Internal Server Error");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Bad Request");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}
