package com.example.datossinmvvm

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
interface UserDao {
    @Query("SELECT * FROM User")
    suspend fun getAll(): List<User>

    @Insert
    suspend fun insert(user: User)

    @Query("DELETE FROM User WHERE uid = (SELECT MAX(uid) FROM User)")
    suspend fun deleteLast()

}