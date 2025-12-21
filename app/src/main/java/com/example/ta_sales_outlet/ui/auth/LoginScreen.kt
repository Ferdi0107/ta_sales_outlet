package com.example.ta_sales_outlet.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.pref.UserPreferences // Import ini wajib
import kotlinx.coroutines.launch // Import coroutine
import org.mindrot.jbcrypt.BCrypt
import java.sql.ResultSet
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // Scope untuk menjalankan fungsi saveSession
    val userPreferences = remember { UserPreferences(context) } // Akses ke penyimpanan HP

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFF6200EE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- LOGO ---
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = primaryColor.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Logo",
                modifier = Modifier.padding(20.dp),
                tint = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Selamat Datang!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Silahkan login ke akun Konveksi X", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // --- INPUT EMAIL ---
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- INPUT PASSWORD ---
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- TOMBOL LOGIN ---
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true

                    Thread {
                        try {
                            val connection = MySQLHelper.connect()
                            if (connection != null) {
                                val query = "SELECT * FROM users WHERE email = '$email'"
                                val statement = connection.createStatement()
                                val resultSet: ResultSet = statement.executeQuery(query)

                                if (resultSet.next()) {
                                    var dbHash = resultSet.getString("password")
                                    val dbName = resultSet.getString("nama")
                                    val dbRole = resultSet.getString("role")
                                    val dbId = resultSet.getInt("idusers") // AMBIL ID

                                    if (dbHash.startsWith("$2y$")) {
                                        dbHash = dbHash.replace("\$2y\$", "\$2a\$")
                                    }

                                    val isPasswordMatch = try {
                                        BCrypt.checkpw(password, dbHash)
                                    } catch (e: Exception) { false }

                                    if (isPasswordMatch) {
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            // --- BAGIAN PENTING YANG DITAMBAHKAN ---
                                            scope.launch {
                                                userPreferences.saveSession(
                                                    token = "manual_login",
                                                    role = dbRole,
                                                    name = dbName,
                                                    userId = dbId // Simpan ID ke HP
                                                )

                                                Toast.makeText(context, "Login Berhasil! Halo $dbName", Toast.LENGTH_SHORT).show()
                                                isLoading = false
                                                onLoginSuccess(dbRole)
                                            }
                                            // ----------------------------------------
                                        }
                                    } else {
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            Toast.makeText(context, "Password Salah", Toast.LENGTH_SHORT).show()
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        Toast.makeText(context, "Email tidak terdaftar", Toast.LENGTH_SHORT).show()
                                        isLoading = false
                                    }
                                }
                                connection.close()
                            } else {
                                (context as? android.app.Activity)?.runOnUiThread {
                                    Toast.makeText(context, "Gagal koneksi Database", Toast.LENGTH_SHORT).show()
                                    isLoading = false
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            (context as? android.app.Activity)?.runOnUiThread {
                                isLoading = false
                            }
                        }
                    }.start()

                } else {
                    Toast.makeText(context, "Mohon isi semua kolom", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("MASUK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Belum punya akun? ", color = Color.Gray)
            Text(
                "Daftar Outlet",
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    // Pastikan rute ini sudah didaftarkan di NavHost MainActivity
                    navController.navigate("register_outlet")
                }
            )
        }
    }
}