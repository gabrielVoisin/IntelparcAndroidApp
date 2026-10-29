package com.drautomation.intelpark.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drautomation.intelpark.R
import com.drautomation.intelpark.ui.theme.IP

@Composable
fun Logo(size: Int = 36, dark: Boolean = false, subtitle: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.28f).dp)).background(IP.Yellow),
            contentAlignment = Alignment.Center,
        ) {
            Text("P", color = IP.Ink, fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.58f).sp)
        }
        Column {
            Row {
                Text("Intel", color = if (dark) Color.White else IP.Ink, fontWeight = FontWeight.Medium, fontSize = (size * 0.5f).sp)
                Text("Park", color = if (dark) IP.Yellow else IP.Ink, fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.5f).sp)
            }
            if (subtitle) {
                Text(stringResource(R.string.by_dr_automation).uppercase(), color = if (dark) IP.OnDarkMuted else IP.Muted,
                    style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, yellow: Boolean = false,
                  enabled: Boolean = true, loading: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (yellow) IP.Yellow else IP.Ink,
            contentColor = if (yellow) IP.Ink else Color.White,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = if (yellow) IP.Ink else Color.White)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, IP.Ink),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = IP.Ink, containerColor = Color.White),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).border(1.dp, IP.Field, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = IP.Ink)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onBack != null) BackButton(onBack)
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
        }
    }
}

@Composable
fun StepHeader(step: Int, label: String, onBack: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onBack != null) BackButton(onBack)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.step_of, step, 4, label).uppercase(), style = MaterialTheme.typography.labelSmall, color = IP.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i < step) IP.Ink else Color(0xFFDEDBD2)))
                }
            }
        }
    }
}

@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    mono: Boolean = false,
    singleLine: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it, color = Color(0xFF8A8F96)) } },
            singleLine = singleLine,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IP.Ink, unfocusedBorderColor = IP.Field,
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
            ),
        )
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, color: Color = Color.White, border: Boolean = true, onClick: (() -> Unit)? = null,
            content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = color,
        border = if (border) BorderStroke(1.dp, IP.Line) else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun Pill(text: String, background: Color, foreground: Color) {
    Text(
        text.uppercase(),
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(background).padding(horizontal = 10.dp, vertical = 4.dp),
        color = foreground, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp,
    )
}

/** Segmented choice (two or more options), as on the mock-up. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(IP.Segment).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (on) IP.Ink else Color.Transparent)
                    .semantics { role = Role.Tab; this.selected = on }
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) Color.White else IP.Body, style = MaterialTheme.typography.labelMedium, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, sub: String? = null) {
    Surface(
        modifier = modifier.heightIn(min = 48.dp).semantics { this.selected = selected }.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) IP.Ink else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.5.dp, if (selected) IP.Ink else IP.Field),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(text, color = if (selected) Color.White else IP.Ink, style = MaterialTheme.typography.labelMedium, fontSize = 14.sp)
            if (sub != null) Text(sub, color = if (selected) IP.Yellow else IP.Muted, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
        }
    }
}

@Composable
fun ErrorText(message: String?) {
    if (message != null) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(IP.AmberSoft).padding(12.dp),
            color = IP.Amber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun InfoBanner(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(IP.BlueSoft).padding(12.dp),
        color = IP.BlueDark, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun RowScope.Stat(value: String, label: String, background: Color, foreground: Color) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(background).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, color = foreground, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(label, color = foreground, style = MaterialTheme.typography.labelMedium, fontSize = 12.sp)
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = IP.Ink)
    }
}

@Composable
fun VSpace(height: Int) = Spacer(Modifier.height(height.dp))

@Composable
fun HSpace(width: Int) = Spacer(Modifier.width(width.dp))
