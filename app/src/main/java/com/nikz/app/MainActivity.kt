package com.nikz.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnExtract: Button
    private lateinit var container: LinearLayout
    private lateinit var scroll: ScrollView

    private var extractedDir: File? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFF0D0D0D.toInt())
        }

        tvStatus = TextView(this).apply {
            text = "Tekan Extract dulu"
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 12f
        }

        btnExtract = Button(this).apply {
            text = "⚡ EXTRACT ASSETS"
        }

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll = ScrollView(this).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(tvStatus)
                addView(btnExtract)
                addView(container)
            })
        }

        root.addView(scroll)
        setContentView(root)

        btnExtract.setOnClickListener { doExtract() }
    }

    private fun doExtract() {
        scope.launch {
            tvStatus.text = "⏳ Cek root..."
            val msg = withContext(Dispatchers.IO) {
                if (!RootExecutor.isRootAvailable())
                    return@withContext "❌ Root tidak ada"
                val dir = NikzAssetManager.extractAll(this@MainActivity)
                extractedDir = dir
                NikzAssetManager.chmodAll(dir)
                "✅ ${dir.listFiles()?.size ?: 0} files extracted"
            }
            tvStatus.text = msg
            buildButtons()
        }
    }

    private fun buildButtons() {
        container.removeAllViews()
        val dir = extractedDir ?: return
        val files = dir.listFiles()?.sortedBy { it.name } ?: return

        files.forEach { file ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
            }

            val btn = Button(this).apply {
                text = file.name.take(28)
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            }

            val status = TextView(this).apply {
                text = "–"
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 11f
                setPadding(12, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            btn.setOnClickListener {
                status.text = "⏳"
                status.setTextColor(0xFFFFFF00.toInt())
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        RootExecutor.run(file)
                    }
                    if (result.success) {
                        status.text = "✅ OK"
                        status.setTextColor(0xFF00FF88.toInt())
                    } else {
                        status.text = "❌ ${result.exitCode}"
                        status.setTextColor(0xFFFF4444.toInt())
                    }
                }
            }

            row.addView(btn)
            row.addView(status)
            container.addView(row)
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
