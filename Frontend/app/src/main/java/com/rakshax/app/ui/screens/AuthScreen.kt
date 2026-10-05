package com.rakshax.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onAuthSubmit: suspend (
        isRegister: Boolean,
        name: String,
        email: String,
        phone: String,
        password: String,
        age: Int,
        usePhoneLogin: Boolean
    ) -> String? = { _, _, _, _, _, _, _ -> null },
    onGoogleAuth: suspend (firebaseIdToken: String) -> String? = { "Google sign-in is unavailable." }
) {
    var isRegisterTab by remember { mutableStateOf(false) }
    var usePhoneLogin by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(com.google.android.gms.common.api.ApiException::class.java)
                val googleIdToken = account.idToken
                    ?: throw IllegalStateException("Google did not return an ID token.")
                scope.launch {
                    isSubmitting = true
                    errorMessage = try {
                        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        val user = FirebaseAuth.getInstance()
                            .signInWithCredential(credential)
                            .await()
                            .user
                            ?: throw IllegalStateException("Firebase did not return a user.")
                        val firebaseIdToken = user.getIdToken(true).await().token
                            ?: throw IllegalStateException("Firebase did not return an ID token.")
                        onGoogleAuth(firebaseIdToken)
                    } catch (error: Exception) {
                        error.localizedMessage ?: "Google sign-in failed."
                    }
                    isSubmitting = false
                    if (errorMessage == null) onAuthSuccess()
                }
            } catch (error: Exception) {
                errorMessage = error.localizedMessage ?: "Google sign-in failed."
            }
        } else {
            errorMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Header
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(EmergencyRed)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "RakshaX Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Personal Safety & Physical SOS Hub",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Tab Selector (Login / Register)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .padding(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isRegisterTab) SurfaceElevated else Color.Transparent)
                    .clickable {
                        isRegisterTab = false
                        errorMessage = null
                    }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "Sign In",
                    color = if (!isRegisterTab) TextPrimary else TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isRegisterTab) SurfaceElevated else Color.Transparent)
                    .clickable {
                        isRegisterTab = true
                        errorMessage = null
                    }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "Register",
                    color = if (isRegisterTab) TextPrimary else TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isRegisterTab) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(4.dp)
            ) {
                listOf("Email", "Phone").forEachIndexed { index, label ->
                    val selected = usePhoneLogin == (index == 1)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) SurfaceElevated else Color.Transparent)
                            .clickable {
                                usePhoneLogin = index == 1
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) TextPrimary else TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Form Fields
        if (isRegisterTab) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (with +91)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = age,
                onValueChange = { age = it.filter(Char::isDigit).take(3) },
                label = { Text("Age") },
                leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null, tint = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (isRegisterTab || !usePhoneLogin) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!isRegisterTab && usePhoneLogin) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = TextSecondary
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = SurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(26.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = EmergencyRed,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = {
                val enteredAge = age.toIntOrNull()
                if (isRegisterTab && (enteredAge == null || enteredAge !in 1..120)) {
                    errorMessage = "Enter a valid age between 1 and 120."
                    return@Button
                }
                if (!isRegisterTab && usePhoneLogin && phone.isBlank()) {
                    errorMessage = "Enter your phone number to continue."
                    return@Button
                }
                scope.launch {
                    isSubmitting = true
                    errorMessage = onAuthSubmit(
                        isRegisterTab,
                        name,
                        email,
                        phone,
                        password,
                        enteredAge ?: 0,
                        usePhoneLogin
                    )
                    isSubmitting = false
                    if (errorMessage == null) onAuthSuccess()
                }
            },
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmergencyRed,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = when {
                    isRegisterTab -> "CREATE ACCOUNT"
                    usePhoneLogin -> "SIGN IN WITH PHONE"
                    else -> "SECURE SIGN IN"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                val webClientIdResource = context.resources.getIdentifier(
                    "default_web_client_id",
                    "string",
                    context.packageName
                )
                if (webClientIdResource == 0) {
                    errorMessage = "Firebase project rakshax-ec3c5 has no Google OAuth client. Enable Google sign-in, add this app's SHA-1/SHA-256, then replace google-services.json with the updated download."
                } else {
                    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(webClientIdResource))
                        .requestEmail()
                        .build()
                    googleSignInLauncher.launch(GoogleSignIn.getClient(context, options).signInIntent)
                }
            },
            enabled = !isSubmitting,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("CONTINUE WITH GOOGLE", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onAuthSuccess,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Text("ENTER HACKATHON DEMO MODE", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
