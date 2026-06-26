package com.fieldcrm.android.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldTheme

data class OnboardingSlide(
    val title: String,
    val description: String,
    val illustrationText: String
)

@Composable
fun OnboardingScreen(
    role: UserRole?,
    onDismiss: () -> Unit
) {
    val currentRole = role ?: UserRole.LOAN_OFFICER

    // Skip onboarding for Auditor as they have no release changes
    if (currentRole == UserRole.AUDITOR) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    val slides = when (currentRole) {
        UserRole.BRANCH_MANAGER -> listOf(
            OnboardingSlide(
                title = "New: Action Shortcuts",
                description = "Tap inline concur or return buttons directly from the application list card to speed up operations.",
                illustrationText = "BM CONCUR / RETURN Shortcuts"
            ),
            OnboardingSlide(
                title = "Document Flags Overview",
                description = "See which document files failed compliance or OCR immediately under the review tab.",
                illustrationText = "Audit Compliance Flag view"
            )
        )
        else -> listOf( // Default for Loan Officer / others
            OnboardingSlide(
                title = "New: Swipe to upload",
                description = "Swipe any application card right to quickly upload a missing document or guarantor form.",
                illustrationText = "📷 Swipe Right to Upload"
            ),
            OnboardingSlide(
                title = "Fast Search & Filter",
                description = "Tap search results directly to jump into the loan origination form wizard steps.",
                illustrationText = "🔍 Live reference search"
            )
        )
    }

    var currentSlideIndex by remember { mutableStateOf(0) }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        contentAlignment = Alignment.Center
    ) {
        if (isTablet) {
            // Tablet Layout: Centered Card
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                FieldCard {
                    OnboardingContent(
                        slides = slides,
                        currentSlideIndex = currentSlideIndex,
                        onSlideIndexChange = { currentSlideIndex = it },
                        onDismiss = onDismiss,
                        isTablet = true
                    )
                }
            }
        } else {
            // Phone Layout: Full Screen, actions pushed to the bottom thumb zone
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                OnboardingContent(
                    slides = slides,
                    currentSlideIndex = currentSlideIndex,
                    onSlideIndexChange = { currentSlideIndex = it },
                    onDismiss = onDismiss,
                    isTablet = false
                )
            }
        }
    }
}

@Composable
fun OnboardingContent(
    slides: List<OnboardingSlide>,
    currentSlideIndex: Int,
    onSlideIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    isTablet: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress dots at top
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            slides.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (index == currentSlideIndex) FieldTheme.colors.purple600 else FieldTheme.colors.gray800,
                            shape = CircleShape
                        )
                )
            }
        }

        // Crossfade animation for swiping slides
        Crossfade(
            targetState = currentSlideIndex,
            label = "onboarding_slide_crossfade",
            modifier = Modifier.height(280.dp)
        ) { index ->
            val slide = slides[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Illustration Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = slide.illustrationText,
                        style = FieldTheme.typography.mono,
                        color = FieldTheme.colors.purple400,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = slide.title,
                    style = FieldTheme.typography.display,
                    color = FieldTheme.colors.gray100,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = slide.description,
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (!isTablet) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Next, Skip or Done bottom navigation actions (at least 48dp height)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip target height 48dp
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clickable { onDismiss() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Skip",
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.gray500
                )
            }
            
            val isLastSlide = currentSlideIndex == slides.size - 1
            PrimaryButton(
                text = if (isLastSlide) "Get Started" else "Next",
                onClick = {
                    if (isLastSlide) {
                        onDismiss()
                    } else {
                        onSlideIndexChange(currentSlideIndex + 1)
                    }
                },
                modifier = Modifier
                    .width(140.dp)
                    .height(48.dp)
            )
        }
    }
}
