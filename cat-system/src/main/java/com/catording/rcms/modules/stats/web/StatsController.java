package com.catording.rcms.modules.stats.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.catording.rcms.common.api.ApiResponse;
import com.catording.rcms.modules.order.entity.ConsumeOrderEntity;
import com.catording.rcms.modules.order.mapper.ConsumeOrderMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final ConsumeOrderMapper orderMapper;

    public StatsController(ConsumeOrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public record RevenueResp(BigDecimal totalBeforeDiscount, BigDecimal totalDiscount, BigDecimal totalReceived, long orderCount) {}

    @GetMapping("/revenue")
    public ApiResponse<RevenueResp> revenue(@RequestParam(required = false) String startTime,
                                           @RequestParam(required = false) String endTime) {
        LocalDateTime st = parse(startTime, true);
        LocalDateTime et = parse(endTime, false);
        if (st == null && et == null) {
            LocalDate today = LocalDate.now();
            st = today.atStartOfDay();
            et = LocalDateTime.of(today, LocalTime.MAX);
        }

        List<ConsumeOrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<ConsumeOrderEntity>()
                .eq(ConsumeOrderEntity::getStatus, "CLOSED")
                .ge(st != null, ConsumeOrderEntity::getClosedAt, st)
                .le(et != null, ConsumeOrderEntity::getClosedAt, et));

        BigDecimal before = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        for (ConsumeOrderEntity o : orders) {
            if (o.getAmountBeforeDiscount() != null) before = before.add(o.getAmountBeforeDiscount());
            if (o.getDiscountAmount() != null) discount = discount.add(o.getDiscountAmount());
            if (o.getAmountAfterDiscount() != null) received = received.add(o.getAmountAfterDiscount());
        }
        return ApiResponse.ok(new RevenueResp(before, discount, received, orders.size()));
    }

    private static LocalDateTime parse(String s, boolean start) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim().replace('T', ' ');
        if (v.length() == 10) v = v + (start ? " 00:00:00" : " 23:59:59");
        return LocalDateTime.parse(v.replace(' ', 'T'));
    }
}

