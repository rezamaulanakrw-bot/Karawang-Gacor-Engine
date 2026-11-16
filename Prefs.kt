package id.karawang.gacorengine

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("gacor", Context.MODE_PRIVATE)

    fun isActive(c: Context) = sp(c).getBoolean("active", false)
    fun setActive(c: Context, v: Boolean) = sp(c).edit().putBoolean("active", v).apply()

    fun minPrice(c: Context) = sp(c).getInt("min_price", 10000)
    fun include(c: Context) = sp(c).getString("include", "indomaret, apotek, alfamart, pasar") ?: ""
    fun exclude(c: Context) = sp(c).getString("exclude", "bandara, perbaikan rating") ?: ""
    fun accept(c: Context) = sp(c).getString("accept", "minta order, ambil order, terima, lanjut") ?: ""
    fun delayMin(c: Context) = sp(c).getInt("delay_min", 0)
    fun delayMax(c: Context) = sp(c).getInt("delay_max", 80)

    fun saveFilters(
        c: Context, price: Int, include: String, exclude: String,
        accept: String, dMin: Int, dMax: Int
    ) {
        sp(c).edit()
            .putInt("min_price", price)
            .putString("include", include)
            .putString("exclude", exclude)
            .putString("accept", accept)
            .putInt("delay_min", dMin.coerceAtLeast(0))
            .putInt("delay_max", dMax.coerceAtLeast(dMin.coerceAtLeast(0)))
            .apply()
    }

    fun log(c: Context, line: String) {
        val t = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val old = sp(c).getString("log", "") ?: ""
        val lines = (listOf("$t $line") + old.split("\n").filter { it.isNotBlank() }).take(8)
        sp(c).edit().putString("log", lines.joinToString("\n")).apply()
    }

    fun getLog(c: Context) = sp(c).getString("log", "(belum ada)") ?: ""

    fun toList(s: String): List<String> =
        s.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
}
