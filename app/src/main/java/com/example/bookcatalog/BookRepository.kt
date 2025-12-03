package com.example.bookcatalog

import android.content.Context
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class BookRepository(private val context: Context) {

    private val fileName = "books.csv"
    private val header = "id;title;author;genre;year;pages;price"

    fun loadBooks(): MutableList<Book> {
        val file = File(context.filesDir, fileName)

        if (!file.exists()) {
            // если файла нет, то создаём пустой с заголовком
            file.writeText(header)
            return mutableListOf()
        }

        val books = mutableListOf<Book>()

        BufferedReader(file.reader()).use { reader ->
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(";")
                if (parts.size == 7) {
                    books.add(
                        Book(
                            id = parts[0].toInt(),
                            title = parts[1],
                            author = parts[2],
                            genre = parts[3],
                            year = parts[4].toInt(),
                            pages = parts[5].toInt(),
                            price = parts[6].toDouble()
                        )
                    )
                }
            }
        }
        return books
    }

    fun saveBooks(books: List<Book>) {
        val file = File(context.filesDir, fileName)
        BufferedWriter(FileWriter(file)).use { writer ->
            writer.write(header)
            writer.newLine()
            for (book in books) {
                val csvLine = listOf(
                    book.id.toString(),
                    book.title,
                    book.author,
                    book.genre,
                    book.year.toString(),
                    book.pages.toString(),
                    book.price.toString()
                ).joinToString(";")
                writer.write(csvLine)
                writer.newLine()
            }
        }
    }
}