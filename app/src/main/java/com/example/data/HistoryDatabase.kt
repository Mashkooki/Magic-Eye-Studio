package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "stereogram_history")
data class StereogramHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val shapeId: String,
    val textValue: String,
    val patternId: String,
    val invertDepth: Boolean,
    val width: Int,
    val height: Int,
    val stripWidth: Int,
    val depthMultiplier: Int,
    val blurRadius: Int,
    val stereogramFilePath: String,
    val depthFilePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface StereogramHistoryDao {
    @Query("SELECT * FROM stereogram_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<StereogramHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: StereogramHistoryEntity): Long

    @Query("DELETE FROM stereogram_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [StereogramHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): StereogramHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "magic_eye_studio.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
