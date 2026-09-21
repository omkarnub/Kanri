package com.omkarnub.kanri.ui.common

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.omkarnub.kanri.R

/**
 * Returns the drawable resource ID corresponding to a category's name or icon name.
 */
fun getCategoryIconRes(categoryName: String?, iconName: String? = null): Int {
    if (!iconName.isNullOrBlank()) {
        when (iconName.lowercase()) {
            "taxi", "cab", "auto" -> return R.drawable.ic_category_taxi
            "train", "metro", "subway" -> return R.drawable.ic_category_train
            "restaurant", "fastfood", "food" -> return R.drawable.ic_category_food
            "shopping_cart", "cart", "grocery" -> return R.drawable.ic_category_groceries
            "shopping_bag", "bag", "shop" -> return R.drawable.ic_category_shopping
            "directions_car", "car", "transport" -> return R.drawable.ic_category_transport
            "receipt", "bolt", "bill", "utilities" -> return R.drawable.ic_category_bills
            "payments", "wallet", "salary", "income" -> return R.drawable.ic_category_salary
            "local_hospital", "medical_services", "health" -> return R.drawable.ic_category_health
            "local_atm", "atm", "cash" -> return R.drawable.ic_category_cash
            "sports_esports", "movie", "entertainment" -> return R.drawable.ic_category_entertainment
            "school", "book", "education" -> return R.drawable.ic_category_education
            "trending_up", "investment" -> return R.drawable.ic_category_investment
            "coffee", "cafe" -> return R.drawable.ic_category_coffee
            "fitness", "gym" -> return R.drawable.ic_category_fitness
            "flight", "plane", "travel" -> return R.drawable.ic_category_flight
            "gift", "donation" -> return R.drawable.ic_category_gift
            "home", "rent" -> return R.drawable.ic_category_home
            "pet", "pets" -> return R.drawable.ic_category_pet
        }
    }

    val lower = categoryName?.lowercase() ?: return R.drawable.ic_category_other

    return when {
        lower.contains("taxi") || lower.contains("cab") || lower.contains("auto") ||
                lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") -> R.drawable.ic_category_taxi

        lower.contains("train") || lower.contains("metro") || lower.contains("railway") ||
                lower.contains("irctc") || lower.contains("subway") -> R.drawable.ic_category_train

        lower.contains("coffee") || lower.contains("cafe") || lower.contains("starbucks") ||
                lower.contains("tea") || lower.contains("chai") -> R.drawable.ic_category_coffee

        lower.contains("gym") || lower.contains("fitness") || lower.contains("workout") -> R.drawable.ic_category_fitness

        lower.contains("flight") || lower.contains("airline") || lower.contains("airplane") ||
                lower.contains("airport") || lower.contains("vacation") || lower.contains("trip") -> R.drawable.ic_category_flight

        lower.contains("gift") || lower.contains("donation") || lower.contains("charity") ||
                lower.contains("present") -> R.drawable.ic_category_gift

        lower.contains("rent") || lower.contains("society") || lower.contains("housing") ||
                lower.contains("apartment") || lower.contains("flat") || lower.contains("home") -> R.drawable.ic_category_home

        lower.contains("pet") || lower.contains("puppy") || lower.contains("kitten") ||
                lower.split(" ", "-", "_", "/").any { it in setOf("cat", "cats", "dog", "dogs", "vet") } -> R.drawable.ic_category_pet

        lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") ||
                lower.contains("burger") || lower.contains("eat") || lower.contains("swiggy") ||
                lower.contains("zomato") -> R.drawable.ic_category_food

        lower.contains("grocer") || lower.contains("supermarket") || lower.contains("vegetable") ||
                lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") ||
                lower.contains("ration") -> R.drawable.ic_category_groceries

        lower.contains("shop") || lower.contains("cloth") || lower.contains("apparel") ||
                lower.contains("mall") || lower.contains("wear") || lower.contains("amazon") ||
                lower.contains("flipkart") || lower.contains("myntra") -> R.drawable.ic_category_shopping

        lower.contains("transport") || lower.contains("commute") || lower.contains("fuel") ||
                lower.contains("petrol") || lower.contains("diesel") || lower.contains("bus") ||
                lower.contains("car") || lower.contains("ride") -> R.drawable.ic_category_transport

        lower.contains("bill") || lower.contains("utilit") || lower.contains("electric") ||
                lower.contains("power") || lower.contains("recharge") || lower.contains("wifi") ||
                lower.contains("broadband") || lower.contains("water") || lower.contains("gas") ||
                lower.contains("subscription") || lower.contains("dth") -> R.drawable.ic_category_bills

        lower.contains("salary") || lower.contains("income") || lower.contains("earning") ||
                lower.contains("wage") || lower.contains("bonus") || lower.contains("dividend") ||
                lower.contains("interest") || lower.contains("freelance") -> R.drawable.ic_category_salary

        lower.contains("health") || lower.contains("medic") || lower.contains("pharma") ||
                lower.contains("doctor") || lower.contains("hospital") || lower.contains("clinic") ||
                lower.contains("dentist") || lower.contains("lab") -> R.drawable.ic_category_health

        lower.contains("atm") || lower.contains("cash") || lower.contains("withdraw") -> R.drawable.ic_category_cash

        lower.contains("entertain") || lower.contains("movie") || lower.contains("cinema") ||
                lower.contains("game") || lower.contains("gaming") || lower.contains("fun") ||
                lower.contains("party") || lower.contains("show") -> R.drawable.ic_category_entertainment

        lower.contains("educat") || lower.contains("course") || lower.contains("school") ||
                lower.contains("college") || lower.contains("tuition") || lower.contains("book") ||
                lower.contains("study") || lower.contains("exam") -> R.drawable.ic_category_education

        lower.contains("invest") || lower.contains("stock") || lower.contains("mutual") ||
                lower.contains("crypto") || lower.contains("gold") || lower.contains("deposit") ||
                lower.contains("sip") -> R.drawable.ic_category_investment

        else -> R.drawable.ic_category_other
    }
}

data class CategoryIconOption(
    val iconName: String,
    val label: String,
    val drawableRes: Int
)

val AVAILABLE_CATEGORY_ICONS = listOf(
    CategoryIconOption("taxi", "Taxi / Auto", R.drawable.ic_category_taxi),
    CategoryIconOption("restaurant", "Food", R.drawable.ic_category_food),
    CategoryIconOption("train", "Train", R.drawable.ic_category_train),
    CategoryIconOption("shopping_cart", "Groceries", R.drawable.ic_category_groceries),
    CategoryIconOption("shopping_bag", "Shopping", R.drawable.ic_category_shopping),
    CategoryIconOption("directions_car", "Car / Fuel", R.drawable.ic_category_transport),
    CategoryIconOption("receipt", "Bills", R.drawable.ic_category_bills),
    CategoryIconOption("coffee", "Coffee", R.drawable.ic_category_coffee),
    CategoryIconOption("fitness", "Gym", R.drawable.ic_category_fitness),
    CategoryIconOption("flight", "Travel", R.drawable.ic_category_flight),
    CategoryIconOption("gift", "Gift", R.drawable.ic_category_gift),
    CategoryIconOption("home", "Home", R.drawable.ic_category_home),
    CategoryIconOption("pet", "Pet", R.drawable.ic_category_pet),
    CategoryIconOption("sports_esports", "Games", R.drawable.ic_category_entertainment),
    CategoryIconOption("local_hospital", "Health", R.drawable.ic_category_health),
    CategoryIconOption("school", "Education", R.drawable.ic_category_education),
    CategoryIconOption("payments", "Salary", R.drawable.ic_category_salary),
    CategoryIconOption("trending_up", "Invest", R.drawable.ic_category_investment),
    CategoryIconOption("local_atm", "ATM", R.drawable.ic_category_cash),
    CategoryIconOption("category", "Other", R.drawable.ic_category_other)
)

/**
 * Modern SVG category icon composable.
 */
@Composable
fun CategoryIcon(
    categoryName: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    iconName: String? = null,
    contentDescription: String? = categoryName
) {
    Icon(
        painter = painterResource(id = getCategoryIconRes(categoryName, iconName)),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
