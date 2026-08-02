package com.fieldcrm.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.theme.FieldIcons

@Composable
fun ConfirmationDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmButtonText: String = "Confirm",
    cancelButtonText: String = "Cancel",
    isDestructive: Boolean = false,
    icon: ImageVector = if (isDestructive) FieldIcons.AlertOutlined else FieldIcons.InfoOutlined
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(FieldTheme.shapes.cardRadius),
            colors = CardDefaults.cardColors(
                containerColor = FieldTheme.colors.gray900,
                contentColor = FieldTheme.colors.gray100
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                val iconBgColor = if (isDestructive) {
                    FieldTheme.colors.statusDanger.copy(alpha = 0.1f)
                } else {
                    FieldTheme.colors.purple900.copy(alpha = 0.1f)
                }
                val iconColor = if (isDestructive) {
                    FieldTheme.colors.statusDanger
                } else {
                    FieldTheme.colors.purple400
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(iconBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = title,
                    style = FieldTheme.typography.title,
                    color = FieldTheme.colors.gray100,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = description,
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDestructive) {
                        // For destructive confirmations, the safe option (Cancel) is dominant/filled
                        Button(
                            onClick = onCancel,
                            shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FieldTheme.colors.purple600,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = cancelButtonText,
                                style = FieldTheme.typography.bodyStrong
                            )
                        }

                        // Destructive "Confirm" is secondary (outlined red)
                        OutlinedButton(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FieldTheme.colors.statusDanger.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = FieldTheme.colors.statusDanger
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = confirmButtonText,
                                style = FieldTheme.typography.bodyStrong
                            )
                        }
                    } else {
                        // For routine confirmations, "Confirm" is filled, "Cancel" is outlined
                        Button(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FieldTheme.colors.purple600,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = confirmButtonText,
                                style = FieldTheme.typography.bodyStrong
                            )
                        }

                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FieldTheme.colors.gray700),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = FieldTheme.colors.gray300
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = cancelButtonText,
                                style = FieldTheme.typography.bodyStrong
                            )
                        }
                    }
                }
            }
        }
    }
}
