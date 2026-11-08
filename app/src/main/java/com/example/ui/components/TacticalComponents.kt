package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TacticalOrange
import com.example.ui.theme.SurfaceDark

@Composable
fun TacticalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = TacticalOrange,
    contentColor: Color = Color.Black
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.LightGray
        )
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
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
            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(TacticalOrange)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val color = when (status.lowercase()) {
        "completed", "success" -> Color(0xFF4CAF50)
        "in_progress", "queued" -> TacticalOrange
        "failed" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
