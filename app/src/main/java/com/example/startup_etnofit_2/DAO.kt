// DAO.kt
package com.example.startup_etnofit_2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ChecksDataDao {
    @Insert
    suspend fun insert(checksData: ChecksData)

    @Query("SELECT * FROM checks_data WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getChecksDataByYearAndMonth(year: Int, month: Int): ChecksData?

    @Update
    suspend fun update(checksData: ChecksData)

    @Query("SELECT * FROM checks_data WHERE year = :year")
    suspend fun getChecksDataByYear(year: Int): List<ChecksData>
}

@Dao
interface ReckoningDataDao {
    @Insert
    suspend fun insert(reckoningData: ReckoningData)

    @Query("SELECT * FROM reckoning_data WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getReckoningDataByYearAndMonth(year: Int, month: Int): ReckoningData?

    @Update
    suspend fun update(reckoningData: ReckoningData)
}

@Dao
interface PreviousReckoningDataDao {
    @Insert
    suspend fun insert(previousReckoningData: PreviousReckoningData)

    @Query("SELECT * FROM previous_reckoning_data WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getPreviousReckoningDataByYearAndMonth(year: Int, month: Int): PreviousReckoningData?

    @Update
    suspend fun update(previousReckoningData: PreviousReckoningData)

    @Query("SELECT * FROM previous_reckoning_data WHERE year = :year ORDER BY month DESC LIMIT 1")
    suspend fun getLatestPreviousReckoningDataByYear(year: Int): PreviousReckoningData?
}