package com.example.rendinxr.core.presentation.navigation

sealed class Screen(val route: String) {
    object Scan : Screen("scan")
    object Review : Screen("review")
}