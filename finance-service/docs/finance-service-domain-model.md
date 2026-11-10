> Historical reference. See [current contracts](../../docs/api-contracts.md) and [decisions](../../docs/implementation-decisions.md).

# Finance Service Domain Model

## Core Entities

### Tuition
- id: Long
- studentId: Long
- classId: Long
- amount: BigDecimal
- currency: String
- dueDate: LocalDate
- status: PENDING | PARTIAL | PAID | OVERDUE | CANCELLED
- branchId: Long

### Payment
- id: Long
- tuitionId: Long
- amount: BigDecimal
- paymentMethod: CASH | BANK_TRANSFER | CARD | WALLET
- referenceCode: String
- paidAt: LocalDateTime
- status: PENDING | PAID | FAILED | REFUNDED | PARTIAL
- branchId: Long

### Expense
- id: Long
- category: OPERATING_COST | TEACHER_SALARY | MARKETING | UTILITY | ASSET
- title: String
- amount: BigDecimal
- currency: String
- branchId: Long
- description: String
- status: PENDING | APPROVED | REJECTED | PAID

### Promotion
- id: Long
- code: String
- name: String
- discountType: PERCENT | FIXED_AMOUNT
- discountValue: Integer
- startDate: LocalDate
- endDate: LocalDate
- branchId: Long
- status: ACTIVE | INACTIVE | EXPIRED

### Invoice
- id: Long
- studentId: Long
- paymentId: Long
- invoiceNumber: String
- totalAmount: BigDecimal
- status: PAID | PENDING | CANCELLED
- branchId: Long

## Relationships

- Tuition connects to payment and invoice flows.
- Payment belongs to one tuition and one branch.
- Expense is attached to one branch for operational accounting.
- Promotion is branch-scoped and time-bounded.
- Invoice summarizes a finalized payment into a printable document.
