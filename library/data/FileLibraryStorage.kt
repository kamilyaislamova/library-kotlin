package library.data

import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import library.api.*
import library.data.LibrarySerializer.decodeCatalog
import library.data.LibrarySerializer.decodeState
import library.data.LibrarySerializer.encodeCatalog
import library.data.LibrarySerializer.encodeState

class FileLibraryStorage(storagePath: Path) : LibraryStorage {

    private val booksFilePath: Path = storagePath.resolve("books.xml")
    private val stateFilePath: Path = storagePath.resolve("state.json")

    private var catalog: BookCatalog = loadCatalog()
    private var state: LibraryState = loadState()

    override suspend fun allBooks(): List<Book> = catalog.books

    override suspend fun allowedBooks(): List<Book> {
        return catalog.books.filter { book ->
            state.borrowedBooks.none { it.bookId == book.id }
        }
    }

    override suspend fun borrowedBooksInfo(): List<BorrowedBook> = state.borrowedBooks

    override suspend fun borrowBook(bookId: String, userId: String): Instant {
        val returnDeadline = Clock.System.now() + 7.days
        require(
            state.borrowedBooks.find { it.bookId == bookId } == null,
        ) { "this book has been already taken: $bookId" }
        require(catalog.books.find { it.id == bookId } != null) { "book with this id $bookId doesn't exist" }
        val borrowedBook = BorrowedBook(bookId, userId, returnDeadline)
        state.borrowedBooks += borrowedBook
        saveState()
        return returnDeadline
    }

    override suspend fun returnBook(bookId: String, userId: String) {
        state.borrowedBooks = state.borrowedBooks.filterNot { it.bookId == bookId && it.userId == userId }
        saveState()
    }

    override suspend fun createUser(email: String, name: String): User {
        val userId = UUID.randomUUID().toString()
        if (state.users.find { it.email == email } != null) {
            throw IllegalArgumentException("there is an user with the same email already: $email")
        } else {
            val user = User(userId, email, name)
            state.users += user
            saveState()
            return user
        }
    }

    override suspend fun findUser(userId: String): User {
        val user: User? = state.users.find { it.id == userId }
        if (user == null) {
            throw IllegalArgumentException("there is no user with this id: $userId")
        }
        return user
    }

    private fun loadCatalog(): BookCatalog {
        return if (Files.exists(booksFilePath)) {
            val xmlContent = Files.readString(booksFilePath)
            decodeCatalog(xmlContent)
        } else {
            BookCatalog(books = emptyList())
        }
    }

    private fun saveCatalog() {
        val xmlContent = encodeCatalog(catalog)
        Files.writeString(booksFilePath, xmlContent)
    }

    private fun loadState(): LibraryState {
        return if (Files.exists(stateFilePath)) {
            val jsonContent = Files.readString(stateFilePath)
            decodeState(jsonContent)
        } else {
            LibraryState(users = emptyList(), borrowedBooks = emptyList())
        }
    }

    private fun saveState() {
        val jsonContent = encodeState(state)
        Files.writeString(stateFilePath, jsonContent)
    }
}
