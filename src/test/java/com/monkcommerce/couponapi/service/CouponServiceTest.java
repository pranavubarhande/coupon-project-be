package com.monkcommerce.couponapi.service;

import com.monkcommerce.couponapi.dto.CartDto;
import com.monkcommerce.couponapi.dto.CouponDtos;
import com.monkcommerce.couponapi.entity.Coupon;
import com.monkcommerce.couponapi.model.CouponType;
import com.monkcommerce.couponapi.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    void setup() {
        couponRepository.deleteAll();
    }

    private CartDto sampleCart() {
        CartDto cart = new CartDto();
        CartDto.CartItemDto i1 = new CartDto.CartItemDto();
        i1.setProduct_id(1L);
        i1.setQuantity(6);
        i1.setPrice(new BigDecimal("50"));
        CartDto.CartItemDto i2 = new CartDto.CartItemDto();
        i2.setProduct_id(2L);
        i2.setQuantity(3);
        i2.setPrice(new BigDecimal("30"));
        CartDto.CartItemDto i3 = new CartDto.CartItemDto();
        i3.setProduct_id(3L);
        i3.setQuantity(2);
        i3.setPrice(new BigDecimal("25"));
        cart.setItems(List.of(i1, i2, i3));
        return cart;
    }

    private Coupon createCartWise(BigDecimal threshold, BigDecimal discount, LocalDate expiry) {
        CouponDtos.CreateCouponRequest req = new CouponDtos.CreateCouponRequest();
        req.setType(CouponType.CART_WISE);
        req.setName("CartWise Test");
        CouponDtos.Details details = new CouponDtos.Details();
        details.setThreshold(threshold);
        details.setDiscount(discount);
        req.setDetails(details);
        req.setExpiry_date(expiry);
        return couponService.create(req);
    }

    private Coupon createProductWise(long productId, BigDecimal discount, LocalDate expiry) {
        CouponDtos.CreateCouponRequest req = new CouponDtos.CreateCouponRequest();
        req.setType(CouponType.PRODUCT_WISE);
        req.setName("ProductWise Test");
        CouponDtos.Details details = new CouponDtos.Details();
        details.setProduct_id(productId);
        details.setDiscount(discount);
        req.setDetails(details);
        req.setExpiry_date(expiry);
        return couponService.create(req);
    }

    @Test
    void cartWiseDiscountAppliesWhenOverThreshold() {
        createCartWise(new BigDecimal("100"), new BigDecimal("10"), LocalDate.now().plusDays(1));
        List<Map<String, Object>> applicable = couponService.applicableCoupons(sampleCart());
        assertFalse(applicable.isEmpty(), "Expected at least one applicable coupon");
        BigDecimal discount = (BigDecimal) applicable.get(0).get("discount");
        // Cart total: 6*50 + 3*30 + 2*25 = 300 + 90 + 50 = 440; 10% => 44.00
        assertEquals(new BigDecimal("44.00"), discount);
    }

    @Test
    void expiredCouponIsExcludedFromApplicable() {
        createCartWise(new BigDecimal("100"), new BigDecimal("10"), LocalDate.now().minusDays(1));
        List<Map<String, Object>> applicable = couponService.applicableCoupons(sampleCart());
        assertTrue(applicable.isEmpty(), "Expired coupons should not be listed as applicable");
    }

    @Test
    void applyingExpiredCouponThrows() {
        Coupon c = createProductWise(1L, new BigDecimal("20"), LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> couponService.applyCoupon(c.getId(), sampleCart()));
    }
}
