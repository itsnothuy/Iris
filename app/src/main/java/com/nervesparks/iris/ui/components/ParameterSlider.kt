package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.R

/**
 * A reusable parameter slider component for model configuration.
 *
 * @param label The label to display above the slider
 * @param value The current value of the parameter
 * @param onValueChange Callback when the slider value changes
 * @param valueRange The range of valid values for the slider
 * @param steps Number of discrete steps between min and max (0 for continuous)
 * @param helpText Optional help text explaining the parameter
 * @param valueFormatter Optional formatter to display the value (default shows 2 decimal places for Float)
 */
@Composable
fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    helpText: String? = null,
    valueFormatter: (Float) -> String = { "%.2f".format(it) },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Label and current value
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueFormatter(value),
                color = Color(0xFF6200EE),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Help text if provided
        if (helpText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helpText,
                color = Color.Gray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Slider
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6200EE),
                activeTrackColor = Color(0xFF6200EE),
                inactiveTrackColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Integer variant of ParameterSlider for parameters like top_k and context length.
 */
@Composable
fun ParameterSliderInt(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    helpText: String? = null,
    modifier: Modifier = Modifier
) {
    val floatValue = value.toFloat()
    val floatRange = valueRange.first.toFloat()..valueRange.last.toFloat()
    val steps = valueRange.last - valueRange.first - 1
    
    ParameterSlider(
        label = label,
        value = floatValue,
        onValueChange = { onValueChange(it.toInt()) },
        valueRange = floatRange,
        steps = if (steps > 0 && steps < 1000) steps else 0, // Limit steps for performance
        helpText = helpText,
        valueFormatter = { it.toInt().toString() },
        modifier = modifier
    )
}
