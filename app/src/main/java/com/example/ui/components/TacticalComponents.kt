package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun TacticalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = PrimaryOrange,
    contentColor: Color = AppBackground
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp).fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = CardSurfaceElevated,
            disabledContentColor = TextMuted
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
        )
    }
}

@Composable
fun TacticalCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(0.18f), spotColor = Color.Black.copy(0.18f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            letterSpacing = 0.2.sp
        )
    )
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val key = status.lowercase()
    val (bg, fg) = when {
        key.contains("success") || key.contains("complete") || key.contains("done") -> MintGlow to MintSuccess
        key.contains("progress") || key.contains("run") || key.contains("queued")   -> PrimaryGlow to PrimaryOrange
        key.contains("fail") || key.contains("error") || key.contains("cancel")     -> SoftErrorGlow to SoftError
        key.contains("pending") || key.contains("wait")                             -> WarningGlow to PendingAmber
        else -> Color.White.copy(alpha = 0.06f) to TextSecondary
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).background(fg, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.replace("_", " ").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(color = fg, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
            )
        }
    }
}
