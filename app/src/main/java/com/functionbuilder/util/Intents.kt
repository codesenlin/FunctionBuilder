package com.functionbuilder.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** 打开网页 / QQ 会话 / QQ 群，失败时静默返回 false（旧版是直接崩或者闪一下）。 */
object Intents {

    fun openUrl(context: Context, url: String): Boolean = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)

    /** 对应旧版 `mqqwpa://im/chat?chat_type=wpa&uin=...`。 */
    fun openQqChat(context: Context, uin: String, title: String = "QQ聊天窗口"): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mqqwpa://im/chat?chat_type=wpa&uin=$uin&version=1"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    /** 对应旧版 `mqqapi://card/show_pslcard?...&card_type=group`。 */
    fun openQqGroup(context: Context, uin: String, title: String = "选择QQ"): Boolean = runCatching {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$uin&card_type=group&source=qrcode"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
