# University Library Application (Kotlin)

A concurrent library management application written in Kotlin.

The project implements a simple system for managing books, users, and borrowed books in a university library. Data is persisted in text files using XML and JSON serialization.

## Features

- Book catalog stored in **XML**
- Library state stored in **JSON**
- User registration
- Borrowing and returning books
- Tracking borrowed books and return deadlines
- Overdue book notifications via email
- Thread-safe operations using **actor model and Kotlin coroutines**

## Architecture

The application consists of three main parts:

### 1. Serialization Layer

`LibrarySerializer` is responsible for converting domain objects to and from text formats.

- **XML** for the book catalog
- **JSON** for users and borrowed books

Libraries used:

- `kotlinx.serialization`
- `xmlutil`

Supported operations:

- encode/decode `BookCatalog` (XML)
- encode/decode `LibraryState` (JSON)

---

### 2. File Storage

`FileLibraryStorage` implements persistent storage using text files.

Data files:

- `books.xml` — catalog of books
- `state.json` — users and borrowed books

Responsibilities:

- loading data from files
- saving state after each modification
- managing users and borrowed books
- validating borrowing rules

Key operations:

- `allBooks()` – list all books
- `allowedBooks()` – list books available for borrowing
- `borrowBook()` – borrow a book for 7 days
- `returnBook()` – return a book
- `createUser()` – register a new user

---

### 3. Concurrent Application Layer

`LibraryApplication` provides a thread-safe interface for working with the storage.

Concurrency is implemented using:

- **Kotlin coroutines**
- **Actor model**
- **Flow-based message processing**

All operations are represented as messages (`LibraryMsg`) and processed sequentially in a single coroutine.

This guarantees consistent state even when multiple clients access the library concurrently.

Supported operations:

- retrieving books
- creating users
- borrowing and returning books
- sending overdue notifications

---

## Overdue Notifications

The system can send email notifications to users who have overdue books.

Email sending is handled through an `EmailSender` interface.

---

## Technologies

- **Kotlin**
- **Kotlin Coroutines**
- **Flow**
- **kotlinx.serialization**
- **xmlutil**
- **Gradle**

## Project Structure
 library
 ├── api                # domain models
 ├── data               # serialization and file storage
 ├── notifications      # email sending interface
 └── LibraryApplication # concurrent application layer
