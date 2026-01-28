package com.catording.rcms.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("consume_order_item")
public class ConsumeOrderItemEntity {
    private Long id;
    private Long orderId;
    private Long dishId;
    private String dishNameSnapshot;
    private BigDecimal unitPriceSnapshot;
    private Integer quantity;
    private BigDecimal lineAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

