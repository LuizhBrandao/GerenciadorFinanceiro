package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.controller.dto.WebhookReceiptResult;
import br.com.gerenciadorfinanceiro.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/stripe")
    public ResponseEntity<WebhookReceiptResult> handleStripeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader) {

        WebhookReceiptResult result = webhookService.receberEProcessar(rawPayload, signatureHeader);
        return ResponseEntity.ok(result);
    }
}
