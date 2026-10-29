package com.drautomation.intelpark.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drautomation.intelpark.R
import com.drautomation.intelpark.ui.common.errorMessage
import com.drautomation.intelpark.ui.components.AppCard
import com.drautomation.intelpark.ui.components.ChoiceChip
import com.drautomation.intelpark.ui.components.ErrorText
import com.drautomation.intelpark.ui.components.Field
import com.drautomation.intelpark.ui.components.InfoBanner
import com.drautomation.intelpark.ui.components.Logo
import com.drautomation.intelpark.ui.components.PrimaryButton
import com.drautomation.intelpark.ui.components.StepHeader
import com.drautomation.intelpark.ui.theme.IP

@Composable
private fun StepLayout(step: Int, label: String, title: String, text: String, onBack: (() -> Unit)?,
                       vm: AuthViewModel, bottom: @Composable ColumnScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StepHeader(step, label, onBack)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
        }
        content()
        ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
        Spacer(Modifier.height(8.dp))
        bottom()
    }
}

@Composable
fun SignupAccountScreen(vm: AuthViewModel, onBack: () -> Unit, onLogin: () -> Unit, onDone: () -> Unit) {
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    val valid = first.isNotBlank() && last.isNotBlank() && email.contains("@") && password.length >= 8 && accepted
    StepLayout(1, stringResource(R.string.step_account), stringResource(R.string.signup_title), stringResource(R.string.signup_text), onBack, vm,
        bottom = {
            PrimaryButton(stringResource(R.string.continue_), { vm.register(first, last, email, password, onDone) }, enabled = valid, loading = vm.loading)
            TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.already_account), color = IP.Blue, style = MaterialTheme.typography.labelMedium)
            }
        }) {
        Logo(size = 36)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(first, { first = it }, stringResource(R.string.first_name), Modifier.weight(1f), placeholder = "Camille")
            Field(last, { last = it }, stringResource(R.string.last_name), Modifier.weight(1f), placeholder = "Martin")
        }
        Field(email, { email = it }, stringResource(R.string.email), placeholder = "camille@example.com", keyboardType = KeyboardType.Email)
        Field(password, { password = it }, stringResource(R.string.password), placeholder = stringResource(R.string.password_hint),
            password = true, keyboardType = KeyboardType.Password)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(accepted, { accepted = it }, colors = CheckboxDefaults.colors(checkedColor = IP.Ink))
            Text(stringResource(R.string.accept_terms), style = MaterialTheme.typography.bodyMedium, color = IP.Body)
        }
    }
}

private val COUNTRIES = listOf("CH" to "CH", "FR" to "F", "DE" to "D", "IT" to "I")

@Composable
fun SignupPlateScreen(vm: AuthViewModel, onBack: (() -> Unit)?, onDone: () -> Unit) {
    StepLayout(2, stringResource(R.string.step_vehicle), stringResource(R.string.plate_title), stringResource(R.string.plate_text), onBack, vm,
        bottom = {
            PrimaryButton(stringResource(R.string.continue_), { vm.addPlate(onDone) }, enabled = vm.plate.trim().length >= 2, loading = vm.loading)
            Text(stringResource(R.string.plate_more), style = MaterialTheme.typography.bodyMedium, color = IP.Muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }) {
        PlatePreview(vm.plate, vm.plateCountry)
        Text(stringResource(R.string.plate_country), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            COUNTRIES.forEach { (iso, label) ->
                ChoiceChip(label, vm.plateCountry == iso, { vm.plateCountry = iso }, Modifier.weight(1f))
            }
        }
        Field(vm.plate, { vm.plate = it.uppercase() }, stringResource(R.string.plate_number), placeholder = "GE 123456", mono = true)
    }
}

@Composable
private fun PlatePreview(plate: String, country: String) {
    val shown = plate.ifBlank { "— — —" }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(76.dp).clip(RoundedCornerShape(10.dp))
            .background(Color.White).border(3.dp, IP.Ink, RoundedCornerShape(10.dp))
            .semantics { contentDescription = shown },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (country != "CH") {
            Box(Modifier.width(34.dp).fillMaxHeight().background(IP.Blue), contentAlignment = Alignment.BottomCenter) {
                Text(COUNTRIES.first { it.first == country }.second, color = Color.White, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        Text(shown, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, fontSize = 28.sp, maxLines = 1)
        if (country == "CH") {
            Box(Modifier.padding(end = 14.dp).size(30.dp).background(Color(0xFFD52B1E)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 6.dp, height = 18.dp).background(Color.White))
                Box(Modifier.size(width = 18.dp, height = 6.dp).background(Color.White))
            }
        }
    }
}

@Composable
fun SignupAddressScreen(vm: AuthViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    var line by rememberSaveable { mutableStateOf(vm.driver?.addressLine.orEmpty()) }
    var postcode by rememberSaveable { mutableStateOf(vm.driver?.postcode.orEmpty()) }
    var city by rememberSaveable { mutableStateOf(vm.driver?.city.orEmpty()) }
    var country by rememberSaveable { mutableStateOf(vm.plateCountry) }
    StepLayout(3, stringResource(R.string.step_address), stringResource(R.string.address_title), stringResource(R.string.address_text), onBack, vm,
        bottom = {
            PrimaryButton(stringResource(R.string.continue_), { vm.saveAddress(line, postcode, city, country, onDone) },
                enabled = line.isNotBlank() && postcode.isNotBlank() && city.isNotBlank(), loading = vm.loading)
        }) {
        Field(line, { line = it }, stringResource(R.string.street), placeholder = stringResource(R.string.street_hint))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(postcode, { postcode = it }, stringResource(R.string.postcode), Modifier.width(120.dp), placeholder = "1200",
                keyboardType = KeyboardType.Number, mono = true)
            Field(city, { city = it }, stringResource(R.string.city), Modifier.weight(1f), placeholder = stringResource(R.string.city))
        }
        Text(stringResource(R.string.country), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CH" to R.string.country_ch, "FR" to R.string.country_fr, "DE" to R.string.country_de, "IT" to R.string.country_it)
                .forEach { (iso, label) -> ChoiceChip(stringResource(label), country == iso, { country = iso }, Modifier.weight(1f)) }
        }
    }
}

@Composable
fun SignupPhoneScreen(vm: AuthViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    var dial by rememberSaveable { mutableStateOf(if (vm.plateCountry == "FR") "+33" else if (vm.plateCountry == "DE") "+49" else if (vm.plateCountry == "IT") "+39" else "+41") }
    var number by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var alerts by rememberSaveable { mutableStateOf(true) }
    StepLayout(4, stringResource(R.string.step_phone), stringResource(R.string.phone_title), stringResource(R.string.phone_text), onBack, vm,
        bottom = {
            PrimaryButton(stringResource(R.string.create_my_account), { vm.verify(code, onDone) }, yellow = true,
                enabled = vm.codeSentTo != null && code.length == 6, loading = vm.loading)
        }) {
        Text(stringResource(R.string.mobile_number), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("+41", "+33", "+49", "+39").forEach { ChoiceChip(it, dial == it, { dial = it }, Modifier.weight(1f)) }
        }
        Field(number, { number = it }, stringResource(R.string.number), placeholder = "79 123 45 67", keyboardType = KeyboardType.Phone, mono = true)
        if (vm.codeSentTo == null) {
            PrimaryButton(stringResource(R.string.send_code), { vm.sendCode(dial, number) }, enabled = number.count { it.isDigit() } >= 6,
                loading = vm.loading)
        } else {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.code_received), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.sendCode(dial, number) }) { Text(stringResource(R.string.resend), color = IP.Blue) }
                }
                Field(code, { code = it.filter(Char::isDigit).take(6) }, stringResource(R.string.code_6_digits), placeholder = "481 516",
                    keyboardType = KeyboardType.NumberPassword, mono = true)
            }
            vm.debugCode?.let { InfoBanner(stringResource(R.string.debug_code, it)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(alerts, { alerts = it }, colors = CheckboxDefaults.colors(checkedColor = IP.Ink))
            Text(stringResource(R.string.sms_alerts), style = MaterialTheme.typography.bodyMedium, color = IP.Body)
        }
    }
}
