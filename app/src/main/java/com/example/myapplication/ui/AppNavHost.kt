package com.example.myapplication.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.AppContainer
import com.example.myapplication.ui.camera.CameraScreen
import com.example.myapplication.ui.composer.ComposerScreen
import com.example.myapplication.ui.composer.PICKED_PLACE_KEY
import com.example.myapplication.ui.composer.encodePickedPlace
import com.example.myapplication.ui.feed.FeedScreen
import com.example.myapplication.ui.location.LocationPickerScreen

private object Routes {
    const val FEED = "feed"
    const val CAPTURE = "capture"
    const val COMPOSE = "compose"
    const val PICK_PLACE = "pickPlace"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.FEED) {
        composable(Routes.FEED) {
            FeedScreen(
                repository = container.postRepository,
                onAddPhoto = { navController.navigate(Routes.CAPTURE) },
            )
        }

        composable(Routes.CAPTURE) {
            CameraScreen(
                onClose = { navController.popBackStack() },
                onCaptured = { file ->
                    navController.navigate("${Routes.COMPOSE}?photo=${Uri.encode(file.absolutePath)}") {
                        popUpTo(Routes.CAPTURE) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = "${Routes.COMPOSE}?photo={photo}",
            arguments = listOf(navArgument("photo") { type = NavType.StringType }),
        ) { entry ->
            ComposerScreen(
                photoPath = entry.arguments?.getString("photo").orEmpty(),
                container = container,
                savedStateHandle = entry.savedStateHandle,
                onClose = { navController.popBackStack() },
                onPublished = { navController.popBackStack(Routes.FEED, inclusive = false) },
                onPickPlace = { latitude, longitude ->
                    navController.navigate("${Routes.PICK_PLACE}?lat=$latitude&lng=$longitude")
                },
            )
        }

        composable(
            route = "${Routes.PICK_PLACE}?lat={lat}&lng={lng}",
            arguments = listOf(
                navArgument("lat") {
                    type = NavType.StringType
                    defaultValue = "0"
                },
                navArgument("lng") {
                    type = NavType.StringType
                    defaultValue = "0"
                },
            ),
        ) { entry ->
            LocationPickerScreen(
                initialLatitude = entry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0,
                initialLongitude = entry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0,
                container = container,
                onClose = { navController.popBackStack() },
                onPicked = { place ->
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(PICKED_PLACE_KEY, encodePickedPlace(place))
                    navController.popBackStack()
                },
            )
        }
    }
}
