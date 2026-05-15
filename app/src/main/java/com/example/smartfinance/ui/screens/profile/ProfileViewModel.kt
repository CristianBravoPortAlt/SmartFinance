package com.example.smartfinance.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.data.repository.CategoryRepository
import com.example.smartfinance.data.repository.GoalRepository
import com.example.smartfinance.data.repository.NotificationRepository
import com.example.smartfinance.data.repository.TransactionRepository
import com.example.smartfinance.data.repository.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val categoryRepository: CategoryRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val currentUser: StateFlow<UserEntity?> = userRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateUser(
        name: String, 
        budget: Double, 
        phone: String, 
        occupation: String,
        theme: String = currentUser.value?.themePreference ?: "system",
        hasSalary: Boolean = currentUser.value?.hasSalary ?: false,
        salaryAmount: Double? = currentUser.value?.salaryAmount,
        salaryPayDay: Int? = currentUser.value?.salaryPayDay
    ) {
        viewModelScope.launch {
            currentUser.value?.let {
                val updatedUser = it.copy(
                    name = name, 
                    monthlyBudget = budget,
                    phone = phone,
                    occupation = occupation,
                    themePreference = theme,
                    hasSalary = hasSalary,
                    salaryAmount = salaryAmount,
                    salaryPayDay = salaryPayDay
                )
                userRepository.updateUser(updatedUser)
            }
        }
    }

    fun updateProfilePicture(uri: android.net.Uri) {
        viewModelScope.launch {
            _error.value = null
            android.util.Log.d("ProfileViewModel", "Saving profile picture locally for URI: $uri")
            val localPath = userRepository.uploadProfilePicture(uri)
            if (localPath != null) {
                android.util.Log.d("ProfileViewModel", "Save successful, Path: $localPath")
                currentUser.value?.let {
                    val updatedUser = it.copy(profilePictureUri = localPath)
                    userRepository.updateUser(updatedUser)
                }
            } else {
                android.util.Log.e("ProfileViewModel", "Save failed: localPath is null")
                _error.value = "Error al guardar la imagen en el dispositivo."
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun deleteAccount(password: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        
        if (user == null || email == null) {
            onComplete(false, "Usuario no autenticado")
            return
        }

        viewModelScope.launch {
            try {
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()

                try {
                    user.sendEmailVerification().await()
                } catch (e: Exception) {
                    android.util.Log.e("ProfileViewModel", "Failed to send email: ${e.message}")
                }

                val uid = user.uid

                transactionRepository.deleteAllTransactionsByUser(uid)
                goalRepository.deleteAllGoalsByUser(uid)
                categoryRepository.deleteAllCategoriesByUser(uid)
                notificationRepository.deleteAllNotificationsByUser(uid)
                
                currentUser.value?.let {
                    userRepository.deleteUser(it)
                }

                user.delete().await()

                onComplete(true, null)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error deleting account", e)
                onComplete(false, e.localizedMessage ?: "Error desconocido al borrar la cuenta")
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        auth.signOut()
        onComplete()
    }
}
