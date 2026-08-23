package com.openzilla.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.WavingHand
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.openzilla.app.R
import com.openzilla.app.data.HabitCostType

/**
 * Preset categories offered by the "add habit" wizard. Icons come from Material Symbols —
 * generic, license-clean glyphs — rather than reproducing any brand's logo, so this stays a
 * fresh, generic app and not a lookalike of any specific service.
 */
enum class HabitCategory(val key: String, @StringRes val labelRes: Int, val icon: ImageVector, val suggestedType: HabitCostType) {
    GENERIC("generic", R.string.category_generic, Icons.Filled.Block, HabitCostType.EVENT),
    SMOKING("smoking", R.string.category_smoking, Icons.Filled.SmokingRooms, HabitCostType.MONEY),
    ALCOHOL("alcohol", R.string.category_alcohol, Icons.Filled.LocalBar, HabitCostType.MONEY),
    DRUGS("drugs", R.string.category_drugs, Icons.Filled.Vaccines, HabitCostType.MONEY),
    PILLS("pills", R.string.category_pills, Icons.Filled.Medication, HabitCostType.MONEY),
    WEED("weed", R.string.category_weed, Icons.Filled.Grass, HabitCostType.MONEY),
    ADULT_CONTENT("adult", R.string.category_adult, Icons.Filled.Visibility, HabitCostType.TIME),
    GAMING("gaming", R.string.category_gaming, Icons.Filled.SportsEsports, HabitCostType.TIME),
    TV("tv", R.string.category_tv, Icons.Filled.Tv, HabitCostType.TIME),
    SHOPPING("shopping", R.string.category_shopping, Icons.Filled.ShoppingBag, HabitCostType.MONEY),
    SOCIAL("social", R.string.category_social, Icons.Filled.Forum, HabitCostType.TIME),
    STREAMING("streaming", R.string.category_streaming, Icons.Filled.PlayCircle, HabitCostType.TIME),
    SOCIALIZING("crowds", R.string.category_crowds, Icons.Filled.Groups, HabitCostType.TIME),
    FAST_FOOD("fastfood", R.string.category_fastfood, Icons.Filled.Fastfood, HabitCostType.MONEY),
    SWEETS("sweets", R.string.category_sweets, Icons.Filled.Cake, HabitCostType.MONEY),
    OVEREATING("overeating", R.string.category_overeating, Icons.Filled.DinnerDining, HabitCostType.EVENT),
    CUSTOM("custom", R.string.category_custom, Icons.Outlined.WavingHand, HabitCostType.EVENT);

    companion object {
        fun byKey(key: String): HabitCategory = entries.firstOrNull { it.key == key } ?: GENERIC
        val iconChoices: List<Pair<String, ImageVector>> = listOf(
            "generic" to Icons.Filled.Block,
            "star" to Icons.Filled.Star,
            "smoking" to Icons.Filled.SmokingRooms,
            "alcohol" to Icons.Filled.LocalBar,
            "drugs" to Icons.Filled.Vaccines,
            "pills" to Icons.Filled.Medication,
            "weed" to Icons.Filled.Grass,
            "adult" to Icons.Filled.Visibility,
            "gaming" to Icons.Filled.SportsEsports,
            "tv" to Icons.Filled.Tv,
            "shopping" to Icons.Filled.ShoppingBag,
            "social" to Icons.Filled.Forum,
            "streaming" to Icons.Filled.PlayCircle,
            "crowds" to Icons.Filled.Groups,
            "fastfood" to Icons.Filled.Fastfood,
            "sweets" to Icons.Filled.Cake,
            "overeating" to Icons.Filled.DinnerDining
        )
        fun iconFor(key: String): ImageVector = iconChoices.firstOrNull { it.first == key }?.second ?: Icons.Filled.Block
    }
}
