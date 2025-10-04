package com.monkcommerce.couponapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monkcommerce.couponapi.entity.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, Long> { }


