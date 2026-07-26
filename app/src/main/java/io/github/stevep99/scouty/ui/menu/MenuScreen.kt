package io.github.stevep99.scouty.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(
    onFace: () -> Unit,
    onMovement: () -> Unit,
    onSettings: () -> Unit,
    showMovement: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Scouty", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onFace,
            modifier = Modifier.width(220.dp).height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) { Text("Face", fontSize = 20.sp) }

        Spacer(modifier = Modifier.height(16.dp))

        if (showMovement) {
            Button(
                onClick = onMovement,
                modifier = Modifier.width(220.dp).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text("Movement", fontSize = 20.sp) }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onSettings,
            modifier = Modifier.width(220.dp).height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D4C41))
        ) { Text("Settings", fontSize = 20.sp) }
    }
}
