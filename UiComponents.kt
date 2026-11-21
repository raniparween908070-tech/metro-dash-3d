package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MetroBgCard
import com.example.ui.theme.MetroBgDeep
import com.example.ui.theme.MetroBgElevated
import com.example.ui.theme.MetroCyan
import com.example.ui.theme.MetroDiamond
import com.example.ui.theme.MetroGold
import com.example.ui.theme.MetroOutline
import com.example.ui.theme.MetroTextPrimary
import com.example.ui.theme.MetroTextSecondary

@Composable
fun CurrencyPill(
    iconColor: Color,
    value: String,
    isDiamond: Boolean = false,
    isBoard: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableMod = if (onClick != null) modifier.clickable { onClick() } else modifier
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = clickableMod
            .clip(RoundedCornerShape(50))
            .background(MetroBgCard.copy(alpha = 0.92f))
            .border(1.dp, iconColor.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = when {
                isDiamond -> Icons.Filled.Diamond
                isBoard -> Icons.Filled.Skateboarding
                else -> Icons.Filled.MonetizationOn
            },
            contentDescription = when {
                isDiamond -> "Diamonds"
                isBoard -> "Hoverboards"
                else -> "Coins"
            },
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MetroTextPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun MetroTopBar(
    title: String,
    coins: Int,
    diamonds: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(MetroBgElevated)
                    .border(1.dp, MetroOutline, CircleShape)
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MetroTextPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MetroTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyPill(iconColor = MetroGold, value = formatCompactNumber(coins))
            CurrencyPill(iconColor = MetroDiamond, value = formatCompactNumber(diamonds), isDiamond = true)
        }
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    body: String,
    confirmLabel: String,
    confirmColor: Color = MetroCyan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MetroBgCard,
        titleContentColor = MetroTextPrimary,
        textContentColor = MetroTextSecondary,
        title = {
            Text(title, style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Text(body, style = MaterialTheme.typography.bodyLarge)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor,
                    contentColor = MetroBgDeep
                )
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = MetroTextPrimary)
            }
        }
    )
}

fun formatCompactNumber(value: Int): String = when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000f)
    value >= 10_000 -> String.format("%.1fK", value / 1_000f)
    else -> "%,d".format(value)
}

fun formatMeters(meters: Long): String = "%,d m".format(meters)
