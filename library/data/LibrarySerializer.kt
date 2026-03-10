package library.data

import java.lang.System.lineSeparator
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import library.api.BookCatalog
import library.api.LibraryState
import nl.adaptivity.xmlutil.serialization.XML

fun String.normalizeXml(): String = replace("\n", lineSeparator())

object LibrarySerializer {

    private val xml = XML {
        indentString = "    "
    }

    fun decodeCatalog(string: String): BookCatalog = xml.decodeFromString<BookCatalog>(string)

    fun encodeCatalog(st: BookCatalog): String = xml.encodeToString(st).normalizeXml()

    fun decodeState(string: String): LibraryState = Json.decodeFromString<LibraryState>(string)

    fun encodeState(state: LibraryState): String = Json.encodeToString(state)
}
