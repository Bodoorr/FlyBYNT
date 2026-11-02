# FlyBYNT

Flight booking microservices: auth, flight, seat, passenger and booking, sharing one JWT.

| Service | Port |
|---|---|
| flight-service | 8081 |
| seat-service | 8082 |
| booking-service | 8083 |
| passenger-service | 8084 |
| auth-service | 8085 |
| MongoDB (Docker) | 27018 on the host |

## Run everything with Docker

1. Create `.env` in the project root (it is gitignored):
   ```
   cp .env.example .env
   ```
   and set `AIRLABS_API_KEY=<your key>`.
2. Start:
   ```
   docker compose up -d --build
   ```
3. Logs: `docker compose logs -f booking-service`. Stop: `docker compose down` (add `-v` to also wipe the database).

## Try it

```bash
# 1. register and log in; the token is in the "message" field
curl -X POST localhost:8085/auth/users/register -H "Content-Type: application/json" \
  -d '{"firstName":"Ali","lastName":"Hasan","email":"ali@example.com","password":"Passw0rd!"}'
curl -X POST localhost:8085/auth/users/login -H "Content-Type: application/json" \
  -d '{"email":"ali@example.com","password":"Passw0rd!"}'
TOKEN=<token>

# 2. once per fresh database: create the seat map for aircraft A320-1
curl -X POST localhost:8082/aircraft/A320-1/seats/generate -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"sections":[{"seatClass":"BUSINESS","fromRow":1,"toRow":3,"letters":"ACDF","windowLetters":"AF","aisleLetters":"CD"},{"seatClass":"ECONOMY","fromRow":4,"toRow":30,"letters":"ABCDEF","windowLetters":"AF","aisleLetters":"CD"}]}'

# 3. search flights (uses one AirLabs request), create a passenger
curl "localhost:8081/flights?departure=BAH" -H "Authorization: Bearer $TOKEN"
curl -X POST localhost:8084/passengers -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"firstName":"Ali","lastName":"Hasan","passportNumber":"P1234567","dateOfBirth":"1995-05-05","nationality":"BH"}'

# 4. book (seatId = a seat's id from step 2), then cancel
curl -X POST localhost:8083/bookings -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"flightId":"<flight id>","passengerId":"<passenger id>","seatId":"<seat id>","idempotencyKey":"any-unique-string"}'
curl -X POST localhost:8083/bookings/<booking id>/cancel -H "Authorization: Bearer $TOKEN"
```

Every endpoint except register/login needs `Authorization: Bearer <token>`.

## Run without Docker (IntelliJ)

Start MongoDB on `localhost:27017`, set `AIRLABS_API_KEY` in flight-service's run configuration, and run each `*Application` class. All other settings default to localhost.
