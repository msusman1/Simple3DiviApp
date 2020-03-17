package com.fleeksoft.simple3diviapp.ui

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.fleeksoft.simple3diviapp.PersonListActivity
import com.fleeksoft.simple3diviapp.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val permissions = listOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 4)
        button_enroll.setOnClickListener {
            startActivity(Intent(this, EnrollActivity::class.java))
        }
        button_recognize.setOnClickListener {
            startActivity(Intent(this, RecognizeActivity::class.java))
        }
        button_registered_person.setOnClickListener {
            startActivity(Intent(this, PersonListActivity::class.java))
        }
    }

}
