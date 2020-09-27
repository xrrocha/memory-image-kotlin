package memimg.util

object PropertyHolder {

    private val byOwnerMap: MutableMap<Any, MutableMap<String, Any>> = mutableMapOf()

    fun <T> get(owner: Any, name: String, source: () -> Any): T {
        if (!byOwnerMap.containsKey(owner)) {
            byOwnerMap[owner] = mutableMapOf()
        }
        val byNameMap = byOwnerMap[owner]!!
        if (!byNameMap.containsKey(name)) {
            byNameMap[name] = source()
        }
        @Suppress("UNCHECKED_CAST")
        return byNameMap[name]!! as T
    }
}
