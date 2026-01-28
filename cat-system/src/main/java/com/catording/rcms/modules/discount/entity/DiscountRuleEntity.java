package com.catording.rcms.modules.discount.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("discount_rule")
public class DiscountRuleEntity {
    private Long id;
    private String name;
    private String type; // FIXED_RATE/FULL_REDUCTION/MEMBER
    private String paramsJson;
    private Integer enabled; // 0/1
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

