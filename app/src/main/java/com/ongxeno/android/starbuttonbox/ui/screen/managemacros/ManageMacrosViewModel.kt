package com.ongxeno.android.starbuttonbox.ui.screen.managemacros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.Macro
import com.ongxeno.android.starbuttonbox.data.toUi
import com.ongxeno.android.starbuttonbox.datasource.MacroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the ManageMacrosScreen.
 * Includes search/filter functionality.
 *
 * @param macroRepository Repository to fetch macro data.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class) // For debounce and flatMapLatest/combine
@HiltViewModel
class ManageMacrosViewModel @Inject constructor(
    private val macroRepository: MacroRepository
) : ViewModel() {

    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()

    // State to control if the search UI is active in the AppBar
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // Original flow of all macros from the repository
    private val _allMacros: StateFlow<List<Macro>> = macroRepository.getAllMacros()
        .map { entityList ->
            entityList.map { entity -> entity.toUi() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * StateFlow that emits the filtered list of macros based on the searchTerm.
     */
    val filteredMacrosState: StateFlow<List<Macro>> = combine(
        _allMacros,
        _searchTerm.debounce(300) // Add debounce to avoid filtering on every keystroke
    ) { macros, term ->
        if (term.isBlank()) {
            macros // Return all macros if search term is blank
        } else {
            filterMacros(macros, term)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList() // Initial value before combination/filtering
    )

    /**
     * Updates the search term.
     * @param newTerm The new search query.
     */
    fun onSearchTermChanged(newTerm: String) {
        _searchTerm.value = newTerm
    }

    /**
     * Toggles the search UI in the AppBar.
     * Clears the search term when deactivating search.
     * @param isActive True to activate search mode, false to deactivate.
     */
    fun setSearchActive(isActive: Boolean) {
        _isSearchActive.value = isActive
        if (!isActive) {
            _searchTerm.value = "" // Clear search term when search is deactivated
        }
    }

    /**
     * Filters a list of macros based on a search query.
     * Results are ordered with partial (substring) matches appearing before
     * ordered character (fuzzy) matches.
     *
     * @param macros The original list of macros.
     * @param query The search query.
     * @return A new list containing macros that match the query, prioritized.
     */
    private fun filterMacros(macros: List<Macro>, query: String): List<Macro> {
        val lowerQuery = query.lowercase().trim()
        if (lowerQuery.isEmpty()) return macros

        val partialMatchResults = mutableListOf<Macro>()
        val orderedCharMatchResults = mutableListOf<Macro>()

        macros.forEach { macro ->
            val fieldsToSearch = listOf(
                macro.title.lowercase(),
                macro.description.lowercase(),
                macro.label.lowercase()
            )

            // Check for partial text match (substring)
            val isPartialMatch = fieldsToSearch.any { field ->
                field.contains(lowerQuery)
            }

            if (isPartialMatch) {
                partialMatchResults.add(macro)
            } else {
                // If not a partial match, check for ordered character match
                val isOrderedMatch = fieldsToSearch.any { field ->
                    isOrderedCharacterMatch(field, lowerQuery)
                }
                if (isOrderedMatch) {
                    orderedCharMatchResults.add(macro)
                }
            }
        }

        // Combine results: partial matches first, then ordered character matches
        // (orderedCharMatchResults will not contain items already in partialMatchResults
        // due to the 'else' condition above)
        return partialMatchResults + orderedCharMatchResults
    }

    /**
     * Checks if all characters in the query appear in the target string,
     * in the same order, but not necessarily contiguously. Case-insensitive.
     *
     * Example: target="Flight Ready", query="flrdy" -> true
     *
     * @param target The string to search within.
     * @param query The query string with characters to find in order.
     * @return True if it's an ordered character match, false otherwise.
     */
    private fun isOrderedCharacterMatch(target: String, query: String): Boolean {
        if (query.isEmpty()) return true // Empty query matches everything
        if (target.isEmpty()) return false // Cannot match in empty target

        var queryIndex = 0
        var targetIndex = 0

        while (queryIndex < query.length && targetIndex < target.length) {
            if (query[queryIndex] == target[targetIndex]) {
                queryIndex++
            }
            targetIndex++
        }
        // If we've gone through all characters in the query, it's a match
        return queryIndex == query.length
    }
}
