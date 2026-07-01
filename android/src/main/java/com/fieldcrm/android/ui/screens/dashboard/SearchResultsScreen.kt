package com.fieldcrm.android.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.SearchViewModel
import org.koin.androidx.compose.koinViewModel

data class SearchResultApplication(val name: String, val refNo: String)
data class SearchResultDocument(val title: String, val type: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    onBackClick: () -> Unit,
    onNavigateToApplication: (String) -> Unit
) {
    val searchViewModel: SearchViewModel = koinViewModel()
    val searchState by searchViewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        searchViewModel.search(searchQuery)
    }

    val recentSearches = listOf("Kalu", "MMFB-041", "Adaeze", "Guarantor")

    val filteredApps = searchState.results?.applications?.map {
        SearchResultApplication(it.borrower_name, it.ref_no)
    } ?: emptyList()

    val filteredDocs = emptyList<SearchResultDocument>()

    val debouncedQuery = searchQuery

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val searchBarHeight = if (isTablet) 44.dp else 48.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search applications or references...",
                                style = FieldTheme.typography.body,
                                color = FieldTheme.colors.gray500
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = FieldIcons.SearchOutlined,
                                contentDescription = "Search",
                                tint = FieldTheme.colors.gray500
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp) // Minimum touch target
                                ) {
                                    Icon(
                                        imageVector = FieldIcons.CloseOutlined,
                                        contentDescription = "Clear",
                                        tint = FieldTheme.colors.gray400
                                    )
                                }
                            }
                        },
                        textStyle = FieldTheme.typography.bodyStrong.copy(color = FieldTheme.colors.gray100),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FieldTheme.colors.gray900,
                            unfocusedContainerColor = FieldTheme.colors.gray900,
                            focusedBorderColor = FieldTheme.colors.purple600,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(searchBarHeight)
                            .padding(end = 12.dp),
                        shape = RoundedCornerShape(FieldTheme.shapes.inputRadius)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp) // Minimum touch target
                    ) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FieldTheme.colors.gray950
                )
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            if (searchQuery.isBlank()) {
                // Recent searches view (chip layout)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "RECENT SEARCHES",
                        style = FieldTheme.typography.label,
                        color = FieldTheme.colors.gray500
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentSearches.forEach { search ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = FieldTheme.colors.gray900,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { searchQuery = search }
                                    .padding(horizontal = 16.dp, vertical = 8.dp) // Accessible padding
                            ) {
                                Text(
                                    text = search,
                                    style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                    color = FieldTheme.colors.gray300
                                )
                            }
                        }
                    }
                }
            } else if (searchState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FieldTheme.colors.purple600)
                }
            } else if (debouncedQuery.isNotEmpty() && filteredApps.isEmpty() && filteredDocs.isEmpty()) {
                // No results state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = FieldIcons.ShieldOutlined, // Shield brand mark substitute
                        contentDescription = "No Results",
                        tint = FieldTheme.colors.purple600,
                        modifier = Modifier
                            .size(64.dp)
                            .alpha(0.3f) // Shield mark at 30% opacity
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No results for '$debouncedQuery'",
                        style = FieldTheme.typography.display.copy(fontSize = 18.sp),
                        color = FieldTheme.colors.gray100,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check the spelling or try the reference number format.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp)
                    )
                }
            } else {
                // Display search results: List content width centered on Tablet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (isTablet) 32.dp else 0.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = if (isTablet) 600.dp else Dp.Unspecified)
                            .fillMaxWidth()
                    ) {
                        // Applications Section
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray950)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "APPLICATIONS (${filteredApps.size})",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                            }
                        }
                        
                        if (filteredApps.isNotEmpty()) {
                            items(filteredApps) { app ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clickable { onNavigateToApplication(app.refNo) },
                                    colors = CardDefaults.cardColors(containerColor = FieldTheme.colors.gray900),
                                    shape = RoundedCornerShape(FieldTheme.shapes.cardRadius)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = FieldIcons.BadgeOutlined,
                                            contentDescription = "Application",
                                            tint = FieldTheme.colors.purple400,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.name,
                                                style = FieldTheme.typography.bodyStrong,
                                                color = FieldTheme.colors.gray100
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = app.refNo,
                                                style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                                color = FieldTheme.colors.gray400
                                            )
                                        }
                                        Icon(
                                            imageVector = FieldIcons.ChevronRightOutlined,
                                            contentDescription = "Go",
                                            tint = FieldTheme.colors.gray500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = FieldTheme.colors.gray900.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(FieldTheme.shapes.cardRadius)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "No matching applications",
                                            style = FieldTheme.typography.body,
                                            color = FieldTheme.colors.gray500
                                        )
                                    }
                                }
                            }
                        }

                        // Documents Section
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray950)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "DOCUMENTS (${filteredDocs.size})",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                            }
                        }
                        
                        if (filteredDocs.isNotEmpty()) {
                            items(filteredDocs) { doc ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clickable { /* preview document */ },
                                    colors = CardDefaults.cardColors(containerColor = FieldTheme.colors.gray900),
                                    shape = RoundedCornerShape(FieldTheme.shapes.cardRadius)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = FieldIcons.DocumentOutlined,
                                            contentDescription = "Document",
                                            tint = FieldTheme.colors.purple400,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doc.title,
                                                style = FieldTheme.typography.bodyStrong,
                                                color = FieldTheme.colors.gray100
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = doc.type,
                                                style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                                                color = FieldTheme.colors.gray500
                                            )
                                        }
                                        Icon(
                                            imageVector = FieldIcons.ChevronRightOutlined,
                                            contentDescription = "Go",
                                            tint = FieldTheme.colors.gray500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = FieldTheme.colors.gray900.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(FieldTheme.shapes.cardRadius)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "No matching documents",
                                            style = FieldTheme.typography.body,
                                            color = FieldTheme.colors.gray500
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
