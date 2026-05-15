package com.example.smartfinance.data.repository

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.smartfinance.data.local.ExpenseSplitEntity
import com.example.smartfinance.data.local.GroupDao
import com.example.smartfinance.data.local.GroupEntity
import com.example.smartfinance.data.local.GroupExpenseEntity
import com.example.smartfinance.data.local.SettlementEntity
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.data.local.UserDao
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.data.local.UserGroupCrossRef
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val transactionRepository: TransactionRepository
) {
    private val groupsCollection = firestore.collection("groups")
    private val expensesCollection = firestore.collection("group_expenses")
    private val splitsCollection = firestore.collection("expense_splits")
    private val settlementsCollection = firestore.collection("settlements")
    private val invitationsCollection = firestore.collection("group_invitations")

    fun getGroupsForUser(userId: String): Flow<List<GroupEntity>> {
        return groupsCollection
            .whereArrayContains("memberIds", userId)
            .snapshots()
            .map { it.toObjects<FirestoreGroup>().map { fg -> fg.toEntity() } }
            .onEach { groups ->
                groups.forEach { groupDao.createGroup(it) }
            }
    }

    suspend fun getGroupByIdSimple(groupId: String): GroupEntity? {
        val snapshot = groupsCollection.document(groupId).get().await()
        return snapshot.toObject<FirestoreGroup>()?.toEntity()
    }

    fun getInvitationsForUser(userId: String): Flow<List<GroupInvitation>> {
        return invitationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "pending")
            .snapshots()
            .map { it.toObjects<GroupInvitation>() }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPendingGroupInvitationsWithGroups(userId: String): Flow<List<Pair<GroupInvitation, GroupEntity>>> {
        return getInvitationsForUser(userId).flatMapLatest { invitations ->
            if (invitations.isEmpty()) flowOf(emptyList())
            else {
                val groupIds = invitations.map { it.groupId }.distinct()
                firestore.collection("groups").whereIn("id", groupIds).snapshots().map { groupSnapshots ->
                    val groups = groupSnapshots.toObjects<FirestoreGroup>().map { it.toEntity() }
                    invitations.mapNotNull { invitation ->
                        val group = groups.find { it.id == invitation.groupId }
                        if (group != null) invitation to group else null
                    }
                }
            }
        }
    }

    suspend fun acceptInvitation(invitationId: String) {
        val snapshot = invitationsCollection.document(invitationId).get().await()
        val invitation = snapshot.toObject<GroupInvitation>() ?: return
        
        val groupDoc = groupsCollection.document(invitation.groupId)
        val groupSnapshot = groupDoc.get().await()
        val group = groupSnapshot.toObject<FirestoreGroup>() ?: return
        
        if (!group.memberIds.contains(invitation.userId)) {
            val updatedMembers = group.memberIds.toMutableList().apply { add(invitation.userId) }
            groupDoc.update("memberIds", updatedMembers).await()
            groupDao.addUserToGroup(UserGroupCrossRef(userId = invitation.userId, groupId = invitation.groupId))
        }
        
        invitationsCollection.document(invitationId).update("status", "accepted").await()
    }

    suspend fun rejectInvitation(invitationId: String) {
        invitationsCollection.document(invitationId).update("status", "rejected").await()
    }

    fun getGroupById(groupId: String): Flow<GroupEntity?> {
        return groupsCollection.document(groupId).snapshots().map { 
            it.toObject<FirestoreGroup>()?.toEntity() 
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getGroupMembers(groupId: String): Flow<List<UserEntity>> {
        return groupsCollection.document(groupId).snapshots().flatMapLatest { snapshot ->
            val memberIds = snapshot.toObject<FirestoreGroup>()?.memberIds ?: emptyList()
            if (memberIds.isEmpty()) flowOf(emptyList())
            else {
                firestore.collection("users").whereIn("id", memberIds).snapshots().map {
                    it.toObjects<UserEntity>()
                }.onEach { users ->
                    users.forEach { userDao.registerUser(it) }
                }
            }
        }
    }

    suspend fun createGroup(name: String, description: String, adminId: String): String {
        val docRef = groupsCollection.document()
        val group = FirestoreGroup(
            id = docRef.id,
            name = name,
            description = description,
            inviteCode = UUID.randomUUID().toString().take(8).uppercase(),
            adminId = adminId,
            memberIds = listOf(adminId)
        )
        docRef.set(group).await()
        groupDao.createGroup(group.toEntity())
        return docRef.id
    }

    suspend fun joinGroup(inviteCode: String, userId: String): Boolean {
        val query = groupsCollection.whereEqualTo("inviteCode", inviteCode).limit(1).get().await()
        if (query.isEmpty) return false
        
        val doc = query.documents.first()
        val group = doc.toObject<FirestoreGroup>() ?: return false
        
        if (group.memberIds.contains(userId)) return true
        
        val updatedMembers = group.memberIds.toMutableList().apply { add(userId) }
        doc.reference.update("memberIds", updatedMembers).await()
        groupDao.addUserToGroup(UserGroupCrossRef(userId = userId, groupId = group.id))
        return true
    }

    suspend fun leaveGroup(groupId: String, userId: String) {
        val groupDoc = groupsCollection.document(groupId)
        val snapshot = groupDoc.get().await()
        val group = snapshot.toObject<FirestoreGroup>() ?: return
        
        val updatedMembers = group.memberIds.toMutableList().apply { remove(userId) }
        groupDoc.update("memberIds", updatedMembers).await()
        groupDao.removeUserFromGroup(userId, groupId)
    }
    
    suspend fun inviteUserToGroup(groupId: String, userId: String, senderId: String) {
        val docRef = invitationsCollection.document()
        val invitation = GroupInvitation(
            id = docRef.id,
            groupId = groupId,
            userId = userId,
            senderId = senderId,
            status = "pending"
        )
        docRef.set(invitation).await()
    }

    fun getExpensesForGroup(groupId: String): Flow<List<GroupExpenseEntity>> {
        return expensesCollection
            .whereEqualTo("groupId", groupId)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects<GroupExpenseEntity>() }
    }

    suspend fun addExpense(expense: GroupExpenseEntity, splits: List<ExpenseSplitEntity>) {
        val docRef = expensesCollection.document()

        // 1. Crear transacción personal para el que paga
        val transaction = TransactionEntity(
            userId = expense.paidByUserId,
            title = "Gasto Grupo: ${expense.description}",
            amount = expense.amount,
            type = TransactionType.EXPENSE,
            description = "Gasto realizado en el grupo"
        )
        transactionRepository.insertTransaction(transaction)

        // 2. Guardar el gasto general del grupo en Firestore
        val finalExpense = expense.copy(id = docRef.id, linkedTransactionId = transaction.id)
        docRef.set(finalExpense).await()

        // 3. Generar las divisiones (splits) de la deuda
        splits.forEach { split ->
            val splitId = "${finalExpense.id}_${split.userId}"
            splitsCollection.document(splitId).set(split.copy(expenseId = finalExpense.id)).await()
        }
    }

    suspend fun deleteExpense(expenseId: String) {
        val snapshot = expensesCollection.document(expenseId).get().await()
        val expense = snapshot.toObject<GroupExpenseEntity>()
        
        expense?.linkedTransactionId?.let { txId ->
            transactionRepository.getTransactionById(txId)?.let { tx ->
                transactionRepository.deleteTransaction(tx)
            }
        }

        expensesCollection.document(expenseId).delete().await()
        val splits = splitsCollection.whereEqualTo("expenseId", expenseId).get().await()
        splits.documents.forEach { it.reference.delete().await() }
        
        groupDao.deleteExpense(expenseId)
        groupDao.deleteSplitsForExpense(expenseId)
    }

    suspend fun getSplitsForExpense(expenseId: String): List<ExpenseSplitEntity> {
        return splitsCollection.whereEqualTo("expenseId", expenseId).get().await().toObjects<ExpenseSplitEntity>()
    }

    fun getSettlementsForGroup(groupId: String): Flow<List<SettlementEntity>> {
        return settlementsCollection
            .whereEqualTo("groupId", groupId)
            .snapshots()
            .map { it.toObjects<SettlementEntity>() }
    }

    suspend fun addSettlement(settlement: SettlementEntity) {
        val docRef = settlementsCollection.document()
        
        val debtorTx = TransactionEntity(
            userId = settlement.fromUserId,
            title = "Pago Grupo: Liquidación",
            amount = settlement.amount,
            type = TransactionType.EXPENSE,
            description = "Pago de deuda en grupo"
        )
        transactionRepository.insertTransaction(debtorTx)
        
        val creditorTx = TransactionEntity(
            userId = settlement.toUserId,
            title = "Cobro Grupo: Liquidación",
            amount = settlement.amount,
            type = TransactionType.INCOME,
            description = "Cobro de deuda en grupo"
        )
        transactionRepository.insertTransaction(creditorTx)
        
        val finalSettlement = settlement.copy(
            id = docRef.id,
            debtorTransactionId = debtorTx.id,
            creditorTransactionId = creditorTx.id
        )
        docRef.set(finalSettlement).await()
    }

    suspend fun deleteSettlement(settlementId: String) {
        val snapshot = settlementsCollection.document(settlementId).get().await()
        val settlement = snapshot.toObject<SettlementEntity>()
        
        settlement?.debtorTransactionId?.let { txId ->
            transactionRepository.getTransactionById(txId)?.let { tx ->
                transactionRepository.deleteTransaction(tx)
            }
        }
        settlement?.creditorTransactionId?.let { txId ->
            transactionRepository.getTransactionById(txId)?.let { tx ->
                transactionRepository.deleteTransaction(tx)
            }
        }

        settlementsCollection.document(settlementId).delete().await()
        groupDao.deleteSettlement(settlementId)
    }
}

data class FirestoreGroup(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val inviteCode: String = "",
    val adminId: String = "",
    val memberIds: List<String> = emptyList()
) {
    fun toEntity() = GroupEntity(id, name, description, inviteCode, adminId)
}

@Entity(tableName = "group_invitations")
data class GroupInvitation(
    @PrimaryKey val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val senderId: String = "",
    val status: String = "pending", // pending, accepted o rejected
    val timestamp: Long = System.currentTimeMillis()
)
