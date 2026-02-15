package ch.jacem.starclone.ui.screens.remoteconfig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.R
import ch.jacem.starclone.rclone.Provider
import ch.jacem.starclone.rclone.ProviderOption
import java.util.Locale

@Composable
fun RemoteConfigFormScreen(
    provider: Provider,
    isEdit: Boolean,
    existingValues: Map<String, String>,
    useOauth: Boolean,
    searchQuery: String,
    showAdvanced: Boolean,
    onSave: (remoteName: String, options: Map<String, String>) -> Unit,
) {
    val optionValues = remember { mutableStateMapOf<String, String>() }
    var remoteName by remember {
        mutableStateOf(existingValues[RCLONE_CONFIG_NAME_KEY] ?: "")
    }

    // Initialize from existing values
    remember(existingValues) {
        existingValues.forEach { (key, value) ->
            if (key != RCLONE_CONFIG_NAME_KEY) {
                optionValues[key] = value
            }
        }
        true
    }

    val filteredOptions = remember(provider.options, searchQuery, showAdvanced) {
        provider.options.filter { option ->
            if (option.advanced && !showAdvanced) return@filter false
            if (searchQuery.isBlank()) return@filter true
            option.name.contains(searchQuery, ignoreCase = true) ||
                option.help.contains(searchQuery, ignoreCase = true)
        }
    }

    val showNameField = remember(searchQuery) {
        searchQuery.isBlank() ||
            "remote name".contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onSave(remoteName, optionValues.toMap()) },
            ) {
                Icon(
                    if (useOauth && !isEdit) {
                        Icons.AutoMirrored.Outlined.ArrowForward
                    } else {
                        Icons.Outlined.Save
                    },
                    contentDescription = stringResource(
                        if (useOauth && !isEdit) R.string.next else R.string.save
                    ),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (showNameField) {
                item(key = "__remote_name") {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.remote_properties_remote_name),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = remoteName,
                                onValueChange = { remoteName = it },
                                label = { Text(stringResource(R.string.remote_name_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isEdit,
                            )
                        }
                    }
                }
            }

            items(
                items = filteredOptions,
                key = { it.name },
                contentType = { "option" },
            ) { option ->
                OptionCard(
                    option = option,
                    value = optionValues[option.name]
                        ?: option.default,
                    onValueChanged = { optionValues[option.name] = it },
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun OptionCard(
    option: ProviderOption,
    value: String,
    onValueChanged: (String) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = option.getNameCapitalized(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            if (option.help.isNotBlank()) {
                Text(
                    text = option.help,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))

            when {
                option.type == "bool" -> BoolField(
                    option = option,
                    value = value,
                    onValueChanged = onValueChanged,
                )
                option.type == "SizeSuffix" -> SizeSuffixField(
                    value = value,
                    onValueChanged = onValueChanged,
                )
                option.isPassword -> StringField(
                    option = option,
                    value = value,
                    onValueChanged = onValueChanged,
                    isPassword = true,
                )
                option.examples.isNotEmpty() && option.type != "CommaSepList" -> DropdownField(
                    option = option,
                    value = value,
                    onValueChanged = onValueChanged,
                )
                option.type == "int" -> StringField(
                    option = option,
                    value = value,
                    onValueChanged = onValueChanged,
                    keyboardType = KeyboardType.Number,
                )
                else -> StringField(
                    option = option,
                    value = value,
                    onValueChanged = onValueChanged,
                )
            }
        }
    }
}

@Composable
private fun StringField(
    option: ProviderOption,
    value: String,
    onValueChanged: (String) -> Unit,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        label = { Text(option.name) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            }
        } else null,
    )
}

@Composable
private fun BoolField(
    option: ProviderOption,
    value: String,
    onValueChanged: (String) -> Unit,
) {
    val checked = value.lowercase(Locale.ROOT).toBooleanStrictOrNull() ?: false
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onValueChanged(it.toString()) },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = option.name,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    option: ProviderOption,
    value: String,
    onValueChanged: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            label = { Text(option.name) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            option.examples.forEach { example ->
                DropdownMenuItem(
                    text = { Text(example.Value) },
                    onClick = {
                        onValueChanged(example.Value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeSuffixField(
    value: String,
    onValueChanged: (String) -> Unit,
) {
    val regex = remember { Regex("(\\d*)(\\D?)") }
    val match = regex.matchEntire(value.lowercase(Locale.ROOT))
    var number by remember(value) { mutableStateOf(match?.groupValues?.get(1) ?: "") }
    var suffix by remember(value) { mutableStateOf(match?.groupValues?.get(2)?.uppercase(Locale.ROOT) ?: "") }
    var suffixExpanded by remember { mutableStateOf(false) }

    val suffixOptions = listOf("P", "T", "G", "M", "K", "B")

    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = number,
            onValueChange = {
                number = it
                onValueChanged(it + suffix)
            },
            label = { Text(stringResource(R.string.dynamic_config_suffixselector_value_hint)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.width(8.dp))
        ExposedDropdownMenuBox(
            expanded = suffixExpanded,
            onExpandedChange = { suffixExpanded = it },
            modifier = Modifier.weight(0.7f),
        ) {
            OutlinedTextField(
                value = suffix,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.dynamic_config_suffixselector_suffix_hint)) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(suffixExpanded) },
            )
            ExposedDropdownMenu(
                expanded = suffixExpanded,
                onDismissRequest = { suffixExpanded = false },
            ) {
                suffixOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            suffix = option
                            onValueChanged(number + option)
                            suffixExpanded = false
                        },
                    )
                }
            }
        }
    }
}

private const val RCLONE_CONFIG_NAME_KEY = "rclone_remote_name"
