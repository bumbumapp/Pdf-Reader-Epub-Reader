/*
 *   MJ PDF
 *   Copyright (C) 2023 Mudlej
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  --------------------------
 *  This code was previously licensed under
 *
 *  MIT License
 *
 *  Copyright (c) 2018 Gokul Swaminathan
 *  Copyright (c) 2023 Mudlej
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.gitlab.mudlej.MjPdfReader.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.*
import android.print.PrintManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.folioreader.Config
import com.folioreader.FolioReader
import com.folioreader.model.HighLight
import com.folioreader.model.locators.ReadLocator
import com.folioreader.util.AdsLoader
import com.folioreader.util.AppUtil
import com.folioreader.util.OnHighlightListener
import com.folioreader.util.ReadLocatorListener
import com.github.barteksc.pdfviewer.PDFView.Configurator
import com.github.barteksc.pdfviewer.PDFView.GONE
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.scroll.ScrollHandle
import com.github.barteksc.pdfviewer.util.Constants
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.gitlab.mudlej.MjPdfReader.Launcher
import com.gitlab.mudlej.MjPdfReader.Launchers
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.data.Preferences
import com.gitlab.mudlej.MjPdfReader.data.SearchResult
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityMainBinding
import com.gitlab.mudlej.MjPdfReader.databinding.PasswordDialogBinding
import com.gitlab.mudlej.MjPdfReader.entity.RecentPaths
import com.gitlab.mudlej.MjPdfReader.manager.database.DatabaseManager
import com.gitlab.mudlej.MjPdfReader.manager.database.DatabaseManagerImpl
import com.gitlab.mudlej.MjPdfReader.manager.fullscreen.FullScreenOptionsManager
import com.gitlab.mudlej.MjPdfReader.manager.fullscreen.FullScreenOptionsManagerImpl
import com.gitlab.mudlej.MjPdfReader.repository.AppDatabase
import com.gitlab.mudlej.MjPdfReader.ui.*
import com.gitlab.mudlej.MjPdfReader.ui.bookmark.BookmarksActivity
import com.gitlab.mudlej.MjPdfReader.ui.link.LinksActivity
import com.gitlab.mudlej.MjPdfReader.ui.search.SearchActivity2
import com.gitlab.mudlej.MjPdfReader.ui.settings.SettingsActivity
import com.gitlab.mudlej.MjPdfReader.util.*
import com.google.android.gms.ads.AdRequest
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.shockwave.pdfium.PdfPasswordException
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.*
import java.io.*
import java.lang.Runnable
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), OnHighlightListener, ReadLocatorListener,
    FolioReader.OnClosedListener {
    enum class AdditionalOptions { APP_SETTINGS, METADATA, ADVANCED_CONFIG }
    private lateinit var recentFiles: List<RecentPaths>

    private val shouldStopExtracting: MutableMap<Int, Boolean> = mutableMapOf()
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var folioReader:FolioReader
    private var doubleBackToExitPressedOnce = false
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private lateinit var fullScreenOptionsManager: FullScreenOptionsManager
    private lateinit var databaseManager: DatabaseManager
    private lateinit var pref: Preferences
    private lateinit var pdfsAdapter:PdfsAdapter
    private val pdf = PDF()
    private var checkVisiblity:Boolean = false
    private lateinit var customView: View
    private var pathFromUri:String?=null

    private val launchers = Launchers(
        Launcher(this, pdf).pdfPicker(),
        Launcher(this, pdf).saveToDownloadPermission(::saveDownloadedFileAfterPermissionRequest),
        Launcher(this, pdf).readFileErrorPermission(::restartAppIfGranted),
        Launcher(this, pdf).settings(::displayFromUri)
    )

    private lateinit var appTitle: TextView
    private lateinit var showSearchBar: () -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "-----------onCreate: ${pdf.name} ")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setCustomActionBar()
        // To avoid FileUriExposedException, (https://stackoverflow.com/questions/38200282/)
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())

        // init
        pref = Preferences(PreferenceManager.getDefaultSharedPreferences(this))
        fullScreenOptionsManager = FullScreenOptionsManagerImpl(binding, pdf, pref.getHideDelay().toLong())
        databaseManager = DatabaseManagerImpl(AppDatabase.getInstance(applicationContext))

        Constants.THUMBNAIL_RATIO = pref.getThumbnailRation()
        Constants.PART_SIZE = pref.getPartSize()

        epubreaderLoad()

        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        // Create PDF by restoring it in case of an activity restart OR open filer picker
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState)

        } else {
            if (intent.getStringExtra("uri")!=null){
                pdf.uri=Uri.parse(intent.getStringExtra("uri"))
            }else {
                pdf.uri = intent.data
                if (pdf.uri != null) {
                    pathFromUri = getDriveFilePath(pdf.uri!!, this)
                }
                if (pdf.uri == null) {
                   checkVisibilityForRecentFiles()
                }
            }
        }
       binding.landingOpenFab.setOnClickListener {
           pickFile()
       }
        binding.info.setOnClickListener {
           showDialogAbout()
       }
        binding.settings.setOnClickListener {
            startActivity(Intent(this,SettingsActivity::class.java))
        }
        displayFromUri(pdf.uri)
        setButtonsFunctionalities()
        setUpSecondBar()
        getRecentFiles()
        //showAppFeaturesDialogOnFirstRun()
    }
    private fun showNoTextInPageToast() {
        Toast.makeText(this, "Couldn't find text in this page.", Toast.LENGTH_LONG).show()
    }
    private fun showCopyPageTextDialog(
        activity: MainActivity,
        pageNumber: Int,
        pageText: String,
        pref: Preferences,
        bypass: Boolean = false
    ) {
        if (!bypass && !pref.getCopyTextDialog()) return

        // create a custom view to make the text selectable
        val pageTextView = TextView(activity)
        pageTextView.setPadding(30, 20, 30, 0)
        pageTextView.setTextIsSelectable(true)
        pageTextView.setTextColor(ContextCompat.getColor(activity, R.color.topBarBackgroundColor))
        pageTextView.textSize = 18f
        pageTextView.text = pageText

        val scrollView = ScrollView(activity)
        scrollView.addView(pageTextView)
        //scrollView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        //scrollView.scrollBarSize = 2

        AlertDialog.Builder(activity, R.style.MJDialogThemeLight)
            .setView(scrollView)
            .setTitle("${activity.getString(R.string.selectable_text)} #${pageNumber + 1}")
            .setNegativeButton(activity.getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(activity.getString(R.string.copy_all)) { dialog, _ ->
                val copyLabel = "${activity.getString(R.string.page)} #${pageNumber} Text"
                copyToClipboard(activity, copyLabel, pageText)

                // show message to user before closing
                Toast.makeText(activity, activity.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .also {
                // don't show this button if the click came from the action bar
                if (!bypass) {
                    it.setNeutralButton(activity.getString(R.string.dont_pop_up)) { dialog, _ ->
                        pref.setCopyTextDialog(false)
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }
    private fun copyPageText(bypass: Boolean) {
        val pageNumber = pdf.pageNumber
        if (shouldStopExtracting.getOrElse(pageNumber) { false }) {
            return
        }

        var pageText = ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var pathFromU = pdf.uri?.let { getFilePathForN(it,this@MainActivity) }
                val pdfReader = PdfReader(pathFromU)
                pageText = PdfTextExtractor.getTextFromPage(pdfReader, pageNumber + 1)
            }
            catch (e: Throwable) {
                Log.e("PDFium", "extractPageText($pageNumber): error while extracting text", e)
                showFailedExtractTextSnackbar(pageNumber)
            }

            withContext(Dispatchers.Main) {
                if (pageText.isEmpty() || pageText.isBlank())
                    showNoTextInPageToast()
                else
                    showCopyPageTextDialog(this@MainActivity, pageNumber, pageText, pref, bypass)
            }
        }
    }
    fun initPdf(pdf: PDF, uri: Uri) {
        pdf.uri = uri
        pdf.fileHash = computeHash(this@MainActivity, pdf)
        if (pdf.fileHash == null) {
            respondToNoFileHash()
        }
    }

    private fun respondToNoFileHash() {
        //throw IllegalStateException("Failed to compute file hash")
        Toast.makeText(
            this,
            "Can't hash the file! Last visited page won't be remembered in this session.",
            Toast.LENGTH_LONG
        ).show()
    }
    private fun showFailedExtractTextSnackbar(pageNumber: Int) {
        Snackbar.make(binding.root, "Failed to extract text of this file.", Snackbar.LENGTH_SHORT)
            .setAction("Stop this message") { shouldStopExtracting[pageNumber] = true }
            .show()
    }

    private fun setCustomActionBar() {
        val actionBar = supportActionBar
        // Disable the default and enable the custom
        actionBar?.setDisplayShowTitleEnabled(false)
        actionBar?.setDisplayShowCustomEnabled(true)

        customView = layoutInflater.inflate(R.layout.actionbar_title, null)
        appTitle = customView.findViewById(R.id.actionbarTitle)



        fun titleClickListener() {
            val title = pdf.getTitle()
            if (title.isNotBlank()) {
                Toast.makeText(this, title, Toast.LENGTH_LONG).show()
            }
        }
        appTitle.setOnClickListener { titleClickListener() }

        // Apply the custom view
        actionBar?.customView = customView
    }




    private fun pickFile() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"  // Allow all file types
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "application/epub+zip"))
            }
            launchers.pdfPicker.launch(intent)
        } catch (e: ActivityNotFoundException) {
            // alert user that file manager not working
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_LONG).show()
        }
    }

    fun displayFromUri(uri: Uri?) {
        if (uri == null){
            return
        }
        if (pathFromUri!=null){
            pdf.uri=FileProvider.getUriForFile(
                   this, AndroidFileCache.getProviderAuthority(this),
                    File(pathFromUri)
                )
            Log.d("pathFromUri", pathFromUri!!)
            }

       var persistentUri=false

        if (intent.data == null) {
            try {
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                persistentUri = true
            } catch (e: Exception) {
                Log.d("EXCEPTIONREAD", e.message.toString())
            }
        }


        addRecentFiles(pdf.uri)
        Log.d("EXCEPTIONREAD", "e.message.toString()")
        supportActionBar?.show()
        checkVisiblity=false
        val mimeType = contentResolver.getType(uri)
        if (mimeType!!.endsWith("/epub+zip")) {
            getFileEpubReaderFile(pdf.uri!!)
              finish()

        }else {
            pdf.name = getFileName(this, pdf.uri!!,persistentUri)
            updateAppTitle()
            pdf.resetLength()
            setTaskDescription(ActivityManager.TaskDescription(pdf.name))
            val scheme = uri.scheme
            if (scheme != null && scheme.contains("http")) {
                downloadOrShowDownloadedFile(uri)
            } else {
                Log.d("pathFromUri2", ""+pdf.uri!!)

                initPdfViewAndLoad(binding.pdfView.fromUri(pdf.uri))

                // start extracting text in the background
                //if (!pdf.isExtractingTextFinished) extractPdfText()
            }
        }

    }

    private fun updateAppTitle() {
        appTitle.text = pdf.getTitleWithPageNumber()
    }

    private fun initPdfViewAndLoad(viewConfigurator: Configurator) {
        // attempt to find a saved location for the pdf else assign zero
        if (pdf.pageNumber == 0) {
            lifecycleScope.launchWhenCreated {
                val hash = computeHash(this@MainActivity, pdf) ?: return@launchWhenCreated
                val pageNumber = databaseManager.findLocation(hash)

                pdf.fileHash = hash
                pdf.pageNumber = pageNumber

                withContext(Dispatchers.Main) {
                    initPdfViewAndLoad(viewConfigurator, pageNumber)
                }
            }
        } else initPdfViewAndLoad(viewConfigurator, pdf.pageNumber)
    }

    private fun initPdfViewAndLoad(viewConfigurator: Configurator, pageNumber: Int) {
        configureTheme()

        val pdfView = binding.pdfView
        pdfView.useBestQuality(pref.getHighQuality())
        pdfView.minZoom = Preferences.minZoomDefault
        pdfView.midZoom = Preferences.midZoomDefault
        pdfView.maxZoom = pref.getMaxZoom()
        pdfView.zoomTo(pdf.zoom)

        viewConfigurator   // creates a PDFView.Configurator
            .defaultPage(pageNumber)
            .onPageChange { page: Int, pageCount: Int -> setCurrentPage(page, pageCount) }
            .enableAnnotationRendering(Preferences.annotationRenderingDefault)
            .enableAntialiasing(pref.getAntiAliasing())
            .onTap { fullScreenOptionsManager.showAllTemporarilyOrHide(); true }
            .scrollHandle(createScrollHandle())
            .onLongPress{ copyPageText(false) }
            .spacing(Preferences.spacingDefault)
            .onError { exception: Throwable -> handleFileOpeningError(exception) }
            .onPageError { page: Int, error: Throwable -> reportLoadPageError(page, error) }
            .pageFitPolicy(FitPolicy.WIDTH)
            .password(pdf.password)
            .swipeHorizontal(pref.getHorizontalScroll())
            .autoSpacing(pref.getHorizontalScroll())
            .pageSnap(pref.getPageSnap())
            .pageFling(pref.getPageFling())
            .nightMode(pref.getPdfDarkTheme())
            .load()


        // Show the page scroll handler for 3 seconds when the pdf is loaded then hide it.
        pdfView.performTap()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createScrollHandle(): ScrollHandle {
        // hiding the handle if the pdf.length is 1 will happen when pdf.length is set in setPdfLength()
        val handle = DefaultScrollHandle(this)
        handle.setOnTouchListener(fullScreenOptionsManager.getOnTouchListener())
        handle.setOnClickListener { goToPage() }
        return handle
    }






    private fun setUpSecondBar() {
        val buttons: MutableList<ImageView> = mutableListOf()

        // padding values
        val paddingHorDp = 36
        val paddingVerDp = 8
        val density = resources.displayMetrics.density
        val paddingHorizontal = (paddingHorDp * density).toInt()    // convert to pixels
        val paddingVertical = (paddingVerDp * density).toInt()      // convert to pixels

        val toggleTheme = ImageView(this)
        toggleTheme.setImageResource(R.drawable.ic_toggle_theme)
        toggleTheme.setOnClickListener { switchPdfTheme() }
        buttons.add(toggleTheme)

        val openFile = ImageView(this)
        openFile.setImageResource(R.drawable.ic_folder)
        openFile.setOnClickListener { pickFile() }
        buttons.add(openFile)



        val bookmarks = ImageView(this)
        bookmarks.setImageResource(R.drawable.ic_book_bookmark)
        bookmarks.setOnClickListener { showBookmarks() }
        buttons.add(bookmarks)



        val shareFile = ImageView(this)
        shareFile.setImageResource(R.drawable.ic_share)
        shareFile.setOnClickListener { shareFile(pdf.uri, FileType.PDF) }
        buttons.add(shareFile)




        for (button in buttons) {
            button.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
            binding.secondBarLayout.addView(button)
        }

        // show it or hide it based on preferences
        if (pref.getSecondBarEnabled() && !pdf.isFullScreenToggled) {
            binding.secondBarLayout.visibility = View.VISIBLE
        }
        else {
            binding.secondBarLayout.visibility = View.GONE
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setButtonsFunctionalities() {
        exitFullScreenListener(binding)
        setBrightnessButtons(binding)
        setAutoScrollButtons(binding)
        binding.apply {
            rotateScreenButton.setOnClickListener { rotateScreenButtonListener() }
            brightnessButton.setOnClickListener { brightnessButtonListener(binding) }
            autoScrollButton.setOnClickListener { autoScrollButtonListener(binding) }
            screenshotButton.setOnClickListener { takeScreenshot() }
            toggleHorizontalSwipeButton.setOnClickListener { horizontalSwipeButtonListener(binding) }
        }
    }

    private fun rotateScreenButtonListener() {
        requestedOrientation =
            if (pdf.isPortrait) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        pdf.togglePortrait()
    }

    private fun horizontalSwipeButtonListener(binding: ActivityMainBinding) {
        binding.apply {
            if (pdfView.isHorizontalSwipeDisabled) {
                enableHorizontalSwiping(binding)
            } else {
                disableHorizontalSwiping(binding)
            }
        }
        fixButtonsColor()
    }

    override fun onStop() {
        Log.d("STOPPP","fvje")

        checkVisiblity = binding.pickFile.isVisible
        super.onStop()
    }

    override fun onPause() {
        Log.d("PAsusse","fvje")

        checkVisiblity = binding.pickFile.isVisible

        super.onPause()
    }
    private fun enableHorizontalSwiping(binding: ActivityMainBinding) {
        binding.toggleHorizontalSwipeImage.setImageResource(R.drawable.ic_horizontal_swipe_locked)
        binding.pdfView.isHorizontalSwipeDisabled = false
    }

    private fun disableHorizontalSwiping(binding: ActivityMainBinding) {
        binding.toggleHorizontalSwipeImage.setImageResource(R.drawable.ic_allow_horizontal_swipe)
        binding.pdfView.isHorizontalSwipeDisabled = true
    }

    private fun setBrightnessButtons(binding: ActivityMainBinding) {
        // init the seekbar
        val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        binding.brightnessSeekBar.progress = brightness
        binding.brightnessPercentage.text = "$brightness%"
        binding.brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar == null) return
                // Don't override system's brightness if the user didn't manually asked for it
                if (fromUser) updateBrightness(progress)
            }
        })
    }

    private fun setAutoScrollButtons(binding: ActivityMainBinding) {
        var isAutoScrolling = false
        val delay = 1L
        val scrollUnit = Preferences.AUTO_SCROLL_UNIT
        var scrollBy = -scrollUnit * pref.getScrollSpeed()

        binding.autoScrollSpeedText.text = simplifySpeed(scrollBy).toString()

        binding.increaseScrollSpeedButton.setOnClickListener {
            scrollBy = changeScrollingSpeed(scrollBy, scrollUnit, isIncreasing = true)
            saveScrollSpeed(scrollBy)
        }
        binding.decreaseScrollSpeedButton.setOnClickListener {
            if (scrollBy.absoluteValue > scrollUnit) {
                scrollBy = changeScrollingSpeed(scrollBy, scrollUnit, isIncreasing = false)
                saveScrollSpeed(scrollBy)
            }
        }

        // check this out: https://stackoverflow.com/questions/7938516/continuously-increase-integer-value-as-the-button-is-pressed
        val handler = Handler(mainLooper)
        lateinit var runnable: Runnable
        val HANDLER_DELAY = 100L

        fun createUpdatingSpeedRunnable(isIncreasing: Boolean): Boolean {
            runnable = Runnable {
                if (!binding.increaseScrollSpeedButton.isPressed && !binding.decreaseScrollSpeedButton.isPressed) {
                    return@Runnable
                }

                scrollBy = changeScrollingSpeed(scrollBy, scrollUnit, isIncreasing)
                handler.postDelayed(runnable, HANDLER_DELAY)
            }
            handler.postDelayed(runnable, HANDLER_DELAY)
            return true
        }

        binding.increaseScrollSpeedButton.setOnLongClickListener { createUpdatingSpeedRunnable(isIncreasing = true) }
        binding.decreaseScrollSpeedButton.setOnLongClickListener { createUpdatingSpeedRunnable(isIncreasing = false) }

        binding.reverseAutoScrollButton.setOnClickListener { scrollBy = -scrollBy }

        binding.toggleAutoScrollButton.setOnClickListener {
            isAutoScrolling = !isAutoScrolling

            if (!isAutoScrolling) {
                stopAutoScrolling(binding)
                return@setOnClickListener
            } else {
                binding.toggleAutoScrollButton.setImageResource(R.drawable.ic_pause)
            }

            fun scroll() {
                autoScrollHandler.postDelayed({
                    binding.pdfView.moveRelativeTo(0F, scrollBy.toFloat())
                    binding.pdfView.loadPages()

                    if (isAutoScrolling || pdf.pageNumber < pdf.length) {
                        scroll()
                    }
                }, delay)
            }
            scroll()
        }
    }

    private fun saveScrollSpeed(scrollBy: Double) {
        pref.setScrollSpeed(simplifySpeed(scrollBy))
    }

    private fun stopAutoScrolling(binding: ActivityMainBinding) {
        binding.toggleAutoScrollButton.setImageResource(R.drawable.ic_start)
        autoScrollHandler.removeCallbacksAndMessages(null)
    }

    private fun autoScrollButtonListener(binding: ActivityMainBinding) {
        binding.apply {
            if (toggleAutoScrollButton.isVisible) {
                increaseScrollSpeedButton.visibility = View.GONE
                decreaseScrollSpeedButton.visibility = View.GONE
                reverseAutoScrollButton.visibility = View.GONE
                toggleAutoScrollButton.visibility = View.GONE
                autoScrollSpeedText.visibility = View.GONE
            } else {
                increaseScrollSpeedButton.visibility = View.VISIBLE
                decreaseScrollSpeedButton.visibility = View.VISIBLE
                reverseAutoScrollButton.visibility = View.VISIBLE
                toggleAutoScrollButton.visibility = View.VISIBLE
                autoScrollSpeedText.visibility = View.VISIBLE
            }
        }
    }

    private fun brightnessButtonListener(binding: ActivityMainBinding) {
        binding.apply {
            if (brightnessSeekBar.isVisible) {
                brightnessSeekBar.visibility = View.GONE
                brightnessPercentage.visibility = View.GONE
            } else {
                brightnessSeekBar.visibility = View.VISIBLE
                brightnessPercentage.visibility = View.VISIBLE
            }
        }
    }

    private fun exitFullScreenListener(binding: ActivityMainBinding) {
        binding.exitFullScreenButton.setOnClickListener {
            unlockScreenOrientation()
            toggleFullscreen()
            stopAutoScrolling(binding)
            enableHorizontalSwiping(binding)

            // A try to give the brightness control back to the system but this won't work
            // updateBrightness(brightness)
        }
    }

    private fun unlockScreenOrientation() {
        // set orientation to unspecified so that the screen rotation will be unlocked
        // this is because PORTRAIT / LANDSCAPE modes will lock the app in them
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun simplifySpeed(scrollBy: Double): Int {
        return (scrollBy.absoluteValue *  (1 / Preferences.AUTO_SCROLL_UNIT)).toInt()
    }

    private fun changeScrollingSpeed(scrollBy: Double, interval: Double, isIncreasing: Boolean): Double {
        val newSpeed = if (isIncreasing) {
            (scrollBy.absoluteValue + interval) * scrollBy.sign
        } else {
            if (scrollBy.absoluteValue > interval) {
                (scrollBy.absoluteValue - interval) * scrollBy.sign
            } else {
                scrollBy
            }
        }

        binding.autoScrollSpeedText.text = simplifySpeed(newSpeed).toString()
        return newSpeed
    }

    private fun updateBrightness(brightness: Int) {
        binding.brightnessPercentage.text = "$brightness%"
        val layout = window.attributes
        window.attributes.screenBrightness = brightness.toFloat() / 100
        window.attributes = layout
    }

    public override fun onResume() {
        Log.i(TAG, "-----------onResume: ${pdf.name} ")
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (pref.getScreenOn()) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // check if there is a pdf at first
        // if (pdf.uri == null) return
        checkPdfuri(pdf.uri)

        // restore the full screen mode if was toggled On
        restoreFullScreenIfNeeded()

        // Prompt the user to restore the previous zoom if there is one saved other than the default
        // pdfZoom != binding.pdfView.getZoom())   // doesn't work for some peculiar reason
        if (pdf.zoom != 1f) {
            Snackbar.make(
                findViewById(R.id.mainLayout),
                getString(R.string.ask_restore_zoom), Snackbar.LENGTH_LONG
            )
                .setAction(getString(R.string.restore)) {
                    binding.pdfView.zoomWithAnimation(pdf.zoom)
                }
                .show()
        }
        fixButtonsColor()
    }

    private fun checkPdfuri(uri: Uri?) {
        if (uri != null){
            binding.pickFile.visibility = View.GONE
            if (!checkVisiblity) {
                binding.pdfView.visibility = View.VISIBLE
            }else{
                binding.pickFile.visibility = View.VISIBLE
            }


        }
        if (uri == null){
            Log.d("RESUMEEEE",""+checkVisiblity)

            binding.secondBarLayout.visibility=View.GONE
            supportActionBar?.hide()
        }

    }

    private fun restoreFullScreenIfNeeded() {
        if (pdf.isFullScreenToggled) {
            pdf.isFullScreenToggled = false
            toggleFullscreen()
        }
    }

    private fun checkPermission() {
        pickFile()
    }
    private fun getDriveFilePath(uri: Uri, context: Context): String? {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
        val file = File(context.cacheDir, name)
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable = inputStream!!.available()

            //int bufferSize = 1024;
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            Log.e("File Size", "Size " + file.length())
            inputStream.close()
            outputStream.close()
            Log.e("File Path", "Path " + file.path)
        } catch (e: Exception) {
            Log.e("Exception", e.message!!)
        }
        return file.path
    }

    private fun addRecentFiles(data: Uri?) {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month0fYear = calendar.get(Calendar.MONTH) + 1
        var day:String?=dayOfMonth.toString()
        var month:String?=month0fYear.toString()
        if (dayOfMonth<10){
            day = "0$day"
        }
        if (month0fYear<10){
            month = "0$month"
        }

        val date="$month/$day"
        binding.progressBar.visibility = View.GONE
        CoroutineScope(Dispatchers.IO).launch {
            data?.let {
                val boxStore: BoxStore = BoxStoreManager.getBoxStore(applicationContext)
                val path=data.toString()

                var parent=getParentFolderFromUri(it)
                if (parent=="item" || parent=="cache"){
                    parent = "This Device"
                }
                val fileName= getFileName(this@MainActivity, it, true)
                val recentFilePathsBox: Box<RecentPaths> = boxStore.boxFor(RecentPaths::class.java)
                val recentPaths=recentFilePathsBox.all
                val isInDatabase = async {checkFileInDatabase(recentPaths,path,date,fileName,recentFilePathsBox)}.await()
                Log.d("!isInDatabase","!isInDatabase jhdkdfd")

                if (!isInDatabase){

                    val recentFile = RecentPaths(
                        path = path,
                        parent = parent,
                        name = fileName,
                        date = date
                    )
                    if(fileName.endsWith(".pdf") || fileName.endsWith(".epub")) {
                        recentFilePathsBox.put(recentFile)
                    }

                }
            }
        }



    }
    private fun getFileEpubReaderFile(uri: Uri){
        val path = getFilePathForN(uri,this)
            var config = AppUtil.getSavedConfig(applicationContext)
            if (config == null) config = Config()
            config.allowedDirection = Config.AllowedDirection.VERTICAL_AND_HORIZONTAL
            folioReader.setConfig(config, true).openBook(path)
            getRecentFiles()



    }
    private fun getFilePathForN(uri: Uri, context: Context): String? {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
        val file = File(context.filesDir, name)
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable = inputStream!!.available()

            //int bufferSize = 1024;
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            Log.e("File Size", "Size " + file.length())
            inputStream.close()
            outputStream.close()
            Log.e("File Path", "Path " + file.path)
            Log.e("File Size", "Size " + file.length())
        } catch (e: java.lang.Exception) {
            Log.e("Exception", e.message!!)
        }
        return file.path
    }

    private fun epubreaderLoad() {
        folioReader = FolioReader.get()

        folioReader = FolioReader.get()
            .setOnHighlightListener(this)
            .setReadLocatorListener(this)
            .setOnClosedListener(this)
    }


    private fun getParentFolderFromUri(uriF: Uri?): String? {
        val uri:Uri? = if (intent.data!=null){
            intent.data
        } else{
            uriF
        }
        val path: String? = uri?.path
        val file = File(path!!)
        return Objects.requireNonNull(file.parentFile).name
    }



    private fun checkFileInDatabase(
        recentPaths: List<RecentPaths>,
        path: String?,
        date: String,
        fileName: String?,
        recentFilePathsBox: Box<RecentPaths>
    ):Boolean{
        recentPaths.forEach{recentPath->
            if (recentPath.name==fileName) {
                val recentFile = RecentPaths(recentPath.id,
                    path = path,
                    parent = recentPath.parent,
                    name = recentPath.name,
                    date = date
                )
                recentFilePathsBox.put(recentFile)
                return true
            }
        }
        return false
    }

    private fun fixButtonsColor() {
        // changes buttons color
        val color = if (pref.getPdfDarkTheme()) R.color.bright else R.color.dark
        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.exitFullScreenImage.drawable),
            ContextCompat.getColor(this, color)
        )
        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.rotateScreenImage.drawable),
            ContextCompat.getColor(this, color)
        )
        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.brightnessButton.drawable),
            ContextCompat.getColor(this, color)
        )
        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.autoScrollButton.drawable),
            ContextCompat.getColor(this, color)
        )
        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.screenshotImage.drawable),
            ContextCompat.getColor(this, color)
        )
        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.toggleHorizontalSwipeImage.drawable),
            ContextCompat.getColor(this, color)
        )
    }

    private fun shareFile(uri: Uri?, type: FileType) {
        if (uri == null) {
            checkHasFile()  // only to show the message
            return
        }
        val sharingIntent: Intent =
            if (uri.scheme != null && uri.scheme!!.startsWith("http"))
                plainTextShareIntent(getString(R.string.share_file), pdf.uri.toString())
            else if (type == FileType.PDF)
                fileShareIntent(getString(R.string.share_file), pdf.name, uri)
            else if (type == FileType.IMAGE)
                imageShareIntent(getString(R.string.share_file), pdf.name, uri)
            else return

        try {
            startActivity(sharingIntent)
        } catch (e: Throwable) {
            Toast.makeText(this, "Error sharing the file. (${e.message})", Toast.LENGTH_LONG).show()
        }
    }

    private fun configureTheme() {
        // This should be moved to the onCreate or xml files
        window.statusBarColor = Color.parseColor("#221f35")

        val pdfView = binding.pdfView

        // set background color behind pages
        if (!pref.getPdfDarkTheme()) pdfView.setBackgroundColor(Preferences.pdfDarkBackgroundColor) else pdfView.setBackgroundColor(
            Preferences.pdfLightBackgroundColor
        )
        if (pref.getAppFollowSystemTheme()) {
            if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        } else {
            if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun reportLoadPageError(page: Int, error: Throwable) {
        val message = resources.getString(R.string.cannot_load_page) + page + " " + error
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, message)
    }

    private fun handleFileOpeningError(exception: Throwable) {
        if (exception is PdfPasswordException) {
            if (pdf.password != null) {
                Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show()
                pdf.password = null // prevent the toast if the user rotates the screen
            }
            askForPdfPassword()
        } else if (couldNotOpenFileDueToMissingPermission(exception)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                launchers.readFileErrorPermission.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }else{
                launchers.readFileErrorPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            Toast.makeText(this, R.string.file_opening_error, Toast.LENGTH_LONG).show()
            Log.e(TAG, getString(R.string.file_opening_error), exception)
        }
    }

    private fun couldNotOpenFileDueToMissingPermission(e: Throwable): Boolean {
       val permission = if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)
           Manifest.permission.READ_MEDIA_IMAGES
       else
           Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission)
            == PackageManager.PERMISSION_GRANTED
        ) return false
        val exceptionMessage = e.message
        return e is FileNotFoundException && exceptionMessage != null
                && exceptionMessage.contains(getString(R.string.permission_denied))
    }

    private fun restartAppIfGranted(isPermissionGranted: Boolean) {
        if (isPermissionGranted) {
            // This is a quick and dirty way to make the system restart the current activity *and the current app process*.
            // This is needed because on Android 6 storage permission grants do not take effect until
            // the app process is restarted.
            exitProcess(0)
        } else {
            Toast.makeText(this, R.string.file_opening_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleFullscreen() {
        fun showUi() {
            supportActionBar?.show()
            if (pref.getSecondBarEnabled()) {
                binding.secondBarLayout.visibility = View.VISIBLE
            }
            binding.pdfView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }

        fun hideUi() {
            supportActionBar?.hide()
            binding.secondBarLayout.visibility = View.GONE
            binding.pdfView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )
        }

        if (!pdf.isFullScreenToggled) {
            binding.adView.visibility = View.GONE
            hideUi()
            pdf.isFullScreenToggled = true
            fullScreenOptionsManager.hideAll()

            // show how to exit Full Screen dialog
            if (pref.getShowFeaturesDialog()) {
                showHowToExitFullscreenDialog(this, pref)
            }
        } else {
            binding.adView.visibility = View.VISIBLE

            showUi()
            pdf.isFullScreenToggled = false
            fullScreenOptionsManager.showAllTemporarilyOrHide()
        }
    }

    private fun downloadOrShowDownloadedFile(uri: Uri) {
        if (pdf.downloadedPdf == null) {
            pdf.downloadedPdf = lastCustomNonConfigurationInstance as ByteArray?
        }
        if (pdf.downloadedPdf != null) {
            initPdfViewAndLoad(binding.pdfView.fromBytes(pdf.downloadedPdf))
        } else {
            // we will get the pdf asynchronously with the DownloadPDFFile object
            binding.progressBar.visibility = View.VISIBLE
            val downloadPDFFile =
                DownloadPDFFile(this)
            downloadPDFFile.execute(uri.toString())
        }
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        return pdf.downloadedPdf
    }

    fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    fun saveToFileAndDisplay(pdfFileContent: ByteArray?) {
        pdf.downloadedPdf = pdfFileContent
        saveToDownloadFolderIfAllowed(pdfFileContent)
        initPdfViewAndLoad(binding.pdfView.fromBytes(pdfFileContent))
    }

    private fun saveToDownloadFolderIfAllowed(fileContent: ByteArray?) {
        if (canWriteToDownloadFolder(this)) {
            trySaveToDownloads(fileContent, false)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                     launchers.saveToDownloadPermission.launch(Manifest.permission.READ_MEDIA_IMAGES)
            else
                launchers.saveToDownloadPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        }
    }

    private fun trySaveToDownloads(fileContent: ByteArray?, showSuccessMessage: Boolean) {
        try {
            val downloadDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            writeBytesToFile(downloadDirectory, pdf.name, fileContent)
            if (showSuccessMessage) {
                Toast.makeText(this, R.string.saved_to_download, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e(TAG, getString(R.string.save_to_download_failed), e)
            Toast.makeText(this, R.string.save_to_download_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDownloadedFileAfterPermissionRequest(isPermissionGranted: Boolean) {
        if (isPermissionGranted) {
            trySaveToDownloads(pdf.downloadedPdf, true)
        } else {
            Toast.makeText(this, R.string.save_to_download_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navToAppSettings() {
        launchers.settings.launch(Intent(this, SettingsActivity::class.java))
    }

    private fun setCurrentPage(pageNumber: Int, pageCount: Int) {
        pdf.pageNumber = pageNumber
        setPdfLength(pageCount)
        updateAppTitle()

        val hash = pdf.fileHash ?: computeHash(this, pdf)
        if (hash == null) {
            respondToNoFileHash()
            return
        }

        lifecycleScope.launchWhenCreated {
            databaseManager.saveLocationInBackground(hash, pageNumber)
        }
    }

    private fun setPdfLength(pageCount: Int) {
        pdf.initPdfLength(pageCount)
        if (pageCount == 1) {
            fullScreenOptionsManager.permanentlyHidePageHandle()
        }
    }

    private fun printFile() {
        if (checkHasFile()) {
            val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
            try {
                printManager.print(
                    pdf.name,
                    PdfDocumentAdapter(this, pdf.uri), null
                )
            } catch (e: Throwable) {
                Toast.makeText(this, "Failed to print. Error message: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun askForPdfPassword() {
        val dialogBinding = PasswordDialogBinding.inflate(layoutInflater)
        showAskForPasswordDialog(this, pdf, dialogBinding, ::displayFromUri)
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu2, menu)
        menu.showOptionalIcons()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.fullscreenOption ->AdsLoader.showAds(this){toggleFullscreen()}
            R.id.switchThemeOption -> switchPdfTheme()
            R.id.openFileOption -> pickFile()
            R.id.bookmarksListOption -> showBookmarks()
            R.id.goToPageOption -> goToPage()
            R.id.linksListOption -> showLinks()
            R.id.shareFileOption -> shareFile(pdf.uri, FileType.PDF)
            R.id.printFileOption -> printFile()
            R.id.toggleSecondBar -> toggleSecondBar()
            R.id.additionalOptionsOption -> showAdditionalOptions()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // set search functionality
        val searchViewc = menu.findItem(R.id.searchOption).actionView as SearchView
        searchViewc.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                fun startSearchActivity() {
                    Intent(this@MainActivity, SearchActivity2::class.java).also { searchIntent ->
                        searchIntent.putExtra(PDF.filePathKey, pdf.uri.toString())
                        searchIntent.putExtra(PDF.searchQueryKey, query)
                        startActivityForResult(searchIntent, PDF.startSearchActivity)
                    }
                }

                if (query.isBlank() || query.length < 3) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.too_short_query))
                        .setMessage(getString(R.string.too_short_query_message).format(query))
                        .setNeutralButton(getString(R.string.proceed_anyway)) { _, _ ->
                            startSearchActivity()
                        }
                        .setPositiveButton(getText(R.string.ok)) { badQueryDialog, _ ->
                            badQueryDialog.dismiss()
                        }
                        .show()
                }
                else {
                    startSearchActivity()
                }
                return false
            }

            override fun onQueryTextChange(query: String) = false
        })

        // create a lambda to trigger the search
        showSearchBar = { menu.performIdentifierAction(R.id.searchOption, 0) }

        return super.onPrepareOptionsMenu(menu)
    }


    private fun toggleSecondBar() {
        binding.apply {
            if (secondBarLayout.visibility == View.VISIBLE) {
                secondBarLayout.visibility = View.GONE
                pref.setSecondBarEnabled(false)
            }
            else {
                secondBarLayout.visibility = View.VISIBLE
                pref.setSecondBarEnabled(true)
            }
        }
    }

    private fun showLinks() {
        Intent(this@MainActivity, LinksActivity::class.java).also { linksIntent ->
            linksIntent.putExtra(PDF.filePathKey, pdf.uri.toString())
            startActivityForResult(linksIntent, PDF.startLinksActivity)
        }
    }

    private fun showBookmarks() {
        Intent(this@MainActivity, BookmarksActivity::class.java).also { bookmarkIntent ->
            bookmarkIntent.putExtra(PDF.filePathKey, pdf.uri.toString())
            startActivityForResult(bookmarkIntent, PDF.startBookmarksActivity)
        }
    }

    private fun goToPage() {
        fun goToPage(pageIndex: Int) {
            binding.pdfView.jumpTo(pageIndex)
        }
        showGoToPageDialog(this, pdf.pageNumber, pdf.length, ::goToPage)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (pref.getTurnPageByVolumeButtons()) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> binding.pdfView.jumpTo(++pdf.pageNumber)
                KeyEvent.KEYCODE_VOLUME_UP -> binding.pdfView.jumpTo(--pdf.pageNumber)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun showAdditionalOptions() {

        data class Item(val title: String, val icon: Int)

        val settingsMap = mapOf(
            AdditionalOptions.APP_SETTINGS to Item(getString(R.string.app_settings), R.drawable.ic_settings),
            AdditionalOptions.METADATA to Item(getString(R.string.file_metadata), R.drawable.meta_info),
            AdditionalOptions.ADVANCED_CONFIG to Item(
                getString(R.string.advanced_config),
                R.drawable.ic_display_settings
            ),
        )

        // create a dialog for additional options and set their functionalities

        // Custom Adapter for the dialog so we can use icons for the items
        val items = settingsMap.values.toTypedArray()
        val adapter: ListAdapter = object : ArrayAdapter<Item>(
            this,
            android.R.layout.select_dialog_item,
            android.R.id.text1,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                // Use super class to create the View
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById(android.R.id.text1) as TextView

                textView.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0)
                textView.text = items[position].title
                textView.setTextColor(resources.getColor(R.color.topBarColor))

                val padding = (10 * resources.displayMetrics.density + 0.5f).toInt()
                textView.compoundDrawablePadding = padding
                return view
            }
        }

        AlertDialog.Builder(this, R.style.MJDialogThemeDark)
            .setTitle(getString(R.string.settings))
            .setAdapter(adapter) { dialog, item ->
                when (item) {
                    AdditionalOptions.APP_SETTINGS.ordinal -> {
                        navToAppSettings()
                    }
                    AdditionalOptions.METADATA.ordinal -> {
                        if (checkHasFile()) showMetaDialog(this, binding.pdfView.documentMeta)
                    }
                    AdditionalOptions.ADVANCED_CONFIG.ordinal -> {
                        showPartSizeDialog(this, pref)
                    }

                }
                dialog.dismiss()
            }.show()
    }

    private fun checkHasFile(): Boolean {
        if (!pdf.hasFile()) {
            Snackbar.make(
                binding.root, getString(R.string.no_pdf_in_app),
                Snackbar.LENGTH_LONG
            ).show()
            return false
        }
        return true
    }

    private fun switchPdfTheme() {
        if (checkHasFile()) {
            pref.setPdfDarkTheme(!pref.getPdfDarkTheme())
            recreate()
        }
    }

    private fun screenShot(view: View): Bitmap? {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun takeScreenshot() {
        val now = DateFormat.format("yyyy_MM_dd-hh_mm_ss", Date())
        try {
            val fileName = "${pdf.name.removeSuffix(".pdf")} - ${now}.jpg"
            val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

            fullScreenOptionsManager.showAllTemporarilyOrHide()
            val bitmap = screenShot(binding.pdfView) ?: return

            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, PDF.SCREENSHOT_IMAGE_QUALITY, outputStream)
            outputStream.flush()
            outputStream.close()

            val uri = saveImage(bitmap, fileName)
            Snackbar.make(binding.root, getString(R.string.screenshot_saved), Snackbar.LENGTH_SHORT).also {
                it.setAction(getString(R.string.share)) { shareFile(uri, FileType.IMAGE) }
                it.show()
            }
        } catch (e: Throwable) {
            // Several error may come out with file handling or DOM
            Toast.makeText(this, getString(R.string.failed_save_screenshot), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun saveImage(bitmap: Bitmap, fileName: String): Uri? {
        val (fileOutputStream: OutputStream?, imageUri: Uri?) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/*")

            // e.g.     ~/Pictures/app_name/screenshot1.jpg
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/${getString(R.string.mj_app_name)}/"
            )

            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            Pair(imageUri?.let { contentResolver.openOutputStream(it) }, imageUri)
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            val image = File(imagesDir, fileName)
            Pair(FileOutputStream(image), image.toUri())
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, PDF.SCREENSHOT_IMAGE_QUALITY, fileOutputStream)
        fileOutputStream?.close()
        return imageUri
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(PDF.uriKey, pdf.uri)
        outState.putString(PDF.fileHashKey, pdf.fileHash)
        outState.putInt(PDF.pageNumberKey, pdf.pageNumber)
        outState.putString(PDF.passwordKey, pdf.password)
        outState.putBoolean(PDF.isFullScreenToggledKey, pdf.isFullScreenToggled)
        outState.putFloat(PDF.zoomKey, binding.pdfView.zoom)
        outState.putBoolean(PDF.isExtractingTextFinishedKey, pdf.isExtractingTextFinished)

    }

    private fun restoreInstanceState(savedState: Bundle) {
        pdf.uri = savedState.getParcelable(PDF.uriKey)
        pdf.fileHash = savedState.getString(PDF.fileHashKey)
        pdf.pageNumber = savedState.getInt(PDF.pageNumberKey)
        pdf.password = savedState.getString(PDF.passwordKey)
        pdf.isFullScreenToggled = savedState.getBoolean(PDF.isFullScreenToggledKey)
        pdf.zoom = savedState.getFloat(PDF.zoomKey)
        pdf.isExtractingTextFinished = savedState.getBoolean(PDF.isExtractingTextFinishedKey)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        binding.progressBar.visibility = View.GONE
            when (requestCode) {
                PDF.startBookmarksActivity -> {
                    if (resultCode == PDF.BOOKMARK_RESULT_OK) {
                        val pageIndex =
                            intent?.getIntExtra(PDF.chosenBookmarkKey, pdf.pageNumber) ?: return
                        binding.pdfView.jumpTo(pageIndex)
                    }
                }
                PDF.startLinksActivity -> {
                    if (resultCode == PDF.LINK_RESULT_OK) {
                        val pageNumber =
                            intent?.getIntExtra(PDF.linkResultKey, pdf.pageNumber) ?: return
                        val pageIndex = pageNumber - 1
                        binding.pdfView.jumpTo(pageIndex)
                    }
                }
                PDF.startSearchActivity -> {
                    if (resultCode == PDF.SEARCH_RESULT_OK) {
                        val searchResultJson = intent?.getStringExtra(PDF.searchResultKey) ?: return
                        val searchResultType = object : TypeToken<SearchResult>() {}.type
                        val searchResult =
                            Gson().fromJson<SearchResult>(searchResultJson, searchResultType)

                        // highlight the result text
                        val succeeded = binding.pdfView.createHighlightText(
                            searchResult.pageNumber,
                            searchResult.originalIndex,
                            searchResult.inputEnd - searchResult.inputStart,
                            true
                        )

                        if (!succeeded) {
                            Toast.makeText(
                                this,
                                "Failed to highlight search result",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            binding.pdfView.resetZoomWithAnimation()         // it won't work if the user was zoomed in before searching
                            binding.pdfView.reloadPages()
                        }

                        // remove newlines and tabs in the Snackbar message
                        val resultText = searchResult.text
                            .replace("\n", " ")
                            .replace("\t", " ")

                        // show a snackbar with a button that will remove the highlight (it wills still be cached for a bit)
                        Snackbar.make(
                            binding.root,
                            "Result: $resultText",
                            Snackbar.LENGTH_INDEFINITE
                        )
                            .setAction(getString(R.string.ok)) {
                                binding.pdfView.clearSearchResultsHighlight(searchResult.pageNumber)
                            }
                            .show()

                        binding.pdfView.jumpUsingPageNumber(searchResult.pageNumber)
                    }

            }
        }
    }

    override fun onBackPressed() {
        if (!binding.pickFile.isVisible){
            checkVisibilityForRecentFiles()
            getRecentFiles()
            binding.adView.visibility = View.VISIBLE
        }else {
            if (doubleBackToExitPressedOnce) {
                finishAffinity()
            }

            doubleBackToExitPressedOnce = true
            Toast.makeText(this, getString(R.string.press_back_again), Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed(
                { doubleBackToExitPressedOnce = false },
                2000
            )
        }
    }

    private fun checkVisibilityForRecentFiles() {
        binding.pickFile.visibility=View.VISIBLE
        binding.pdfView.visibility=View.GONE
        binding.secondBarLayout.visibility=View.GONE
        supportActionBar?.hide()


    }


    private fun getRecentFiles() {
        binding.emptyFolder.visibility=View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            val boxStore: BoxStore = BoxStoreManager.getBoxStore(applicationContext)
            val recentFilePathsBox: Box<RecentPaths> = boxStore.boxFor(RecentPaths::class.java)
            recentFiles = recentFilePathsBox.all

            if (recentFiles.isEmpty()){
                checkPermission()
                binding.emptyFolder.visibility=View.VISIBLE
            }else{
            Log.d("intent.data",""+recentFiles)
            withContext(Dispatchers.Main) {

                pdfsAdapter = PdfsAdapter(recentFiles, object : ItemOnClickListener {
                    override fun onItemClick(uri: Uri) {
                        AdsLoader.showAds(this@MainActivity){
                            whenClickedItem(uri)
                        }

                    }

                })
                binding.recycleViewPdfs.apply {
                    adapter = pdfsAdapter
                    layoutManager = LinearLayoutManager(this@MainActivity)
                }



            }

        }
        }
    }

    private fun whenClickedItem(uri: Uri) {
        try {
            val intent =
                Intent(this@MainActivity, MainActivity::class.java)
            intent.putExtra("uri", uri.toString())
            startActivity(intent)
        } catch (e: Exception) {
            Log.d("Exception", e.message.toString())
        }
    }

    override fun onHighlight(highlight: HighLight?, type: HighLight.HighLightAction?) {

    }

    override fun saveReadLocator(readLocator: ReadLocator?) {
    }

    override fun onFolioReaderClosed() {

    }

    //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    private fun showDialogAbout() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE) // before
        dialog.setContentView(R.layout.dialog_about)
        dialog.setCancelable(true)
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        val back = ColorDrawable(Color.TRANSPARENT)
        val margin = 20
        val inset = InsetDrawable(back, margin)
        dialog.window!!.setBackgroundDrawable(inset)
        (dialog.findViewById<View>(R.id.bt_close) as ImageButton).setOnClickListener { if (dialog != null) dialog.dismiss() }
        (dialog.findViewById<View>(R.id.bt_licence) as Button).setOnClickListener {
            val url = "https://gitlab.com/mudlej_android/mj_pdf_reader/-/blob/main/LICENSE"
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
            startActivity(urlIntent)
        }
        (dialog.findViewById<View>(R.id.app_source_code) as Button).setOnClickListener {
            val url = "https://github.com/bumbumapp/Pdf-Reader-Epub-Reader"
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
            startActivity(urlIntent)
        }
        dialog.show()
        dialog.window!!.attributes = lp
    }

}

enum class FileType { IMAGE, PDF }


/*
    * pdf.pageNumber && pdf.length:
        will be set by PDFView::onPageChange() -> setCurrentPage()

    * pdf.password:
        will be set by PDFView::onError() -> handleFileOpeningError() -> askForPdfPassword()
 */