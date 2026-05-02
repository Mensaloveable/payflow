package com.payments.controller;

import com.payments.entity.Transaction;
import com.payments.model.PaymentMethod;
import com.payments.model.PaymentRequest;
import com.payments.model.PaymentResult;
import com.payments.model.PaymentStats;
import com.payments.service.PaymentService;
import com.payments.util.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/")
    public String index(Model model, HttpServletRequest request) {
        PageResponse<Transaction> recentTransactions = paymentService.getRecentTransactions(0, 20);
        PaymentStats stats = paymentService.getStats();
        List<Object[]> methodBreakdown = paymentService.getMethodBreakdown();
        int remainingQuota = paymentService.getRemainingQuota(getClientIp(request));

        model.addAttribute("paymentRequest", new PaymentRequest());
        model.addAttribute("paymentMethods", PaymentMethod.values());
//        model.addAttribute("transactions",      txPage.getContent());
        model.addAttribute("transactions", recentTransactions.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("methodBreakdown", methodBreakdown);
        model.addAttribute("remainingQuota", remainingQuota);
        model.addAttribute("totalPages", recentTransactions.getTotalPages());
        return "index";
    }

    @PostMapping("/process")
    public String processPayment(
            @ModelAttribute PaymentRequest paymentRequest,
            HttpServletRequest request,
            RedirectAttributes redirectAttrs) {

        PaymentResult result = paymentService.processPayment(paymentRequest, getClientIp(request));
        redirectAttrs.addFlashAttribute("result", result);
        return "redirect:/";
    }

    @GetMapping("/transactions")
    @ResponseBody
    public PageResponse<Transaction> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return paymentService.getRecentTransactions(page, size);
    }

    @GetMapping("/transactions/{id}")
    @ResponseBody
    public Transaction getTransaction(@PathVariable String id) {
        return paymentService.getByTransactionId(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }

    @GetMapping("/stats")
    @ResponseBody
    public PaymentStats getStats() {
        return paymentService.getStats();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
