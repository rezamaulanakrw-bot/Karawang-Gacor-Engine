package id.karawang.gacorengine

import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: Button
    private lateinit var etPrice: EditText
    private lateinit var etInclude: EditText
    private lateinit var etExclude: EditText
    private lateinit var etAccept: EditText
    private lateinit var etDelayMin: EditText
    private lateinit var etDelayMax: EditText
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnToggle = findViewById(R.id.btnToggle)
        etPrice = findViewById(R.id.etPrice)
        etInclude = findViewById(R.id.etInclude)
        etExclude = findViewById(R.id.etExclude)
        etAccept = findViewById(R.id.etAccept)
        etDelayMin = findViewById(R.id.etDelayMin)
        etDelayMax = findViewById(R.id.etDelayMax)
        tvLog = findViewById(R.id.tvLog)

        etPrice.setText(Prefs.minPrice(this).toString())
        etInclude.setText(Prefs.include(this))
        etExclude.setText(Prefs.exclude(this))
        etAccept.setText(Prefs.accept(this))
        etDelayMin.setText(Prefs.delayMin(this).toString())
        etDelayMax.setText(Prefs.delayMax(this).toString())

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            } else {
                Toast.makeText(this, "Izin overlay sudah aktif", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            Prefs.saveFilters(
                this,
                etPrice.text.toString().toIntOrNull() ?: 10000,
                etInclude.text.toString(),
                etExclude.text.toString(),
                etAccept.text.toString(),
                etDelayMin.text.toString().toIntOrNull() ?: 0,
                etDelayMax.text.toString().toIntOrNull() ?: 80
            )
            Toast.makeText(this, "Filter disimpan", Toast.LENGTH_SHORT).show()
            refreshUi()
        }

        btnToggle.setOnClickListener {
            if (!isServiceEnabled()) {
                Toast.makeText(this, "Aktifkan aksesibilitas dulu", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val now = !Prefs.isActive(this)
            Prefs.setActive(this, now)
            if (now && Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingWidgetService::class.java))
            } else if (!now) {
                stopService(Intent(this, FloatingWidgetService::class.java))
            }
            refreshUi()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onStart() {
        super.onStart()
        refreshUi()
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, GacorService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }

    private fun refreshUi() {
        val active = Prefs.isActive(this)
        val acc = isServiceEnabled()
        val overlay = Settings.canDrawOverlays(this)

        if (active) {
            btnToggle.text = "STOP MESIN"
            btnToggle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
        } else {
            btnToggle.text = "3. START MESIN"
            btnToggle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
        }

        tvStatus.text = "• Aksesibilitas: ${if (acc) "AKTIF" else "BELUM AKTIF"}\n" +
            "• Overlay: ${if (overlay) "AKTIF" else "BELUM AKTIF"}\n" +
            "• Mesin: ${if (active) "BERJALAN" else "BERHENTI"}\n" +
            "• Harga min: Rp ${Prefs.minPrice(this)}\n" +
            "• Jeda klik: ${Prefs.delayMin(this)}-${Prefs.delayMax(this)} ms\n" +
            "• Target: Maxim Driver (com.taxsee.driver)"
        tvLog.text = Prefs.getLog(this)
    }
}
