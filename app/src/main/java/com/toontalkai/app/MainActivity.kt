package com.toontalkai.app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.VideoView
import android.widget.MediaController
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.IOException
import java.util.concurrent.Executors
import org.json.JSONObject

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("toon_talk_settings", MODE_PRIVATE) }
    private lateinit var promptInput: EditText
    private lateinit var keyInput: EditText
    private lateinit var appKeyInput: EditText
    private lateinit var connectButton: Button
    private lateinit var statusText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var imagePreview: ImageView
    private lateinit var videoPreview: VideoView
    private lateinit var imageButton: Button
    private lateinit var videoButton: Button
    private lateinit var saveButton: Button
    private var latestImage: Bitmap? = null
    private var latestVideo: File? = null
    private var latestType: String? = null
    private var pendingSave: (() -> Unit)? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(13, 18, 39)
        window.navigationBarColor = Color.rgb(13, 18, 39)
        window.decorView.systemUiVisibility = 0

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(24))
            setBackgroundColor(Color.rgb(13, 18, 39))
        }
        val title = TextView(this).apply {
            text = "🎨 Toon Talk AI"
            textSize = 29f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(4))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        page.addView(title, matchWrap())
        val subtitle = TextView(this).apply {
            text = "Turn your imagination into images and videos"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(186, 196, 220))
            setPadding(0, 0, 0, dp(20))
        }
        page.addView(subtitle, matchWrap())

        val keyTitle = label("Pollinations API key")
        page.addView(keyTitle, matchWrap())
        keyInput = EditText(this).apply {
            hint = "Paste your authorized sk_ key"
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(145, 155, 180))
            setPadding(dp(13), dp(12), dp(13), dp(12))
            setBackgroundColor(Color.rgb(31, 39, 66))
            setText(prefs.getString("api_key", "") ?: "")
        }
        page.addView(keyInput, matchWrap())
        val keyActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val saveKeyButton = makeButton("Save key", Color.rgb(48, 75, 132))
        saveKeyButton.setOnClickListener {
            val key = keyInput.text.toString().trim()
            if (key.isBlank()) {
                statusText.text = "Paste your Pollinations key first."
            } else {
                prefs.edit().putString("api_key", key).apply()
                hideKeyboard()
                statusText.text = "Key saved privately on this device."
                Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show()
            }
        }
        keyActions.addView(saveKeyButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        val getKeyButton = makeButton("Get API key", Color.rgb(47, 111, 96))
        getKeyButton.setOnClickListener {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://enter.pollinations.ai/")))
        }
        val keyGap = View(this)
        keyActions.addView(keyGap, LinearLayout.LayoutParams(dp(8), 1))
        keyActions.addView(getKeyButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        page.addView(keyActions, matchWrap())

        val appKeyTitle = label("App Key for sign-in / developer attribution (optional)")
        appKeyTitle.setPadding(0, dp(14), 0, dp(6))
        page.addView(appKeyTitle, matchWrap())
        appKeyInput = EditText(this).apply {
            hint = "App Key starts with pk_ (if you created one)"
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(145, 155, 180))
            setPadding(dp(13), dp(12), dp(13), dp(12))
            setBackgroundColor(Color.rgb(31, 39, 66))
            setText(prefs.getString("app_key", "") ?: "")
        }
        page.addView(appKeyInput, matchWrap())
        val earningsHint = TextView(this).apply {
            text = "To receive developer earnings, create an App Key at enter.pollinations.ai/keys and enable earnings for it. Without that key, you can still connect and generate using your own Pollinations balance."
            textSize = 12f
            setTextColor(Color.rgb(145, 155, 180))
            setPadding(dp(2), dp(5), dp(2), dp(2))
        }
        page.addView(earningsHint, matchWrap())
        connectButton = makeButton("🔗  Connect Pollinations", Color.rgb(37, 126, 103))
        connectButton.setOnClickListener { connectPollinations() }
        page.addView(connectButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
            topMargin = dp(8)
        })

        val promptTitle = label("Describe your scene")
        promptTitle.setPadding(0, dp(20), 0, dp(7))
        page.addView(promptTitle, matchWrap())
        promptInput = EditText(this).apply {
            hint = "Example: A cute cartoon village, warm sunset, colorful 3D animation..."
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(145, 155, 180))
            setPadding(dp(14), dp(13), dp(14), dp(13))
            setBackgroundColor(Color.rgb(31, 39, 66))
            minLines = 3
            maxLines = 6
            gravity = Gravity.TOP
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        page.addView(promptInput, matchWrap())

        imageButton = makeButton("✨  Generate Image", Color.rgb(105, 79, 202))
        imageButton.setOnClickListener { generateImage() }
        val imageParams = matchWrap().apply { topMargin = dp(14) }
        page.addView(imageButton, imageParams)

        videoButton = makeButton("🎬  Create 4-second Video", Color.rgb(193, 83, 116))
        videoButton.setOnClickListener { generateVideo() }
        val videoParams = matchWrap().apply { topMargin = dp(8) }
        page.addView(videoButton, videoParams)

        progress = ProgressBar(this).apply { visibility = View.GONE; isIndeterminate = true }
        val progressRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(2))
            addView(progress, LinearLayout.LayoutParams(dp(36), dp(36)))
        }
        page.addView(progressRow, matchWrap())

        statusText = TextView(this).apply {
            text = "Ready. Add your Pollinations key, then describe what you want to create."
            textSize = 14f
            setTextColor(Color.rgb(200, 209, 230))
            setPadding(dp(2), dp(8), dp(2), dp(12))
        }
        page.addView(statusText, matchWrap())

        imagePreview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            contentDescription = "Generated image preview"
            setBackgroundColor(Color.rgb(24, 30, 52))
        }
        val imagePreviewParams = matchWrap().apply { topMargin = dp(4); bottomMargin = dp(8) }
        page.addView(imagePreview, imagePreviewParams)

        videoPreview = VideoView(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.BLACK)
        }
        page.addView(videoPreview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(240)).apply {
            topMargin = dp(4)
            bottomMargin = dp(8)
        })

        saveButton = makeButton("Save result to Gallery", Color.rgb(35, 126, 103))
        saveButton.visibility = View.GONE
        saveButton.setOnClickListener { saveLatestResult() }
        page.addView(saveButton, matchWrap())

        val note = TextView(this).apply {
            text = "AI generation uses your Pollinations account and may consume its credits. Keep your key private. Video availability depends on the selected service/model."
            textSize = 12f
            setTextColor(Color.rgb(145, 155, 180))
            setPadding(0, dp(22), 0, 0)
        }
        page.addView(note, matchWrap())

        val scroll = ScrollView(this).apply {
            fillViewport = true
            addView(page)
        }
        setContentView(scroll)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun label(textValue: String) = TextView(this).apply {
        text = textValue
        textSize = 14f
        setTextColor(Color.rgb(215, 223, 242))
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private fun makeButton(textValue: String, color: Int) = Button(this).apply {
        text = textValue
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(color)
        minHeight = dp(48)
    }

    private fun hideKeyboard() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(promptInput.windowToken, 0)
        manager.hideSoftInputFromWindow(keyInput.windowToken, 0)
    }

    private fun apiKey(): String {
        val typed = keyInput.text.toString().trim()
        if (typed.isNotBlank()) {
            prefs.edit().putString("api_key", typed).apply()
            return typed
        }
        return prefs.getString("api_key", "") ?: ""
    }

    private fun validateInputs(): Pair<String, String>? {
        val prompt = promptInput.text.toString().trim()
        val key = apiKey()
        if (prompt.isBlank()) {
            statusText.text = "Please describe your scene first."
            return null
        }
        if (key.isBlank()) {
            statusText.text = "Add your Pollinations API key. Tap Get API key if you need one."
            return null
        }
        hideKeyboard()
        return Pair(prompt, key)
    }

    private fun setBusy(busy: Boolean, message: String) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        imageButton.isEnabled = !busy
        videoButton.isEnabled = !busy
        connectButton.isEnabled = !busy
        imageButton.alpha = if (busy) 0.55f else 1f
        videoButton.alpha = if (busy) 0.55f else 1f
        statusText.text = message
    }

    private fun connectPollinations() {
        val appKey = appKeyInput.text.toString().trim()
        if (appKey.isNotBlank() && !appKey.startsWith("pk_")) {
            statusText.text = "The App Key should start with pk_. Leave it blank if you do not have one."
            return
        }
        if (appKey.isNotBlank()) prefs.edit().putString("app_key", appKey).apply()
        hideKeyboard()
        setBusy(true, "Starting secure Pollinations sign-in…")
        worker.execute {
            try {
                val body = JSONObject().apply {
                    if (appKey.isNotBlank()) put("client_id", appKey)
                }.toString()
                val startConnection = (URL("https://enter.pollinations.ai/api/device/code").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }
                val startBody: String
                try {
                    startConnection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = startConnection.responseCode
                    val stream = if (code in 200..299) startConnection.inputStream else startConnection.errorStream
                    startBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    if (code !in 200..299) throw IOException("Could not start sign-in (HTTP $code): $startBody")
                } finally {
                    startConnection.disconnect()
                }
                val startJson = JSONObject(startBody)
                val deviceCode = startJson.optString("device_code")
                val userCode = startJson.optString("user_code")
                if (deviceCode.isBlank() || userCode.isBlank()) throw IOException("Pollinations did not return a sign-in code. Please try again.")
                val verificationUrl = startJson.optString("verification_uri_complete").takeIf { it.isNotBlank() }
                    ?: ("https://enter.pollinations.ai/device?user_code=" + URLEncoder.encode(userCode, "UTF-8"))
                runOnUiThread {
                    statusText.text = "Sign-in code: $userCode\nThe browser is opening. Approve the request there; keep this app open."
                    try {
                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(verificationUrl)))
                    } catch (_: Exception) {
                        statusText.text = "Open https://enter.pollinations.ai/device and enter code $userCode"
                    }
                }
                val deadline = System.currentTimeMillis() + 15 * 60 * 1000
                var approvedKey: String? = null
                while (System.currentTimeMillis() < deadline && approvedKey == null) {
                    Thread.sleep(5000)
                    val tokenConnection = (URL("https://enter.pollinations.ai/api/device/token").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 15000
                        readTimeout = 20000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Accept", "application/json")
                    }
                    val tokenBody: String
                    val tokenCode: Int
                    try {
                        tokenConnection.outputStream.use {
                            it.write(JSONObject().put("device_code", deviceCode).toString().toByteArray(Charsets.UTF_8))
                        }
                        tokenCode = tokenConnection.responseCode
                        val stream = if (tokenCode in 200..299) tokenConnection.inputStream else tokenConnection.errorStream
                        tokenBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    } finally {
                        tokenConnection.disconnect()
                    }
                    val tokenJson = try { JSONObject(tokenBody) } catch (_: Exception) { JSONObject() }
                    val accessToken = tokenJson.optString("access_token")
                    if (tokenCode in 200..299 && accessToken.startsWith("sk_")) {
                        approvedKey = accessToken
                    } else {
                        val error = tokenJson.optString("error")
                        when (error) {
                            "authorization_pending" -> runOnUiThread { statusText.text = "Waiting for approval… Code: $userCode. Approve in the browser, then return here." }
                            "slow_down" -> Thread.sleep(5000)
                            "expired_token", "access_denied" -> throw IOException("Pollinations sign-in $error. Tap Connect Pollinations to try again.")
                            else -> if (tokenCode !in 200..299 && error.isBlank()) {
                                throw IOException("Sign-in check failed (HTTP $tokenCode): $tokenBody")
                            }
                        }
                    }
                }
                val finalKey = approvedKey ?: throw IOException("Sign-in timed out. Please tap Connect Pollinations and try again.")
                prefs.edit().putString("api_key", finalKey).apply()
                runOnUiThread {
                    keyInput.setText(finalKey)
                    setBusy(false, "Pollinations connected! You can now generate images and videos.")
                    Toast.makeText(this, "Pollinations connected", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread { setBusy(false, "Connection failed: ${e.message ?: "Please try again."}") }
            }
        }
    }

    private fun generateImage() {
        val inputs = validateInputs() ?: return
        latestType = null
        saveButton.visibility = View.GONE
        videoPreview.stopPlayback()
        videoPreview.visibility = View.GONE
        imagePreview.visibility = View.GONE
        setBusy(true, "Generating image… This may take a little while.")
        worker.execute {
            try {
                val prompt = URLEncoder.encode(inputs.first, "UTF-8").replace("+", "%20")
                val url = URL("https://gen.pollinations.ai/image/$prompt?model=flux&width=1024&height=1024&safe=true")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 30000
                    readTimeout = 180000
                    setRequestProperty("Authorization", "Bearer ${inputs.second}")
                    setRequestProperty("Accept", "image/*")
                }
                try {
                    val code = connection.responseCode
                    if (code !in 200..299) throw IOException(readApiError(connection, code))
                    val type = connection.contentType ?: ""
                    if (!type.startsWith("image/")) throw IOException("The service did not return an image (content type: $type). Check your key and model access.")
                    val bytes = connection.inputStream.use { it.readBytes() }
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw IOException("The image response could not be opened.")
                    runOnUiThread {
                        latestImage = bitmap
                        latestVideo = null
                        latestType = "image"
                        imagePreview.setImageBitmap(bitmap)
                        imagePreview.visibility = View.VISIBLE
                        videoPreview.visibility = View.GONE
                        saveButton.visibility = View.VISIBLE
                        setBusy(false, "Image created! Tap Save result to Gallery to keep it.")
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                runOnUiThread { setBusy(false, friendlyError(e)) }
            }
        }
    }

    private fun generateVideo() {
        val inputs = validateInputs() ?: return
        latestType = null
        saveButton.visibility = View.GONE
        videoPreview.stopPlayback()
        videoPreview.visibility = View.GONE
        imagePreview.visibility = View.GONE
        setBusy(true, "Creating video… Video generation can take several minutes.")
        worker.execute {
            try {
                val prompt = URLEncoder.encode(inputs.first, "UTF-8").replace("+", "%20")
                val url = URL("https://gen.pollinations.ai/video/$prompt?model=google%2Fveo-3.1-fast&duration=4")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 30000
                    readTimeout = 300000
                    setRequestProperty("Authorization", "Bearer ${inputs.second}")
                    setRequestProperty("Accept", "video/mp4,application/octet-stream")
                }
                try {
                    val code = connection.responseCode
                    if (code !in 200..299) throw IOException(readApiError(connection, code))
                    val type = connection.contentType ?: ""
                    if (type.contains("json", true) || type.startsWith("text/")) throw IOException(connection.inputStream.bufferedReader().use { it.readText().take(700) })
                    val file = File(cacheDir, "toon_talk_${System.currentTimeMillis()}.mp4")
                    connection.inputStream.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
                    if (!file.exists() || file.length() < 1024) throw IOException("The video response was empty. Please try again.")
                    runOnUiThread {
                        latestImage = null
                        latestVideo = file
                        latestType = "video"
                        imagePreview.visibility = View.GONE
                        videoPreview.visibility = View.VISIBLE
                        videoPreview.setMediaController(MediaController(this))
                        videoPreview.setVideoURI(Uri.fromFile(file))
                        videoPreview.setOnPreparedListener { media -> media.isLooping = true; videoPreview.start() }
                        saveButton.visibility = View.VISIBLE
                        setBusy(false, "Video created! Use the player, then save it to Gallery.")
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                runOnUiThread { setBusy(false, friendlyError(e)) }
            }
        }
    }

    private fun readApiError(connection: HttpURLConnection, code: Int): String {
        val stream = try { connection.errorStream ?: connection.inputStream } catch (_: Exception) { null }
        val body = try { stream?.bufferedReader()?.use { it.readText().take(900) } ?: "" } catch (_: Exception) { "" }
        val hint = when (code) {
            401 -> "API key missing or invalid (401). Check the key you entered."
            402 -> "Pollinations account/key balance or budget is exhausted (402). Check your account."
            403 -> "Access denied (403). Check model permissions for this key."
            429 -> "Rate limit reached (429). Wait a little and try again."
            else -> "Pollinations returned HTTP $code."
        }
        return if (body.isBlank()) hint else "$hint\n$body"
    }

    private fun friendlyError(error: Exception): String {
        val message = error.message ?: "Unknown error"
        return when {
            message.contains("UnknownHost", true) || message.contains("Unable to resolve", true) ->
                "No internet connection. Connect to Wi-Fi/mobile data and try again."
            message.contains("timeout", true) || message.contains("timed out", true) ->
                "The request timed out. Video may take longer; please try again."
            else -> "Generation failed: $message"
        }
    }

    private fun saveLatestResult() {
        when (latestType) {
            "image" -> {
                val bitmap = latestImage
                if (bitmap == null) {
                    statusText.text = "There is no image to save yet."
                    return
                }
                saveMedia("image") {
                    val name = "ToonTalkAI_${System.currentTimeMillis()}.png"
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Toon Talk AI")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }
                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IOException("Could not create a gallery image.")
                    try {
                        contentResolver.openOutputStream(uri)?.use { output ->
                            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw IOException("Could not encode image.")
                        } ?: throw IOException("Could not open the gallery destination.")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            contentResolver.update(uri, values, null, null)
                        }
                    } catch (e: Exception) {
                        contentResolver.delete(uri, null, null)
                        throw e
                    }
                    uri
                }
            }
            "video" -> {
                val file = latestVideo
                if (file == null) {
                    statusText.text = "There is no video to save yet."
                    return
                }
                saveMedia("video") {
                    val name = "ToonTalkAI_${System.currentTimeMillis()}.mp4"
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, name)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Toon Talk AI")
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                    }
                    val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IOException("Could not create a gallery video.")
                    try {
                        contentResolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } }
                            ?: throw IOException("Could not open the gallery destination.")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Video.Media.IS_PENDING, 0)
                            contentResolver.update(uri, values, null, null)
                        }
                    } catch (e: Exception) {
                        contentResolver.delete(uri, null, null)
                        throw e
                    }
                    uri
                }
            }
            else -> statusText.text = "Generate an image or video first."
        }
    }

    private fun saveMedia(kind: String, writer: () -> Uri) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingSave = { saveMedia(kind, writer) }
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 91)
            return
        }
        try {
            val uri = writer()
            statusText.text = "Saved to your Gallery: $uri"
            Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            statusText.text = "Could not save to Gallery: ${e.message}"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 91) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingSave?.invoke()
            } else {
                statusText.text = "Storage permission is needed to save to Gallery on this Android version."
            }
            pendingSave = null
        }
    }

    override fun onDestroy() {
        if (::videoPreview.isInitialized) videoPreview.stopPlayback()
        worker.shutdownNow()
        super.onDestroy()
    }
}