package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MetroViewModel
import com.example.ui.screens.CharactersScreen
import com.example.ui.screens.DailyRewardsScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StagesScreen
import com.example.ui.theme.MetroBgCard
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroDashTheme
import com.example.ui.theme.MetroTextPrimary

object MetroRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GAME = "game/{stage}"
    const val STAGES = "stages"
    const val CHARACTERS = "characters"
    const val SHOP = "shop"
    const val MISSIONS = "missions"
    const val DAILY = "daily"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val PRIVACY = "privacy"

    fun gameRoute(stage: Int): String = "game/$stage"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MetroDashTheme {
                MetroDashApp()
            }
        }
    }
}

@Composable
fun MetroDashApp(viewModel: MetroViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val runSummary by viewModel.lastRunSummary.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = MetroRoutes.SPLASH
        ) {
            composable(MetroRoutes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        viewModel.startMenuMusic()
                        navController.navigate(MetroRoutes.HOME) {
                            popUpTo(MetroRoutes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(MetroRoutes.HOME) {
                HomeScreen(
                    state = state,
                    onPlayStage = { stage ->
                        viewModel.startMenuMusic()
                        navController.navigate(MetroRoutes.gameRoute(stage))
                    },
                    onOpenStages = { navController.navigate(MetroRoutes.STAGES) },
                    onOpenCharacters = { navController.navigate(MetroRoutes.CHARACTERS) },
                    onOpenShop = { navController.navigate(MetroRoutes.SHOP) },
                    onOpenMissions = { navController.navigate(MetroRoutes.MISSIONS) },
                    onOpenDailyRewards = { navController.navigate(MetroRoutes.DAILY) },
                    onOpenSettings = { navController.navigate(MetroRoutes.SETTINGS) },
                    onOpenHelp = { navController.navigate(MetroRoutes.HELP) },
                    onOpenPrivacy = { navController.navigate(MetroRoutes.PRIVACY) },
                    onUpdateName = { viewModel.updatePlayerName(it) }
                )
            }

            composable(
                route = MetroRoutes.GAME,
                arguments = listOf(navArgument("stage") { type = NavType.IntType })
            ) { entry ->
                val stage = entry.arguments?.getInt("stage") ?: state.profile.currentStage
                GameScreen(
                    stageNumber = stage,
                    state = state,
                    viewModel = viewModel,
                    runSummary = runSummary,
                    onNavigateNextStage = { next ->
                        viewModel.selectStage(next)
                        navController.navigate(MetroRoutes.gameRoute(next)) {
                            popUpTo(MetroRoutes.HOME)
                        }
                    },
                    onExitHome = {
                        navController.popBackStack(MetroRoutes.HOME, inclusive = false)
                    }
                )
            }

            composable(MetroRoutes.STAGES) {
                StagesScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSelectAndPlayStage = { stage ->
                        viewModel.selectStage(stage)
                        navController.navigate(MetroRoutes.gameRoute(stage))
                    }
                )
            }

            composable(MetroRoutes.CHARACTERS) {
                CharactersScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onBuyCharacter = { viewModel.buyCharacter(it) },
                    onEquipCharacter = { viewModel.equipCharacter(it) },
                    onBuyOutfit = { viewModel.buyOutfit(it) },
                    onEquipOutfit = { viewModel.equipOutfit(it) }
                )
            }

            composable(MetroRoutes.SHOP) {
                ShopScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onBuyCharacter = { viewModel.buyCharacter(it) },
                    onEquipCharacter = { viewModel.equipCharacter(it) },
                    onBuyOutfit = { viewModel.buyOutfit(it) },
                    onEquipOutfit = { viewModel.equipOutfit(it) },
                    onBuyHoverboard = { viewModel.buyHoverboard(it) },
                    onEquipHoverboard = { viewModel.equipHoverboard(it) },
                    onBuyHoverboardCharges = { viewModel.buyHoverboardCharges() },
                    onUpgradePowerUp = { viewModel.upgradePowerUp(it) },
                    onExchangeDiamonds = { viewModel.exchangeDiamonds(it) }
                )
            }

            composable(MetroRoutes.MISSIONS) {
                MissionsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onClaimMission = { viewModel.claimMission(it) }
                )
            }

            composable(MetroRoutes.DAILY) {
                DailyRewardsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onClaimDaily = { viewModel.claimDailyReward() }
                )
            }

            composable(MetroRoutes.SETTINGS) {
                SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenPrivacy = { navController.navigate(MetroRoutes.PRIVACY) },
                    onOpenHelp = { navController.navigate(MetroRoutes.HELP) }
                )
            }

            composable(MetroRoutes.HELP) {
                HelpSupportScreen(
                    state = state,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(MetroRoutes.PRIVACY) {
                PrivacyPolicyScreen(
                    state = state,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MetroBgCard,
                contentColor = MetroTextPrimary,
                actionColor = MetroCyan
            )
        }
    }
}
