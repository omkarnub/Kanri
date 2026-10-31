package com.omkarnub.kanri.data.update

object SemanticVersionComparator {

    /**
     * Returns true if [remoteVersion] is strictly newer than [currentVersion].
     * Supports formats like "1.0.2", "v1.0.3", "v1.0.2.1", etc.
     */
    fun isNewer(currentVersion: String, remoteVersion: String): Boolean {
        val currentNums = parseVersionNumbers(currentVersion)
        val remoteNums = parseVersionNumbers(remoteVersion)

        val maxLen = maxOf(currentNums.size, remoteNums.size)
        for (i in 0 until maxLen) {
            val curr = currentNums.getOrElse(i) { 0 }
            val remote = remoteNums.getOrElse(i) { 0 }
            if (remote > curr) return true
            if (remote < curr) return false
        }
        return false
    }

    fun cleanVersion(version: String): String {
        return version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split("-", "+", " ")[0]
            .trim()
    }

    private fun parseVersionNumbers(version: String): List<Int> {
        val clean = cleanVersion(version)
        if (clean.isBlank()) return emptyList()
        return clean.split(".")
            .map { segment ->
                segment.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
    }
}
