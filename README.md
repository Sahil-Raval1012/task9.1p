# Lost & Found App – Task 9.1P (Geo Features)

An Android application extending Task 7.1P with Google Maps integration, Places autocomplete, current-location detection, and radius-based search.

---

## Features

### From Task 7.1P (preserved)
- Create Lost / Found adverts with name, phone, description, date, category, and image
- View all adverts in a searchable, filterable list
- View advert details and remove resolved items

### New in Task 9.1P
- **Places Autocomplete** — tap the Location field to search and pick any address via Google Places
- **Get Current Location** — tap the button to auto-fill location using your device's GPS + reverse geocoding
- **Show on Map** — view all adverts as map markers (red = Lost, blue = Found)
- **Radius-Based Search** — filter map markers to only show items within 1–50 km of your current location using the Haversine formula

---

## Project Structure

```
app/src/main/
├── java/com/example/lostandfound/
│   ├── MainActivity.kt            Home screen (3 buttons)
│   ├── CreateAdvertActivity.kt    Create advert with location autocomplete
│   ├── ListActivity.kt            Searchable list of adverts
│   ├── DetailActivity.kt          Advert detail + remove
│   ├── MapActivity.kt             Google Maps with all markers + radius filter
│   ├── adapter/
│   │   └── ItemAdapter.kt         RecyclerView adapter
│   └── db/
│       ├── Item.kt                Data model (includes lat/lng)
│       └── DatabaseHelper.kt      SQLite helper (v2 with lat/lng migration)
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   ├── activity_create.xml
    │   ├── activity_list.xml
    │   ├── activity_detail.xml
    │   ├── activity_map.xml
    │   └── item_row.xml
    └── values/
        ├── strings.xml
        ├── colors.xml
        └── themes.xml
```

---

## Setup — Google Maps API Key (Required)

The app requires a Google Maps API key with **Maps SDK for Android** and **Places API** enabled.

### Steps
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project (or select an existing one)
3. Enable these APIs:
   - **Maps SDK for Android**
   - **Places API**
4. Create an API key under **Credentials**
5. Open `app/src/main/res/values/strings.xml`
6. Replace `YOUR_MAPS_API_KEY_HERE` with your actual key:
   ```xml
   <string name="google_maps_key" translatable="false">AIzaSy...your_key...</string>
   ```

> **Note:** Without a valid API key the map will not load and Places autocomplete will not work, but the rest of the app (create, list, detail) functions normally.

---

## Running the App

1. Open this project in **Android Studio**
2. Add your Google Maps API key (see above)
3. Run on an emulator (API 26+) or physical Android device
4. Grant **Location** permission when prompted

---

## Architecture

| Layer | Technology |
|---|---|
| UI | XML layouts, ViewBinding |
| Navigation | Activity intents |
| Data | SQLite via `SQLiteOpenHelper` |
| Maps | Google Maps SDK for Android |
| Location autocomplete | Google Places SDK |
| Device location | Fused Location Provider |
| Distance calculation | Haversine formula (pure Kotlin) |

---

## Database Migration

Version 1 → 2 adds `latitude REAL` and `longitude REAL` columns via `ALTER TABLE` in `onUpgrade`, preserving all existing data.

---

## Radius Search Algorithm

Distance between two coordinates is calculated using the **Haversine formula**:

```
d = 2R * arcsin(sqrt(sin^2(delta_lat/2) + cos(lat1)*cos(lat2)*sin^2(delta_lon/2)))
```

Items without coordinates (added before geo features were introduced) are excluded from the map view.
