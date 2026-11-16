package id.karawang.gacorengine

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.random.Random

class GacorService : AccessibilityService() {

    companion object {
        private const val TAG = "GacorEngine"
        private const val TARGET_PKG = "com.taxsee.driver"
        private val PRICE_REGEX = Regex("rp\\.?\\s*([0-9][0-9.,]*)")
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastScan = 0L
    private var cooldownUntil = 0L
    private var pending = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !Prefs.isActive(this)) return
        if (event.packageName?.toString() != TARGET_PKG) return

        val now = SystemClock.uptimeMillis()
        if (pending || now < cooldownUntil || now - lastScan < 60) return
        lastScan = now

        val root = rootInActiveWindow ?: return
        val accept = Prefs.toList(Prefs.accept(this))
        val button = findAcceptButton(root, 0, accept) ?: return

        val screenText = collectText(root, 0).lowercase()
        val reject = rejectReason(screenText)
        if (reject != null) {
            cooldownUntil = now + 1500
            Prefs.log(this, "TOLAK: $reject")
            return
        }

        val dMin = Prefs.delayMin(this)
        val dMax = Prefs.delayMax(this)
        val delay = if (dMax <= 0) 0L else Random.nextLong(dMin.toLong(), dMax.toLong() + 1)

        if (delay == 0L) {
            doClick(button)
        } else {
            pending = true
            handler.postDelayed({
                pending = false
                if (Prefs.isActive(this)) doClick(button)
            }, delay)
        }
    }

    private fun doClick(button: AccessibilityNodeInfo) {
        try {
            button.refresh()
            val ok = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            cooldownUntil = SystemClock.uptimeMillis() + 3000
            Prefs.log(this, if (ok) "KLIK OK" else "KLIK GAGAL (tombol tidak merespons)")
            Log.d(TAG, "klik=$ok")
        } catch (e: Exception) {
            Prefs.log(this, "ERROR klik: ${e.javaClass.simpleName}")
        }
    }

    private fun findAcceptButton(
        node: AccessibilityNodeInfo, depth: Int, words: List<String>
    ): AccessibilityNodeInfo? {
        if (depth > 25) return null
        val label = "${node.text ?: ""} ${node.contentDescription ?: ""}".lowercase().trim()
        if (label.isNotEmpty() && words.any { label == it || label.startsWith(it) }) {
            var n: AccessibilityNodeInfo? = node
            var up = 0
            while (n != null && up < 4) {
                if (n.isClickable && n.isEnabled) return n
                n = n.parent
                up++
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findAcceptButton(child, depth + 1, words)
            if (found != null) return found
        }
        return null
    }

    private fun collectText(node: AccessibilityNodeInfo, depth: Int): String {
        if (depth > 25) return ""
        val sb = StringBuilder()
        node.text?.let { sb.append(it).append(' ') }
        node.contentDescription?.let { sb.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            sb.append(collectText(child, depth + 1))
        }
        return sb.toString()
    }

    private fun rejectReason(text: String): String? {
        Prefs.toList(Prefs.exclude(this)).firstOrNull { text.contains(it) }
            ?.let { return "kata \"$it\"" }

        val include = Prefs.toList(Prefs.include(this))
        if (include.isNotEmpty() && include.none { text.contains(it) })
            return "tidak ada kata kunci ambil"

        val price = extractPrice(text)
        val min = Prefs.minPrice(this)
        if (price in 1 until min) return "harga Rp$price < Rp$min"
        return null
    }

    private fun extractPrice(text: String): Int {
        val m = PRICE_REGEX.find(text) ?: return 0
        val raw = m.groupValues[1].replace(Regex("[.,]00$"), "")
        return raw.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
    }

    override fun onInterrupt() { Log.d(TAG, "Layanan terputus") }
    override fun onServiceConnected() { Prefs.log(this, "Layanan terhubung") }
}
