# Finance Service API Contract

## Authentication

Every request should send:

```http
Authorization: Bearer <jwt>
```

## Base path

```text
/api/v1/finance
```

## Tuition

### GET /api/v1/finance/tuition
List tuition records.

### GET /api/v1/finance/tuition/{id}
Get one tuition item.

### POST /api/v1/finance/tuition
Create tuition.

### PUT /api/v1/finance/tuition/{id}
Update tuition.

### PATCH /api/v1/finance/tuition/{id}/status
Update tuition status.

## Payment

### GET /api/v1/finance/payments
List payments.

### GET /api/v1/finance/payments/{id}
Get one payment.

### POST /api/v1/finance/payments
Create payment.

### PUT /api/v1/finance/payments/{id}
Update payment.

### PATCH /api/v1/finance/payments/{id}/status
Update payment status.

## Expense

### GET /api/v1/finance/expenses
List expenses.

### GET /api/v1/finance/expenses/{id}
Get one expense.

### POST /api/v1/finance/expenses
Create expense.

### PUT /api/v1/finance/expenses/{id}
Update expense.

### PATCH /api/v1/finance/expenses/{id}/status
Update expense status.

## Promotion

### GET /api/v1/finance/promotions
List promotions.

### GET /api/v1/finance/promotions/{id}
Get one promotion.

### POST /api/v1/finance/promotions
Create promotion.

### PUT /api/v1/finance/promotions/{id}
Update promotion.

### PATCH /api/v1/finance/promotions/{id}/status
Update promotion status.

## Invoice

### GET /api/v1/finance/invoices
List invoices.

### GET /api/v1/finance/invoices/{id}
Get one invoice.

### POST /api/v1/finance/invoices
Create invoice.

### PATCH /api/v1/finance/invoices/{id}/status
Update invoice status.
