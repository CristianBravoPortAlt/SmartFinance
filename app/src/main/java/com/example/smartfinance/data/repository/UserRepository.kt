package com.example.smartfinance.data.repository

import com.example.smartfinance.data.local.UserDao
import com.example.smartfinance.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: android.content.Context
) {
    private val usersCollection = firestore.collection("users")

    val currentUserUid: String?
        get() = auth.currentUser?.uid

    /**
     * Emite el UID del usuario cuando cambia el estado de auth
     */
    val authStateFlow: Flow<String?> = callbackFlow {
        trySend(auth.currentUser?.uid)
        
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            android.util.Log.d("UserRepository", "Auth state changed, new UID: $uid")
            trySend(uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { 
            auth.removeAuthStateListener(listener) 
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return authStateFlow.flatMapLatest { uid ->
            if (uid != null) {
                android.util.Log.d("UserRepository", "Fetching user from Firestore for UID: $uid")
                usersCollection.document(uid).snapshots().map { snapshot ->
                    snapshot.toObject<UserEntity>()
                }.onEach { user ->
                    if (user != null) {
                        userDao.registerUser(user)
                    }
                }
            } else {
                android.util.Log.d("UserRepository", "No UID, emitting null user")
                flowOf(null)
            }
        }
    }

    suspend fun syncUserWithFirebase() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            val snapshot = usersCollection.document(uid).get().await()
            if (!snapshot.exists()) {
                android.util.Log.d("UserRepository", "Syncing user: Creating Firestore profile for $uid")
                val newUser = UserEntity(
                    id = uid,
                    name = firebaseUser.displayName ?: "Usuario",
                    email = firebaseUser.email ?: ""
                )
                usersCollection.document(uid).set(newUser).await()
                userDao.registerUser(newUser)
            } else {
                snapshot.toObject<UserEntity>()?.let { userDao.registerUser(it) }
            }
        }
    }

    suspend fun registerUser(user: UserEntity) {
        usersCollection.document(user.id).set(user).await()
        userDao.registerUser(user)
    }

    suspend fun getUserById(userId: String): UserEntity? {
        val snapshot = usersCollection.document(userId).get().await()
        return if (snapshot.exists()) {
            val user = snapshot.toObject<UserEntity>()
            if (user != null) userDao.registerUser(user)
            user
        } else {
            userDao.getUserById(userId)
        }
    }

    suspend fun updateUser(user: UserEntity) {
        android.util.Log.d("UserRepository", "Updating user in Firestore and Room: $user")
        usersCollection.document(user.id).set(user).await()
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: UserEntity) {
        usersCollection.document(user.id).delete().await()
        userDao.deleteUser(user)
    }
    
    suspend fun getUserByEmail(email: String): UserEntity? {
        val query = usersCollection.whereEqualTo("email", email).limit(1).get().await()
        return if (!query.isEmpty) {
            query.documents.first().toObject<UserEntity>()
        } else {
            userDao.getUserByEmail(email)
        }
    }

    private val imageStorageManager = com.example.smartfinance.utils.ImageStorageManager

    fun uploadProfilePicture(uri: android.net.Uri): String? {
        android.util.Log.d("UserRepository", "Saving profile picture locally for URI: $uri")
        return imageStorageManager.saveImageToInternalStorage(context, uri)
    }
}
