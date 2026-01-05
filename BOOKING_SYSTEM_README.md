# Slot-Based Booking System

## Overview

A robust slot-based booking system with:
- **Hourly slots** (9 AM to 7 PM)
- **Maximum 2 bookings per slot**
- **Race condition safe** with pessimistic locking
- **Payment integration** with temporary reservations
- **Automatic cleanup** of expired bookings

## Features

### ✅ Core Features
- Slot management per branch/city and date
- Booking reservation with 10-minute timeout
- Payment confirmation flow
- Automatic expiration of pending bookings
- JWT-based authentication
- Transactional safety with pessimistic locking

### ✅ Concurrency Handling
- **Pessimistic locking** (`FOR UPDATE`) on slot rows
- **Transactional booking** prevents overbooking
- **Database-level constraints** ensure data integrity

## Database Setup

### 1. Run Booking Tables SQL
```bash
psql -U neeravjain -d inventory_db -f database/create_booking_tables.sql
```

### 2. Create Slots for Testing
Use the `SlotService` to create slots for a date range:

```java
@Autowired
private SlotService slotService;

// Create slots for next 30 days
slotService.createSlotsForDateRange(branchId, LocalDate.now(), LocalDate.now().plusDays(30));
```

Or create a REST endpoint to trigger slot creation.

## API Endpoints

### Authentication Required
All booking endpoints require JWT token in header:
```
Authorization: Bearer <your_jwt_token>
```

### 1. Get Available Slots
**GET** `/api/bookings/slots?branchId={id}&slotDate={date}`

**Query Parameters:**
- `branchId` (required): Branch ID
- `slotDate` (required): Date in format `YYYY-MM-DD`

**Response:**
```json
[
  {
    "id": 1,
    "branchId": 1,
    "branchName": "Downtown Branch",
    "city": "New York",
    "slotDate": "2024-12-25",
    "startTime": "09:00:00",
    "endTime": "10:00:00",
    "maxCapacity": 2,
    "bookedCount": 1,
    "availableCount": 1,
    "isAvailable": true
  }
]
```

### 2. Reserve Slot
**POST** `/api/bookings/reserve`

**Request Body:**
```json
{
  "branchId": 1,
  "slotDate": "2024-12-25",
  "startTime": "09:00:00",
  "endTime": "10:00:00"
}
```

**Response:**
```json
{
  "bookingId": 1,
  "paymentUrl": "/payment/1",
  "expiresAt": "2024-12-25T10:10:00",
  "message": "Slot reserved. Please complete payment within 10 minutes."
}
```

**Status Codes:**
- `201 Created`: Slot reserved successfully
- `404 Not Found`: Branch or slot not found
- `409 Conflict`: Slot is full
- `401 Unauthorized`: Invalid or missing JWT token

### 3. Confirm Payment
**POST** `/api/bookings/confirm-payment`

**Request Body:**
```json
{
  "bookingId": 1,
  "paymentId": "pay_123456789",
  "paymentSuccess": true
}
```

**Response:**
```json
{
  "id": 1,
  "userId": 1,
  "slotId": 1,
  "status": "CONFIRMED",
  "paymentId": "pay_123456789",
  "reservedAt": "2024-12-25T10:00:00",
  "expiresAt": "2024-12-25T10:10:00",
  "slotDate": "2024-12-25",
  "startTime": "09:00:00",
  "endTime": "10:00:00",
  "branchName": "Downtown Branch",
  "city": "New York"
}
```

**Status Codes:**
- `200 OK`: Payment confirmed
- `400 Bad Request`: Booking expired or invalid status
- `403 Forbidden`: Booking doesn't belong to user
- `404 Not Found`: Booking not found

### 4. Get My Bookings
**GET** `/api/bookings/my-bookings`

**Response:**
```json
[
  {
    "id": 1,
    "userId": 1,
    "slotId": 1,
    "status": "CONFIRMED",
    "paymentId": "pay_123456789",
    "slotDate": "2024-12-25",
    "startTime": "09:00:00",
    "endTime": "10:00:00",
    "branchName": "Downtown Branch",
    "city": "New York"
  }
]
```

## Booking Flow

### Step-by-Step Process

1. **User selects slot**
   - Frontend calls `GET /api/bookings/slots` to show availability
   - User sees available slots with capacity

2. **User clicks "Proceed to Payment"**
   - Frontend calls `POST /api/bookings/reserve`
   - Backend:
     - Starts transaction
     - Locks slot row (`FOR UPDATE`)
     - Checks `booked_count < max_capacity`
     - If available: increments `booked_count`, creates `PENDING` booking
     - If full: returns 409 Conflict

3. **User redirected to payment**
   - Booking is in `PENDING` status
   - Reservation expires in 10 minutes
   - Slot capacity is temporarily reserved

4. **Payment result**
   - **Success**: Update booking to `CONFIRMED`
   - **Failure**: Update booking to `EXPIRED`, release slot capacity

5. **Automatic cleanup**
   - Scheduled job runs every 2 minutes
   - Finds `PENDING` bookings older than 10 minutes
   - Marks them `EXPIRED` and releases slot capacity

## Concurrency Safety

### Pessimistic Locking
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM SlotEntity s WHERE s.id = :slotId")
Optional<SlotEntity> findByIdWithLock(@Param("slotId") Long slotId);
```

### Transactional Flow
```java
@Transactional
public ReserveSlotResponse reserveSlot(...) {
    // 1. Lock slot row
    SlotEntity slot = slotRepository.findByBranchDateAndTimeWithLock(...);
    
    // 2. Check availability (inside transaction)
    if (!slot.isAvailable()) {
        throw new Exception("Slot full");
    }
    
    // 3. Increment booked_count
    slot.setBookedCount(slot.getBookedCount() + 1);
    
    // 4. Create booking
    BookingEntity booking = new BookingEntity();
    booking.setStatus(PENDING);
    
    // All in one transaction - atomic operation
}
```

## Scheduled Cleanup

The system automatically cleans up expired bookings:

```java
@Scheduled(fixedRate = 120000) // Every 2 minutes
public void expirePendingBookings() {
    // Find PENDING bookings older than 10 minutes
    // Mark as EXPIRED
    // Release slot capacity
}
```

## Testing

### 1. Setup Database
```sql
-- Run create_booking_tables.sql
```

### 2. Create Test Data
```sql
-- Branches are auto-inserted in SQL script
-- Create slots using SlotService or manually
```

### 3. Test Flow
1. Login to get JWT token
2. Get available slots
3. Reserve a slot
4. Confirm payment (success/failure)
5. Check bookings

### Example cURL Commands

```bash
# 1. Login
TOKEN=$(curl -X POST http://localhost:2020/api/auth/signup-or-login \
  -H "Content-Type: application/json" \
  -d '{"isSignup":false,"email":"user@example.com","password":"pass"}' \
  | jq -r '.authToken')

# 2. Get slots
curl -X GET "http://localhost:2020/api/bookings/slots?branchId=1&slotDate=2024-12-25" \
  -H "Authorization: Bearer $TOKEN"

# 3. Reserve slot
curl -X POST http://localhost:2020/api/bookings/reserve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "branchId": 1,
    "slotDate": "2024-12-25",
    "startTime": "09:00:00",
    "endTime": "10:00:00"
  }'
```

## Database Schema

### Branches Table
- `id`, `name`, `city`, `address`, `is_active`
- Soft delete support

### Slots Table
- `id`, `branch_id`, `slot_date`, `start_time`, `end_time`
- `max_capacity` (default: 2), `booked_count`
- Unique constraint: `(branch_id, slot_date, start_time, end_time)`

### Bookings Table
- `id`, `user_id`, `slot_id`, `status`, `payment_id`
- `reserved_at`, `expires_at`
- Foreign keys to `users` and `slots`

## Important Notes

1. **Never rely on frontend checks** - All validation happens in backend transactions
2. **Pessimistic locking** ensures no overbooking
3. **10-minute reservation timeout** - PENDING bookings expire automatically
4. **Slot capacity** is released on payment failure or timeout
5. **JWT authentication** required for all booking endpoints

## Future Enhancements

- Payment gateway integration
- Email notifications
- Booking cancellation
- Waitlist for full slots
- Recurring bookings
- Admin dashboard

