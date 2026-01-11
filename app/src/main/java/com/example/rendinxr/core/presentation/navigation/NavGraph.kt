package com.example.rendinxr.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.rendinxr.feature.review.presentation.ReviewScreen
import com.example.rendinxr.feature.scan.presentation.ScanScreen

@Composable
fun NavGraph(
    navController: NavHostController,
) {

    NavHost(
        navController = navController,
        startDestination = Screen.Scan.route
    ) {
        composable(route = Screen.Scan.route) {
            ScanScreen(
                onNavigateToReview = {
                    navController.navigate(Screen.Review.route)
                }
            )
        }

        composable(route = Screen.Review.route) {
            ReviewScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}