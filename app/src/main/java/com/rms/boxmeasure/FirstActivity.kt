package com.rms.boxmeasure

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FirstActivity : AppCompatActivity() {
    private lateinit var btMeasure: Button
    private lateinit var tvWidth: TextView
    private lateinit var tvLength: TextView
    private lateinit var tvHeight: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        // 初始化 Views
        btMeasure = findViewById(R.id.bt_measure)
        tvWidth = findViewById(R.id.tv_width)
        tvLength = findViewById(R.id.tv_length)
        tvHeight = findViewById(R.id.tv_height)

        setListener()

    }

    private fun setListener() {
        btMeasure.setOnClickListener {
            startActivityForResult(Intent(this, ArMeasureActivity::class.java), 201)
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 201 && resultCode == RESULT_OK) {
            val width = data!!.getDoubleExtra("width", 0.0)
            val height = data!!.getDoubleExtra("height", 0.0)
            val length = data!!.getDoubleExtra("length", 0.0)
            tvWidth.text = "宽：${String.format("%.1f", width * 100)}CM"
            tvLength.text = "长：${String.format("%.1f", length * 100)}CM"
            tvHeight.text = "高：${String.format("%.1f", height * 100)}CM"
        }
    }
}