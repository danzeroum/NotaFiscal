package br.com.nfe.processor.core.domain.service.dto;

import br.com.nfe.processor.core.domain.valueobject.Cnpj;
import java.math.BigDecimal;

public class ParsedInvoice {
    private final String accessKey;
    private final String emitterName;
    private final Cnpj emitterTaxId;
    private final String recipientName;
    private final String recipientTaxId;
    private final int itemCount;
    private final BigDecimal totalAmount;
    private final BigDecimal productsAmount;
    private final BigDecimal icmsAmount;
    private final BigDecimal ipiAmount;
    private final BigDecimal issAmount;
    private final String cfop;

    public ParsedInvoice(
            String accessKey,
            String emitterName,
            Cnpj emitterTaxId,
            String recipientName,
            String recipientTaxId,
            int itemCount,
            BigDecimal totalAmount,
            BigDecimal productsAmount,
            BigDecimal icmsAmount,
            BigDecimal ipiAmount,
            BigDecimal issAmount,
            String cfop) {
        this.accessKey = accessKey;
        this.emitterName = emitterName;
        this.emitterTaxId = emitterTaxId;
        this.recipientName = recipientName;
        this.recipientTaxId = recipientTaxId;
        this.itemCount = itemCount;
        this.totalAmount = totalAmount;
        this.productsAmount = productsAmount;
        this.icmsAmount = icmsAmount;
        this.ipiAmount = ipiAmount;
        this.issAmount = issAmount;
        this.cfop = cfop;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getEmitterName() {
        return emitterName;
    }

    public Cnpj getEmitterTaxId() {
        return emitterTaxId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientTaxId() {
        return recipientTaxId;
    }

    public int getItemCount() {
        return itemCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getProductsAmount() {
        return productsAmount;
    }

    public BigDecimal getIcmsAmount() {
        return icmsAmount;
    }

    public BigDecimal getIpiAmount() {
        return ipiAmount;
    }

    public BigDecimal getIssAmount() {
        return issAmount;
    }

    public String getCfop() {
        return cfop;
    }
}
