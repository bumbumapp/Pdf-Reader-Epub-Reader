package com.gitlab.mudlej.MjPdfReader.manager.database

interface DatabaseManager {

    suspend fun saveLocationInBackground(fileHash: String, pageNumber: Int)

    suspend fun findLocation(fileHash: String): Int

}