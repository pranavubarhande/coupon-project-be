package com.monkcommerce.couponapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monkcommerce.couponapi.dto.CartDto;
import com.monkcommerce.couponapi.dto.CouponDtos;
import com.monkcommerce.couponapi.entity.Coupon;
import com.monkcommerce.couponapi.model.CouponType;
import com.monkcommerce.couponapi.repository.CouponRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    public Coupon create(CouponDtos.CreateCouponRequest req) {
        Coupon coupon = new Coupon();
        coupon.setName(req.getName());
        coupon.setType(req.getType());
        coupon.setDetailsJson(writeJson(req.getDetails()));
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon update(Long id, CouponDtos.UpdateCouponRequest req) {
        Coupon coupon = couponRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Coupon not found"));
        coupon.setName(req.getName());
        coupon.setType(req.getType());
        coupon.setDetailsJson(writeJson(req.getDetails()));
        return couponRepository.save(coupon);
    }

    public List<Coupon> list() { return couponRepository.findAll(); }

    public Coupon get(Long id) { return couponRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Coupon not found")); }

    public void delete(Long id) { couponRepository.deleteById(id); }

    private String writeJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    private CouponDtos.Details readDetails(Coupon coupon) {
        try { return objectMapper.readValue(coupon.getDetailsJson(), CouponDtos.Details.class); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    public BigDecimal cartTotal(CartDto cart) {
        return cart.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Map<String, Object>> applicableCoupons(CartDto cart) {
        BigDecimal total = cartTotal(cart);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Coupon coupon : couponRepository.findAll()) {
            CouponDtos.Details d = readDetails(coupon);
            BigDecimal discount = switch (coupon.getType()) {
                case CART_WISE -> applicableCartWise(total, d);
                case PRODUCT_WISE -> applicableProductWise(cart, d);
                case BXGY -> applicableBxGy(cart, d);
            };
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("coupon_id", coupon.getId());
                map.put("type", coupon.getType().name().toLowerCase());
                map.put("discount", discount);
                result.add(map);
            }
        }
        return result;
    }

    private BigDecimal applicableCartWise(BigDecimal total, CouponDtos.Details d) {
        if (d.getThreshold() == null || d.getDiscount() == null) return BigDecimal.ZERO;
        if (total.compareTo(d.getThreshold()) <= 0) return BigDecimal.ZERO;
        return total.multiply(d.getDiscount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal applicableProductWise(CartDto cart, CouponDtos.Details d) {
        if (d.getProduct_id() == null || d.getDiscount() == null) return BigDecimal.ZERO;
        return cart.getItems().stream()
                .filter(i -> i.getProduct_id().equals(d.getProduct_id()))
                .findFirst()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                        .multiply(d.getDiscount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal applicableBxGy(CartDto cart, CouponDtos.Details d) {
        if (d.getBuy_products() == null || d.getGet_products() == null) return BigDecimal.ZERO;
        int repetitions = computeBxGyRepetitions(cart, d);
        if (d.getRepetition_limit() != null) repetitions = Math.min(repetitions, d.getRepetition_limit());
        if (repetitions <= 0) return BigDecimal.ZERO;
        BigDecimal freeValue = BigDecimal.ZERO;
        Map<Long, CartDto.CartItemDto> cartMap = new HashMap<>();
        for (CartDto.CartItemDto item : cart.getItems()) cartMap.put(item.getProduct_id(), item);
        for (CouponDtos.BuyGet get : d.getGet_products()) {
            CartDto.CartItemDto item = cartMap.get(get.getProduct_id());
            if (item != null) {
                int freeQty = get.getQuantity() * repetitions;
                BigDecimal itemValue = item.getPrice().multiply(BigDecimal.valueOf(freeQty));
                freeValue = freeValue.add(itemValue);
            }
        }
        return freeValue;
    }

    private int computeBxGyRepetitions(CartDto cart, CouponDtos.Details d) {
        Map<Long, Integer> counts = new HashMap<>();
        for (CartDto.CartItemDto item : cart.getItems()) counts.put(item.getProduct_id(), item.getQuantity());
        int times = Integer.MAX_VALUE;
        if (d.getBuy_products() == null || d.getBuy_products().isEmpty()) return 0;
        for (CouponDtos.BuyGet buy : d.getBuy_products()) {
            int available = counts.getOrDefault(buy.getProduct_id(), 0);
            times = Math.min(times, available / buy.getQuantity());
        }
        if (times == Integer.MAX_VALUE) return 0;
        return times;
    }

    public Map<String, Object> applyCoupon(Long id, CartDto cart) {
        Coupon coupon = get(id);
        CouponDtos.Details d = readDetails(coupon);
        Map<String, Object> response = new LinkedHashMap<>();

        BigDecimal totalPrice = cartTotal(cart);
        BigDecimal totalDiscount = switch (coupon.getType()) {
            case CART_WISE -> applicableCartWise(totalPrice, d);
            case PRODUCT_WISE -> applicableProductWise(cart, d);
            case BXGY -> applicableBxGy(cart, d);
        };

        List<Map<String, Object>> items = new ArrayList<>();
        for (CartDto.CartItemDto i : cart.getItems()) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("product_id", i.getProduct_id());
            it.put("quantity", i.getQuantity());
            it.put("price", i.getPrice());
            BigDecimal itemDiscount = BigDecimal.ZERO;
            if (coupon.getType() == CouponType.PRODUCT_WISE && Objects.equals(d.getProduct_id(), i.getProduct_id())) {
                itemDiscount = i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                        .multiply(d.getDiscount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            if (coupon.getType() == CouponType.BXGY) {
                int reps = computeBxGyRepetitions(cart, d);
                if (d.getRepetition_limit() != null) reps = Math.min(reps, d.getRepetition_limit());
                for (CouponDtos.BuyGet get : Optional.ofNullable(d.getGet_products()).orElse(List.of())) {
                    if (Objects.equals(get.getProduct_id(), i.getProduct_id())) {
                        int freeQty = get.getQuantity() * Math.max(reps, 0);
                        itemDiscount = itemDiscount.add(i.getPrice().multiply(BigDecimal.valueOf(freeQty)));
                        it.put("quantity", i.getQuantity() + freeQty);
                    }
                }
            }
            it.put("total_discount", itemDiscount);
            items.add(it);
        }

        response.put("updated_cart", Map.of(
                "items", items,
                "total_price", totalPrice,
                "total_discount", totalDiscount,
                "final_price", totalPrice.subtract(totalDiscount)
        ));
        return response;
    }
}


