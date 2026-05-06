package global.govstack.cross_border_pay.controller;

import global.govstack.cross_border_pay.dto.SignatureDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final RestTemplate restTemplate;
    private final String signatureUrl;
    private final String privateKey;
    private final String correlationId;
    private final String tenantId;

    public PaymentController(
            @Value("${signature.url}") String signatureUrl,
            @Value("${signature.tenant}") String tenantId,
            @Value("${signature.correlation-id}") String correlationId,
            @Value("${signature.private-key}") String privateKey) {
        this.restTemplate = new RestTemplate();
        this.signatureUrl = signatureUrl;
        this.privateKey = privateKey;
        this.tenantId = tenantId;
        this.correlationId = correlationId;
    }


    @PostMapping("/transaction")
    @ResponseStatus(HttpStatus.CREATED)
    public String requestForSignature(@RequestBody final SignatureDto signatureDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Accept", "application/json");
        headers.add("X-CorrelationID", correlationId);
        headers.add("Platform-TenantId", tenantId);
        headers.add("privateKey", privateKey);


        var requestId = "447b043f-ef7e-4c40-9f4f-083772987cc6";
        var paymentMode = "MASTERCARD_CBS";
        var payerAndPayeeIdentifierType = "MSISDN";
        var payerIdentifier = "27000000000";
        var currency = "ZAR";

        String csv =
                "id,request_id,payment_mode,payer_identifier_type,payer_identifier," +
                        "payee_identifier_type,payee_identifier,amount,currency,note\n" +
                        signatureDto.getId() + "," +
                        requestId + "," +
                        paymentMode + "," +
                        payerAndPayeeIdentifierType + "," +
                        payerIdentifier + "," +
                        payerAndPayeeIdentifierType + "," +
                        signatureDto.getPayeeIdentifier() + "," +
                        signatureDto.getAmount() + "," +
                        currency + "," +
                        "\"" + signatureDto.getNote() + "\"";

        ByteArrayResource resource = new ByteArrayResource(csv.getBytes()) {
            @Override
            public String getFilename() {
                return "bulk.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("data", resource);

        log.info("Generated CSV:\n{}", csv);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        String signature = restTemplate.exchange(
                signatureUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        ).getBody();


        HttpHeaders batchHeaders = new HttpHeaders();
        batchHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        batchHeaders.add("Accept", "application/json");
        batchHeaders.add("X-Signature", signature);
        batchHeaders.add("X-CorrelationID", correlationId);
        batchHeaders.add("Platform-TenantId", tenantId);
        batchHeaders.add("type", "csv");
        batchHeaders.add("filename", "bulk.csv");
        batchHeaders.add("X-CallbackURL", "http://ph-ee-connector-mock-payment-schema:8080/batches/" + correlationId + "/callback");
        batchHeaders.add("Purpose", "Batch payment");

        HttpEntity<MultiValueMap<String, Object>> batchRequest =
                new HttpEntity<>(body, batchHeaders);

        String batchResponse = restTemplate.exchange(
                "https://bulk-processor.mifos.sandbox.govstack.global/batchtransactions",
                HttpMethod.POST,
                batchRequest,
                String.class
        ).getBody();

        return batchResponse;
    }
}
