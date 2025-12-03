package com.example.bookcatalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookcatalog.databinding.ItemBookBinding

class BookAdapter(
    private val books: List<Book>,
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]

        with(holder.binding) {
            textTitle.text = book.title
            textAuthor.text = book.author
            textGenre.text = book.genre
            textYear.text = "${book.year} г."
            textPages.text = "${book.pages} стр."
            textPrice.text = "${book.price} ₽"

            root.setOnClickListener { onBookClick(book) }
        }
    }

    override fun getItemCount(): Int = books.size
}
