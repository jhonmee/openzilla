package com.openzilla.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openzilla.app.data.AppSettings
import com.openzilla.app.ui.addhabit.AddHabitScreen
import com.openzilla.app.ui.detail.HabitDetailScreen
import com.openzilla.app.ui.home.HomeScreen
import com.openzilla.app.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val ADD_HABIT = "add_habit?habitId={habitId}"
    const val DETAIL = "detail/{habitId}"
    const val SETTINGS = "settings"
    fun addHabit(habitId: Long? = null) = if (habitId == null) "add_habit" else "add_habit?habitId=$habitId"
    fun detail(habitId: Long) = "detail/$habitId"
}

@Composable
fun OpenZillaNavHost(settings: AppSettings) {
    val navController = rememberNavController()

    // Cambios de pantalla instantáneos: sin fundidos ni desplazamientos. Además de ser lo
    // que se pidió, es la opción más barata posible — no hay ninguna animación que componer
    // ni ninguna capa extra que dibujar durante la navegación.
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddHabit = { navController.navigate(Routes.addHabit()) },
                onOpenHabit = { id -> navController.navigate(Routes.detail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.ADD_HABIT,
            arguments = listOf(navArgument("habitId") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            val habitId = entry.arguments?.getLong("habitId") ?: -1L
            AddHabitScreen(
                editingHabitId = if (habitId == -1L) null else habitId,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) { entry ->
            val habitId = entry.arguments?.getLong("habitId") ?: return@composable
            HabitDetailScreen(
                habitId = habitId,
                currencySymbol = settings.currencySymbol,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.addHabit(id)) },
                onDeleted = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
