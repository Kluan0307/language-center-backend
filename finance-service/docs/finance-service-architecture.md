> Historical reference. See [current contracts](../../docs/api-contracts.md) and [decisions](../../docs/implementation-decisions.md).

# Finance Service Architecture

## Overview

Finance service is a Spring Boot microservice responsible for managing tuition, payment, expense, promotion, invoice, and financial reporting workflows. The service follows the same architecture pattern as academic-service: package-by-domain, shared common utilities, and JWT-based secure access.

## Module Structure

- `common`: shared exception and pagination helpers
- `security`: JWT validation and branch scope enforcement
- `tuition`: tuition records and collection management
- `payment`: payment transactions and payment states
- `expense`: operating and financial expenses
- `promotion`: discount campaigns and branch-specific promotion rules
- `invoice`: invoice generation and status updates

## Security Model

- All endpoints except root are protected by JWT
- Token claims expected: `sub`, `role`, `accountId`, `profileId`, `branchId`
- `SUPER_ADMIN` can access all branches
- non-admin roles are scoped to their own branch

## Base URL

`/api/v1/finance`

## Domain model

### Tuition
- `studentId`
- `classId`
- `amount`
- `currency`
- `dueDate`
- `status`
- `branchId`

### Payment
- `tuitionId`
- `amount`
- `paymentMethod`
- `referenceCode`
- `paidAt`
- `status`
- `branchId`

### Expense
- `category`
- `title`
- `amount`
- `currency`
- `branchId`
- `description`
- `status`

### Promotion
- `code`
- `name`
- `discountType`
- `discountValue`
- `startDate`
- `endDate`
- `branchId`
- `status`

### Invoice
- `studentId`
- `paymentId`
- `invoiceNumber`
- `totalAmount`
- `status`
- `branchId`

## Notes

The current application is still a functional skeleton and is structured to support business expansion without violating the service boundaries already established in the platform.
