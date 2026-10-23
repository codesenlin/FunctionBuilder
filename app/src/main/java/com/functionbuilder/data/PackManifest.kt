package com.functionbuilder.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 行为包 `manifest.json` 的读写。
 *
 * 对应旧版的 `src/json.myu`：读/写 header.name、header.description、
 * modules[0].description，以及“保存”时重新生成 header 与 modules 的 uuid。
 */
object PackManifest {

    fun parse(text: String): JSONObject? =
        runCatching { JSONObject(text) }.getOrNull()

    fun pretty(o: JSONObject): String = o.toString(4)

    fun header(o: JSONObject): JSONObject? = o.optJSONObject("header")

    fun modules(o: JSONObject): JSONArray? = o.optJSONArray("modules")

    fun headerName(o: JSONObject?): String =
        o?.optJSONObject("header")?.optString("name").orEmpty()

    fun headerDescription(o: JSONObject?): String =
        o?.optJSONObject("header")?.optString("description").orEmpty()

    fun headerUuid(o: JSONObject?): String =
        o?.optJSONObject("header")?.optString("uuid").orEmpty()

    fun moduleDescription(o: JSONObject?): String =
        o?.optJSONArray("modules")?.optJSONObject(0)?.optString("description").orEmpty()

    fun withName(o: JSONObject, name: String): JSONObject = o.apply {
        val h = optJSONObject("header") ?: JSONObject().also { put("header", it) }
        h.put("name", name)
    }

    fun withDescription(o: JSONObject, description: String): JSONObject = o.apply {
        val h = optJSONObject("header") ?: JSONObject().also { put("header", it) }
        h.put("description", description)
    }

    fun withModuleDescription(o: JSONObject, description: String): JSONObject = o.apply {
        val list = optJSONArray("modules") ?: JSONArray().also { put("modules", it) }
        if (list.length() == 0) list.put(JSONObject())
        val first = list.optJSONObject(0) ?: JSONObject().also { list.put(0, it) }
        first.put("description", description)
        list.put(0, first)
    }

    /** 对应 `json.uuid()`：给 header 和 modules[0] 各换一个新的 uuid。 */
    fun withNewUuids(o: JSONObject): JSONObject = o.apply {
        val h = optJSONObject("header") ?: JSONObject().also { put("header", it) }
        h.put("uuid", UUID.randomUUID().toString())
        val list = optJSONArray("modules") ?: JSONArray().also { put("modules", it) }
        if (list.length() == 0) list.put(JSONObject())
        val first = list.optJSONObject(0) ?: JSONObject().also { list.put(0, it) }
        first.put("uuid", UUID.randomUUID().toString())
        list.put(0, first)
    }

    /** 保留原 uuid，仅在缺失时补上（用于让旧包也能正常装载）。 */
    fun withUuidIfMissing(o: JSONObject): JSONObject = o.apply {
        val h = optJSONObject("header") ?: JSONObject().also { put("header", it) }
        if (h.optString("uuid").isBlank()) h.put("uuid", UUID.randomUUID().toString())
        if (h.optJSONArray("version") == null) h.put("version", JSONArray().put(0).put(0).put(1))
        val list = optJSONArray("modules") ?: JSONArray().also { put("modules", it) }
        if (list.length() == 0) list.put(JSONObject())
        val first = list.optJSONObject(0) ?: JSONObject().also { list.put(0, it) }
        if (first.optString("uuid").isBlank()) first.put("uuid", UUID.randomUUID().toString())
        if (first.optString("type").isBlank()) first.put("type", "data")
        if (first.optJSONArray("version") == null) first.put("version", JSONArray().put(0).put(0).put(1))
        list.put(0, first)
    }
}

/**
 * world_behavior_packs.json 的合并。
 *
 * 对应 `src/json.myu` 里的 `json.header(n)`。旧版是把一行 JSON 直接拼到
 * 数组结尾（对格式敏感、容易写坏），这里改成正经的 JSONArray 解析 + 去重。
 */
object WorldPackIndex {

    private const val FILE_NAME = "world_behavior_packs.json"

    fun fileName() = FILE_NAME

    fun merge(existingJsonText: String?, packUuid: String, version: List<Int> = listOf(0, 0, 1)): String {
        val array = runCatching { JSONArray(existingJsonText.orEmpty()) }.getOrNull() ?: JSONArray()

        val filtered = JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optString("pack_id") != packUuid) filtered.put(item)
        }

        val entry = JSONObject()
        entry.put("pack_id", packUuid)
        entry.put("version", JSONArray(version))
        filtered.put(entry)

        return filtered.toString(4)
    }
}
