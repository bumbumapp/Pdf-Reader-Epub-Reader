package com.gitlab.mudlej.MjPdfReader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.ui.main.MainActivity
import com.gitlab.mudlej.MjPdfReader.util.openSelectedDocument

class Launcher(private val activity: MainActivity, private val pdf: PDF) {
    fun pdfPicker(): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result:ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val selectedDocumentUri = result.data?.data
                openSelectedDocument(activity, pdf, selectedDocumentUri)
            }
        }
    }

    fun saveToDownloadPermission(requestFunction:(Boolean) -> (Unit))
        = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isPermissionGranted: Boolean -> requestFunction(isPermissionGranted)
        }

    fun readFileErrorPermission(requestFunction:(Boolean) -> (Unit))
        = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isPermissionGranted: Boolean -> requestFunction(isPermissionGranted)
        }

    fun settings(requestFunction: (Uri?) -> Unit)
        = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            requestFunction(pdf.uri)
        }
}

class Launchers(
    val pdfPicker: ActivityResultLauncher<Intent>,
    val saveToDownloadPermission: ActivityResultLauncher<String>,
    val readFileErrorPermission: ActivityResultLauncher<String>,
    val settings: ActivityResultLauncher<Intent>,
)