package com.example.smartfinance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroup(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUserToGroup(crossRef: UserGroupCrossRef)

    @Query("""
        SELECT * FROM groups 
        INNER JOIN UserGroupCrossRef ON groups.id = UserGroupCrossRef.groupId 
        WHERE userId = :userId
    """)
    fun getGroupsForUser(userId: String): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE inviteCode = :code LIMIT 1")
    suspend fun getGroupByInviteCode(code: String): GroupEntity?

    @Query("SELECT * FROM users INNER JOIN UserGroupCrossRef ON users.id = UserGroupCrossRef.userId WHERE groupId = :groupId")
    fun getGroupMembers(groupId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    fun getGroupById(groupId: String): Flow<GroupEntity?>

    @Query("DELETE FROM UserGroupCrossRef WHERE userId = :userId AND groupId = :groupId")
    suspend fun removeUserFromGroup(userId: String, groupId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addExpense(expense: GroupExpenseEntity): Long

    @Query("DELETE FROM group_expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    @Query("SELECT * FROM group_expenses WHERE groupId = :groupId ORDER BY date DESC")
    fun getExpensesForGroup(groupId: String): Flow<List<GroupExpenseEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpense(expenseId: String): List<ExpenseSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSettlement(settlement: SettlementEntity)

    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteSettlement(settlementId: String)

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC")
    fun getSettlementsForGroup(groupId: String): Flow<List<SettlementEntity>>
}
