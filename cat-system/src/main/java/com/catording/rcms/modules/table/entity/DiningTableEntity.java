package com.catording.rcms.modules.table.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dining_table")
public class DiningTableEntity {
    private Long id;
    private String code;
    private String type; // HALL/ROOM
    private Integer capacity;
    private String status; // FREE/OCCUPIED
    private Long currentOrderId;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

