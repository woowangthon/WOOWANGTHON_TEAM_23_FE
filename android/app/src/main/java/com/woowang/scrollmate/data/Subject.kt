package com.woowang.scrollmate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 과목은 기기에만 저장함. 서버는 과목을 모름
@Entity(tableName = "subject")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // ISO 날짜 (yyyy-MM-dd). 시험일이 없으면 null
    val examDate: String? = null,
    val enabled: Boolean = true,
)
