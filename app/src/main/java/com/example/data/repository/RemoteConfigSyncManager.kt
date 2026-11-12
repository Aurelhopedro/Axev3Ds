package com.example.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AXEV/SyncManager"

/**
 * RemoteConfigSyncManager — Opção C
 *
 * Combina:
 *  - Polling automático a cada [pollIntervalMs] milissegundos
 *  - Método [syncNow] para forçar sincronização imediata (botão manual)
 *
 * Uso típico (ViewModel ou Application):
 *
 *   val syncManager = RemoteConfigSyncManager(context)
 *   syncManager.start()
 *
 *   // Observar flags em tempo real:
 *   syncManager.flags.collect { flags -> ... }
 *
 *   // Observar workflows em tempo real:
 *   syncManager.workflowConfig.collect { config -> ... }
 *
 *   // Estado de sincronização (idle / syncing / success / error):
 *   syncManager.syncState.collect { state -> ... }
 *
 *   // Botão "Sincronizar":
 *   syncManager.syncNow()
 *
 *   // Parar quando a app vai para background (opcional):
 *   syncManager.stop()
 */
class RemoteConfigSyncManager(
    private val context: Context,
    private val pollIntervalMs: Long = 5 * 60 * 1000L   // 5 minutos por defeito
) {

    // ── Repositórios ──────────────────────────────────────────────────────────

    private val flagsRepo          = FlagsRepository(context)
    private val remoteConfigRepo   = RemoteConfigRepository(context)

    // ── Scope de coroutines ───────────────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var isSyncing = false

    // ── Flows públicos ────────────────────────────────────────────────────────

    private val _flags = MutableStateFlow<FlagsRepository.AppFlags?>(null)
    val flags: StateFlow<FlagsRepository.AppFlags?> = _flags.asStateFlow()

    private val _workflowConfig = MutableStateFlow<RemoteConfigRepository.RemoteConfig?>(null)
    val workflowConfig: StateFlow<RemoteConfigRepository.RemoteConfig?> = _workflowConfig.asStateFlow()

    sealed class SyncState {
        object Idle    : SyncState()
        object Syncing : SyncState()
        data class Success(val timestampMs: Long = System.currentTimeMillis()) : SyncState()
        data class Error(val message: String)   : SyncState()
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Inicia o polling automático.
     * Faz uma sincronização imediata ao arrancar e depois repete a cada [pollIntervalMs].
     */
    fun start() {
        if (pollingJob?.isActive == true) return
        Log.i(TAG, "start: polling cada ${pollIntervalMs / 1000}s")
        pollingJob = scope.launch {
            while (isActive) {
                doSync()
                delay(pollIntervalMs)
            }
        }
    }

    /**
     * Para o polling automático.
     */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        Log.i(TAG, "stop: polling parado")
    }

    /**
     * Força uma sincronização imediata (botão manual "Sincronizar").
     * Ignora se já houver uma sincronização em curso.
     */
    fun syncNow() {
        if (isSyncing) {
            Log.d(TAG, "syncNow: já em curso, ignorado")
            return
        }
        scope.launch { doSync() }
    }

    // ── Sincronização interna ─────────────────────────────────────────────────

    private suspend fun doSync() {
        if (isSyncing) return
        isSyncing = true
        _syncState.value = SyncState.Syncing
        Log.d(TAG, "doSync: a sincronizar…")

        try {
            // Flags e workflow-config em paralelo
            val flagsDeferred  = scope.async { flagsRepo.load() }
            val configDeferred = scope.async { fetchWorkflowConfig() }

            val newFlags  = flagsDeferred.await()
            val newConfig = configDeferred.await()

            _flags.value          = newFlags
            _workflowConfig.value = newConfig

            Log.i(TAG, "doSync: ✅ sync concluído (maintenance=${newFlags.maintenanceMode}, workflows=${newConfig?.workflows?.size ?: 0})")
            _syncState.value = SyncState.Success()

        } catch (e: Exception) {
            Log.e(TAG, "doSync: erro — ${e.message}", e)
            _syncState.value = SyncState.Error(e.message ?: "Erro desconhecido")
        } finally {
            isSyncing = false
        }
    }

    private suspend fun fetchWorkflowConfig(): RemoteConfigRepository.RemoteConfig? {
        return try {
            // Reutiliza o fetch interno via reflexão do campo privado não é possível,
            // por isso chamamos fetchAllWorkflows que é público e devolve os YAMLs.
            // Para o config (metadados), usamos fetchWorkflowIfNewer como proxy.
            // Se quiseres expor fetchConfig como internal, faz-o no RemoteConfigRepository.
            null  // placeholder — ver nota abaixo
        } catch (e: Exception) {
            null
        }
    }

    // ── Limpeza ───────────────────────────────────────────────────────────────

    fun destroy() {
        scope.cancel()
        Log.i(TAG, "destroy: scope cancelado")
    }
}
