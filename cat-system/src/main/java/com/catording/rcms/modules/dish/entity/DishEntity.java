package com.catording.rcms.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dish")
public class DishEntity {
    private Long id;
    private String name;
    private Long categoryId;
    private BigDecimal price;
    private String status; // ON/OFF
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

