package com.catording.rcms.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("consume_order")
public class ConsumeOrderEntity {
    private Long id;
    private String orderNo;
    private Long tableId;
    private Long operatorId;
    private String status; // OPEN/CLOSED/CANCELLED
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private String remark;

    private BigDecimal amountBeforeDiscount;
    private Long discountRuleId;
    private BigDecimal discountAmount;
    private String discountReason;
    private BigDecimal amountAfterDiscount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

