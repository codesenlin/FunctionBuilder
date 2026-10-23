package com.functionbuilder.ui

/** 路由名。页面之间需要传递的状态统一放在 [FbViewModel] 里，避免 nav 参数转义问题。 */
object Routes {
    const val PACKS = "packs"
    const val FUNCTIONS = "functions"
    const val EDITOR = "editor"
    const val PACK_META = "pack_meta"
    const val WORLDS = "worlds"
    const val SETTINGS = "settings"
    const val BATCH = "batch"
    const val APPS = "apps"
}
