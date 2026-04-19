package com.example.smartfinance.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "movie" -> Icons.Default.Movie
        "directions_car" -> Icons.Default.DirectionsCar
        "attach_money" -> Icons.Default.AttachMoney
        "card_giftcard" -> Icons.Default.CardGiftcard
        "shopping_cart" -> Icons.Default.ShoppingCart
        "home" -> Icons.Default.Home
        "medical_services" -> Icons.Default.MedicalServices
        "school" -> Icons.Default.School
        "fitness_center" -> Icons.Default.FitnessCenter
        "commute" -> Icons.Default.Commute
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "trending_up" -> Icons.Default.TrendingUp
        else -> Icons.Default.Category
    }
}
