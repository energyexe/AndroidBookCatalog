package com.example.bookcatalog

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import android.content.Intent
import com.example.bookcatalog.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: BookRepository
    private lateinit var adapter: BookAdapter
    private var books: MutableList<Book> = mutableListOf()
    private var filteredBooks: MutableList<Book> = mutableListOf()
    private var currentQuery: String = ""
    private val exportCsvLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let { saveCsvToUri(it) }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        repository = BookRepository(this)
        books = repository.loadBooks().toMutableList()
        if (books.isEmpty()) {
            books.addAll(SampleData.createSampleBooks())
            repository.saveBooks(books)
        }
        filteredBooks = books.toMutableList()

        adapter = BookAdapter(filteredBooks) { book ->
            showEditDialog(book)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator?.apply {
            addDuration = 200      // появление
            removeDuration = 200   // удаление
            changeDuration = 150   // обновление элемента
            moveDuration = 150     // сортировка + перестановка
        }

        binding.fabAdd.setOnClickListener {
            showAddDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.queryHint = getString(R.string.search_hint)
        searchView?.isSubmitButtonEnabled = false

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterBooks(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty()
                filterBooks(currentQuery)
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_title -> {
                books.sortBy { it.title.lowercase() }
                applyCurrentFilter()
                true
            }
            R.id.action_sort_author -> {
                books.sortBy { it.author.lowercase() }
                applyCurrentFilter()
                true
            }
            R.id.action_sort_year -> {
                books.sortBy { it.year }
                applyCurrentFilter()
                true
            }
            R.id.action_sort_price -> {
                books.sortBy { it.price }
                applyCurrentFilter()
                true
            }
            R.id.action_export -> {
                exportCsvLauncher.launch("каталог_книг.csv")
                return true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            R.id.action_exit -> {
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun filterBooks(query: String) {
        val search = query.trim().lowercase()

        filteredBooks.clear()

        if (search.isEmpty()) {
            filteredBooks.addAll(books)
        } else {
            filteredBooks.addAll(
                books.filter { book ->
                    book.title.lowercase().contains(search) ||
                            book.author.lowercase().contains(search) ||
                            book.genre.lowercase().contains(search) ||
                            book.year.toString().contains(search) ||
                            book.pages.toString().contains(search) ||
                            book.price.toString().contains(search)
                }
            )
        }
        adapter.notifyDataSetChanged()
    }

    private fun applyCurrentFilter() {
        filterBooks(currentQuery)
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_book, null)
        val titleInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputTitle)
        val authorInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputAuthor)
        val yearInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputYear)
        val pagesInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputPages)
        val priceInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputPrice)
        val genreInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputGenre)

        AlertDialog.Builder(this)
            .setTitle(R.string.add_book)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val title = titleInput.text?.toString().orEmpty()
                val author = authorInput.text?.toString().orEmpty()
                val genre = genreInput.text?.toString().orEmpty()
                val yearText = yearInput.text?.toString().orEmpty()
                val pagesText = pagesInput.text?.toString().orEmpty()
                val priceText = priceInput.text?.toString().orEmpty()

                val errorMessages = mutableListOf<String>()

                val year = yearText.toIntOrNull()?.takeIf { it in 1564..2025 } ?: run {
                    errorMessages.add(getString(R.string.error_year))
                    null
                }
                val pages = pagesText.toIntOrNull()?.takeIf { it in 1..100100} ?: run {
                    errorMessages.add(getString(R.string.error_pages))
                    null
                }
                val price = priceText.toDoubleOrNull()?.takeIf { it in 0.0..2385768000.0 } ?: run {
                    errorMessages.add(getString(R.string.error_price))
                    null
                }

                if (title.isBlank()) errorMessages.add(getString(R.string.error_title))
                if (author.isBlank()) errorMessages.add(getString(R.string.error_author))
                if (genre.isBlank()) errorMessages.add(getString(R.string.error_genre))

                if (errorMessages.isNotEmpty() || year == null || pages == null || price == null) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.error_title_common)
                        .setMessage(errorMessages.joinToString("\n"))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                } else {
                    val newId = (books.maxOfOrNull { it.id } ?: 0) + 1
                    val book = Book(
                        id = newId,
                        title = title,
                        author = author,
                        year = year,
                        pages = pages,
                        price = price,
                        genre = genre
                    )
                    books.add(book)
                    applyCurrentFilter()
                    repository.saveBooks(books)
                }

                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showEditDialog(book: Book) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_book, null)

        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.editTitle)
        val authorInput = dialogView.findViewById<TextInputEditText>(R.id.editAuthor)
        val genreInput = dialogView.findViewById<TextInputEditText>(R.id.editGenre)
        val yearInput = dialogView.findViewById<TextInputEditText>(R.id.editYear)
        val pagesInput = dialogView.findViewById<TextInputEditText>(R.id.editPages)
        val priceInput = dialogView.findViewById<TextInputEditText>(R.id.editPrice)

        // заполняем текущими данными
        titleInput.setText(book.title)
        authorInput.setText(book.author)
        genreInput.setText(book.genre)
        yearInput.setText(book.year.toString())
        pagesInput.setText(book.pages.toString())
        priceInput.setText(book.price.toString())

        AlertDialog.Builder(this)
            .setTitle("Редактировать книгу")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, _ ->

                val title = titleInput.text?.toString().orEmpty()
                val author = authorInput.text?.toString().orEmpty()
                val genre = genreInput.text?.toString().orEmpty()
                val yearText = yearInput.text?.toString().orEmpty()
                val pagesText = pagesInput.text?.toString().orEmpty()
                val priceText = priceInput.text?.toString().orEmpty()

                val errorMessages = mutableListOf<String>()

                val year = yearText.toIntOrNull()?.takeIf { it in 1564..2025 } ?: run {
                    errorMessages.add(getString(R.string.error_year))
                    null
                }
                val pages = pagesText.toIntOrNull()?.takeIf { it in 1..100100 } ?: run {
                    errorMessages.add(getString(R.string.error_pages))
                    null
                }
                val price = priceText.toDoubleOrNull()?.takeIf { it in 0.0..2385768000.0 } ?: run {
                    errorMessages.add(getString(R.string.error_price))
                    null
                }

                if (title.isBlank()) errorMessages.add(getString(R.string.error_title))
                if (author.isBlank()) errorMessages.add(getString(R.string.error_author))
                if (genre.isBlank()) errorMessages.add(getString(R.string.error_genre))

                if (errorMessages.isNotEmpty() || year == null || pages == null || price == null) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.error_title_common)
                        .setMessage(errorMessages.joinToString("\n"))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                } else {
                    val updated = book.copy(
                        title = title,
                        author = author,
                        genre = genre,
                        year = year,
                        pages = pages,
                        price = price
                    )

                    val index = books.indexOfFirst { it.id == book.id }
                    if (index != -1) {
                        books[index] = updated
                    }
                    repository.saveBooks(books)
                    applyCurrentFilter()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Удалить") { _, _ ->
                books.removeAll { it.id == book.id }
                repository.saveBooks(books)
                applyCurrentFilter()
            }
            .show()
    }

    private fun saveCsvToUri(uri: android.net.Uri) {
        try {
            val csvContent = buildCsvFromBooks()

            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            Toast.makeText(this, "CSV успешно экспортирован", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    private fun buildCsvFromBooks(): String {
        val header = "id;title;author;genre;year;pages;price"

        val lines = books.map { book ->
            listOf(
                book.id,
                book.title,
                book.author,
                book.genre,
                book.year,
                book.pages,
                book.price
            ).joinToString(";")
        }

        return (listOf(header) + lines).joinToString("\n")
    }
}
