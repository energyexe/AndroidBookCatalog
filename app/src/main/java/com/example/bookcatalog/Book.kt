package com.example.bookcatalog

data class Book(
    val id: Int,
    val title: String,
    val author: String,
    val year: Int,
    val pages: Int,
    val price: Double,
    val genre: String
) : Comparable<Book> {

    // перегрузка операторов <, >, <=, >=
    override fun compareTo(other: Book): Int = this.price.compareTo(other.price)

    // перегрузка оператора +
    operator fun plus(other: Book): Book {
        val avgPrice = (this.price + other.price) / 2.0
        val totalPages = this.pages + other.pages
        return this.copy(
            pages = totalPages,
            price = avgPrice
        )
    }

    // строка для поиска по всем полям
    fun toSearchString(): String {
        return "$title $author $genre $year $pages $price $id"
    }
}