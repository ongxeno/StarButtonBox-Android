package com.ongxeno.android.starbuttonbox.datasource

import com.ongxeno.android.starbuttonbox.datasource.room.MacroDao
import com.ongxeno.android.starbuttonbox.datasource.room.MacroEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MacroRepository that uses the Room MacroDao as its data source.
 */
@Singleton // Ensure only one instance of the repository
class MacroRepositoryImpl @Inject constructor(
    private val macroDao: MacroDao // Inject the DAO provided by Hilt
) : MacroRepository {

    override fun getAllMacros(): Flow<List<MacroEntity>> {
        // Directly return the Flow from the DAO
        return macroDao.getAllMacros()
    }

    override fun getMacrosByGameId(gameId: String): Flow<List<MacroEntity>> {
        return macroDao.getMacrosByGameId(gameId)
    }

    override suspend fun getMacroById(macroId: String): MacroEntity? {
        return macroDao.getMacroById(macroId)
    }

    override suspend fun addOrUpdateMacro(macro: MacroEntity) {
        // Use the DAO's insert-or-replace strategy
        macroDao.insertOrUpdateMacro(macro)
    }

    override suspend fun addOrUpdateAllMacros(macros: List<MacroEntity>) {
        macroDao.insertOrUpdateAllMacros(macros)
    }

    override suspend fun deleteMacroById(macroId: String) {
        macroDao.deleteMacroById(macroId)
    }

    override suspend fun deleteMacro(macro: MacroEntity) {
        macroDao.deleteMacro(macro)
    }

    override suspend fun clearAllMacros() {
        macroDao.clearAllMacros()
    }

    // If we needed mapping between MacroEntity and a domain Macro, it would happen here.
    // Example:
    // override fun getAllMacrosDomain(): Flow<List<Macro>> {
    //     return macroDao.getAllMacros().map { entityList ->
    //         entityList.map { it.toDomainModel() } // Assuming an extension function toDomainModel()
    //     }
    // }
}
