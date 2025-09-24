package br.com.nfe.processor.core.domain.model;

import br.com.nfe.processor.core.domain.valueobject.Cnpj;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "access_key", nullable = false, length = 60)
    private String accessKey;

    @Column(name = "emitter_name", nullable = false)
    private String emitterName;

    @Column(name = "emitter_tax_id", nullable = false)
    private Cnpj emitterTaxId;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_tax_id", nullable = false)
    private String recipientTaxId;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "products_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal productsAmount;

    @Column(name = "icms_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal icmsAmount;

    @Column(name = "ipi_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal ipiAmount;

    @Column(name = "iss_amount", precision = 15, scale = 2)
    private BigDecimal issAmount;

    @Column(name = "cfop", length = 10)
    private String cfop;

    @OneToMany(mappedBy = "invoice")
    private List<Issue> issues = new ArrayList<>();

    @OneToMany(mappedBy = "invoice")
    private List<ValidationReport> validations = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getEmitterName() {
        return emitterName;
    }

    public void setEmitterName(String emitterName) {
        this.emitterName = emitterName;
    }

    public Cnpj getEmitterTaxId() {
        return emitterTaxId;
    }

    public void setEmitterTaxId(Cnpj emitterTaxId) {
        this.emitterTaxId = emitterTaxId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientTaxId() {
        return recipientTaxId;
    }

    public void setRecipientTaxId(String recipientTaxId) {
        this.recipientTaxId = recipientTaxId;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getProductsAmount() {
        return productsAmount;
    }

    public void setProductsAmount(BigDecimal productsAmount) {
        this.productsAmount = productsAmount;
    }

    public BigDecimal getIcmsAmount() {
        return icmsAmount;
    }

    public void setIcmsAmount(BigDecimal icmsAmount) {
        this.icmsAmount = icmsAmount;
    }

    public BigDecimal getIpiAmount() {
        return ipiAmount;
    }

    public void setIpiAmount(BigDecimal ipiAmount) {
        this.ipiAmount = ipiAmount;
    }

    public BigDecimal getIssAmount() {
        return issAmount;
    }

    public void setIssAmount(BigDecimal issAmount) {
        this.issAmount = issAmount;
    }

    public String getCfop() {
        return cfop;
    }

    public void setCfop(String cfop) {
        this.cfop = cfop;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public List<ValidationReport> getValidations() {
        return validations;
    }
}
