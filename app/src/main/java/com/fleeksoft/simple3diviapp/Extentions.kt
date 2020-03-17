package com.fleeksoft.simple3diviapp

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    GlobalScope.launch(Dispatchers.Main) {
        Toast.makeText(this@toast, message, length).show()
    }
}

fun Activity.snack(
    message: String,
    view: View = findViewById(android.R.id.content),
    length: Int = Snackbar.LENGTH_SHORT
) {
    GlobalScope.launch(Dispatchers.Main) {
        Snackbar.make(view, message, length).show()
    }
}

fun Activity.snackSuccess(
    message: String,
    view: View = findViewById(android.R.id.content),
    length: Int = Snackbar.LENGTH_SHORT,
    block: ((Snackbar) -> Unit)? = null
) {
    GlobalScope.launch(Dispatchers.Main) {
        val snackbar = Snackbar.make(view, "", length)
        val layout = snackbar.getView() as SnackbarLayout
        val textView =
            layout.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
        textView.visibility = View.INVISIBLE

        val snackView: View = layoutInflater.inflate(R.layout.toast_layout, null)
        val imageView = snackView.findViewById<ImageView>(R.id.image_toast) as ImageView
        imageView.setImageResource(R.drawable.ic_success)
        val textViewTop = snackView.findViewById<TextView>(R.id.text_toast) as TextView
        textViewTop.setText(message)
        textViewTop.setTextColor(Color.WHITE)
        layout.setPadding(0, 0, 0, 0)
        layout.addView(snackView, 0)
        snackbar.show()
        block?.invoke(snackbar)

    }

}

fun Activity.snackError(
    message: String,
    view: View = findViewById(android.R.id.content),
    length: Int = Snackbar.LENGTH_SHORT
) {
    GlobalScope.launch(Dispatchers.Main) {
        val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
        val layout = snackbar.getView() as SnackbarLayout
        val textView =
            layout.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
        textView.visibility = View.INVISIBLE

        val snackView: View = layoutInflater.inflate(R.layout.toast_layout, null)
        val imageView = snackView.findViewById<ImageView>(R.id.image_toast) as ImageView
        imageView.setImageResource(R.drawable.ic_error)
        val textViewTop = snackView.findViewById<TextView>(R.id.text_toast) as TextView
        textViewTop.setText(message)
        textViewTop.setTextColor(Color.WHITE)
        layout.setPadding(0, 0, 0, 0)
        layout.addView(snackView, 0)
        snackbar.show()
    }
}

fun Fragment.snack(message: String, view: View, length: Int = Snackbar.LENGTH_SHORT) {

    Snackbar.make(view, message, length).show()
}


class MarginItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        with(outRect) {
            if (parent.getChildAdapterPosition(view) == 0) {
                top = spaceHeight
            }
            left = spaceHeight
            right = spaceHeight
            bottom = spaceHeight
        }
    }
}


fun Activity.getAppVersion(): String? {
    var version = ""
    try {
        val pInfo = packageManager.getPackageInfo(this.packageName, 0)
        version = "${pInfo.versionName}(${pInfo.versionCode})"
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return version
}

enum class WokerStatus {
    PRESENT,
    ABSENT,
    LEAVE,

}


/** Combination of all flags required to put activity into immersive mode */
const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

/** Milliseconds used for UI animations */
const val ANIMATION_FAST_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L

/**
 * Simulate a button click, including a small delay while it is being pressed to trigger the
 * appropriate animations.
 */
fun ImageButton.simulateClick(delay: Long = ANIMATION_FAST_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    }, delay)
}

/** Pad this view with the insets provided by the device cutout (i.e. notch) */
@RequiresApi(Build.VERSION_CODES.P)
fun View.padWithDisplayCutout() {

    /** Helper method that applies padding from cutout's safe insets */
    fun doPadding(cutout: DisplayCutout) = setPadding(
        cutout.safeInsetLeft,
        cutout.safeInsetTop,
        cutout.safeInsetRight,
        cutout.safeInsetBottom
    )

    // Apply padding using the display cutout designated "safe area"
    rootWindowInsets?.displayCutout?.let { doPadding(it) }

    // Set a listener for window insets since view.rootWindowInsets may not be ready yet
    setOnApplyWindowInsetsListener { _, insets ->
        insets.displayCutout?.let { doPadding(it) }
        insets
    }
}

/** Same as [AlertDialog.show] but setting immersive mode in the dialog's window */
fun AlertDialog.showImmersive() {
    // Set the dialog to not focusable
    window?.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    )

    // Make sure that the dialog's window is in full screen
    window?.decorView?.systemUiVisibility = FLAGS_FULLSCREEN

    // Show the dialog while still in immersive mode
    show()

    // Set the dialog to focusable again
    window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
}

fun Activity.showNameEnterDialog(block: (String?) -> Unit) {
    val builder = MaterialAlertDialogBuilder(this)
    builder.setTitle("Enter Name")
    val viewInflated = LayoutInflater.from(this).inflate(R.layout.input_dialog_layout, null, false)
    val input = viewInflated.findViewById<EditText>(R.id.input)
    val inputLayout = viewInflated.findViewById<TextInputLayout>(R.id.input_layout)

    builder.setView(viewInflated)
    builder.setCancelable(false)
    builder.setPositiveButton("Add", null)
    builder.setNegativeButton("Cancel") { dialog, which -> block(null); dialog.cancel() }
    val nameDialog = builder.create()
    nameDialog.setOnShowListener { dialog ->
        val positiveButton = nameDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val negativeButton = nameDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        positiveButton.setOnClickListener {
            val newName = input.text.toString()
            if (TextUtils.isEmpty(newName) || newName.trim { it <= ' ' }.length < 3) {
                input.error = "Please Enter name"
                return@setOnClickListener
            }
            dialog.dismiss()
            block(newName.trim())
        }

    }
    nameDialog.show()
}
