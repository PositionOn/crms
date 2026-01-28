package com.catording.rcms.modules.table.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.modules.table.entity.DiningTableEntity;
import com.catording.rcms.modules.table.mapper.DiningTableMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableController {
    private final DiningTableMapper tableMapper;

    public TableController(DiningTableMapper tableMapper) {
        this.tableMapper = tableMapper;
    }

    @GetMapping
    public ApiResponse<List<DiningTableEntity>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(tableMapper.selectList(new LambdaQueryWrapper<DiningTableEntity>()
                .eq(status != null && !status.isBlank(), DiningTableEntity::getStatus, status)
                .orderByAsc(DiningTableEntity::getCode)));
    }

    @Data
    public static class CreateReq {
        @NotBlank
        private String code;
        @NotBlank
        private String type; // HALL/ROOM
        @NotNull
        @Min(1)
        private Integer capacity;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid CreateReq req) {
        DiningTableEntity e = new DiningTableEntity();
        e.setCode(req.getCode().trim());
        e.setType(req.getType());
        e.setCapacity(req.getCapacity());
        e.setStatus("FREE");
        try {
            tableMapper.insert(e);
        } catch (Exception ex) {
            throw BizException.badRequest("桌台编号已存在");
        }
        return ApiResponse.ok(e.getId());
    }

    @Data
    public static class UpdateReq {
        @NotBlank
        private String code;
        @NotBlank
        private String type;
        @NotNull
        @Min(1)
        private Integer capacity;
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody @Valid UpdateReq req) {
        DiningTableEntity e = tableMapper.selectById(id);
        if (e == null) throw BizException.badRequest("桌台不存在");
        e.setCode(req.getCode().trim());
        e.setType(req.getType());
        e.setCapacity(req.getCapacity());
        try {
            tableMapper.updateById(e);
        } catch (Exception ex) {
            throw BizException.badRequest("桌台编号已存在");
        }
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        DiningTableEntity e = tableMapper.selectById(id);
        if (e == null) return ApiResponse.ok(null);
        if ("OCCUPIED".equalsIgnoreCase(e.getStatus())) throw BizException.badRequest("桌台占用中，不能删除");
        tableMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    @Data
    public static class BatchStatusReq {
        @NotNull
        private List<Long> ids;
        @NotBlank
        private String status; // FREE/OCCUPIED
    }

    @PostMapping("/batch-status")
    public ApiResponse<Void> batchStatus(@RequestBody @Valid BatchStatusReq req) {
        if (req.getIds() == null || req.getIds().isEmpty()) throw BizException.badRequest("ids 不能为空");
        for (Long id : req.getIds()) {
            DiningTableEntity e = tableMapper.selectById(id);
            if (e == null) continue;
            if ("OCCUPIED".equalsIgnoreCase(e.getStatus()) && "FREE".equalsIgnoreCase(req.getStatus())) {
                // 允许释放，但仅做简单实现；收银结账会自动释放
            }
            e.setStatus(req.getStatus());
            if ("FREE".equalsIgnoreCase(req.getStatus())) e.setCurrentOrderId(null);
            tableMapper.updateById(e);
        }
        return ApiResponse.ok(null);
    }
}

