package com.openzilla.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
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

/**
 * Corta a propósito: lo justo para que se lea de dónde viene y a dónde va la pantalla, sin
 * que se note espera. Es también el tiempo que dura la animación de vuelta cuando el gesto
 * de retroceso predictivo se suelta.
 */
private const val NAV_DURATION_MILLIS = 250

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideIn(
    towards: AnimatedContentTransitionScope.SlideDirection
) = slideIntoContainer(towards, tween(NAV_DURATION_MILLIS, easing = FastOutSlowInEasing)) +
    fadeIn(tween(NAV_DURATION_MILLIS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOut(
    towards: AnimatedContentTransitionScope.SlideDirection
) = slideOutOfContainer(towards, tween(NAV_DURATION_MILLIS, easing = FastOutSlowInEasing)) +
    fadeOut(tween(NAV_DURATION_MILLIS))

@Composable
fun OpenZillaNavHost(settings: AppSettings) {
    val navController = rememberNavController()

    // Avanzar desplaza hacia la izquierda y volver hacia la derecha, que es el movimiento que
    // el sistema espera. Al mantener el gesto de retroceso, Android va aplicando por su cuenta
    // el progreso del gesto a estas mismas transiciones: por eso se puede ir viendo la
    // pantalla anterior y cancelar el gesto a medias sin llegar a salir.
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { slideIn(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOut(AnimatedContentTransitionScope.SlideDirection.Start) },
        popEnterTransition = { slideIn(AnimatedContentTransitionScope.SlideDirection.End) },
        popExitTransition = { slideOut(AnimatedContentTransitionScope.SlideDirection.End) }
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
