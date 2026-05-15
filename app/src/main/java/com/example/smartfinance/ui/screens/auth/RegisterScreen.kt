package com.example.smartfinance.ui.screens.auth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val state = viewModel.state
    
    var currentStep by remember { mutableIntStateOf(0) }
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var initialBalance by remember { mutableStateOf("") }
    var hasSalary by remember { mutableStateOf(false) }
    var salaryAmount by remember { mutableStateOf("") }
    var salaryDay by remember { mutableStateOf("1") }
    var monthlyLimit by remember { mutableStateOf("500") }
    
    // Pedir permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.register(
            name = name,
            email = email,
            pin = password,
            initialBalance = initialBalance.toDoubleOrNull() ?: 0.0,
            monthlyBudget = monthlyLimit.toDoubleOrNull() ?: 500.0,
            hasSalary = hasSalary,
            salaryAmount = salaryAmount.toDoubleOrNull(),
            salaryPayDay = salaryDay.toIntOrNull()
        )
    }

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            viewModel.resetState()
            onRegisterSuccess()
        } else if (state is AuthState.VerificationSent) {
            currentStep = 6
        }
    }

    if (currentStep == 6) {
        LaunchedEffect(Unit) {
            while (state !is AuthState.Success) {
                viewModel.checkEmailVerificationStatus()
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            //Hace que vayan apareciendo los requisitos del registro para un registro guiado al usuario
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                }, label = "OnboardingStep"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(
                        onContinue = { currentStep = 1 },
                        onLogin = onNavigateToLogin
                    )
                    1 -> BasicInfoStep(
                        name = name, onNameChange = { name = it },
                        email = email, onEmailChange = { email = it },
                        password = password, onPasswordChange = { password = it },
                        passwordVisible = passwordVisible, onTogglePassword = { passwordVisible = !passwordVisible },
                        onBack = { currentStep = 0 },
                        onContinue = { 
                            val isPasswordValid = password.length >= 8 &&
                                    password.any { it.isUpperCase() } &&
                                    password.any { it.isLowerCase() } &&
                                    password.any { it.isDigit() }
                            if (name.isNotBlank() && email.contains("@") && isPasswordValid) {
                                currentStep = 2 
                            }
                        }
                    )
                    2 -> MoneyStep(
                        balance = initialBalance,
                        onBalanceChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) initialBalance = it },
                        onBack = { currentStep = 1 },
                        onContinue = { currentStep = 3 }
                    )
                    3 -> SalaryStep(
                        hasSalary = hasSalary, onHasSalaryChange = { hasSalary = it },
                        amount = salaryAmount, onAmountChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) salaryAmount = it },
                        day = salaryDay, onDayChange = { if (it.isEmpty() || (it.toIntOrNull() in 1..31)) salaryDay = it },
                        onBack = { currentStep = 2 },
                        onContinue = { currentStep = 4 }
                    )
                    4 -> LimitStep(
                        limit = monthlyLimit,
                        onLimitChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) monthlyLimit = it },
                        onBack = { currentStep = 3 },
                        onContinue = { currentStep = 5 }
                    )
                    5 -> PermissionsStep(
                        onBack = { currentStep = 4 },
                        onContinue = { currentStep = 7 }, // Move to Terms
                        isLoading = state is AuthState.Loading
                    )
                    6 -> VerificationStep(
                        email = email,
                        onCheckVerification = { viewModel.checkEmailVerificationStatus() }
                    )
                    7 -> TermsStep(
                        onBack = { currentStep = 5 },
                        onAccept = {
                            val permissions = mutableListOf(Manifest.permission.READ_CONTACTS)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(permissions.toTypedArray())
                        },
                        isLoading = state is AuthState.Loading
                    )
                }
            }
            
            if (state is AuthState.Error && (currentStep == 7 || currentStep == 6)) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
fun TermsStep(onBack: () -> Unit, onAccept: () -> Unit, isLoading: Boolean) {
    var accepted by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Términos y Condiciones", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = """
                        Bienvenido a SmartFinance. Al registrarte, aceptas los siguientes términos:
                        
                        1. Uso de la cuenta: Eres responsable de mantener la confidencialidad de tu cuenta.
                        2. Datos personales: SmartFinance respeta tu privacidad y protege tus datos financieros localmente y encriptados en la nube.
                        3. Propósito: Esta aplicación es una herramienta de gestión financiera personal y no ofrece asesoramiento financiero legal.
                        4. Responsabilidad: El usuario es el único responsable de la veracidad de los datos introducidos.
                        
                        Al continuar, confirmas que has leído y aceptas que SmartFinance gestione tus transacciones según nuestra política de privacidad.
                    """.trimIndent(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { accepted = !accepted }
                .padding(vertical = 8.dp)
        ) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("Acepto los términos y condiciones de uso", fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Atrás") }
            Button(
                onClick = onAccept,
                enabled = accepted && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Registrarse")
                }
            }
        }
    }
}

@Composable
fun VerificationStep(email: String, onCheckVerification: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.MarkEmailRead, 
            null, 
            modifier = Modifier.size(80.dp), 
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Confirma tu correo", 
            fontSize = 24.sp, 
            fontWeight = FontWeight.Bold, 
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Te hemos enviado un correo de verificación a $email. Por favor, confírmalo para poder entrar.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onCheckVerification,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Ya lo he verificado", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Entrarás automáticamente en cuanto confirmes el correo.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WelcomeStep(onContinue: () -> Unit, onLogin: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Savings, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        Text("¡Bienvenido!", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Vamos a hacerte unas preguntas para tu cuenta.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("¿Ya tienes una cuenta?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onLogin) {
            Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BasicInfoStep(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean, onTogglePassword: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val isNameValid = name.isNotBlank()
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length >= 8 &&
            password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() } &&
            password.any { it.isDigit() }

    Column {
        Text("Datos Básicos", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Nombre Completo") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Person, null) },
            isError = name.isNotEmpty() && !isNameValid,
            supportingText = {
                if (name.isNotEmpty() && !isNameValid) {
                    Text("El nombre no puede estar vacío")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = email.isNotEmpty() && !isEmailValid,
            supportingText = {
                if (email.isNotEmpty() && !isEmailValid) {
                    Text("Introduce un correo válido (ej: usuario@email.com)")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("Contraseña") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            isError = password.isNotEmpty() && !isPasswordValid,
            supportingText = {
                if (password.isNotEmpty() && !isPasswordValid) {
                    Text("Min. 8 caracteres, una mayúscula, una minúscula y un número")
                }
            },
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Atrás") }
            Button(
                onClick = onContinue,
                enabled = isNameValid && isEmailValid && isPasswordValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continuar")
            }
        }
    }
}

@Composable
fun MoneyStep(balance: String, onBalanceChange: (String) -> Unit, onBack: () -> Unit, onContinue: () -> Unit) {
    val isBalanceValid = balance.isNotEmpty() && balance.toDoubleOrNull() != null
    
    Column {
        Text("Fondos Iniciales", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("¿Con cuánto dinero quieres empezar?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = balance, onValueChange = onBalanceChange,
            label = { Text("Saldo inicial") },
            suffix = { Text("€") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            isError = balance.isNotEmpty() && !isBalanceValid,
            supportingText = {
                if (balance.isNotEmpty() && !isBalanceValid) {
                    Text("Introduce un número válido")
                }
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Atrás") }
            Button(
                onClick = onContinue, 
                enabled = isBalanceValid,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Continuar") }
        }
    }
}

@Composable
fun SalaryStep(
    hasSalary: Boolean, onHasSalaryChange: (Boolean) -> Unit,
    amount: String, onAmountChange: (String) -> Unit,
    day: String, onDayChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val isAmountValid = !hasSalary || (amount.isNotEmpty() && amount.toDoubleOrNull() != null)
    val isDayValid = !hasSalary || (day.isNotEmpty() && day.toIntOrNull() != null && day.toInt() in 1..31)

    Column {
        Text("Salario Mensual", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("¿Quieres añadir un salario mensual?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onHasSalaryChange(!hasSalary) }) {
            Switch(checked = hasSalary, onCheckedChange = onHasSalaryChange)
            Spacer(modifier = Modifier.width(16.dp))
            Text(if (hasSalary) "Sí, añadir salario" else "No")
        }

        if (hasSalary) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amount, onValueChange = onAmountChange,
                label = { Text("Cuánto") },
                suffix = { Text("€") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                isError = amount.isNotEmpty() && !isAmountValid,
                supportingText = {
                    if (amount.isNotEmpty() && !isAmountValid) {
                        Text("Introduce un monto válido")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = day, onValueChange = onDayChange,
                label = { Text("Qué día de cada mes (1-31)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                isError = day.isNotEmpty() && !isDayValid,
                supportingText = {
                    if (day.isNotEmpty() && !isDayValid) {
                        Text("Día no válido (debe ser entre 1 y 31)")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Atrás") }
            Button(
                onClick = onContinue, 
                enabled = isAmountValid && isDayValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continuar")
            }
        }
    }
}

@Composable
fun LimitStep(limit: String, onLimitChange: (String) -> Unit, onBack: () -> Unit, onContinue: () -> Unit) {
    val isLimitValid = limit.isNotEmpty() && limit.toDoubleOrNull() != null
    
    Column {
        Text("Presupuesto", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("¿Cuánto quieres que sea el límite de gasto mensual?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = limit, onValueChange = onLimitChange,
            label = { Text("Límite mensual") },
            suffix = { Text("€") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            isError = limit.isNotEmpty() && !isLimitValid,
            supportingText = {
                if (limit.isNotEmpty() && !isLimitValid) {
                    Text("Introduce un límite válido")
                }
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Atrás") }
            Button(
                onClick = onContinue, 
                enabled = isLimitValid, 
                shape = RoundedCornerShape(12.dp)
            ) { Text("Continuar") }
        }
    }
}

@Composable
fun PermissionsStep(onBack: () -> Unit, onContinue: () -> Unit, isLoading: Boolean) {
    Column {
        Text("Permisos Finales", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("¿Quieres dar permisos a los contactos para añadirlos a la lista de amigos y permisos de notificaciones?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Sí, conceder y registrarse", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Atrás")
        }
    }
}
