package com.fleeksoft.simple3diviapp.ui

import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fleeksoft.simple3diviapp.toast

/**
 * Created by MSUSMAN on 4/11/2018.
 */

abstract class DiviBaseActivity : AppCompatActivity(), ExceptionHandler {

    val dllNativePath by lazy { getApplicationInfo().nativeLibraryDir + "/libfacerec.so"; }
    val confDirPath = "/sdcard/faceapp/conf/facerec/";
    val licenseDirPath = "/sdcard/faceapp/license";
    protected fun setBackArrowEnabled(toolbar: Toolbar, title: String) {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = title

    }
    override fun onException(str: String) {
        Log.d("DiviBaseActivity",str)
        toast(str,Toast.LENGTH_LONG)
        finish()
    }

    private val distance_threshold = 8800f
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            //            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}

interface ExceptionHandler {
    fun onException(str: String)
}
