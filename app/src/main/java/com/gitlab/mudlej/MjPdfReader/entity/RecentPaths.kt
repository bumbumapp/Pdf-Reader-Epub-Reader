package com.gitlab.mudlej.MjPdfReader.entity

import android.net.Uri
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class RecentPaths(
    @Id(assignable = true) var id: Long = 0,
    var path: String?=null,
    var date: String?=null,
    var parent:String?=null,
    var name:String?=null
)