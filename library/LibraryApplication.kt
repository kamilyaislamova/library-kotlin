package library

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import library.api.Book
import library.api.BorrowedBook
import library.api.User
import library.data.LibraryStorage
import library.notifications.EmailSender

sealed class LibraryMsg<T : Any> {
    open val response: CompletableDeferred<T> = CompletableDeferred()

    data class GetAllBooks(override val response: CompletableDeferred<List<Book>>) : LibraryMsg<List<Book>>()
    data class GetAllowedBooks(override val response: CompletableDeferred<List<Book>>) : LibraryMsg<List<Book>>()
    data class GetBorrowedBooksInfo(
        override val response: CompletableDeferred<List<BorrowedBook>>,
    ) : LibraryMsg<List<BorrowedBook>>()
    data class CreateUser(
        val email: String,
        val name: String,
        override val response: CompletableDeferred<User>,
    ) : LibraryMsg<User>()
    data class FindUser(val userId: String, override val response: CompletableDeferred<User>) : LibraryMsg<User>()
    data class BorrowBook(
        val bookId: String,
        val userId: String,
        override val response: CompletableDeferred<Instant>,
    ) : LibraryMsg<Instant>()
    data class ReturnBook(val bookId: String, val userId: String) : LibraryMsg<Any>()
    data object SendOverdueBooksNotification : LibraryMsg<Any>()
}

class LibraryApplication(
    private val storage: LibraryStorage,
    private val emailSender: EmailSender,
) : LibraryStorage {

    private val updatesFlow = MutableSharedFlow<LibraryMsg<*>>(replay = 10)

    private suspend fun<T : Any> general(clas: LibraryMsg<T>): T {
        updatesFlow.emit(clas)
        return clas.response.await()
    }

    override suspend fun allBooks(): List<Book> = general(LibraryMsg.GetAllBooks(response = CompletableDeferred()))

    override suspend fun allowedBooks(): List<Book> = general(
        LibraryMsg.GetAllowedBooks(response = CompletableDeferred()),
    )

    override suspend fun borrowedBooksInfo(): List<BorrowedBook> = general(
        LibraryMsg.GetBorrowedBooksInfo(response = CompletableDeferred()),
    )

    override suspend fun borrowBook(bookId: String, userId: String): Instant = general(
        LibraryMsg.BorrowBook(bookId, userId, response = CompletableDeferred()),
    )

    override suspend fun returnBook(bookId: String, userId: String) = updatesFlow.emit(
        LibraryMsg.ReturnBook(bookId, userId),
    )

    override suspend fun createUser(email: String, name: String): User = general(
        LibraryMsg.CreateUser(email, name, response = CompletableDeferred()),
    )

    override suspend fun findUser(userId: String): User = general(
        LibraryMsg.FindUser(userId, response = CompletableDeferred()),
    )

    suspend fun sendOverdueBooksNotification() {
        val overdueBooks = borrowedBooksInfo().filter { it.returnDeadline <= Clock.System.now() }
        for (overdueBook in overdueBooks) {
            val user = storage.findUser(overdueBook.userId)
            emailSender.send(
                to = user.email,
                subject = "Overdue Book Notification",
                text = "The book with ID '${overdueBook.bookId}' is overdue. Please return it as soon as possible.",
            )
        }
    }

    suspend fun run() {
        updatesFlow.collect { msg ->
            try {
                when (msg) {
                    is LibraryMsg.GetAllBooks -> msg.response.complete(storage.allBooks())
                    is LibraryMsg.GetAllowedBooks -> msg.response.complete(storage.allowedBooks())
                    is LibraryMsg.GetBorrowedBooksInfo -> msg.response.complete(storage.borrowedBooksInfo())
                    is LibraryMsg.CreateUser -> msg.response.complete(storage.createUser(msg.email, msg.name))
                    is LibraryMsg.FindUser -> msg.response.complete(storage.findUser(msg.userId))
                    is LibraryMsg.BorrowBook -> msg.response.complete(storage.borrowBook(msg.bookId, msg.userId))
                    is LibraryMsg.ReturnBook -> storage.returnBook(msg.bookId, msg.userId)
                    is LibraryMsg.SendOverdueBooksNotification -> sendOverdueBooksNotification()
                }
            } catch (e: IllegalArgumentException) {
                msg.response.completeExceptionally(e)
            }
        }
    }
}
