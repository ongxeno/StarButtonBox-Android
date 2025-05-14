package com.ongxeno.android.starbuttonbox.ui.screen.managemacros

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.data.Macro

/**
 * Screen for managing (viewing) macros with search functionality in the AppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMacrosScreen(
    viewModel: ManageMacrosViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val filteredMacros by viewModel.filteredMacrosState.collectAsStateWithLifecycle()
    val searchTerm by viewModel.searchTerm.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()

    val rowNumberColumnWidth: Dp = 32.dp
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField( // Using BasicTextField or TextField for more control
                            value = searchTerm,
                            onValueChange = { viewModel.onSearchTermChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .padding(end = 0.dp), // Remove padding to allow clear button to be closer
                            placeholder = { Text("Search Macros...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                // Optionally, trigger search explicitly if needed, though debounce handles it
                            }),
                            colors = TextFieldDefaults.colors( // Use colors for TextField
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, // No underline
                                unfocusedIndicatorColor = Color.Transparent, // No underline
                                disabledIndicatorColor = Color.Transparent, // No underline
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface // Ensure text color matches theme
                            )
                        )
                    } else {
                        Text("Manage Macros")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            viewModel.setSearchActive(false) // Deactivate search and clear term
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            // Show back arrow normally, or if search is active (acts as "close search")
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isSearchActive) "Close Search" else "Back"
                        )
                    }
                },
                actions = {
                    if (isSearchActive) {
                        // Clear button for the search text field
                        if (searchTerm.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchTermChanged("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear Search Text"
                                )
                            }
                        }
                    } else {
                        // Search action icon to activate search mode
                        IconButton(onClick = { viewModel.setSearchActive(true) }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Macros"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Fixed Header Row (No changes here)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", Modifier.width(rowNumberColumnWidth), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                Text("Title", Modifier.weight(1f).padding(start = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Description", Modifier.weight(1.5f).padding(start = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            // Scrollable Content (No changes here, uses filteredMacros)
            if (filteredMacros.isEmpty()) {
                val message = if (searchTerm.isBlank() && !isSearchActive) { // Adjusted condition
                    "No macros found."
                } else {
                    "No macros match your search for \"$searchTerm\"."
                }
                Text(message, Modifier.fillMaxWidth().padding(16.dp).weight(1f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(filteredMacros, key = { _, macro -> macro.id }) { index, macro ->
                        MacroListItem(index = index, macro = macro, rowNumberWidth = rowNumberColumnWidth)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroListItem(index: Int, macro: Macro, rowNumberWidth: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text((index + 1).toString(), Modifier.width(rowNumberWidth), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Text(macro.title, Modifier.weight(1f).padding(start = 8.dp, end = 8.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(macro.description, Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
