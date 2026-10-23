package com.example.data.repository

import com.example.data.local.AetherDao
import com.example.data.local.MacroAutomationStepEntity
import com.example.data.local.VoiceMacroEntity
import com.example.data.local.VoiceMacroWithSteps
import com.example.data.security.E2EEVault
import com.example.domain.MacroActionStep
import com.example.domain.MacroSequenceCodec
import com.example.domain.MacroStepActionType
import kotlinx.coroutines.flow.Flow

/**
 * Clean Architecture Repository abstracting Room Database persistence for user-defined
 * voice-activated macros and their ordered UI automation command sequences.
 */
class VoiceMacroRepository(private val dao: AetherDao) {

    val allMacros: Flow<List<VoiceMacroEntity>> = dao.observeMacros()

    val allMacrosWithSteps: Flow<List<VoiceMacroWithSteps>> = dao.observeMacrosWithSteps()

    suspend fun getMacroWithSteps(macroId: Int): VoiceMacroWithSteps? {
        return dao.getMacroWithStepsById(macroId)
    }

    suspend fun findMacroByTrigger(triggerPhrase: String): VoiceMacroWithSteps? {
        return dao.findMacroWithStepsByTrigger(triggerPhrase.trim())
    }

    suspend fun getOrderedAutomationSteps(macroId: Int): List<MacroAutomationStepEntity> {
        return dao.getAutomationStepsForMacro(macroId)
    }

    suspend fun saveVoiceMacro(
        editingId: Int? = null,
        title: String,
        triggerPhrase: String,
        steps: List<MacroActionStep>,
        isOfflineCritical: Boolean
    ): Long {
        val cleanTitle = title.trim()
        val cleanTrigger = triggerPhrase.trim().lowercase()
        val encodedChain = MacroSequenceCodec.encodeStepsToChain(steps)
        val macroEntity = VoiceMacroEntity(
            id = editingId ?: 0,
            title = cleanTitle,
            triggerPhrase = cleanTrigger,
            stepsJson = encodedChain,
            isOfflineCritical = isOfflineCritical,
            e2eeSignature = E2EEVault.sha256Fingerprint(cleanTrigger + encodedChain)
        )
        val stepEntities = steps.mapIndexed { index, step ->
            MacroAutomationStepEntity(
                macroId = editingId ?: 0,
                stepOrder = index,
                actionType = step.actionType.code,
                selectorType = step.actionType.category.name,
                targetValue = step.targetValue,
                continueOnError = step.continueOnError
            )
        }
        return dao.saveMacroWithAutomationSteps(macroEntity, stepEntities)
    }

    suspend fun recordMacroExecution(macroId: Int) {
        dao.incrementMacroRun(macroId)
    }

    suspend fun deleteVoiceMacro(macroId: Int) {
        dao.deleteMacro(macroId)
    }

    companion object {
        fun mapStepEntitiesToDomain(entities: List<MacroAutomationStepEntity>): List<MacroActionStep> {
            return entities.sortedBy { it.stepOrder }.map { entity ->
                val matchedType = MacroStepActionType.entries.firstOrNull {
                    it.code.equals(entity.actionType, ignoreCase = true)
                } ?: MacroStepActionType.OPEN_APP
                MacroActionStep(
                    id = "db_step_${entity.stepId}_${entity.stepOrder}",
                    actionType = matchedType,
                    targetValue = entity.targetValue,
                    continueOnError = entity.continueOnError
                )
            }
        }
    }
}
