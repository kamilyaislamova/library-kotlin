package library.api
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("BookCatalog", "", "")
data class BookCatalog(
    val books: List<Book>,
)
