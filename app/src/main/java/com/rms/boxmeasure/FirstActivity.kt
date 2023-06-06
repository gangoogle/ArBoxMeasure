package com.rms.boxmeasure

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.rms.boxmeasure.databinding.ActivityFirstBinding

class FirstActivity : AppCompatActivity() {
    lateinit var mBinding: ActivityFirstBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding =
            DataBindingUtil.setContentView<ActivityFirstBinding>(this, R.layout.activity_first)
        setListener()

    }

    private fun setListener() {
        mBinding.btMeasure.setOnClickListener {
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
            mBinding.tvWidth.text = "宽：${String.format("%.1f", width * 100)}CM"
            mBinding.tvLength.text = "长：${String.format("%.1f", length * 100)}CM"
            mBinding.tvHeight.text = "高：${String.format("%.1f", height * 100)}CM"
        }
    }
}