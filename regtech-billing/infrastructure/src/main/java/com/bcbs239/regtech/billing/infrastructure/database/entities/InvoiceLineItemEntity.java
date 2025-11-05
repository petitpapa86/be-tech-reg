package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.invoices.InvoiceLineItem;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceLineItemId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

/**
 * JPA Entity for InvoiceLineItem value object persistence.
 * Maps domain value object to database table structure.
 */
@Entity
@Table(name = "invoice_line_items", schema = "billing")
public class InvoiceLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "invoice_id", nullable = false)
    private String invoiceId;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "unit_amount_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal unitAmountValue;

    @Column(name = "unit_amount_currency", length = 3, nullable = false)
    private String unitAmountCurrency;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "total_amount_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmountValue;

    @Column(name = "total_amount_currency", length = 3, nullable = false)
    private String totalAmountCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Default constructor for JPA
    public InvoiceLineItemEntity() {}

    /**
     * Convert domain value object to JPA entity
     */
    public static InvoiceLineItemEntity fromDomain(InvoiceLineItem lineItem, String invoiceId) {
        InvoiceLineItemEntity entity = new InvoiceLineItemEntity();
        
        entity.id = lineItem.id().value();
        entity.invoiceId = invoiceId;
        entity.description = lineItem.description();
        entity.unitAmountValue = lineItem.unitAmount().amount();
        entity.unitAmountCurrency = lineItem.unitAmount().currency().getCurrencyCode();
        entity.quantity = lineItem.quantity();
        entity.totalAmountValue = lineItem.totalAmount().amount();
        entity.totalAmountCurrency = lineItem.totalAmount().currency().getCurrencyCode();
        entity.createdAt = Instant.now();
        
        return entity;
    }

    /**
     * Convert JPA entity to domain value object
     */
    public InvoiceLineItem toDomain() {
        Currency unitCurrency = Currency.getInstance(this.unitAmountCurrency);
        Currency totalCurrency = Currency.getInstance(this.totalAmountCurrency);
        
        return new InvoiceLineItem(
            new InvoiceLineItemId(this.id),
            this.description,
            Money.of(this.unitAmountValue, unitCurrency),
            this.quantity,
            Money.of(this.totalAmountValue, totalCurrency)
        );
    }

    // Getters and setters for JPA
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getUnitAmountValue() { return unitAmountValue; }
    public void setUnitAmountValue(BigDecimal unitAmountValue) { this.unitAmountValue = unitAmountValue; }

    public String getUnitAmountCurrency() { return unitAmountCurrency; }
    public void setUnitAmountCurrency(String unitAmountCurrency) { this.unitAmountCurrency = unitAmountCurrency; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getTotalAmountValue() { return totalAmountValue; }
    public void setTotalAmountValue(BigDecimal totalAmountValue) { this.totalAmountValue = totalAmountValue; }

    public String getTotalAmountCurrency() { return totalAmountCurrency; }
    public void setTotalAmountCurrency(String totalAmountCurrency) { this.totalAmountCurrency = totalAmountCurrency; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

