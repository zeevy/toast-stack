package com.siliconcircuits.toaststack.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.toaststack.ExperimentalToastStackApi
import com.siliconcircuits.toaststack.ToastStackHost
import com.siliconcircuits.toaststack.rememberToastStackState
import com.siliconcircuits.toaststack.demo.tabs.ActionsTab
import com.siliconcircuits.toaststack.demo.tabs.AdvancedTab
import com.siliconcircuits.toaststack.demo.tabs.AnimationsTab
import com.siliconcircuits.toaststack.demo.tabs.PositionTab
import com.siliconcircuits.toaststack.demo.tabs.StyleTab
import com.siliconcircuits.toaststack.demo.tabs.TypesTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalToastStackApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                DemoScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

private enum class DemoTab(val title: String) {
    Position("Position"),
    Types("Types"),
    Style("Style"),
    Animations("Motion"),
    Actions("Actions"),
    Advanced("Advanced")
}

@OptIn(ExperimentalToastStackApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DemoScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val toastState = rememberToastStackState(maxVisible = 5)
    val tabs = DemoTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text("ToastStack", fontWeight = FontWeight.Bold)
                        },
                        actions = {
                            Surface(
                                onClick = onToggleDarkMode,
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = if (isDarkMode) "Light" else "Dark",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface,
                        edgePadding = 16.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (pagerState.currentPage == index) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                when (tabs[page]) {
                    DemoTab.Position -> PositionTab(toastState)
                    DemoTab.Types -> TypesTab(toastState)
                    DemoTab.Style -> StyleTab(toastState)
                    DemoTab.Animations -> AnimationsTab(toastState)
                    DemoTab.Actions -> ActionsTab(toastState)
                    DemoTab.Advanced -> AdvancedTab(toastState)
                }
            }
        }

        ToastStackHost(state = toastState)
    }
}
