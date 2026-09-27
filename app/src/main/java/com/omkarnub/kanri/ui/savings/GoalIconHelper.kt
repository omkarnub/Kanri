package com.omkarnub.kanri.ui.savings

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.omkarnub.kanri.R

data class GoalIconOption(
    val key: String,
    val label: String,
    val iconRes: Int
)

val AVAILABLE_GOAL_ICONS = listOf(
    GoalIconOption("target", "Target", R.drawable.ic_goal_target),
    GoalIconOption("savings", "Savings Vault", R.drawable.ic_goal_savings),
    GoalIconOption("shield", "Emergency Fund", R.drawable.ic_goal_shield),
    GoalIconOption("laptop", "Tech / Gadget", R.drawable.ic_goal_laptop),
    GoalIconOption("car", "Vehicle", R.drawable.ic_goal_car),
    GoalIconOption("home", "House / Property", R.drawable.ic_goal_home),
    GoalIconOption("travel", "Travel / Vacation", R.drawable.ic_goal_travel),
    GoalIconOption("education", "Education", R.drawable.ic_goal_education),
    GoalIconOption("gift", "Celebration / Gift", R.drawable.ic_goal_gift),
    GoalIconOption("trophy", "Milestone", R.drawable.ic_goal_trophy),
    GoalIconOption("investment", "Investment", R.drawable.ic_goal_investment),
    GoalIconOption("star", "Wishlist", R.drawable.ic_goal_star)
)

fun getGoalIconRes(iconKeyOrEmoji: String?): Int {
    if (iconKeyOrEmoji.isNullOrBlank()) return R.drawable.ic_goal_target

    val key = iconKeyOrEmoji.trim().lowercase()
    return when {
        key == "target" || key == "🎯" -> R.drawable.ic_goal_target
        key == "savings" || key == "vault" || key == "piggy" || key == "bank" -> R.drawable.ic_goal_savings
        key == "shield" || key == "🛡️" || key == "emergency" || key == "safety" -> R.drawable.ic_goal_shield
        key == "laptop" || key == "💻" || key == "📱" || key == "tech" || key == "gadget" || key == "phone" -> R.drawable.ic_goal_laptop
        key == "car" || key == "🚗" || key == "vehicle" || key == "bike" -> R.drawable.ic_goal_car
        key == "home" || key == "🏠" || key == "house" || key == "flat" || key == "property" -> R.drawable.ic_goal_home
        key == "travel" || key == "🏖️" || key == "flight" || key == "trip" || key == "vacation" -> R.drawable.ic_goal_travel
        key == "education" || key == "🎓" || key == "school" || key == "college" || key == "study" -> R.drawable.ic_goal_education
        key == "gift" || key == "💍" || key == "wedding" || key == "anniversary" || key == "celebration" -> R.drawable.ic_goal_gift
        key == "trophy" || key == "🎮" || key == "milestone" || key == "award" -> R.drawable.ic_goal_trophy
        key == "investment" || key == "stocks" || key == "sip" || key == "wealth" -> R.drawable.ic_goal_investment
        key == "star" || key == "wish" || key == "wishlist" -> R.drawable.ic_goal_star
        else -> R.drawable.ic_goal_target
    }
}

@Composable
fun GoalIcon(
    iconKey: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null
) {
    Icon(
        painter = painterResource(id = getGoalIconRes(iconKey)),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
