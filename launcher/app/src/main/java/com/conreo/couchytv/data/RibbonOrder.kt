package com.conreo.couchytv.data

/**
 * What the slanted ribbon shows, in order. The screen and move mode both use
 * these, so a move always lands next to the tile you see beside it.
 */
object RibbonOrder {

    /** Recent tiles: the last [max] apps launched, still installed and not hidden. */
    fun recent(apps: List<AppEntry>, config: LauncherConfig, max: Int = 3): List<AppEntry> {
        val byPkg = apps.associateBy { it.pkg }
        return config.recent.asSequence()
            .mapNotNull { byPkg[it] }
            .filter { it.pkg !in config.hidden }
            .distinctBy { it.pkg }
            .take(max)
            .toList()
    }

    /**
     * Every section's apps once (an app in several sections shows in the first),
     * minus the ones already in Recent, so nothing repeats. The app being moved
     * stays in its section while it moves, even if it is also a Recent app.
     */
    fun flat(
        sections: List<Pair<CategoryCfg, List<AppEntry>>>,
        recent: Set<String>,
        movePkg: String?,
    ): List<Pair<CategoryCfg, AppEntry>> {
        val seen = HashSet<String>()
        val out = ArrayList<Pair<CategoryCfg, AppEntry>>()
        for ((cat, list) in sections) for (app in list) {
            if (!seen.add(app.pkg)) continue
            if (app.pkg in recent && app.pkg != movePkg) continue
            out += cat to app
        }
        return out
    }

    /**
     * Moves [pkg] one visible step ([delta] = ±1) through [visible], and returns
     * the full order ([full], which also holds the apps hidden behind Recent)
     * with [pkg] placed beside its new visible neighbour, plus its new index.
     * Null when it cannot move further that way.
     */
    fun step(pkg: String, delta: Int, visible: List<String>, full: List<String>): Pair<List<String>, Int>? {
        val vi = visible.indexOf(pkg)
        if (vi < 0) return null
        val neighbour = visible.getOrNull(vi + delta) ?: return null
        val out = full.toMutableList()
        out.remove(pkg)
        val ni = out.indexOf(neighbour)
        if (ni < 0) return null
        val at = if (delta > 0) ni + 1 else ni
        out.add(at, pkg)
        return out to at
    }
}
