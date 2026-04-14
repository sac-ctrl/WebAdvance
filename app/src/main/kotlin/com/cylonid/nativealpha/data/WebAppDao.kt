package com.cylonid.nativealpha.data

import androidx.room.*
import com.cylonid.nativealpha.model.WebApp
import kotlinx.coroutines.flow.Flow

@Dao
interface WebAppDao {
    @Query("SELECT * FROM webapps ORDER BY name ASC")
    fun getAllWebApps(): Flow<List<WebApp>>

    @Query("SELECT * FROM webapps WHERE id = :id")
    fun getWebAppById(id: Long): Flow<WebApp?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebApp(webApp: WebApp)

    @Update
    suspend fun updateWebApp(webApp: WebApp)

    @Delete
    suspend fun deleteWebApp(webApp: WebApp)
}