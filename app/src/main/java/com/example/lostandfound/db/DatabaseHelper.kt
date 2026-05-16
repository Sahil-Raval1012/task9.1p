package com.example.lostandfound.db
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    companion object {
        const val DB_NAME = "lostfound.db"
        const val DB_VERSION = 2
        const val TABLE = "items"
        const val COL_ID = "id"
        const val COL_POST_TYPE = "post_type"
        const val COL_NAME = "name"
        const val COL_PHONE = "phone"
        const val COL_DESCRIPTION = "description"
        const val COL_DATE = "date"
        const val COL_LOCATION = "location"
        const val COL_CATEGORY = "category"
        const val COL_IMAGE_PATH = "image_path"
        const val COL_CREATED_AT = "created_at"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
    }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_POST_TYPE TEXT NOT NULL,
                $COL_NAME TEXT NOT NULL,
                $COL_PHONE TEXT NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_LOCATION TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_IMAGE_PATH TEXT,
                $COL_CREATED_AT INTEGER NOT NULL,
                $COL_LATITUDE REAL,
                $COL_LONGITUDE REAL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_LATITUDE REAL")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_LONGITUDE REAL")
        }
    }
    fun insertItem(item: Item): Long {
        val values = ContentValues().apply {
            put(COL_POST_TYPE, item.postType)
            put(COL_NAME, item.name)
            put(COL_PHONE, item.phone)
            put(COL_DESCRIPTION, item.description)
            put(COL_DATE, item.date)
            put(COL_LOCATION, item.location)
            put(COL_CATEGORY, item.category)
            put(COL_IMAGE_PATH, item.imagePath)
            put(COL_CREATED_AT, item.createdAt)
            if (item.latitude != null) put(COL_LATITUDE, item.latitude)
            if (item.longitude != null) put(COL_LONGITUDE, item.longitude)
        }
        writableDatabase.use { db ->
            return db.insert(TABLE, null, values)
        }
    }
    fun getAllItems(category: String? = null, query: String? = null): List<Item> {
        val items = mutableListOf<Item>()
        val selection = StringBuilder()
        val args = mutableListOf<String>()
        if (!category.isNullOrBlank() && category != "All") {
            selection.append("$COL_CATEGORY = ?")
            args.add(category)
        }
        if (!query.isNullOrBlank()) {
            if (selection.isNotEmpty()) selection.append(" AND ")
            selection.append("($COL_NAME LIKE ? OR $COL_DESCRIPTION LIKE ? OR $COL_LOCATION LIKE ?)")
            val like = "%$query%"
            args.add(like); args.add(like); args.add(like)
        }
        val where = if (selection.isEmpty()) null else selection.toString()
        val whereArgs = if (args.isEmpty()) null else args.toTypedArray()
        readableDatabase.use { db ->
            db.query(
                TABLE, null, where, whereArgs, null, null, "$COL_CREATED_AT DESC"
            ).use { c ->
                while (c.moveToNext()) {
                    val latIdx = c.getColumnIndex(COL_LATITUDE)
                    val lngIdx = c.getColumnIndex(COL_LONGITUDE)
                    items.add(
                        Item(
                            id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                            postType = c.getString(c.getColumnIndexOrThrow(COL_POST_TYPE)),
                            name = c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                            phone = c.getString(c.getColumnIndexOrThrow(COL_PHONE)),
                            description = c.getString(c.getColumnIndexOrThrow(COL_DESCRIPTION)),
                            date = c.getString(c.getColumnIndexOrThrow(COL_DATE)),
                            location = c.getString(c.getColumnIndexOrThrow(COL_LOCATION)),
                            category = c.getString(c.getColumnIndexOrThrow(COL_CATEGORY)),
                            imagePath = c.getString(c.getColumnIndexOrThrow(COL_IMAGE_PATH)),
                            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)),
                            latitude = if (latIdx >= 0 && !c.isNull(latIdx)) c.getDouble(latIdx) else null,
                            longitude = if (lngIdx >= 0 && !c.isNull(lngIdx)) c.getDouble(lngIdx) else null
                        )
                    )
                }
            }
        }
        return items
    }
    fun getItem(id: Long): Item? {
        readableDatabase.use { db ->
            db.query(TABLE, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null)
                .use { c ->
                    if (c.moveToFirst()) {
                        val latIdx = c.getColumnIndex(COL_LATITUDE)
                        val lngIdx = c.getColumnIndex(COL_LONGITUDE)
                        return Item(
                            id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                            postType = c.getString(c.getColumnIndexOrThrow(COL_POST_TYPE)),
                            name = c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                            phone = c.getString(c.getColumnIndexOrThrow(COL_PHONE)),
                            description = c.getString(c.getColumnIndexOrThrow(COL_DESCRIPTION)),
                            date = c.getString(c.getColumnIndexOrThrow(COL_DATE)),
                            location = c.getString(c.getColumnIndexOrThrow(COL_LOCATION)),
                            category = c.getString(c.getColumnIndexOrThrow(COL_CATEGORY)),
                            imagePath = c.getString(c.getColumnIndexOrThrow(COL_IMAGE_PATH)),
                            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)),
                            latitude = if (latIdx >= 0 && !c.isNull(latIdx)) c.getDouble(latIdx) else null,
                            longitude = if (lngIdx >= 0 && !c.isNull(lngIdx)) c.getDouble(lngIdx) else null
                        )
                    }
                }
        }
        return null
    }
    fun deleteItem(id: Long): Int {
        writableDatabase.use { db ->
            return db.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
        }
    }
}
