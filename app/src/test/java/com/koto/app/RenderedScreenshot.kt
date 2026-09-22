package com.koto.app

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.robolectric.Shadows.shadowOf

/** Native-rendered window capture without Compose's device-only frame-commit wait. */
internal fun saveRenderedScreenshot(activity: Activity, name: String) {
    val decor = activity.window.decorView
    val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
    var result = -1
    PixelCopy.request(activity.window, bitmap, { result = it }, Handler(Looper.getMainLooper()))
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals("Native window capture", PixelCopy.SUCCESS, result)
    val output = File("build/reports/koto/screenshots/$name.png")
    output.parentFile?.mkdirs()
    output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    bitmap.recycle()
}

/** Capture the topmost dialog/sheet root for local visual QA (API 35 test runtime). */
internal fun saveOverlayScreenshot(name: String) {
    val root = android.view.inspector.WindowInspector.getGlobalWindowViews().last()
    val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
    root.draw(android.graphics.Canvas(bitmap))
    val output = File("build/reports/koto/screenshots/$name.png")
    output.parentFile?.mkdirs()
    output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    bitmap.recycle()
}
