package com.example.lostandfound
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.lostandfound.databinding.ActivityDetailBinding
import com.example.lostandfound.db.DatabaseHelper
import com.example.lostandfound.db.Item
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
class DetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ID = "extra_id"
    }
    private lateinit var binding: ActivityDetailBinding
    private lateinit var dbHelper: DatabaseHelper
    private var itemId: Long = -1L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = DatabaseHelper(this)
        itemId = intent.getLongExtra(EXTRA_ID, -1L)
        if (itemId == -1L) {
            finish(); return
        }
        val item = dbHelper.getItem(itemId)
        if (item == null) {
            Toast.makeText(this, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            finish(); return
        }
        bind(item)
        binding.btnRemove.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_remove_title)
                .setMessage(R.string.confirm_remove_message)
                .setPositiveButton(R.string.remove) { _, _ ->
                    if (dbHelper.deleteItem(itemId) > 0) {
                        Toast.makeText(this, R.string.removed, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
    private fun bind(item: Item) {
        binding.txtTitle.text = getString(R.string.title_format, item.postType, item.name)
        binding.txtAge.text = relativeTime(item.createdAt)
        binding.txtCategory.text = getString(R.string.label_category_value, item.category)
        binding.txtDescription.text = item.description
        binding.txtDate.text = getString(R.string.label_date_value, item.date)
        binding.txtLocation.text = getString(R.string.label_location_value, item.location)
        binding.txtPhone.text = getString(R.string.label_phone_value, item.phone)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        binding.txtPosted.text = getString(R.string.label_posted_value, sdf.format(Date(item.createdAt)))
        if (!item.imagePath.isNullOrEmpty()) {
            val f = File(item.imagePath)
            if (f.exists()) {
                binding.imageDetail.setImageURI(Uri.fromFile(f))
                binding.imageDetail.visibility = View.VISIBLE
            }
        }
    }
    private fun relativeTime(createdAt: Long): String {
        val diff = System.currentTimeMillis() - createdAt
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        return when {
            days >= 1 -> resources.getQuantityString(R.plurals.days_ago, days.toInt(), days.toInt())
            hours >= 1 -> resources.getQuantityString(R.plurals.hours_ago, hours.toInt(), hours.toInt())
            minutes >= 1 -> resources.getQuantityString(R.plurals.minutes_ago, minutes.toInt(), minutes.toInt())
            else -> getString(R.string.just_now)
        }
    }
}
