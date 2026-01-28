package com.catording.rcms.modules.export.web;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.modules.order.entity.ConsumeOrderEntity;
import com.catording.rcms.modules.order.entity.ConsumeOrderItemEntity;
import com.catording.rcms.modules.order.mapper.ConsumeOrderItemMapper;
import com.catording.rcms.modules.order.mapper.ConsumeOrderMapper;
import com.catording.rcms.modules.table.entity.DiningTableEntity;
import com.catording.rcms.modules.table.mapper.DiningTableMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/ledger")
public class ExportController {
    private static final Logger log = LoggerFactory.getLogger(ExportController.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConsumeOrderMapper orderMapper;
    private final ConsumeOrderItemMapper itemMapper;
    private final DiningTableMapper tableMapper;

    public ExportController(ConsumeOrderMapper orderMapper, ConsumeOrderItemMapper itemMapper, DiningTableMapper tableMapper) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.tableMapper = tableMapper;
    }

    @Data
    public static class DetailRow {
        @ExcelProperty("订单号")
        private String orderNo;
        @ExcelProperty("桌台")
        private String tableCode;
        @ExcelProperty("状态")
        private String status;
        @ExcelProperty("开台时间")
        private String openedAt;
        @ExcelProperty("结账时间")
        private String closedAt;
        @ExcelProperty("菜品")
        private String dishName;
        @ExcelProperty("单价")
        private String unitPrice;
        @ExcelProperty("数量")
        private Integer quantity;
        @ExcelProperty("小计")
        private String lineAmount;
        @ExcelProperty("应收(折前)")
        private String amountBeforeDiscount;
        @ExcelProperty("折扣")
        private String discountAmount;
        @ExcelProperty("实收")
        private String amountAfterDiscount;
    }

    @Data
    public static class SummaryRow {
        @ExcelProperty("订单号")
        private String orderNo;
        @ExcelProperty("桌台")
        private String tableCode;
        @ExcelProperty("状态")
        private String status;
        @ExcelProperty("开台时间")
        private String openedAt;
        @ExcelProperty("结账时间")
        private String closedAt;
        @ExcelProperty("应收(折前)")
        private String amountBeforeDiscount;
        @ExcelProperty("折扣")
        private String discountAmount;
        @ExcelProperty("实收")
        private String amountAfterDiscount;
        @ExcelProperty("折扣原因")
        private String discountReason;
    }

    @GetMapping("/export")
    public void export(@RequestParam String mode,
                       @RequestParam(required = false) String tableCode,
                       @RequestParam(required = false) String orderNo,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) throws IOException {
        if (!"detail".equalsIgnoreCase(mode) && !"summary".equalsIgnoreCase(mode)) {
            throw BizException.badRequest("mode 仅支持 detail|summary");
        }
        // 规范化参数（避免大小写/空格导致匹配失败）
        tableCode = tableCode == null ? null : tableCode.trim();
        orderNo = orderNo == null ? null : orderNo.trim();
        status = status == null ? null : status.trim().toUpperCase();

        LocalDateTime st = parseTime(startTime);
        LocalDateTime et = parseTime(endTime);

        // tableCode -> tableId 列表
        Map<Long, String> tableIdToCode = new HashMap<>();
        if (tableCode != null && !tableCode.isBlank()) {
            List<DiningTableEntity> tables = tableMapper.selectList(new LambdaQueryWrapper<DiningTableEntity>()
                    .like(DiningTableEntity::getCode, tableCode));
            // 若输入了桌台筛选但找不到桌台，导出应为空（与列表查询保持一致）
            if (tables.isEmpty()) {
                tables = List.of();
                // 直接走空结果，避免忽略 tableCode 条件
                writeEmpty(mode, response);
                return;
            }
            for (DiningTableEntity t : tables) tableIdToCode.put(t.getId(), t.getCode());
        }

        LambdaQueryWrapper<ConsumeOrderEntity> qw = new LambdaQueryWrapper<ConsumeOrderEntity>()
                .like(orderNo != null && !orderNo.isBlank(), ConsumeOrderEntity::getOrderNo, orderNo)
                .eq(status != null && !status.isBlank(), ConsumeOrderEntity::getStatus, status)
                .ge(st != null, ConsumeOrderEntity::getOpenedAt, st)
                .le(et != null, ConsumeOrderEntity::getOpenedAt, et)
                .orderByDesc(ConsumeOrderEntity::getId);
        if (!tableIdToCode.isEmpty()) {
            qw.in(ConsumeOrderEntity::getTableId, tableIdToCode.keySet());
        }

        List<ConsumeOrderEntity> orders = orderMapper.selectList(qw);
        if (orders.isEmpty()) orders = List.of();
        log.info("ledger export mode={}, status={}, orderNo={}, tableCode={}, st={}, et={}, orders={}",
                mode, status, orderNo, tableCode, st, et, orders.size());

        Set<Long> tableIds = new HashSet<>();
        List<Long> orderIds = new ArrayList<>();
        for (ConsumeOrderEntity o : orders) {
            tableIds.add(o.getTableId());
            orderIds.add(o.getId());
        }
        if (!tableIds.isEmpty() && tableIdToCode.isEmpty()) {
            for (DiningTableEntity t : tableMapper.selectBatchIds(tableIds)) {
                tableIdToCode.put(t.getId(), t.getCode());
            }
        }

        String filename = "ledger-" + mode + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        if ("summary".equalsIgnoreCase(mode)) {
            List<SummaryRow> rows = new ArrayList<>();
            for (ConsumeOrderEntity o : orders) {
                SummaryRow r = new SummaryRow();
                r.setOrderNo(o.getOrderNo());
                r.setTableCode(tableIdToCode.getOrDefault(o.getTableId(), ""));
                r.setStatus(statusLabel(o.getStatus()));
                r.setOpenedAt(o.getOpenedAt() == null ? null : DTF.format(o.getOpenedAt()));
                r.setClosedAt(o.getClosedAt() == null ? null : DTF.format(o.getClosedAt()));
                r.setAmountBeforeDiscount(money(o.getAmountBeforeDiscount()));
                r.setDiscountAmount(money(o.getDiscountAmount()));
                r.setAmountAfterDiscount(money(o.getAmountAfterDiscount()));
                r.setDiscountReason(o.getDiscountReason());
                rows.add(r);
            }
            log.info("ledger export summary rows={}", rows.size());
            EasyExcel.write(response.getOutputStream(), SummaryRow.class)
                    .sheet("台账汇总")
                    .doWrite(rows);
            response.flushBuffer();
        } else {
            List<ConsumeOrderItemEntity> items = orderIds.isEmpty() ? List.of() : itemMapper.selectList(
                    new LambdaQueryWrapper<ConsumeOrderItemEntity>().in(ConsumeOrderItemEntity::getOrderId, orderIds)
                            .orderByAsc(ConsumeOrderItemEntity::getOrderId).orderByAsc(ConsumeOrderItemEntity::getId)
            );
            Map<Long, ConsumeOrderEntity> orderById = new HashMap<>();
            for (ConsumeOrderEntity o : orders) orderById.put(o.getId(), o);

            List<DetailRow> rows = new ArrayList<>();
            for (ConsumeOrderItemEntity it : items) {
                ConsumeOrderEntity o = orderById.get(it.getOrderId());
                if (o == null) continue;
                DetailRow r = new DetailRow();
                r.setOrderNo(o.getOrderNo());
                r.setTableCode(tableIdToCode.getOrDefault(o.getTableId(), ""));
                r.setStatus(statusLabel(o.getStatus()));
                r.setOpenedAt(o.getOpenedAt() == null ? null : DTF.format(o.getOpenedAt()));
                r.setClosedAt(o.getClosedAt() == null ? null : DTF.format(o.getClosedAt()));
                r.setDishName(it.getDishNameSnapshot());
                r.setUnitPrice(money(it.getUnitPriceSnapshot()));
                r.setQuantity(it.getQuantity());
                r.setLineAmount(money(it.getLineAmount()));
                r.setAmountBeforeDiscount(money(o.getAmountBeforeDiscount()));
                r.setDiscountAmount(money(o.getDiscountAmount()));
                r.setAmountAfterDiscount(money(o.getAmountAfterDiscount()));
                rows.add(r);
            }
            log.info("ledger export detail rows={}", rows.size());
            EasyExcel.write(response.getOutputStream(), DetailRow.class)
                    .sheet("台账明细")
                    .doWrite(rows);
            response.flushBuffer();
        }
    }

    private static String money(BigDecimal v) {
        if (v == null) return "0.00";
        // 统一两位小数，避免科学计数法
        return v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String statusLabel(String s) {
        if (s == null) return "";
        return switch (s) {
            case "OPEN" -> "进行中";
            case "CLOSED" -> "已结账";
            case "CANCELLED" -> "已取消";
            default -> s;
        };
    }

    private static void writeEmpty(String mode, HttpServletResponse response) throws IOException {
        if ("summary".equalsIgnoreCase(mode)) {
            EasyExcel.write(response.getOutputStream(), SummaryRow.class)
                    .sheet("台账汇总")
                    .doWrite(List.of());
        } else {
            EasyExcel.write(response.getOutputStream(), DetailRow.class)
                    .sheet("台账明细")
                    .doWrite(List.of());
        }
    }

    private static LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim().replace('T', ' ');
        if (v.length() == 10) v = v + " 00:00:00";
        return LocalDateTime.parse(v.replace(' ', 'T'));
    }
}

