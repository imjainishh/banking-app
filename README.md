# 🏦 Banking Application

A full-stack banking application built with Spring Boot and React.

## Features
- User registration and login with JWT authentication
- Open Savings and Current bank accounts
- Deposit, withdraw, and transfer money between accounts
- Full transaction history with timestamps
- BCrypt password hashing
- Secure endpoints — users can only access their own accounts

## Tech Stack

**Backend**
- Java 18
- Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA
- H2 Database (development)
- JUnit 5 + Mockito (13 tests)

**Frontend**
- React 18
- Vite
- React Router
- Axios

## Architecture
src/

model/       — JPA entities (User, BankAccount, Transaction)

repository/  — Spring Data repositories

service/     — Business logic (@Transactional)

controller/  — REST endpoints

security/    — JWT filter + Spring Security config

exception/   — Custom exceptions + global handler

dto/         — Request/response objects

## API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /api/auth/register | Register new user | Public |
| POST | /api/auth/login | Login + get JWT | Public |
| POST | /api/accounts | Open new account | Required |
| GET | /api/accounts | List my accounts | Required |
| GET | /api/accounts/{id}/balance | Get balance | Required |
| GET | /api/accounts/{id}/transactions | Transaction history | Required |
| POST | /api/transactions/deposit | Deposit money | Required |
| POST | /api/transactions/withdraw | Withdraw money | Required |
| POST | /api/transactions/transfer | Transfer between accounts | Required |

## How to Run Locally

### Backend
```bash
cd banking-app
./mvnw spring-boot:run
```
Server starts at `http://localhost:8080`

H2 console available at `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:bankingdb`
- Username: `sa`
- Password: (blank)

### Frontend
```bash
cd frontend
npm install
npm run dev
```
App opens at `http://localhost:5173`

## Screenshots

### Login
![Login page]

### Dashboard
![Dashboard with account cards]

### Transaction History
![Transaction history]

## Tests
```bash
cd banking-app
./mvnw test
```
13 tests — unit tests with Mockito + integration tests with MockMvc