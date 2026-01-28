package com.catording.rcms.modules.ledger.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.modules.order.entity.ConsumeOrderEntity;
import com.catording.rcms.modules.order.mapper.ConsumeOrderMapper;
import com.catording.rcms.modules.payment.entity.PaymentEntity;
import com.catording.rcms.modules.payment.mapper.PaymentMapper;
import com.catording.rcms.modules.table.entity.DiningTableEntity;
import com.catording.rcms.modules.table.mapper.DiningTableMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
    private final ConsumeOrderMapper orderMapper;
    private final DiningTableMapper tableMapper;
    private final PaymentMapper paymentMapper;

    public LedgerController(ConsumeOrderMapper orderMapper, DiningTableMapper tableMapper, PaymentMapper paymentMapper) {
        this.orderMapper = orderMapper;
        this.tableMapper = tableMapper;
        this.paymentMapper = paymentMapper;
    }

    public record LedgerOrderRow(
            Long id,
            String orderNo,
            Long tableId,
            String tableCode,
            Long operatorId,
            String status,
            LocalDateTime openedAt,
            LocalDateTime closedAt,
            Object amountBeforeDiscount,
            Object discountAmount,
            Object amountAfterDiscount,
            String paymentMethod
    ) {}

    @GetMapping("/orders")
    public ApiResponse<List<LedgerOrderRow>> queryOrders(
            @RequestParam(required = false) String tableCode,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        Map<Long, String> tableIdToCode = new HashMap<>();
        if (tableCode != null && !tableCode.isBlank()) {
            List<DiningTableEntity> tables = tableMapper.selectList(new LambdaQueryWrapper<DiningTableEntity>()
                    .like(DiningTableEntity::getCode, tableCode));
            if (tables.isEmpty()) return ApiResponse.ok(List.of());
            for (DiningTableEntity t : tables) tableIdToCode.put(t.getId(), t.getCode());
        }

        LocalDateTime st = parseTime(startTime);
        LocalDateTime et = parseTime(endTime);

        LambdaQueryWrapper<ConsumeOrderEntity> qw = new LambdaQueryWrapper<ConsumeOrderEntity>()
                .like(orderNo != null && !orderNo.isBlank(), ConsumeOrderEntity::getOrderNo, orderNo)
                .eq(status != null && !status.isBlank(), ConsumeOrderEntity::getStatus, status)
                .eq(operatorId != null, ConsumeOrderEntity::getOperatorId, operatorId)
                .eq(ruleId != null, ConsumeOrderEntity::getDiscountRuleId, ruleId)
                .ge(st != null, ConsumeOrderEntity::getOpenedAt, st)
                .le(et != null, ConsumeOrderEntity::getOpenedAt, et)
                .orderByDesc(ConsumeOrderEntity::getId);
        if (!tableIdToCode.isEmpty()) {
            qw.in(ConsumeOrderEntity::getTableId, tableIdToCode.keySet());
        }

        List<ConsumeOrderEntity> orders = orderMapper.selectList(qw);
        if (orders.isEmpty()) return ApiResponse.ok(List.of());

        // 补 tableCode
        Set<Long> tableIds = new HashSet<>();
        for (ConsumeOrderEntity o : orders) tableIds.add(o.getTableId());
        if (tableIdToCode.isEmpty()) {
            List<DiningTableEntity> tables = tableMapper.selectBatchIds(tableIds);
            for (DiningTableEntity t : tables) tableIdToCode.put(t.getId(), t.getCode());
        }

        // 补支付方式（仅取最新一条）
        Map<Long, String> orderIdToMethod = new HashMap<>();
        List<Long> orderIds = orders.stream().map(ConsumeOrderEntity::getId).toList();
        List<PaymentEntity> pays = paymentMapper.selectList(new LambdaQueryWrapper<PaymentEntity>()
                .in(PaymentEntity::getOrderId, orderIds)
                .orderByDesc(PaymentEntity::getPaidAt));
        for (PaymentEntity p : pays) {
            orderIdToMethod.putIfAbsent(p.getOrderId(), p.getMethod());
        }

        List<LedgerOrderRow> rows = new ArrayList<>();
        for (ConsumeOrderEntity o : orders) {
            String pm = orderIdToMethod.get(o.getId());
            if (method != null && !method.isBlank()) {
                if (pm == null || !method.equalsIgnoreCase(pm)) continue;
            }
            rows.add(new LedgerOrderRow(
                    o.getId(),
                    o.getOrderNo(),
                    o.getTableId(),
                    tableIdToCode.getOrDefault(o.getTableId(), ""),
                    o.getOperatorId(),
                    o.getStatus(),
                    o.getOpenedAt(),
                    o.getClosedAt(),
                    o.getAmountBeforeDiscount(),
                    o.getDiscountAmount(),
                    o.getAmountAfterDiscount(),
                    pm
            ));
        }
        return ApiResponse.ok(rows);
    }

    private static LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        // 兼容 yyyy-MM-ddTHH:mm:ss 或 yyyy-MM-dd HH:mm:ss
        String v = s.trim().replace('T', ' ');
        if (v.length() == 10) v = v + " 00:00:00";
        return LocalDateTime.parse(v.replace(' ', 'T'));
    }
}

