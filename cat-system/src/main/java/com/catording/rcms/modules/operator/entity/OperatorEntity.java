package com.catording.rcms.modules.operator.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operator")
public class OperatorEntity {
    private Long id;
    private String username;
    private String passwordHash;
    private String name;
    private String status; // ENABLED/DISABLED
    private String role;   // ADMIN/CASHIER
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

