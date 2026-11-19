package org.ruoyi.chat.controller.coding;

import org.ruoyi.common.core.domain.R;
import org.ruoyi.chat.service.coding.harness.app.HarnessConflictException;
import org.ruoyi.chat.service.coding.harness.app.HarnessNotFoundException;
import org.ruoyi.chat.service.coding.harness.runtime.HarnessSchedulerCapacityException;
import org.ruoyi.chat.service.coding.harness.store.HarnessOptimisticLockException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CodingHarnessController.class)
public class CodingHarnessExceptionHandler {

    @ExceptionHandler(HarnessNotFoundException.class)
    public ResponseEntity<R<Void>> notFound(HarnessNotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(R.fail(HttpStatus.NOT_FOUND.value(), error.getMessage()));
    }

    @ExceptionHandler(HarnessConflictException.class)
    public ResponseEntity<R<Void>> conflict(HarnessConflictException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(R.fail(HttpStatus.CONFLICT.value(), error.getMessage()));
    }

    @ExceptionHandler(HarnessSchedulerCapacityException.class)
    public ResponseEntity<R<Void>> tooManyRequests(HarnessSchedulerCapacityException error) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", "5")
            .body(R.fail(HttpStatus.TOO_MANY_REQUESTS.value(), error.getMessage()));
    }

    @ExceptionHandler(HarnessOptimisticLockException.class)
    public ResponseEntity<R<Void>> optimisticConflict(HarnessOptimisticLockException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(R.fail(HttpStatus.CONFLICT.value(), error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> badRequest(IllegalArgumentException error) {
        return ResponseEntity.badRequest()
            .body(R.fail(HttpStatus.BAD_REQUEST.value(), error.getMessage()));
    }
}
