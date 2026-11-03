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
import com.example.myapplication.ui.detail.DetailScreen
import com.example.myapplication.ui.feed.FeedScreen

private object Routes {
    const val FEED = "feed"
    const val CAPTURE = "capture"
    const val COMPOSE = "compose"
    const val DETAIL = "detail"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.FEED) {
        composable(Routes.FEED) {
            FeedScreen(
                repository = container.postRepository,
                onAddPhoto = { navController.navigate(Routes.CAPTURE) },
                onOpenDetail = { postId -> navController.navigate("${Routes.DETAIL}/$postId") },
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
                onClose = { navController.popBackStack() },
                onPublished = { navController.popBackStack(Routes.FEED, inclusive = false) },
            )
        }

        composable(
            route = "${Routes.DETAIL}/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.LongType }),
        ) { entry ->
            DetailScreen(
                postId = entry.arguments?.getLong("postId") ?: 0L,
                repository = container.postRepository,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}
