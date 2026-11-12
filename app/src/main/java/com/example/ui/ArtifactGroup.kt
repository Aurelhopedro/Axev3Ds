package com.example.ui

import com.example.data.api.GeneratedFile
import com.example.data.api.WorkflowRun

/**
 * Agrupa um WorkflowRun com os seus artifacts disponíveis.
 * Usado pelo DownloadManagerScreen para listar todas as gerações passadas.
 */
data class ArtifactGroup(
    val run: WorkflowRun,
    val files: List<GeneratedFile>
)
