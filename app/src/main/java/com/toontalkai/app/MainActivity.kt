package com.toontalkai.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(20, 20, 35)
        window.navigationBarColor = Color.rgb(20, 20, 35)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(Color.rgb(15, 15, 28))
        }

        val title = TextView(this).apply {
            text = "🎨 Toon Talk AI"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Create your own stories with AI"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        }
        root.addView(subtitle)

        val prompt = EditText(this).apply {
            hint = "Describe your story or image..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.rgb(38, 38, 55))
            minLines = 3
            gravity = Gravity.TOP
        }
        root.addView(
            prompt,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val result = TextView(this).apply {
            text = "Your creations will appear here."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(20), 0, dp(20))
        }
        root.addView(result)

        val imageButton = Button(this).apply {
            text = "Generate Image"
            setOnClickListener {
                val text = prompt.text.toString().trim()
                result.text = if (text.isEmpty()) {
                    "Please describe the image you want to create."
                } else {
                    "Your image prompt:\\n\\n$text\\n\\nAI image generation will be connected in the next stage."
                }
            }
        }
        root.addView(imageButton)

        val videoButton = Button(this).apply {
            text = "Create Video"
            setOnClickListener {
                result.text =
                    "Video creation will be connected after the AI generation service is configured."
            }
        }
        root.addView(videoButton)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }
}
