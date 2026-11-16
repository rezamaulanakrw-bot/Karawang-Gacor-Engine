package id.karawang.gacorengine

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button

class FloatingWidgetService : Service() {

    private var wm: WindowManager? = null
    private var view: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        val v = LayoutInflater.from(this).inflate(R.layout.widget_floating, null)
        view = v

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm?.addView(v, params)

        val btn = v.findViewById<Button>(R.id.btnWidgetToggle)
        fun refresh() {
            if (Prefs.isActive(this)) {
                btn.text = "SIKAT: ON"
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            } else {
                btn.text = "SIKAT: OFF"
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
            }
        }
        refresh()

        var startX = 0; var startY = 0
        var touchX = 0f; var touchY = 0f
        var moved = false
        btn.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = e.rawX; touchY = e.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    wm?.updateViewLayout(v, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        Prefs.setActive(this, !Prefs.isActive(this))
                        refresh()
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        view?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        view = null
    }
}
