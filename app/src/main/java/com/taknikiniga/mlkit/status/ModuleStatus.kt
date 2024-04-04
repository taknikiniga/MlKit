package com.taknikiniga.mlkit.status

sealed class ModuleStatus {
    data class hasModuleInstalled(var hasInstalled:Int):ModuleStatus()
    data class moduleInstallProgress(var progress:Int) : ModuleStatus()
    data object terminated : ModuleStatus()

}