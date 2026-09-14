package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.Category
import com.example.data.model.Subcategory

/**
 * One pickable icon. [name] is the stable key persisted in the database; [label] and [keywords]
 * feed the search box in the "View All" picker.
 */
data class CategoryIcon(
    val name: String,
    val label: String,
    val vector: ImageVector,
    val keywords: List<String> = emptyList()
)

data class CategoryIconGroup(val title: String, val icons: List<CategoryIcon>)

private fun icon(name: String, label: String, vector: ImageVector, vararg keywords: String) =
    CategoryIcon(name, label, vector, keywords.toList())

/** Every icon a category or subcategory can use, grouped the way the picker shows them. */
val CATEGORY_ICON_GROUPS: List<CategoryIconGroup> = listOf(
    CategoryIconGroup(
        "Food & Drink", listOf(
            icon("restaurant", "Restaurant", Icons.Default.Restaurant, "food", "dining", "eat"),
            icon("fastfood", "Fast food", Icons.Default.Fastfood, "burger", "takeaway"),
            icon("local_pizza", "Pizza", Icons.Default.LocalPizza),
            icon("bakery_dining", "Bakery", Icons.Default.BakeryDining, "breakfast", "bread", "croissant"),
            icon("lunch_dining", "Lunch", Icons.Default.LunchDining, "sandwich"),
            icon("dinner_dining", "Dinner", Icons.Default.DinnerDining, "pasta"),
            icon("ramen_dining", "Noodles", Icons.Default.RamenDining, "ramen", "soup"),
            icon("set_meal", "Meal", Icons.Default.SetMeal, "thali", "fish"),
            icon("local_cafe", "Cafe", Icons.Default.LocalCafe, "coffee", "tea", "chai"),
            icon("emoji_food_beverage", "Tea", Icons.Default.EmojiFoodBeverage, "snacks", "chai"),
            icon("icecream", "Ice cream", Icons.Default.Icecream, "dessert", "sweet"),
            icon("cake", "Cake", Icons.Default.Cake, "birthday", "dessert"),
            icon("local_bar", "Bar", Icons.Default.LocalBar, "drinks", "cocktail"),
            icon("wine_bar", "Wine", Icons.Default.WineBar, "alcohol"),
            icon("liquor", "Liquor", Icons.Default.Liquor, "beer", "alcohol"),
            icon("local_grocery_store", "Groceries", Icons.Default.LocalGroceryStore, "grocery", "supermarket", "vegetables")
        )
    ),
    CategoryIconGroup(
        "Transport", listOf(
            icon("directions_car", "Car", Icons.Default.DirectionsCar, "transport", "drive"),
            icon("local_gas_station", "Fuel", Icons.Default.LocalGasStation, "petrol", "gas", "diesel"),
            icon("ev_station", "EV charging", Icons.Default.EvStation, "electric", "charge"),
            icon("local_taxi", "Taxi", Icons.Default.LocalTaxi, "cab", "uber", "ola"),
            icon("directions_bus", "Bus", Icons.Default.DirectionsBus, "transit"),
            icon("train", "Train", Icons.Default.Train, "railway"),
            icon("subway", "Metro", Icons.Default.Subway, "underground"),
            icon("tram", "Tram", Icons.Default.Tram),
            icon("two_wheeler", "Bike", Icons.Default.TwoWheeler, "motorcycle", "scooter"),
            icon("directions_bike", "Cycle", Icons.AutoMirrored.Filled.DirectionsBike, "bicycle"),
            icon("electric_scooter", "Scooter", Icons.Default.ElectricScooter),
            icon("local_parking", "Parking", Icons.Default.LocalParking),
            icon("toll", "Toll", Icons.Default.Toll, "highway"),
            icon("car_repair", "Car service", Icons.Default.CarRepair, "garage", "mechanic"),
            icon("local_shipping", "Delivery", Icons.Default.LocalShipping, "courier", "shipping", "truck"),
            icon("directions_boat", "Boat", Icons.Default.DirectionsBoat, "ferry", "ship")
        )
    ),
    CategoryIconGroup(
        "Shopping", listOf(
            icon("shopping_bag", "Shopping", Icons.Default.ShoppingBag, "bag", "mall"),
            icon("shopping_cart", "Cart", Icons.Default.ShoppingCart, "online", "amazon"),
            icon("storefront", "Store", Icons.Default.Storefront, "shop", "market"),
            icon("local_mall", "Mall", Icons.Default.LocalMall),
            icon("checkroom", "Clothing", Icons.Default.Checkroom, "clothes", "fashion", "wardrobe"),
            icon("diamond", "Jewellery", Icons.Default.Diamond, "gold", "ring"),
            icon("watch", "Watch", Icons.Default.Watch, "accessories"),
            icon("smartphone", "Phone", Icons.Default.Smartphone, "mobile", "gadget"),
            icon("laptop", "Laptop", Icons.Default.Laptop, "computer", "electronics"),
            icon("headphones", "Audio", Icons.Default.Headphones, "headphones", "earbuds"),
            icon("camera_alt", "Camera", Icons.Default.CameraAlt, "photo"),
            icon("toys", "Toys", Icons.Default.Toys, "kids", "games"),
            icon("card_giftcard", "Gift", Icons.Default.CardGiftcard, "present"),
            icon("redeem", "Voucher", Icons.Default.Redeem, "coupon", "reward"),
            icon("sell", "Deals", Icons.Default.Sell, "tag", "sale", "discount"),
            icon("subscriptions", "Subscription", Icons.Default.Subscriptions, "netflix", "spotify", "recurring")
        )
    ),
    CategoryIconGroup(
        "Home & Bills", listOf(
            icon("home", "Home", Icons.Default.Home, "house", "rent"),
            icon("apartment", "Apartment", Icons.Default.Apartment, "flat", "rent", "building"),
            icon("receipt_long", "Bills", Icons.AutoMirrored.Filled.ReceiptLong, "invoice", "utility"),
            icon("bolt", "Electricity", Icons.Default.Bolt, "power", "energy"),
            icon("water_drop", "Water", Icons.Default.WaterDrop, "utility"),
            icon("propane", "Gas cylinder", Icons.Default.Propane, "lpg", "cooking gas"),
            icon("wifi", "Internet", Icons.Default.Wifi, "broadband", "wifi"),
            icon("phone_android", "Mobile bill", Icons.Default.PhoneAndroid, "recharge", "sim"),
            icon("tv", "TV", Icons.Default.Tv, "cable", "dth"),
            icon("kitchen", "Kitchen", Icons.Default.Kitchen, "appliance", "fridge"),
            icon("chair", "Furniture", Icons.Default.Chair, "sofa", "decor"),
            icon("bed", "Bedroom", Icons.Default.Bed, "mattress"),
            icon("cleaning_services", "Cleaning", Icons.Default.CleaningServices, "maid", "housekeeping"),
            icon("local_laundry_service", "Laundry", Icons.Default.LocalLaundryService, "wash", "ironing"),
            icon("yard", "Garden", Icons.Default.Yard, "plants", "lawn"),
            icon("security", "Insurance", Icons.Default.Security, "protection", "policy")
        )
    ),
    CategoryIconGroup(
        "Health & Wellness", listOf(
            icon("medical_services", "Medical", Icons.Default.MedicalServices, "health", "doctor"),
            icon("local_hospital", "Hospital", Icons.Default.LocalHospital, "clinic", "emergency"),
            icon("local_pharmacy", "Pharmacy", Icons.Default.LocalPharmacy, "medicine", "chemist"),
            icon("medication", "Medicine", Icons.Default.Medication, "pills", "tablets"),
            icon("vaccines", "Vaccine", Icons.Default.Vaccines, "injection"),
            icon("monitor_heart", "Checkup", Icons.Default.MonitorHeart, "heart", "test", "lab"),
            icon("fitness_center", "Gym", Icons.Default.FitnessCenter, "workout", "fitness"),
            icon("directions_run", "Running", Icons.AutoMirrored.Filled.DirectionsRun, "jogging", "sports"),
            icon("self_improvement", "Yoga", Icons.Default.SelfImprovement, "meditation", "wellness"),
            icon("spa", "Spa", Icons.Default.Spa, "massage", "wellness"),
            icon("content_cut", "Salon", Icons.Default.ContentCut, "haircut", "barber", "grooming"),
            icon("favorite", "Self care", Icons.Default.Favorite, "heart", "love"),
            icon("psychology", "Therapy", Icons.Default.Psychology, "mental", "counselling"),
            icon("pool", "Swimming", Icons.Default.Pool),
            icon("sports_soccer", "Football", Icons.Default.SportsSoccer, "soccer", "sports"),
            icon("sports_cricket", "Cricket", Icons.Default.SportsCricket, "sports")
        )
    ),
    CategoryIconGroup(
        "Entertainment", listOf(
            icon("movie", "Movies", Icons.Default.Movie, "cinema", "entertainment"),
            icon("theaters", "Theatre", Icons.Default.Theaters, "show", "drama"),
            icon("live_tv", "Streaming", Icons.Default.LiveTv, "ott", "netflix"),
            icon("music_note", "Music", Icons.Default.MusicNote, "concert", "spotify"),
            icon("sports_esports", "Gaming", Icons.Default.SportsEsports, "games", "console"),
            icon("podcasts", "Podcasts", Icons.Default.Podcasts, "audio"),
            icon("local_activity", "Events", Icons.Default.LocalActivity, "tickets", "concert"),
            icon("celebration", "Party", Icons.Default.Celebration, "festival", "celebration"),
            icon("nightlife", "Nightlife", Icons.Default.Nightlife, "club", "pub"),
            icon("casino", "Betting", Icons.Default.Casino, "lottery", "gambling"),
            icon("attractions", "Amusement", Icons.Default.Attractions, "park", "rides", "fun"),
            icon("stadium", "Stadium", Icons.Default.Stadium, "match", "sports"),
            icon("piano", "Instruments", Icons.Default.Piano, "music", "hobby"),
            icon("palette", "Art", Icons.Default.Palette, "hobby", "craft", "painting"),
            icon("book", "Books", Icons.Default.Book, "reading", "novel"),
            icon("interests", "Hobbies", Icons.Default.Interests, "interests")
        )
    ),
    CategoryIconGroup(
        "Travel", listOf(
            icon("flight", "Flight", Icons.Default.Flight, "travel", "plane", "airline"),
            icon("hotel", "Hotel", Icons.Default.Hotel, "stay", "resort", "lodging"),
            icon("luggage", "Luggage", Icons.Default.Luggage, "trip", "baggage"),
            icon("beach_access", "Vacation", Icons.Default.BeachAccess, "beach", "holiday"),
            icon("explore", "Explore", Icons.Default.Explore, "trip", "adventure"),
            icon("map", "Map", Icons.Default.Map, "tour", "sightseeing"),
            icon("hiking", "Hiking", Icons.Default.Hiking, "trek", "outdoor"),
            icon("sailing", "Sailing", Icons.Default.Sailing, "cruise"),
            icon("park", "Park", Icons.Default.Park, "nature", "picnic"),
            icon("festival", "Festival", Icons.Default.Festival, "fair", "carnival"),
            icon("commute", "Commute", Icons.Default.Commute, "daily", "office"),
            icon("public", "International", Icons.Default.Public, "abroad", "world", "forex")
        )
    ),
    CategoryIconGroup(
        "Education & Work", listOf(
            icon("school", "Education", Icons.Default.School, "college", "tuition", "fees"),
            icon("menu_book", "Study", Icons.AutoMirrored.Filled.MenuBook, "books", "course"),
            icon("history_edu", "Courses", Icons.Default.HistoryEdu, "class", "learning"),
            icon("science", "Science", Icons.Default.Science, "lab", "research"),
            icon("computer", "Computer", Icons.Default.Computer, "software", "tech"),
            icon("backpack", "School kit", Icons.Default.Backpack, "stationery", "supplies"),
            icon("work", "Work", Icons.Default.Work, "business", "office", "job"),
            icon("business_center", "Business", Icons.Default.BusinessCenter, "briefcase", "freelance"),
            icon("handshake", "Consulting", Icons.Default.Handshake, "client", "deal", "partner"),
            icon("print", "Printing", Icons.Default.Print, "stationery", "documents"),
            icon("article", "Documents", Icons.AutoMirrored.Filled.Article, "paperwork", "legal"),
            icon("gavel", "Legal", Icons.Default.Gavel, "lawyer", "court", "fine")
        )
    ),
    CategoryIconGroup(
        "Money & Finance", listOf(
            icon("account_balance_wallet", "Wallet", Icons.Default.AccountBalanceWallet, "salary", "cash"),
            icon("payments", "Salary", Icons.Default.Payments, "pay", "income", "wages"),
            icon("account_balance", "Bank", Icons.Default.AccountBalance, "loan", "emi", "government"),
            icon("savings", "Savings", Icons.Default.Savings, "piggy", "deposit", "fd"),
            icon("credit_card", "Credit card", Icons.Default.CreditCard, "card", "emi"),
            icon("currency_rupee", "Rupee", Icons.Default.CurrencyRupee, "cash", "money"),
            icon("currency_exchange", "Exchange", Icons.Default.CurrencyExchange, "forex", "transfer", "refund"),
            icon("trending_up", "Investment", Icons.AutoMirrored.Filled.TrendingUp, "interest", "growth", "stocks"),
            icon("show_chart", "Stocks", Icons.AutoMirrored.Filled.ShowChart, "shares", "mutual fund", "sip"),
            icon("paid", "Paid", Icons.Default.Paid, "payout", "cashback"),
            icon("percent", "Interest", Icons.Default.Percent, "rate", "tax", "gst"),
            icon("request_quote", "Tax", Icons.Default.RequestQuote, "invoice", "quote"),
            icon("volunteer_activism", "Donation", Icons.Default.VolunteerActivism, "charity", "zakat", "tithe"),
            icon("loyalty", "Rewards", Icons.Default.Loyalty, "points", "cashback"),
            icon("autorenew", "Recurring", Icons.Default.Autorenew, "emi", "subscription", "repeat"),
            icon("rocket_launch", "Crypto", Icons.Default.RocketLaunch, "bitcoin", "startup")
        )
    ),
    CategoryIconGroup(
        "Family & Pets", listOf(
            icon("family_restroom", "Family", Icons.Default.FamilyRestroom, "household", "parents"),
            icon("child_care", "Kids", Icons.Default.ChildCare, "baby", "children"),
            icon("stroller", "Baby", Icons.Default.Stroller, "infant", "pram"),
            icon("elderly", "Parents", Icons.Default.Elderly, "elder", "senior"),
            icon("diversity_3", "Friends", Icons.Default.Diversity3, "social", "group", "outing"),
            icon("pets", "Pets", Icons.Default.Pets, "dog", "cat", "vet"),
            icon("church", "Church", Icons.Default.Church, "religion", "offering"),
            icon("mosque", "Mosque", Icons.Default.Mosque, "religion", "zakat"),
            icon("temple_hindu", "Temple", Icons.Default.TempleHindu, "religion", "puja", "offering")
        )
    ),
    CategoryIconGroup(
        "Services & Misc", listOf(
            icon("build", "Repairs", Icons.Default.Build, "tools", "fix", "maintenance"),
            icon("handyman", "Handyman", Icons.Default.Handyman, "carpenter", "workman"),
            icon("plumbing", "Plumbing", Icons.Default.Plumbing, "pipes", "water"),
            icon("electrical_services", "Electrician", Icons.Default.ElectricalServices, "wiring"),
            icon("home_repair_service", "Home service", Icons.Default.HomeRepairService, "toolbox", "repair"),
            icon("brush", "Painting", Icons.Default.Brush, "decor", "renovation"),
            icon("umbrella", "Emergency", Icons.Default.Umbrella, "rainy day", "contingency"),
            icon("recycling", "Recycling", Icons.Default.Recycling, "eco", "green"),
            icon("star", "Favourite", Icons.Default.Star, "special", "important"),
            icon("bookmark", "Bookmark", Icons.Default.Bookmark, "saved", "tag"),
            icon("extension", "Other", Icons.Default.Extension, "misc", "plugin"),
            icon("category", "General", Icons.Default.Category, "default", "misc", "uncategorised")
        )
    )
)

/** Flat lookup by persisted key. */
val CATEGORY_ICON_MAP: Map<String, CategoryIcon> =
    CATEGORY_ICON_GROUPS.flatMap { it.icons }.associateBy { it.name }

/** Older builds stored a few loose aliases; keep them resolving to the same glyphs. */
private val LEGACY_ICON_ALIASES = mapOf(
    "food" to "restaurant", "dining" to "restaurant",
    "transport" to "directions_car", "car" to "directions_car",
    "shopping" to "shopping_bag", "bag" to "shopping_bag",
    "health" to "medical_services", "hospital" to "local_hospital",
    "entertainment" to "movie", "play" to "movie",
    "bills" to "receipt_long", "invoice" to "receipt_long",
    "travel" to "flight", "plane" to "flight",
    "education" to "school",
    "gym" to "fitness_center",
    "house" to "home",
    "tools" to "build",
    "wallet" to "account_balance_wallet", "salary" to "payments",
    "business" to "business_center",
    "gift" to "card_giftcard",
    "interest" to "trending_up"
)

/** The dozen shown inline in the editor before the user taps "View All". */
val FEATURED_ICON_NAMES = listOf(
    "restaurant", "directions_car", "shopping_bag", "medical_services", "receipt_long", "home",
    "flight", "school", "fitness_center", "family_restroom", "build", "pets"
)

/** Maps a persisted key (including legacy aliases) to its catalog key, or null when unknown. */
fun canonicalIconName(name: String?): String? {
    if (name.isNullOrBlank()) return null
    val key = name.lowercase()
    return if (key in CATEGORY_ICON_MAP) key else LEGACY_ICON_ALIASES[key]
}

/** Resolves a persisted icon key to a glyph, or null when the key is unknown. */
fun findCategoryIcon(name: String?): ImageVector? {
    if (name.isNullOrBlank()) return null
    val key = name.lowercase()
    return CATEGORY_ICON_MAP[key]?.vector ?: LEGACY_ICON_ALIASES[key]?.let { CATEGORY_ICON_MAP[it]?.vector }
}

/** Case-insensitive search across labels, keys and keywords. Blank query returns everything. */
fun searchCategoryIcons(query: String): List<CategoryIconGroup> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return CATEGORY_ICON_GROUPS
    return CATEGORY_ICON_GROUPS.mapNotNull { group ->
        val hits = group.icons.filter { ic ->
            ic.label.lowercase().contains(q) || ic.name.contains(q) || ic.keywords.any { it.contains(q) }
        }
        if (hits.isEmpty()) null else group.copy(icons = hits)
    }
}

/**
 * Best-guess icon key from a subcategory's name, used as the default before the user picks one.
 * Checks the more specific words first so "Metro & Train" lands on the metro glyph, not the car.
 */
fun guessSubcategoryIconName(name: String): String? {
    val lower = name.lowercase()
    fun has(vararg words: String) = words.any { lower.contains(it) }
    return when {
        has("breakfast", "bakery", "bread") -> "bakery_dining"
        has("lunch") -> "lunch_dining"
        has("dinner") -> "dinner_dining"
        has("coffee", "tea", "chai", "snack", "cafe") -> "local_cafe"
        has("pizza") -> "local_pizza"
        has("dessert", "ice cream", "sweet") -> "icecream"
        has("grocer", "vegetable", "supermarket") -> "local_grocery_store"
        has("food", "dining", "meal", "restaurant", "eat") -> "restaurant"
        has("fuel", "petrol", "diesel", "gas station") -> "local_gas_station"
        has("taxi", "cab", "uber", "ola") -> "local_taxi"
        has("metro", "subway") -> "subway"
        has("train", "rail") -> "train"
        has("bus") -> "directions_bus"
        has("bike", "scooter", "motor") -> "two_wheeler"
        has("parking") -> "local_parking"
        has("toll") -> "toll"
        has("car", "drive") -> "directions_car"
        has("flight", "air") -> "flight"
        has("hotel", "stay", "resort") -> "hotel"
        has("travel", "trip", "vacation", "holiday") -> "luggage"
        has("movie", "cinema", "film") -> "movie"
        has("netflix", "stream", "ott", "prime") -> "live_tv"
        has("music", "spotify", "concert") -> "music_note"
        has("game", "gaming") -> "sports_esports"
        has("show", "theater", "theatre", "event", "ticket") -> "local_activity"
        has("party", "celebrat", "festival") -> "celebration"
        has("gym", "workout", "fitness") -> "fitness_center"
        has("yoga", "meditat") -> "self_improvement"
        has("salon", "haircut", "barber", "grooming") -> "content_cut"
        has("pharma", "medicine", "chemist", "tablet") -> "local_pharmacy"
        has("hospital", "clinic", "emergency") -> "local_hospital"
        has("doctor", "health", "dental", "checkup") -> "medical_services"
        has("insurance", "policy") -> "security"
        has("rent", "flat", "apartment") -> "apartment"
        has("electric", "power") -> "bolt"
        has("water") -> "water_drop"
        has("internet", "wifi", "broadband") -> "wifi"
        has("mobile", "phone", "recharge") -> "phone_android"
        has("bill", "utility") -> "receipt_long"
        has("home", "house") -> "home"
        has("maid", "clean", "housekeep") -> "cleaning_services"
        has("laundry", "wash") -> "local_laundry_service"
        has("pet", "dog", "cat", "vet") -> "pets"
        has("baby", "kid", "child") -> "child_care"
        has("school", "college", "tuition", "fee", "course", "class") -> "school"
        has("book") -> "menu_book"
        has("repair", "tool", "fix", "service") -> "build"
        has("cloth", "fashion", "dress", "shoe") -> "checkroom"
        has("amazon", "online", "flipkart") -> "shopping_cart"
        has("gift", "present") -> "card_giftcard"
        has("shop", "mall", "bag") -> "shopping_bag"
        has("subscri") -> "subscriptions"
        has("bonus", "reward", "cashback") -> "loyalty"
        has("salary", "pay", "wage") -> "payments"
        has("invoice", "client", "consult", "freelance") -> "handshake"
        has("dividend", "stock", "share", "mutual", "sip") -> "show_chart"
        has("interest", "invest", "return") -> "trending_up"
        has("loan", "emi", "bank") -> "account_balance"
        has("saving", "deposit") -> "savings"
        has("tax", "gst") -> "request_quote"
        has("donat", "charity") -> "volunteer_activism"
        else -> null
    }
}

/**
 * The glyph a subcategory shows across the app: its own pick first, then a guess from the name,
 * then the parent's icon so nothing ever renders as a blank placeholder.
 */
fun subcategoryIconVector(subcategory: Subcategory?, parent: Category?): ImageVector {
    if (subcategory == null) return findCategoryIcon(parent?.iconName) ?: Icons.Default.Category
    return findCategoryIcon(subcategory.iconName)
        ?: findCategoryIcon(guessSubcategoryIconName(subcategory.name))
        ?: findCategoryIcon(parent?.iconName)
        ?: Icons.Default.Category
}
