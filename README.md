# Coupon Management API

## Prerequisites

- Java 17
- Maven 3.6+

## Features

- CRUD operations for coupons
- Different types of coupons (Cart-wise, Product-wise, BXGY)
- Coupon validation and application
- Cart total calculation with applied discounts

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

- Cart-wise percentage discount over an optional threshold.
- Product-wise percentage discount for a specific `product_id`.
- BxGy deals with:
  - Multiple eligible "buy" products with required quantities.
  - Multiple "get" products with free quantities.
  - Repetition limit applied to the number of times the pattern can be fulfilled.
- Computation of applicable coupons and per-item breakdown when applying a coupon.

### Unimplemented / Partially Implemented Cases (Design Considerations)

- Stackability/combination of multiple coupons in one apply call (currently only one coupon is applied at a time).
- Priority/conflict resolution across coupons when multiple apply.
- Category/brand-based constraints; user-segment constraints; channel constraints.
- Min/max discount caps, absolute amount discounts, and tiered discounts.
- Product exclusions within cart-wise coupons.
- BxGy with cheapest-item free among a set, or choosing from products not present in the cart (current logic only discounts items already in cart as free additions in quantity).
- Coupon codes vs. auto-applied promotions; usage limits per user/global; redemption tracking.
- Coupon scheduling: start/end time windows; time zones; advanced expiry semantics beyond a simple date.
- Currency handling and tax-inclusive/exclusive price handling.
- Inventory awareness, partial fulfillment, and returns/refunds adjustments.

### Assumptions

- Prices in the cart are pre-tax and use the same currency.
- Cart items are identified by `product_id` and carry `price` and `quantity` at request time.
- For BxGy, free items are calculated only for `get_products` that are present in the cart; their quantity is increased accordingly in the response.
- Percentages are provided as whole numbers (e.g., 10 means 10%).
- H2 in-memory database is used; data resets on each app restart.

### Limitations

- No persistence of products/catalog; cart is a transient request payload.
- Floating/rounding uses 2 decimals with HALF_UP; may differ from business expectations.
- No authentication/authorization.
- Error handling is basic; validation messages and not-found errors are provided, but no error codes taxonomy.
- Currency, taxes, shipping, and coupons interaction with them are out of scope.

### Data Model Overview (as used by the API)

- `Coupon` entity persisted with fields: `id`, `name`, `type` (enum: `CART_WISE`, `PRODUCT_WISE`, `BXGY`), `detailsJson` (serialized form of `CouponDtos.Details`).
- `CouponDtos.Details` supports:
  - `threshold`, `discount` for cart-wise.
  - `product_id`, `discount` for product-wise.
  - `buy_products[]`, `get_products[]`, `repetition_limit` for BxGy.
- `CartDto` contains `items[]` with `product_id`, `quantity`, `price`.

### Error Handling

- `404` when a coupon is not found.
- Validation errors for invalid payloads (e.g., missing required fields, negative quantities, etc.).
- `POST /api/applicable-coupons` returns only coupons with positive computed discounts.

### Testing Notes

- Unit tests can be added around `CouponService` methods:
  - `applicableCartWise`, `applicableProductWise`, `applicableBxGy` for correctness and edge cases.
  - `computeBxGyRepetitions` for repetition calculations and limit application.

### Extensibility

- To add a new coupon type, extend `CouponType`, enrich `CouponDtos.Details` with necessary fields, and update switch statements in `CouponService` for applicability and application logic.
