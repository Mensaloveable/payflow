package com.payments.exception;

import com.payments.cache.RateLimiterService.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public String handleRateLimit(RateLimitExceededException ex,
                                  RedirectAttributes redirectAttrs) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        redirectAttrs.addFlashAttribute("errorType", "RATE_LIMIT");
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, RedirectAttributes redirectAttrs) {
        log.error("Unexpected error during payment processing", ex);
        redirectAttrs.addFlashAttribute("errorMessage",
                "An unexpected error occurred. Please try again. (" + ex.getMessage() + ")");
        redirectAttrs.addFlashAttribute("errorType", "SYSTEM_ERROR");
        return "redirect:/";
    }
}
