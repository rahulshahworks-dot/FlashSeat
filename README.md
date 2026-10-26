# FlashSeat

> A high-concurrency, zero-over-engineering flash-ticketing engine built with Java 21, Spring Boot, Redis, and PostgreSQL.

FlashSeat handles sudden traffic spikes during flash sales and concert ticket bookings. It shields downstream databases using an in-memory dynamic waiting room and eliminates double-booking race conditions via atomic Redis Lua scripts.

---

## Architecture Highlights

1. **Dynamic Gatekeeper & Virtual Waiting Room**
   - **Sliding Counter:** Redis `INCR` window monitors incoming traffic density in real-time.
   - **FIFO Queue:** Bursts above threshold are automatically diverted into a Redis Sorted Set (`ZSET`) using timestamps as scores.
   - **Live Progress Updates:** Delivers real-time position/rank updates to clients using Server-Sent Events (`SseEmitter`).
   - **Controlled Admissions:** A scheduled background worker admits queued users in fixed batches, generating single-use cryptographic admission tokens.

2. **Atomic Multi-Seat Reservation**
   - Eliminates partial-hold anomalies and double-booking using a single atomic Redis Lua script (`batch_lock.lua`).
   - All requested seats are verified and locked atomically in-memory with a 5-minute TTL before touching disk.

3. **Hybrid State & Data Integrity**
   - **Permanent Truth:** Final confirmed bookings persist in PostgreSQL.
   - **Volatile Truth:** Active holds exist solely as Redis TTL keys, auto-expiring without complex background sync jobs.
   - **Edge Validation:** Physical layout verification filters out ghost/invalid seat inputs prior to acquiring distributed locks.

4. **Java 21 Virtual Threads Verification**
   - Validated using an internal load simulator powered by Project Loom's `Executors.newVirtualThreadPerTaskExecutor()`, reproducing real-world burst concurrency.

---

## Tech Stack

- **Language:** Java 21 (Virtual Threads / Project Loom)
- **Framework:** Spring Boot 3.3.4 (Spring Data JPA, Spring Web)
- **Caching & Locks:** Redis (Sorted Sets, Atomic Lua scripting)
- **Database:** PostgreSQL 16
- **Tooling:** Docker Compose, Maven, JUnit 5

---

## System Workflow
```text
[Incoming Request]
        │
        ▼
[Traffic Monitor (Redis INCR Window)]
   ├── Normal Traffic  ──> [Direct Admission Token]
   └── Burst Spike     ──> [ZSet Waiting Room] ──(SSE Stream)──> [Admitted in Batches]
                                                                          │
                                                                          ▼
                                                         [Seat Validation (Physical DB Check)]
                                                                          │
                                                                          ▼
                                                         [Atomic Multi-Seat Lock (Redis Lua)]
                                                                          │
                                                                          ▼
                                                         [Persistent Booking (PostgreSQL ACID)]
```
