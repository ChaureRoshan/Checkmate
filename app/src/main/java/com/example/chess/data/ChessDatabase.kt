package com.example.chess.data

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

@Entity(tableName = "game_history")
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val opponentName: String,
    val gameMode: String,
    val playerColor: String,
    val result: String, // "WIN", "LOSS", "DRAW"
    val movesCount: Int,
    val pgn: String,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val guestId: String,
    val displayName: String,
    val rating: Int = 1200,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val avatarIndex: Int = 0,
    val themeName: String = "SLATE"
)

@Dao
interface GameDao {
    @Query("SELECT * FROM game_history ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<GameRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameRecordEntity): Long

    @Query("DELETE FROM game_history")
    suspend fun clearHistory()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)
}

@Database(entities = [GameRecordEntity::class, UserProfileEntity::class], version = 1, exportSchema = false)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getDatabase(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "minimal_chess.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
