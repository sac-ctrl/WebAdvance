package com.cylonid.nativealpha.data

import androidx.room.*
import com.cylonid.nativealpha.manager.Credential
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials WHERE webAppId = :webAppId ORDER BY title ASC")
    fun getCredentialsForApp(webAppId: Long): Flow<List<Credential>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: Credential)

    @Update
    suspend fun updateCredential(credential: Credential)

    @Delete
    suspend fun deleteCredential(credential: Credential)
}