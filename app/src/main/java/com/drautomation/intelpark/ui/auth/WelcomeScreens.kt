package com.drautomation.intelpark.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drautomation.intelpark.R
import com.drautomation.intelpark.ui.common.errorMessage
import com.drautomation.intelpark.ui.components.BackButton
import com.drautomation.intelpark.ui.components.ErrorText
import com.drautomation.intelpark.ui.components.Field
import com.drautomation.intelpark.ui.components.InfoBanner
import com.drautomation.intelpark.ui.components.Logo
import com.drautomation.intelpark.ui.components.PrimaryButton
import com.drautomation.intelpark.ui.components.Segmented
import com.drautomation.intelpark.ui.components.SecondaryButton
import com.drautomation.intelpark.ui.components.VSpace
import com.drautomation.intelpark.ui.theme.IP

@Composable
fun WelcomeScreen(vm: AuthViewModel, onCreate: () -> Unit, onLogin: (staff: Boolean) -> Unit) {
    var showServer by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(IP.Ink).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp, top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo(size = 56, dark = true, subtitle = true)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showServer = true }) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.server_title), tint = IP.OnDarkMuted)
            }
        }
        Column(Modifier.padding(horizontal = 28.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.welcome_title), color = Color.White, fontSize = 36.sp, lineHeight = 40.sp,
                style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.welcome_text), color = IP.OnDarkMuted, style = MaterialTheme.typography.bodyLarge)
        }
        // bays, two of them free (yellow): the image of the mock-up
        Column(Modifier.padding(horizontal = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(listOf(0, 1, 0, 0, 2, 0), listOf(0, 0, 2, 0, 1, 0)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { kind ->
                        Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(8.dp)).then(
                            when (kind) {
                                1 -> Modifier.background(IP.Yellow)
                                2 -> Modifier.border(2.dp, Color(0xFF3B4450), RoundedCornerShape(8.dp))
                                else -> Modifier.background(Color(0xFF2A3038))
                            },
                        ))
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        Column(
            Modifier.padding(12.dp).clip(RoundedCornerShape(28.dp)).background(Color.White).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(stringResource(R.string.create_account), onCreate, yellow = true)
            SecondaryButton(stringResource(R.string.login_my_account), { onLogin(false) }, Modifier.fillMaxWidth())
            TextButton(onClick = { onLogin(true) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = IP.Blue, modifier = Modifier.size(18.dp))
                Text("  " + stringResource(R.string.staff_access), color = IP.Blue, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    if (showServer) ServerDialog(vm.serverUrl, onSave = { vm.saveServer(it); showServer = false }, onDismiss = { showServer = false })
}

@Composable
fun ServerDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var url by rememberSaveable { mutableStateOf(current) }
    val valid = url.startsWith("http://") || url.startsWith("https://")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.server_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.server_text), style = MaterialTheme.typography.bodyMedium)
                Field(url, { url = it.trim() }, stringResource(R.string.server_address), placeholder = "http://192.168.1.50",
                    keyboardType = KeyboardType.Uri)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(url) }, enabled = valid) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private val TurboGreen = Color(0xFF1FA35A)

@Composable
fun LoginScreen(vm: AuthViewModel, staffFirst: Boolean, onBack: () -> Unit, onSignup: () -> Unit,
                onDone: (staff: Boolean, nextStep: String?) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(if (staffFirst) 1 else 0) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val staff = tab == 1
    LaunchedEffect(Unit) { vm.checkTurbo() }
    Column(
        Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            Logo(size = 36)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(44.dp))
        }
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(if (staff) R.string.login_text_staff else R.string.login_text_driver),
            style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
        Segmented(listOf(stringResource(R.string.role_driver), stringResource(R.string.role_staff)), tab, { tab = it; vm.clearError() })
        Field(email, { email = it }, stringResource(if (staff) R.string.agent_id else R.string.email),
            placeholder = if (staff) "agent@example.com" else "camille@example.com", keyboardType = KeyboardType.Email)
        Field(password, { password = it }, stringResource(R.string.password), password = true, keyboardType = KeyboardType.Password)
        if (staff) InfoBanner(stringResource(R.string.staff_notice))
        ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
        VSpace(8)
        PrimaryButton(
            stringResource(if (staff) R.string.open_admin else R.string.login),
            { vm.login(staff, email, password, onDone) },
            enabled = email.isNotBlank() && password.isNotBlank(), loading = vm.loading,
        )
        if (vm.turboEnabled) {
            // TEST ONLY, like the website: shown while the server runs with TURBO_LOGIN=true
            Button(
                onClick = { vm.loginTurbo(staff, onDone) },
                enabled = !vm.loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TurboGreen, contentColor = Color.White),
            ) {
                Text(stringResource(R.string.turbo_login), style = MaterialTheme.typography.labelLarge)
            }
            Text(stringResource(R.string.turbo_hint), style = MaterialTheme.typography.bodySmall, color = IP.Muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        if (!staff) {
            TextButton(onClick = onSignup, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.no_account_create), color = IP.Blue, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
