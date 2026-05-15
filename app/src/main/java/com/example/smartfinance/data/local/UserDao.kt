package com.example.smartfinance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registerUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFriendship(friendship: FriendshipEntity)

    @Query("""
        SELECT users.* FROM users 
        INNER JOIN friendships ON (users.id = friendships.friendId AND friendships.userId = :userId) 
        OR (users.id = friendships.userId AND friendships.friendId = :userId)
        WHERE friendships.status = 'accepted'
    """)
    fun getFriendsForUser(userId: String): Flow<List<UserEntity>>

    @Query("""
        SELECT users.* FROM users 
        INNER JOIN friendships ON users.id = friendships.userId 
        WHERE friendships.friendId = :userId AND friendships.status = 'pending'
    """)
    fun getPendingFriendRequests(userId: String): Flow<List<UserEntity>>

    @Query("""
        UPDATE friendships 
        SET status = :status 
        WHERE (userId = :userId AND friendId = :friendId) 
        OR (userId = :friendId AND friendId = :userId)
    """)
    suspend fun updateFriendshipStatus(userId: String, friendId: String, status: String)

    @Query("""
        DELETE FROM friendships 
        WHERE (userId = :userId AND friendId = :friendId) 
        OR (userId = :friendId AND friendId = :userId)
    """)
    suspend fun deleteFriendship(userId: String, friendId: String)
}
