package com.example.lostandfound
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.lostandfound.databinding.ActivityCreateBinding
import com.example.lostandfound.db.DatabaseHelper
import com.example.lostandfound.db.Item
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
class CreateAdvertActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var imagePath: String? = null
    private var pendingCameraFile: File? = null
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 2001
    }
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { saveImageLocally(it) } }
    private val pickFromFiles = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { saveImageLocally(it) } }
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val file = pendingCameraFile
        if (success && file != null && file.exists() && file.length() > 0) {
            imagePath = file.absolutePath
            binding.imagePreview.setImageURI(Uri.fromFile(file))
            binding.imagePreview.visibility = android.view.View.VISIBLE
        } else {
            file?.delete()
            Toast.makeText(this, getString(R.string.image_error), Toast.LENGTH_SHORT).show()
        }
        pendingCameraFile = null
    }
    private val autocompleteResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            binding.editLocation.setText(place.address ?: place.name)
            selectedLat = place.latLng?.latitude
            selectedLng = place.latLng?.longitude
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        val categories = resources.getStringArray(R.array.categories_create)
        binding.spinnerCategory.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, categories
        )
        binding.editLocation.setOnClickListener { launchPlacesAutocomplete() }
        binding.editLocation.isFocusable = false
        binding.btnGetLocation.setOnClickListener { getCurrentLocation() }
        binding.btnPickImage.setOnClickListener { showImageSourceChooser() }
        binding.btnPickDate.setOnClickListener { showDatePicker() }
        binding.btnSave.setOnClickListener { saveItem() }
    }
    private fun launchPlacesAutocomplete() {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(this)
        autocompleteResult.launch(intent)
    }
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
            return
        }
        Toast.makeText(this, getString(R.string.getting_location), Toast.LENGTH_SHORT).show()
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                selectedLat = location.latitude
                selectedLng = location.longitude
                reverseGeocode(location.latitude, location.longitude)
            } else {
                Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_SHORT).show()
        }
    }
    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val addressText = buildString {
                    if (!addr.thoroughfare.isNullOrEmpty()) append(addr.thoroughfare)
                    if (!addr.locality.isNullOrEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append(addr.locality)
                    }
                    if (!addr.adminArea.isNullOrEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append(addr.adminArea)
                    }
                    if (!addr.countryName.isNullOrEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append(addr.countryName)
                    }
                }.ifEmpty { "$lat, $lng" }
                binding.editLocation.setText(addressText)
            } else {
                binding.editLocation.setText("$lat, $lng")
            }
        } catch (e: Exception) {
            binding.editLocation.setText("$lat, $lng")
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        }
    }
    private fun showImageSourceChooser() {
        val options = arrayOf(
            getString(R.string.image_from_camera),
            getString(R.string.image_from_gallery),
            getString(R.string.image_from_files),
            getString(R.string.image_from_sample)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.image_source_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> pickImage.launch("image/*")
                    2 -> pickFromFiles.launch(arrayOf("image/*"))
                    3 -> showSampleChooser()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    private fun showSampleChooser() {
        val labels = arrayOf(getString(R.string.sample_keys))
        val resIds = intArrayOf(R.drawable.sample_keys)
        AlertDialog.Builder(this)
            .setTitle(R.string.image_from_sample)
            .setItems(labels) { _, which -> saveSampleDrawable(resIds[which]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    private fun saveSampleDrawable(resId: Int) {
        try {
            val drawable: Drawable = ContextCompat.getDrawable(this, resId)
                ?: throw IllegalStateException("Sample image not found")
            val bmp: Bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val size = 600
                val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                canvas.drawColor(Color.WHITE)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                out
            }
            val dir = File(filesDir, "images").apply { if (!exists()) mkdirs() }
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            imagePath = file.absolutePath
            binding.imagePreview.setImageURI(Uri.fromFile(file))
            binding.imagePreview.visibility = android.view.View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.image_error), Toast.LENGTH_SHORT).show()
        }
    }
    private fun launchCamera() {
        try {
            val dir = File(filesDir, "images").apply { if (!exists()) mkdirs() }
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            pendingCameraFile = file
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            takePicture.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.image_error), Toast.LENGTH_SHORT).show()
        }
    }
    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, y, m, d ->
                val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                binding.editDate.setText(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    private fun saveImageLocally(uri: Uri) {
        try {
            val dir = File(filesDir, "images").apply { if (!exists()) mkdirs() }
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            imagePath = file.absolutePath
            binding.imagePreview.setImageURI(Uri.fromFile(file))
            binding.imagePreview.visibility = android.view.View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.image_error), Toast.LENGTH_SHORT).show()
        }
    }
    private fun saveItem() {
        val postType = if (binding.radioLost.isChecked) "Lost" else "Found"
        val name = binding.editName.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val description = binding.editDescription.text.toString().trim()
        val date = binding.editDate.text.toString().trim()
        val location = binding.editLocation.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem?.toString() ?: "Other"

        if (name.isEmpty() || phone.isEmpty() || description.isEmpty() ||
            date.isEmpty() || location.isEmpty()
        ) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }
        if (imagePath.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.image_required), Toast.LENGTH_SHORT).show()
            return
        }
        val createdAt = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val item = Item(
            postType = postType,
            name = name,
            phone = phone,
            description = description,
            date = date,
            location = location,
            category = category,
            imagePath = imagePath,
            createdAt = createdAt,
            latitude = selectedLat,
            longitude = selectedLng
        )
        val id = DatabaseHelper(this).insertItem(item)
        if (id > 0) {
            Toast.makeText(
                this,
                getString(R.string.saved_at, sdf.format(Date(createdAt))),
                Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(this, ListActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
        }
    }
}
