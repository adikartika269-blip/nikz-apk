package com.nikz.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvOutput: TextView
    private lateinit var btnExtract: Button
    private lateinit var btnRun: Button
    private lateinit var spinner: Spinner

    private var extractedDir: File? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus  = findViewById(R.id.tvStatus)
        tvOutput  = findViewById(R.id.tvOutput)
        btnExtract = findViewById(R.id.btnExtract)
        btnRun    = findViewById(R.id.btnRun)
        spinner   = findViewById(R.id.spinner)

        btnExtract.setOnClickListener { doExtract() }
        btnRun.setOnClickListener    { doRun() }
    }

    private fun doExtract() {
        scope.launch {
            tvStatus.text = "⏳ Cek root..."
            val msg = withContext(Dispatchers.IO) {

                if (!RootExecutor.isRootAvailable())
                    return@withContext "❌ Root tidak ada — install Magisk dulu"

                tvStatus.post { tvStatus.text = "⏳ Extracting..." }
                val dir = NikzAssetManager.extractAll(this@MainActivity)
                extractedDir = dir

                tvStatus.post { tvStatus.text = "⏳ chmod..." }
                NikzAssetManager.chmodAll(dir)

                "✅ ${dir.listFiles()?.size ?: 0} files extracted ke:\n${dir.absolutePath}"
            }
            tvStatus.text = msg

            extractedDir?.let { dir ->
                val names = dir.listFiles()?.map { it.name } ?: emptyList()
                spinner.adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
            }
        }
    }

    private fun doRun() {
        val dir = extractedDir ?: run {
            tvOutput.text = "⚠️ Extract dulu"
            return
        }
        val selected = spinner.selectedItem?.toString() ?: return
        val binary = File(dir, selected)

        scope.launch {
            tvOutput.text = "⏳ Running: $selected"
            val result = withContext(Dispatchers.IO) {
                RootExecutor.run(binary)
            }
            tvOutput.text = buildString {
                appendLine(if (result.success) "✅ Exit ${result.exitCode}" else "❌ Exit ${result.exitCode}")
                if (result.stdout.isNotEmpty()) { appendLine("\n── STDOUT ──"); appendLine(result.stdout) }
                if (result.stderr.isNotEmpty()) { appendLine("\n── STDERR ──"); appendLine(result.stderr) }
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
