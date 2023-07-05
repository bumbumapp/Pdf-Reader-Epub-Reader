package com.gitlab.mudlej.MjPdfReader.ui.main

import android.content.Context
import com.gitlab.mudlej.MjPdfReader.entity.MyObjectBox
import io.objectbox.BoxStore

object BoxStoreManager {
    private var boxStore: BoxStore? = null

    fun getBoxStore(context: Context): BoxStore {
        if (boxStore == null) {
            synchronized(BoxStoreManager::class.java) {
                if (boxStore == null) {
                    boxStore = MyObjectBox.builder()
                        .androidContext(context.applicationContext)
                        .build()
                }
            }
        }
        return boxStore!!
    }
}
