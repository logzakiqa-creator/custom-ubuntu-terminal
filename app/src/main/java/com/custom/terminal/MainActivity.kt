package com.custom.terminal

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val tv = TextView(this).apply {
            text = "🚀 Ubuntu Terminal Auto Bootstrapper\n\nĐang khởi tạo môi trường..."
            textSize = 18f
            setPadding(40, 40, 40, 40)
        }
        setContentView(tv)
    }
}
