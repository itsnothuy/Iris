package com.nervesparks.iris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.R
import com.nervesparks.iris.data.LanguagePreference
import com.nervesparks.iris.data.ThemePreference

@Composable
fun SettingsScreen(
    onParamsScreenButtonClicked: () -> Unit,
    onModelsScreenButtonClicked: () -> Unit,
    onAboutScreenButtonClicked: () -> Unit,
    onBenchMarkScreenButtonClicked: () -> Unit,
    viewModel: com.nervesparks.iris.MainViewModel,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xff0f172a),
                            shape = RoundedCornerShape(12.dp),
                        )
                ) {
                    SettingsRow(
                        text = "Models",
                        iconRes = R.drawable.data_exploration_models_svgrepo_com,
                        onClick = onModelsScreenButtonClicked
                    )

                    SettingsDivider()

                    SettingsRow(
                        text = "Change Parameters",
                        iconRes = R.drawable.setting_4_svgrepo_com,
                        onClick = onParamsScreenButtonClicked
                    )

                    SettingsDivider()

                    SettingsRow(
                        text = "BenchMark",
                        iconRes = R.drawable.bench_mark_icon,
                        onClick = onBenchMarkScreenButtonClicked
                    )

                    SettingsDivider()

                    SettingsRow(
                        text = "About",
                        iconRes = R.drawable.information_outline_svgrepo_com,
                        onClick = onAboutScreenButtonClicked
                    )
                }
            }
            
            // Appearance section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xff0f172a),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_appearance),
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    var currentTheme by remember { 
                        mutableStateOf(viewModel.getThemePreference()) 
                    }
                    
                    SettingsSelector(
                        label = stringResource(R.string.settings_theme),
                        options = listOf(
                            ThemePreference.LIGHT to stringResource(R.string.settings_theme_light),
                            ThemePreference.DARK to stringResource(R.string.settings_theme_dark),
                            ThemePreference.SYSTEM to stringResource(R.string.settings_theme_system)
                        ),
                        selectedOption = currentTheme,
                        onOptionSelected = { theme ->
                            currentTheme = theme
                            viewModel.setThemePreference(theme)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var currentLanguage by remember { 
                        mutableStateOf(viewModel.getLanguagePreference()) 
                    }
                    
                    SettingsSelector(
                        label = stringResource(R.string.settings_language),
                        options = listOf(
                            LanguagePreference.ENGLISH to stringResource(R.string.settings_language_english),
                            LanguagePreference.SPANISH to stringResource(R.string.settings_language_spanish)
                        ),
                        selectedOption = currentLanguage,
                        onOptionSelected = { language ->
                            currentLanguage = language
                            viewModel.setLanguagePreference(language)
                        }
                    )
                }
            }
            
            // Privacy section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xff0f172a),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_privacy),
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    var isRedactionEnabled by remember { 
                        mutableStateOf(viewModel.getPrivacyRedactionEnabled()) 
                    }
                    
                    PrivacyToggleRow(
                        text = stringResource(R.string.settings_privacy_redact_pii),
                        description = stringResource(R.string.settings_privacy_redact_pii_description),
                        checked = isRedactionEnabled,
                        onCheckedChange = { enabled ->
                            isRedactionEnabled = enabled
                            viewModel.setPrivacyRedactionEnabled(enabled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun <T> SettingsSelector(
    label: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (option, displayName) ->
                val isSelected = option == selectedOption
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) Color(0xFF1E88E5) else Color(0xFF1a1f2e),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF64B5F6) else Color(0xFF404654),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onOptionSelected(option) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName,
                        color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsRow(text: String, iconRes: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.White
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 7.dp)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
fun SettingsDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.DarkGray,
        thickness = 1.dp
    )
}

@Composable
fun PrivacyToggleRow(
    text: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color(0xFFB0B0B0),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF64B5F6),
                checkedTrackColor = Color(0xFF1E88E5),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
