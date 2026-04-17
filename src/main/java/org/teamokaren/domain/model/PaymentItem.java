package org.teamokaren.domain.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentItem {
    private String sku;
    private String name;
    private Integer quantity;
    private BigDecimal unitPrice;
}
