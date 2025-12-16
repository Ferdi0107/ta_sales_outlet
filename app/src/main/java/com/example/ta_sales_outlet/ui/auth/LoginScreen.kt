package com.example.ta_sales_outlet.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ta_sales_outlet.data.api.ApiClient
import com.example.ta_sales_outlet.data.model.BaseResponse
import com.example.ta_sales_outlet.data.model.User
import com.example.ta_sales_outlet.data.pref.UserPreferences
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit // Callback untuk navigasi setelah login sukses
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }

    // State untuk input data
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Judul
        Text(
            text = "Login Konveksi X",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Input Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Login / Loading
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true

                        // Persiapkan Data Login
                        val request = mapOf("email" to email, "password" to password)

                        // Panggil API Laravel
                        ApiClient.instance.login(request).enqueue(object : Callback<BaseResponse<User>> {
                            override fun onResponse(
                                call: Call<BaseResponse<User>>,
                                response: Response<BaseResponse<User>>
                            ) {
                                isLoading = false
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val userData = response.body()?.data
                                    userData?.let { user ->
                                        // SIMPAN SESI KE HP
                                        scope.launch {
                                            userPreferences.saveSession(
                                                token = user.token ?: "",
                                                role = user.role ?: "",
                                                name = user.name ?: "",
                                                userId = user.id
                                            )
                                            // Panggil navigasi sukses
                                            Toast.makeText(context, "Login Berhasil sebagai ${user.role}", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(user.role ?: "")
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Login Gagal: Cek email/password", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<BaseResponse<User>>, t: Throwable) {
                                isLoading = false
                                Toast.makeText(context, "Error Koneksi: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(context, "Mohon isi semua kolom", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Masuk", fontSize = 18.sp)
            }
        }
    }
}