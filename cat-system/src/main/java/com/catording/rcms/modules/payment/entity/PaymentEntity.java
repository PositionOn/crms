package com.catording.rcms.modules.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class PaymentEntity {
    private Long id;
    private Long orderId;
    private String method; // CASH/WECHAT/ALIPAY/CARD
    private BigDecimal payAmount;
    private BigDecimal changeAmount;
    private LocalDateTime paidAt;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

