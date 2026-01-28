package com.catording.rcms.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.modules.common.OrderNoGenerator;
import com.catording.rcms.modules.discount.entity.DiscountRuleEntity;
import com.catording.rcms.modules.discount.mapper.DiscountRuleMapper;
import com.catording.rcms.modules.discount.service.DiscountService;
import com.catording.rcms.modules.dish.entity.DishEntity;
import com.catording.rcms.modules.dish.mapper.DishMapper;
import com.catording.rcms.modules.order.entity.ConsumeOrderEntity;
import com.catording.rcms.modules.order.entity.ConsumeOrderItemEntity;
import com.catording.rcms.modules.order.mapper.ConsumeOrderItemMapper;
import com.catording.rcms.modules.order.mapper.ConsumeOrderMapper;
import com.catording.rcms.modules.payment.entity.PaymentEntity;
import com.catording.rcms.modules.payment.mapper.PaymentMapper;
import com.catording.rcms.modules.table.entity.DiningTableEntity;
import com.catording.rcms.modules.table.mapper.DiningTableMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private final DiningTableMapper tableMapper;
    private final ConsumeOrderMapper orderMapper;
    private final ConsumeOrderItemMapper itemMapper;
    private final DishMapper dishMapper;
    private final DiscountRuleMapper discountRuleMapper;
    private final DiscountService discountService;
    private final PaymentMapper paymentMapper;
    private final OrderNoGenerator orderNoGenerator;

    public OrderService(DiningTableMapper tableMapper,
                        ConsumeOrderMapper orderMapper,
                        ConsumeOrderItemMapper itemMapper,
                        DishMapper dishMapper,
                        DiscountRuleMapper discountRuleMapper,
                        DiscountService discountService,
                        PaymentMapper paymentMapper,
                        OrderNoGenerator orderNoGenerator) {
        this.tableMapper = tableMapper;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.dishMapper = dishMapper;
        this.discountRuleMapper = discountRuleMapper;
        this.discountService = discountService;
        this.paymentMapper = paymentMapper;
        this.orderNoGenerator = orderNoGenerator;
    }

    @Transactional
    public ConsumeOrderEntity openTable(long tableId, long operatorId) {
        DiningTableEntity t = tableMapper.selectById(tableId);
        if (t == null) throw BizException.badRequest("桌台不存在");
        if ("OCCUPIED".equalsIgnoreCase(t.getStatus())) throw BizException.badRequest("桌台已占用，禁止二次开台");

        // 生成订单号并插入，防止因为重启等原因与历史订单号冲突
        ConsumeOrderEntity order = null;
        for (int i = 0; i < 10; i++) {
            order = new ConsumeOrderEntity();
            order.setOrderNo(orderNoGenerator.next());
            order.setTableId(tableId);
            order.setOperatorId(operatorId);
            order.setStatus("OPEN");
            order.setOpenedAt(LocalDateTime.now());
            order.setAmountBeforeDiscount(BigDecimal.ZERO);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setAmountAfterDiscount(BigDecimal.ZERO);
            try {
                orderMapper.insert(order);
                break;
            } catch (DuplicateKeyException ex) {
                // 订单号重复，重试生成
                if (i == 9) {
                    throw BizException.badRequest("生成订单号失败，请稍后重试");
                }
            }
        }

        // 通过 version 乐观更新避免并发开台
        boolean ok = tableMapper.update(null, new LambdaUpdateWrapper<DiningTableEntity>()
                .set(DiningTableEntity::getStatus, "OCCUPIED")
                .set(DiningTableEntity::getCurrentOrderId, order.getId())
                .setSql("version = version + 1")
                .eq(DiningTableEntity::getId, tableId)
                .eq(DiningTableEntity::getStatus, "FREE")) > 0;
        if (!ok) {
            throw BizException.badRequest("开台失败：桌台状态已变化");
        }
        return orderMapper.selectById(order.getId());
    }

    @Transactional
    public void cancelOpen(long orderId) {
        ConsumeOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) throw BizException.badRequest("订单不存在");
        if (!"OPEN".equalsIgnoreCase(order.getStatus())) throw BizException.badRequest("仅 OPEN 状态可取消开台");

        // 删除明细
        itemMapper.delete(new LambdaQueryWrapper<ConsumeOrderItemEntity>().eq(ConsumeOrderItemEntity::getOrderId, orderId));

        order.setStatus("CANCELLED");
        order.setClosedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        DiningTableEntity t = tableMapper.selectById(order.getTableId());
        if (t != null) {
            t.setStatus("FREE");
            t.setCurrentOrderId(null);
            t.setVersion(t.getVersion() == null ? 1 : t.getVersion() + 1);
            tableMapper.updateById(t);
        }
    }

    @Transactional
    public Long addItem(long orderId, long dishId, int quantity) {
        if (quantity <= 0) throw BizException.badRequest("数量必须为正");
        ConsumeOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) throw BizException.badRequest("订单不存在");
        if (!"OPEN".equalsIgnoreCase(order.getStatus())) throw BizException.badRequest("订单非 OPEN，禁止操作");

        DishEntity dish = dishMapper.selectById(dishId);
        if (dish == null) throw BizException.badRequest("菜品不存在");
        if (!"ON".equalsIgnoreCase(dish.getStatus())) throw BizException.badRequest("菜品已下架");

        ConsumeOrderItemEntity item = new ConsumeOrderItemEntity();
        item.setOrderId(orderId);
        item.setDishId(dishId);
        item.setDishNameSnapshot(dish.getName());
        item.setUnitPriceSnapshot(dish.getPrice());
        item.setQuantity(quantity);
        item.setLineAmount(dish.getPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP));
        itemMapper.insert(item);

        recalcAmounts(orderId);
        return item.getId();
    }

    @Transactional
    public void updateItem(long itemId, int quantity) {
        if (quantity <= 0) throw BizException.badRequest("数量必须为正");
        ConsumeOrderItemEntity item = itemMapper.selectById(itemId);
        if (item == null) throw BizException.badRequest("明细不存在");
        ConsumeOrderEntity order = orderMapper.selectById(item.getOrderId());
        if (order == null || !"OPEN".equalsIgnoreCase(order.getStatus())) throw BizException.badRequest("订单非 OPEN，禁止操作");

        item.setQuantity(quantity);
        item.setLineAmount(item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP));
        itemMapper.updateById(item);

        recalcAmounts(item.getOrderId());
    }

    @Transactional
    public void deleteItem(long itemId) {
        ConsumeOrderItemEntity item = itemMapper.selectById(itemId);
        if (item == null) return;
        ConsumeOrderEntity order = orderMapper.selectById(item.getOrderId());
        if (order == null || !"OPEN".equalsIgnoreCase(order.getStatus())) throw BizException.badRequest("订单非 OPEN，禁止操作");
        itemMapper.deleteById(itemId);
        recalcAmounts(item.getOrderId());
    }

    @Transactional
    public ConsumeOrderEntity applyDiscount(long orderId, Long ruleId, BigDecimal manualDiscountAmount, String reason) {
        ConsumeOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) throw BizException.badRequest("订单不存在");
        if (!"OPEN".equalsIgnoreCase(order.getStatus())) throw BizException.badRequest("订单非 OPEN，禁止操作");

        BigDecimal base = safe(order.getAmountBeforeDiscount());
        BigDecimal discount = BigDecimal.ZERO;
        Long usedRuleId = null;

        if (ruleId != null) {
            DiscountRuleEntity rule = discountRuleMapper.selectById(ruleId);
            if (rule == null) throw BizException.badRequest("折扣规则不存在");
            if (rule.getEnabled() == null || rule.getEnabled() != 1) throw BizException.badRequest("折扣规则未启用");
            var r = discountService.calculate(base, rule);
            discount = r.discountAmount();
            usedRuleId = ruleId;
        }

        if (manualDiscountAmount != null) {
            if (reason == null || reason.isBlank()) throw BizException.badRequest("人工折扣必须填写原因");
            if (manualDiscountAmount.compareTo(BigDecimal.ZERO) < 0) throw BizException.badRequest("折扣金额不合法");
            discount = manualDiscountAmount;
            usedRuleId = ruleId; // 允许同时保留规则ID，便于留痕
        }

        if (discount.compareTo(base) > 0) throw BizException.badRequest("折扣不能大于应收金额");

        order.setDiscountRuleId(usedRuleId);
        order.setDiscountAmount(discount.setScale(2, RoundingMode.HALF_UP));
        order.setDiscountReason(reason);
        order.setAmountAfterDiscount(base.subtract(discount).setScale(2, RoundingMode.HALF_UP));
        orderMapper.updateById(order);
        return orderMapper.selectById(orderId);
    }

    @Transactional
    public void checkout(long orderId, String method, BigDecimal payAmount, long operatorId) {
        ConsumeOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) throw BizException.badRequest("订单不存在");
        if (!"OPEN".equalsIgnoreCase(order.getStatus())) throw BizException.badRequest("订单非 OPEN，禁止结账");

        List<ConsumeOrderItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<ConsumeOrderItemEntity>()
                .eq(ConsumeOrderItemEntity::getOrderId, orderId));
        if (items.isEmpty()) throw BizException.badRequest("明细为空，禁止结账");

        BigDecimal due = safe(order.getAmountAfterDiscount());
        if (payAmount == null || payAmount.compareTo(due) < 0) throw BizException.badRequest("支付金额不足");

        BigDecimal change = payAmount.subtract(due).setScale(2, RoundingMode.HALF_UP);

        PaymentEntity p = new PaymentEntity();
        p.setOrderId(orderId);
        p.setMethod(method);
        p.setPayAmount(payAmount.setScale(2, RoundingMode.HALF_UP));
        p.setChangeAmount(change);
        p.setPaidAt(LocalDateTime.now());
        p.setOperatorId(operatorId);
        paymentMapper.insert(p);

        order.setStatus("CLOSED");
        order.setClosedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        DiningTableEntity t = tableMapper.selectById(order.getTableId());
        if (t != null) {
            t.setStatus("FREE");
            t.setCurrentOrderId(null);
            t.setVersion(t.getVersion() == null ? 1 : t.getVersion() + 1);
            tableMapper.updateById(t);
        }
    }

    public record OrderDetail(ConsumeOrderEntity order, List<ConsumeOrderItemEntity> items, List<PaymentEntity> payments) {}

    public OrderDetail getOrderDetail(long orderId) {
        ConsumeOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) throw BizException.badRequest("订单不存在");
        List<ConsumeOrderItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<ConsumeOrderItemEntity>()
                .eq(ConsumeOrderItemEntity::getOrderId, orderId)
                .orderByAsc(ConsumeOrderItemEntity::getId));
        List<PaymentEntity> pays = paymentMapper.selectList(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getOrderId, orderId)
                .orderByAsc(PaymentEntity::getPaidAt));
        return new OrderDetail(order, items, pays);
    }

    @Transactional
    public void recalcAmounts(long orderId) {
        List<ConsumeOrderItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<ConsumeOrderItemEntity>()
                .eq(ConsumeOrderItemEntity::getOrderId, orderId));
        BigDecimal sum = items.stream()
                .map(i -> safe(i.getLineAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        ConsumeOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) return;

        order.setAmountBeforeDiscount(sum);
        // 若已有折扣，重算最终金额（保持折扣金额不变；若超出应收则截断）
        BigDecimal discount = safe(order.getDiscountAmount());
        if (discount.compareTo(sum) > 0) discount = sum;
        order.setDiscountAmount(discount.setScale(2, RoundingMode.HALF_UP));
        order.setAmountAfterDiscount(sum.subtract(discount).setScale(2, RoundingMode.HALF_UP));
        orderMapper.updateById(order);
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

