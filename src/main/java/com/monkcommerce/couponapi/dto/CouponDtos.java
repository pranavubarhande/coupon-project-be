package com.monkcommerce.couponapi.dto;

import com.monkcommerce.couponapi.model.CouponType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CouponDtos {

    public static class CreateCouponRequest {
        @NotNull
        private CouponType type;
        @NotBlank
        private String name;
        @NotNull
        private Details details;

        public CouponType getType() { return type; }
        public void setType(CouponType type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Details getDetails() { return details; }
        public void setDetails(Details details) { this.details = details; }
    }

    public static class UpdateCouponRequest extends CreateCouponRequest { }

    public static class Details {
        // For cart-wise
        private BigDecimal threshold; // optional
        private BigDecimal discount;  // percent for cart-wise and product-wise

        // For product-wise
        private Long product_id; // optional

        // For BxGy
        private List<BuyGet> buy_products;
        private List<BuyGet> get_products;
        private Integer repetition_limit;

        public BigDecimal getThreshold() { return threshold; }
        public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
        public BigDecimal getDiscount() { return discount; }
        public void setDiscount(BigDecimal discount) { this.discount = discount; }
        public Long getProduct_id() { return product_id; }
        public void setProduct_id(Long product_id) { this.product_id = product_id; }
        public List<BuyGet> getBuy_products() { return buy_products; }
        public void setBuy_products(List<BuyGet> buy_products) { this.buy_products = buy_products; }
        public List<BuyGet> getGet_products() { return get_products; }
        public void setGet_products(List<BuyGet> get_products) { this.get_products = get_products; }
        public Integer getRepetition_limit() { return repetition_limit; }
        public void setRepetition_limit(Integer repetition_limit) { this.repetition_limit = repetition_limit; }
    }

    public static class BuyGet {
        @NotNull
        private Long product_id;
        @Min(1)
        private int quantity;

        public Long getProduct_id() { return product_id; }
        public void setProduct_id(Long product_id) { this.product_id = product_id; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class CouponResponse {
        private Long id;
        private String name;
        private CouponType type;
        private Details details;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public CouponType getType() { return type; }
        public void setType(CouponType type) { this.type = type; }
        public Details getDetails() { return details; }
        public void setDetails(Details details) { this.details = details; }
    }
}


