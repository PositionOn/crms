package com.catording.rcms.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dish_category")
public class DishCategoryEntity {
    private Long id;
    private String name;
    private Integer sort;
    private String status; // ENABLED/DISABLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

