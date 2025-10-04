package com.monkcommerce.couponapi.controller;

import com.monkcommerce.couponapi.dto.CartDto;
import com.monkcommerce.couponapi.dto.CouponDtos;
import com.monkcommerce.couponapi.entity.Coupon;
import com.monkcommerce.couponapi.service.CouponService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Coupons", description = "Manage and apply coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/coupons")
    @Operation(summary = "Create a new coupon")
    public ResponseEntity<Coupon> create(@Valid @RequestBody CouponDtos.CreateCouponRequest req) {
        Coupon c = couponService.create(req);
        return ResponseEntity.created(URI.create("/api/coupons/" + c.getId())).body(c);
    }

    @GetMapping("/coupons")
    @Operation(summary = "List all coupons")
    public List<Coupon> list() { return couponService.list(); }

    @GetMapping("/coupons/{id}")
    @Operation(summary = "Get coupon by id")
    public Coupon get(@PathVariable Long id) { return couponService.get(id); }

    @PutMapping("/coupons/{id}")
    @Operation(summary = "Update coupon by id")
    public Coupon update(@PathVariable Long id, @Valid @RequestBody CouponDtos.UpdateCouponRequest req) {
        return couponService.update(id, req);
    }

    @DeleteMapping("/coupons/{id}")
    @Operation(summary = "Delete coupon by id")
    public void delete(@PathVariable Long id) { couponService.delete(id); }

    @PostMapping("/applicable-coupons")
    @Operation(summary = "Fetch applicable coupons and discounts for given cart")
    public Map<String, Object> applicable(@Valid @RequestBody Map<String, CartDto> request) {
        CartDto cart = request.get("cart");
        List<Map<String, Object>> list = couponService.applicableCoupons(cart);
        Map<String, Object> resp = new HashMap<>();
        resp.put("applicable_coupons", list);
        return resp;
    }

    @PostMapping("/apply-coupon/{id}")
    @Operation(summary = "Apply a specific coupon to the cart")
    public Map<String, Object> apply(@PathVariable Long id, @Valid @RequestBody Map<String, CartDto> request) {
        CartDto cart = request.get("cart");
        return couponService.applyCoupon(id, cart);
    }
}


