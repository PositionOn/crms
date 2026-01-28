package com.catording.rcms.modules.discount.service;

import com.catording.rcms.common.exception.BizException;
import com.catording.rcms.modules.discount.entity.DiscountRuleEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DiscountService {
    private final ObjectMapper objectMapper;

    public DiscountService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record DiscountResult(BigDecimal discountAmount, BigDecimal finalAmount) {}

    public DiscountResult calculate(BigDecimal orderAmount, DiscountRuleEntity rule) {
        if (orderAmount == null) orderAmount = BigDecimal.ZERO;
        if (orderAmount.compareTo(BigDecimal.ZERO) < 0) throw BizException.badRequest("金额不合法");
        if (rule == null) return new DiscountResult(BigDecimal.ZERO, orderAmount);

        BigDecimal discount = BigDecimal.ZERO;
        try {
            JsonNode n = objectMapper.readTree(rule.getParamsJson());
            switch (rule.getType()) {
                case "FIXED_RATE" -> {
                    BigDecimal rate = n.get("rate").decimalValue();
                    if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) > 0) {
                        throw BizException.badRequest("折扣率不合法");
                    }
                    discount = orderAmount.multiply(BigDecimal.ONE.subtract(rate));
                }
                case "FULL_REDUCTION" -> {
                    BigDecimal threshold = n.get("threshold").decimalValue();
                    BigDecimal minus = n.get("minus").decimalValue();
                    if (orderAmount.compareTo(threshold) >= 0) discount = minus;
                }
                case "MEMBER" -> {
                    BigDecimal rate = n.get("rate").decimalValue();
                    discount = orderAmount.multiply(BigDecimal.ONE.subtract(rate));
                }
                default -> throw BizException.badRequest("未知折扣类型");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.badRequest("折扣参数 JSON 不合法");
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;
        if (discount.compareTo(orderAmount) > 0) discount = orderAmount;
        discount = discount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = orderAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        return new DiscountResult(discount, finalAmount);
    }
}

