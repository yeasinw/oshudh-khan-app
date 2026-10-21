package com.yeasinx.oshudhkhan.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yeasinx.oshudhkhan.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    authViewModel: AuthViewModel,
    bottomPadding: Dp,
    onSignIn: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val accountEmail by authViewModel.accountEmail.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(ProfileErrors()) }

    fun startEditing() {
        name = profile.name
        age = profile.age?.toString() ?: ""
        email = profile.email
        errors = ProfileErrors()
        editing = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(24.dp))

            if (editing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    isError = errors.name != null,
                    supportingText = errors.name?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { input -> age = input.filter { it.isDigit() }.take(3) },
                    label = { Text("Age") },
                    isError = errors.age != null,
                    supportingText = errors.age?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    isError = errors.email != null,
                    supportingText = errors.email?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { editing = false }) { Text("Cancel") }
                    Button(onClick = {
                        errors = viewModel.save(name, age, email)
                        if (errors.isEmpty) editing = false
                    }) { Text("Save") }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileRow("Name", profile.name.ifBlank { "Not set" })
                        ProfileRow("Age", profile.age?.toString() ?: "Not set")
                        ProfileRow("Email", profile.email.ifBlank { "Not set" })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { startEditing() }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit profile")
                }
                Spacer(Modifier.height(24.dp))
                AccountCard(
                    accountEmail = accountEmail,
                    onSignIn = onSignIn,
                    onSignOut = { confirmSignOut = true }
                )
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = {
                Text(
                    "Your medicines and profile stay on this device. Signing in to a different " +
                        "account later replaces them with that account's data, so changes made " +
                        "while signed out would be lost."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.signOut()
                    confirmSignOut = false
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AccountCard(accountEmail: String?, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (accountEmail == null) {
                Text(
                    "Sign in to back up your medicines",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onSignIn) { Text("Sign in") }
            } else {
                ProfileRow("Signed in as", accountEmail)
                OutlinedButton(onClick = onSignOut) { Text("Sign out") }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
