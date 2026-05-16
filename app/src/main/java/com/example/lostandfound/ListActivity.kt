package com.example.lostandfound
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lostandfound.adapter.ItemAdapter
import com.example.lostandfound.databinding.ActivityListBinding
import com.example.lostandfound.db.DatabaseHelper
class ListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ItemAdapter
    private var currentCategory: String = "All"
    private var currentQuery: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = DatabaseHelper(this)
        adapter = ItemAdapter(emptyList()) { item ->
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_ID, item.id)
            )
        }
        binding.recyclerItems.layoutManager = LinearLayoutManager(this)
        binding.recyclerItems.adapter = adapter
        val categories = resources.getStringArray(R.array.categories_filter)
        binding.spinnerFilter.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, categories
        )
        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentCategory = categories[pos]
                refresh()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                currentQuery = s?.toString().orEmpty()
                refresh()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    override fun onResume() {
        super.onResume()
        refresh()
    }
    private fun refresh() {
        val items = dbHelper.getAllItems(currentCategory, currentQuery)
        adapter.submit(items)
        binding.txtEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}
