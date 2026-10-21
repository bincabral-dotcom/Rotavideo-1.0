package com.giravideo.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.Transformer
import java.io.File

@UnstableApi
class MainActivity : AppCompatActivity() {
    private var videoUri: Uri? = null
    private var sourceFrame: Bitmap? = null
    private var sourceRotation = 0f
    private var angle = 0
    private var flipX = 1f
    private var flipY = 1f
    private lateinit var preview: android.widget.ImageView
    private lateinit var status: TextView
    private var pendingOutput: Uri? = null

    private val openVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            videoUri = uri
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            loadPreviewFrame(uri)
            status.text = "Prévia estática · pronto para editar"
        }
    }

    private val saveVideo = registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        if (uri != null) { pendingOutput = uri; exportVideo() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 16, 24)
        window.navigationBarColor = Color.rgb(16, 16, 24)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(12))
            setBackgroundColor(Color.rgb(16, 16, 24))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = TextView(this).apply { text = "◉  Rotavideo"; textSize = 24f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val spacer = View(this)
        val version = TextView(this).apply { text = "EDITOR"; textSize = 11f; setTextColor(Color.rgb(196,181,253)); letterSpacing = .15f }
        header.addView(brand); header.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f)); header.addView(version)
        root.addView(header)
        root.addView(TextView(this).apply { text = "Gire e ajuste seus vídeos"; textSize = 14f; setTextColor(Color.rgb(160,158,176)); setPadding(0, dp(5), 0, dp(16)) })

        preview = android.widget.ImageView(this).apply {
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.rgb(6, 6, 10))
            contentDescription = "Prévia estática do vídeo editado"
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f).apply { bottomMargin = dp(16) }
        }
        root.addView(preview)

        val pick = button("＋   Escolher vídeo", true) { openVideo.launch(arrayOf("video/*")) }
        root.addView(pick, rowParams(54, 10))
        root.addView(label("ROTACIONAR"))
        val rotateRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rotateRow.addView(button("↶   −90°", false) { rotate(-90) }, weightParams())
        rotateRow.addView(button("↻   +90°", false) { rotate(90) }, weightParams())
        rotateRow.addView(button("⟳   180°", false) { rotate(180) }, weightParams())
        root.addView(rotateRow, rowParams(54, 6))
        root.addView(label("ESPELHAR"))
        val flipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        flipRow.addView(button("↔   Horizontal", false) { flipX *= -1f; updatePreview() }, weightParams())
        flipRow.addView(button("↕   Vertical", false) { flipY *= -1f; updatePreview() }, weightParams())
        root.addView(flipRow, rowParams(54, 10))

        status = TextView(this).apply { text = "Escolha um vídeo para começar"; textSize = 13f; setTextColor(Color.rgb(160,158,176)); gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(10)) }
        root.addView(status, rowParams(-2, 0))
        root.addView(button("↓   Exportar vídeo", true) { beginExport() }, rowParams(56, 0))
        setContentView(root)
    }

    private fun rotate(delta: Int) {
        angle = ((angle + delta) % 360 + 360) % 360
        updatePreview()
        status.text = "Rotação: $angle°"
    }

    private fun loadPreviewFrame(uri: Uri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            sourceRotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toFloatOrNull() ?: 0f
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            sourceFrame?.recycle()
            sourceFrame = frame?.let {
                val maxDimension = 1600
                val ratio = minOf(1f, maxDimension.toFloat() / maxOf(it.width, it.height))
                if (ratio < 1f) {
                    val scaled = Bitmap.createScaledBitmap(it, (it.width * ratio).toInt(), (it.height * ratio).toInt(), true)
                    if (scaled !== it) it.recycle()
                    scaled
                } else it
            }
            updatePreview()
        } catch (e: Exception) {
            status.text = "Não foi possível gerar a prévia"
            Toast.makeText(this, e.localizedMessage ?: "Falha ao abrir vídeo", Toast.LENGTH_LONG).show()
        } finally {
            retriever.release()
        }
    }

    private fun updatePreview() {
        val frame = sourceFrame ?: run { preview.setImageDrawable(null); return }
        val matrix = Matrix().apply {
            setRotate(sourceRotation + angle)
            postScale(flipX, flipY)
        }
        preview.setImageBitmap(Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, matrix, true))
    }

    private fun beginExport() {
        if (videoUri == null) { Toast.makeText(this, "Escolha um vídeo primeiro", Toast.LENGTH_SHORT).show(); return }
        saveVideo.launch("giravideo_editado.mp4")
    }

    private fun exportVideo() {
        val input = videoUri ?: return
        val outputUri = pendingOutput ?: return
        status.text = "Exportando… mantenha o app aberto"
        val temp = File(cacheDir, "giravideo_export.mp4")
        val effect = ScaleAndRotateTransformation.Builder()
            .setRotationDegrees(angle.toFloat())
            .setScale(flipX, flipY)
            .build()
        val edited = EditedMediaItem.Builder(MediaItem.fromUri(input))
            .setEffects(Effects(emptyList(), listOf(effect)))
            .build()
        val transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult) {
                    try {
                        contentResolver.openOutputStream(outputUri)?.use { out -> temp.inputStream().use { it.copyTo(out) } }
                        temp.delete()
                        status.text = "Exportação concluída · vídeo salvo"
                        Toast.makeText(this@MainActivity, "Vídeo exportado com sucesso", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        status.text = "Não foi possível salvar o vídeo"
                        Toast.makeText(this@MainActivity, e.localizedMessage ?: "Erro ao salvar", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onError(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult, exportException: androidx.media3.transformer.ExportException) {
                    temp.delete()
                    status.text = "Falha ao exportar"
                    Toast.makeText(this@MainActivity, exportException.localizedMessage ?: "Erro na exportação", Toast.LENGTH_LONG).show()
                }
            })
            .build()
        transformer.start(edited, temp.absolutePath)
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text; textSize = 11f; setTextColor(Color.rgb(164,153,206)); letterSpacing = .12f
        setPadding(dp(2), dp(5), 0, dp(3))
    }

    private fun button(text: String, primary: Boolean, action: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 14f; isAllCaps = false; setOnClickListener { action() }
        setTextColor(if (primary) Color.WHITE else Color.rgb(226,222,242))
        backgroundTintList = android.content.res.ColorStateList.valueOf(if (primary) Color.rgb(124,58,237) else Color.rgb(37,35,50))
        if (primary) typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private fun rowParams(heightDp: Int, bottomDp: Int) = LinearLayout.LayoutParams(-1, if (heightDp < 0) heightDp else dp(heightDp)).apply { bottomMargin = dp(bottomDp) }
    private fun weightParams() = LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(5) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() { super.onDestroy(); sourceFrame?.recycle(); sourceFrame = null; preview.setImageDrawable(null) }
}
