package com.example.smartfinance.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfinance.data.local.RecurrenceType
import com.example.smartfinance.data.local.TransactionEntity
import com.example.smartfinance.data.local.TransactionType
import com.example.smartfinance.data.local.UserEntity
import com.example.smartfinance.data.repository.CategoryRepository
import com.example.smartfinance.data.repository.TransactionRepository
import com.example.smartfinance.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    var state by mutableStateOf<AuthState>(AuthState.Idle)
        private set

    init {
        checkUserLoggedIn()
    }

    private fun checkUserLoggedIn() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            state = AuthState.Success
        }
    }

    fun register(
        name: String, 
        email: String, 
        pin: String,
        initialBalance: Double = 0.0,
        monthlyBudget: Double = 500.0,
        hasSalary: Boolean = false,
        salaryAmount: Double? = null,
        salaryPayDay: Int? = null
    ) {
        state = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pin).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    firebaseUser.sendEmailVerification().await()

                    val newUser = UserEntity(
                        id = firebaseUser.uid,
                        name = name,
                        email = email,
                        monthlyBudget = monthlyBudget,
                        initialBalance = initialBalance,
                        hasSalary = hasSalary,
                        salaryAmount = salaryAmount,
                        salaryPayDay = salaryPayDay
                    )
                    userRepository.registerUser(newUser)

                    if (hasSalary && salaryAmount != null && salaryAmount > 0) {
                        val categories = categoryRepository.initializeCategoriesIfNeeded()
                        
                        val salaryCategory = categories.find { it.name.equals("Sueldo", ignoreCase = true) }
                        
                        val calendar = Calendar.getInstance()
                        val requestedDay = salaryPayDay ?: 1
                        
                        val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val finalDay = if (requestedDay > maxDayInMonth) maxDayInMonth else requestedDay
                        
                        calendar.set(Calendar.DAY_OF_MONTH, finalDay)
                        calendar.set(Calendar.HOUR_OF_DAY, 8)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        android.util.Log.d("AuthViewModel", "Salary creation: Requested $requestedDay, Final day $finalDay, Date: ${calendar.time}")

                        val salaryTx = TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            userId = firebaseUser.uid,
                            categoryId = salaryCategory?.id ?: "",
                            title = "Salario Mensual",
                            amount = salaryAmount,
                            dateMillis = calendar.timeInMillis,
                            type = TransactionType.INCOME,
                            description = "Ingreso mensual automático",
                            recurrence = RecurrenceType.MONTHLY
                        )
                        transactionRepository.insertTransaction(salaryTx)
                    }

                    state = AuthState.VerificationSent
                }
            } catch (e: Exception) {
                state = AuthState.Error("Error al registrar: ${e.localizedMessage}")
            }
        }
    }

    fun login(email: String, pin: String) {
        state = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pin).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    if (firebaseUser.isEmailVerified) {
                        val localUser = userRepository.getUserById(firebaseUser.uid)
                        if (localUser == null) {
                            val newUser = UserEntity(
                                id = firebaseUser.uid,
                                name = firebaseUser.displayName ?: "Usuario",
                                email = firebaseUser.email ?: email
                            )
                            userRepository.registerUser(newUser)
                        }
                        state = AuthState.Success
                    } else {
                        state = AuthState.Error("Por favor, verifica tu correo antes de entrar.")
                        auth.signOut()
                    }
                }
            } catch (e: Exception) {
                state = AuthState.Error("Error al iniciar sesión: ${e.localizedMessage}")
            }
        }
    }

    fun checkEmailVerificationStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    currentUser.reload().await()
                    if (currentUser.isEmailVerified) {
                        val localUser = userRepository.getUserById(currentUser.uid)
                        if (localUser != null) {
                            state = AuthState.Success
                        }
                    }
                } catch (_: Exception) {

                }
            }
        }
    }

    fun logout() {
        auth.signOut()
        state = AuthState.Idle
    }

    fun resetState() {
        state = AuthState.Idle
    }
}
