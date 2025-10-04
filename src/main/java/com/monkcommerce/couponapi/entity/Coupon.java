package com.monkcommerce.couponapi.entity;

import com.monkcommerce.couponapi.model.CouponType;
import jakarta.persistence.*;

@Entity
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;

    @Lob
    @Column(nullable = false)
    private String detailsJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CouponType getType() { return type; }
    public void setType(CouponType type) { this.type = type; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
}


