package com.example.bands.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.bands.BandsViewModel


@Composable
fun StatusScreen(navController: NavController, viewModel: BandsViewModel) {
    Text(text = "Status list")
    BottomNavigationMenu(selectedItem = BottomNavigationItem.STATUSLIST, navController = navController )
}