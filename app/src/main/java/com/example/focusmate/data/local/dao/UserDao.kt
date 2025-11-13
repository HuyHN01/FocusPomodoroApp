package com.example.focusmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.focusmate.data.local.entity.UserEntity

@Dao
interface UserDao {
    // Khi đăng nhập/đăng ký, ta lưu thông tin user vào đây
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheUser(user: UserEntity)

    // Lấy thông tin user đã lưu (ví dụ: để hiển thị ở màn hình profile)
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getCachedUser(uid: String): UserEntity?

    // Khi logout, ta xóa thông tin user
    @Query("DELETE FROM users")
    suspend fun clearCachedUser()
}
