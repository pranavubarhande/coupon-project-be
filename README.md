# Coupon Management API

## Prerequisites

- Java 18
- Maven 3.6+

## Features

- CRUD operations for coupons
- Different types of coupons (Cart-wise, Product-wise, BXGY)
- Coupon validation and application
- Cart total calculation with applied discounts
- Coupon expiration dates (optional, inclusive of the given date)

## API Documentation

Once the application is running, you can access:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- API Docs: <http://localhost:8080/api-docs>

### Recommended Sequence to Exercise the APIs

1. Start the app
   - `mvn spring-boot:run`
   - Base URL: `http://localhost:8080/api`
   - Swagger UI: `http://localhost:8080/swagger-ui`
2. Create a few coupons (cart-wise, product-wise, bxgy)
3. List coupons and note their IDs
4. Check applicable coupons for a sample cart
5. Apply a specific coupon to the cart using its ID
6. Optionally update/delete a coupon

Note: When creating or updating a coupon, you can optionally provide an `expiry_date` (YYYY-MM-DD). If set, the coupon is valid through that date; after that it won’t appear in applicable results and cannot be applied.

## Database

The application uses H2 in-memory database.

- H2 Console: <http://localhost:8080/h2-console>
- JDBC URL: <jdbc:h2:mem:coupondb>
- Username: sa
- Password: (empty)

### Curl Commands

Replace `<ID>` with the coupon ID you created.

Create a cart-wise coupon (10% off over 100):

```bash
curl -sS -X POST http://localhost:8080/api/coupons \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "CART_WISE",
    "name": "10% off over 100",
    "details": { "threshold": 100, "discount": 10 }
  }'
```

Create a product-wise coupon (20% off product 1):

```bash
curl -sS -X POST http://localhost:8080/api/coupons \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "PRODUCT_WISE",
    "name": "20% off Product 1",
    "details": { "product_id": 1, "discount": 20 }
  }'
```

Create a BxGy coupon (Buy 3 of product 1 or 2, get 1 of product 3 free; limit 2):

```bash
curl -sS -X POST http://localhost:8080/api/coupons \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "BXGY",
    "name": "Buy 3 of 1 or 2, get 1 of 3",
    "details": {
      "buy_products": [
        { "product_id": 1, "quantity": 3 },
        { "product_id": 2, "quantity": 3 }
      ],
      "get_products": [ { "product_id": 3, "quantity": 1 } ],
      "repetition_limit": 2
    }
  }'
```

List all coupons:

```bash
curl -sS http://localhost:8080/api/coupons
```

Get coupon by ID:

```bash
curl -sS http://localhost:8080/api/coupons/<ID>
```

Update coupon by ID (example: rename):

```bash
curl -sS -X PUT http://localhost:8080/api/coupons/<ID> \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "CART_WISE",
    "name": "10% off over 100 (updated)",
    "details": { "threshold": 100, "discount": 10 }
  }'
```

Delete coupon by ID:

```bash
curl -sS -X DELETE http://localhost:8080/api/coupons/<ID> -i
```

Check applicable coupons for a cart:

```bash
curl -sS -X POST http://localhost:8080/api/applicable-coupons \
  -H 'Content-Type: application/json' \
  -d '{
    "cart": {
      "items": [
        { "product_id": 1, "quantity": 6, "price": 50 },
        { "product_id": 2, "quantity": 3, "price": 30 },
        { "product_id": 3, "quantity": 2, "price": 25 }
      ]
    }
  }'
```

Apply a specific coupon by ID to the cart:

```bash
curl -sS -X POST http://localhost:8080/api/apply-coupon/<ID> \
  -H 'Content-Type: application/json' \
  -d '{
    "cart": {
      "items": [
        { "product_id": 1, "quantity": 6, "price": 50 },
        { "product_id": 2, "quantity": 3, "price": 30 },
        { "product_id": 3, "quantity": 2, "price": 25 }
      ]
    }
  }'
```

### Implemented Cases

- Cart-wide percentage discount that kicks in once a (optional) spend threshold is crossed.
- Product-specific percentage discount targeting a single `product_id`.
- Buy X Get Y (BxGy) offers that support:
  - Multiple eligible “buy” products with required quantities.
  - Multiple “get” products and free quantities.
  - A repetition cap to limit how many times the deal applies.
- Automatic calculation of which coupons apply and a simple per-item discount breakdown when a coupon is applied.
- Optional coupon expiration date: expired coupons are hidden from applicable lists and cannot be applied.

### Unimplemented / Partially Implemented Cases (Design Considerations)

- Stacking multiple coupons in a single apply request. Today we only allow one coupon per apply call. Example: applying a cart-wide 10% and a product-wise 20% together would need stacking rules (order of application, etc.).
- Priority and conflict handling when more than one coupon could apply. If two coupons target the same items, we need a deterministic way to pick which one wins (e.g., highest discount, business priority, or user choice).
- Min/max discount caps, absolute (fixed-amount) discounts, and tiered discounts. For example, “10% off up to $50” (max cap) or “$30 flat off” (absolute) or “5% up to $100, then 10%” (tiered).
- Product exclusions for cart-wide coupons. Allow cart-wide promos to skip specific SKUs or categories, e.g., “10% off everything except gift cards.”
- BxGy variants like “cheapest item free” or granting free items not already in the cart. Current BxGy only discounts items that are already present in the cart.
- Coupon codes vs. auto-applied promos, per-user/global usage limits, and redemption tracking. Needed to control abuse and support marketing campaigns (e.g., first-time user only, max 1 use per user).
- Scheduling beyond a simple expiry date. Support start windows, time zones, blackout periods, and more complex calendars.
- Currency and tax configuration (tax-inclusive vs. tax-exclusive pricing). Real-world carts often require country-specific tax behavior and conversions.
- Inventory awareness, partial fulfillment rules, and returns/refunds adjustments. Discounts may need to adapt if items are out of stock or returned.

### Assumptions

- Each cart item includes `product_id`, `price`, and `quantity` in the request. The service does not look up a product catalog; it trusts the request payload(I know this is wrong, but implemented it as MVP).
- For BxGy, free quantities are only granted for `get_products` already present in the cart. We increase that line’s quantity to reflect the free items.
- Percentage inputs are whole numbers (e.g., `10` means 10%). No fractional percentages are expected.
- H2 in-memory database is used; data is reset on each app restart. But ideally, I would use a proper database.

### Limitations

- No product/catalog persistence. We do not store product data; callers must provide prices and quantities in each request, which may allow inconsistent pricing across calls.
- Monetary rounding uses 2 decimals with HALF_UP. Some businesses prefer different rounding strategies, which can change totals slightly.
- No authentication or authorization. Any client can call the endpoints in this demo setup.
- Basic error handling only. We return validation messages and not-found errors, but there’s no standardized error code scheme yet.
- No scope for currency, taxes, and shipping interactions. Real order totals often depend on these; they’re intentionally omitted here to keep the focus on coupon logic.

### Data Model Overview (as used by the API)

- `Coupon` is stored with: `id`, `name`, `type` (one of `CART_WISE`, `PRODUCT_WISE`, `BXGY`), `detailsJson` (the typed details saved as JSON), and optional `expiryDate`.
- `CouponDtos.Details` carries the inputs needed for each type:
  - Cart-wise: `threshold` (optional minimum spend) and `discount` (percent).
  - Product-wise: `product_id` (target item) and `discount` (percent).
  - BxGy: `buy_products[]` and `get_products[]` (product+quantity pairs) and an optional `repetition_limit`.
- `CartDto` represents the incoming cart: `items[]` with `product_id`, `quantity`, and `price`. The service computes totals and discounts from this payload.

### Error Handling

- `404` when a coupon is not found.
- Validation errors for invalid payloads (e.g., missing required fields, negative quantities, etc.).

### Testing Notes

- Unit tests are added in `CouponServiceTest`.

### Extensibility

- To add a new coupon type, extend `CouponType`, enrich `CouponDtos.Details` with necessary fields, and update switch statements in `CouponService` for applicability and application logic.
