package com.example.lostandfound.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lostandfound.R
import com.example.lostandfound.db.Item
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ItemAdapter(
    private var items: List<Item>,
    private val onClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.VH>() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txt_row_title)
        val meta: TextView = view.findViewById(R.id.txt_row_meta)
    }

    fun submit(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = holder.itemView.context
            .getString(R.string.row_title_format, item.postType, item.name)
        holder.meta.text = holder.itemView.context.getString(
            R.string.row_meta_format,
            item.category,
            sdf.format(Date(item.createdAt))
        )
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
