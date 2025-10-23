package com.swordfish.lemuroid.app.mobile.feature.gamemenu.resolution

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsGroup

@Composable
fun GameMenuResizeModeScreen(
    currentResizeMode: Int,
    onApply: (Int) -> Unit,
) {
    val context = LocalContext.current

    var selectedMode by remember { mutableIntStateOf(currentResizeMode) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        LemuroidSettingsGroup(
            title = {
                Text(
                    text = stringResource(R.string.game_menu_edit_resolution),
                    style = MaterialTheme.typography.titleMedium
                )
            },
        ) {
            ResizeModeOption(
                title = stringResource(R.string.resize_mode_original),
                description = stringResource(R.string.resize_mode_original),
                isSelected = selectedMode == RESIZE_MODE_FIT,
                onClick = { selectedMode = RESIZE_MODE_FIT }
            )

            ResizeModeOption(
                title = stringResource(R.string.resize_mode_fullscreen),
                description = stringResource(R.string.resize_mode_fullscreen),
                isSelected = selectedMode == RESIZE_MODE_FILL,
                onClick = { selectedMode = RESIZE_MODE_FILL }
            )
        }

        Spacer(modifier = Modifier.size(24.dp))

        Button(
            onClick = { onApply(selectedMode) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = selectedMode != currentResizeMode
        ) {
            Text(text = stringResource(R.string.resize_mode_apply))
        }
    }
}

@Composable
private fun ResizeModeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selecionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private const val RESIZE_MODE_FIT = 0
private const val RESIZE_MODE_FILL = 3
