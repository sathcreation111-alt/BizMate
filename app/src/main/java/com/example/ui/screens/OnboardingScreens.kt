package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BizMateBackground
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.components.GradientButton
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.BrightLavender
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.LightLavender
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.Localization

@Composable
fun LanguageSelectionScreen(
    currentLang: String,
    onLanguageSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    var selectedLang by remember(currentLang) { mutableStateOf(currentLang) }

    BizMateBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            BrandHeader()
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(BrightLavender.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                    .border(1.5.dp, BrightLavender.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = BrightLavender,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Choose Your Language",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ඔබේ භාෂාව තෝරන්න",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Language Cards
            LanguageOptionCard(
                title = "සිංහල",
                subtitle = "Sinhala Language",
                isSelected = selectedLang == "si",
                testTag = "language_card_sinhala",
                onClick = {
                    selectedLang = "si"
                    onLanguageSelected("si")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LanguageOptionCard(
                title = "English",
                subtitle = "English Language",
                isSelected = selectedLang == "en",
                testTag = "language_card_english",
                onClick = {
                    selectedLang = "en"
                    onLanguageSelected("en")
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            GradientButton(
                text = if (selectedLang == "si") "ඉදිරියට යන්න" else "Continue",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                testTag = "language_continue_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) BrightLavender else GlassBorder,
        label = "card_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2E194D) else GlassCardBg,
        label = "card_bg"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        label = "card_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(color = BrightLavender),
                onClick = onClick
            )
            .testTag(testTag)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) LightLavender else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PrimaryViolet else Color(0xFF1E1030))
                    .border(
                        1.5.dp,
                        if (isSelected) BrightLavender else GlassBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShopNameSetupScreen(
    language: String,
    onComplete: (String) -> Unit
) {
    var shopNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val heading = if (language == "si") "ආයුබෝවන්!" else "Welcome!"
    val description = if (language == "si") {
        "ඔබේ ව්යාපාරය ආරම්භ කරමු. මුලින්ම කඩේ නම ඇතුළත් කරන්න."
    } else {
        "Let’s get your business started. Enter your shop name."
    }
    val placeholder = if (language == "si") "ඔබේ කඩේ නම ඇතුළත් කරන්න" else "Enter your shop name"
    val buttonText = if (language == "si") "ආරම්භ කරමු" else "Get Started"
    val validationMsg = if (language == "si") "කරුණාකර කඩේ නම ඇතුළත් කරන්න" else "Please enter your shop name"

    BizMateBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            BrandHeader()
            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                borderWidth = 1.5.dp,
                borderColor = BrightLavender.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PrimaryViolet.copy(alpha = 0.25f))
                            .border(1.5.dp, BrightLavender.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = AccentLavender,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = heading,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    GlassTextField(
                        value = shopNameInput,
                        onValueChange = {
                            shopNameInput = it
                            if (errorMessage != null) errorMessage = null
                        },
                        label = if (language == "si") "කඩේ නම" else "Shop Name",
                        placeholder = placeholder,
                        leadingIcon = Icons.Default.Storefront,
                        isError = errorMessage != null,
                        errorMessage = errorMessage,
                        testTag = "shop_name_input"
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    GradientButton(
                        text = buttonText,
                        onClick = {
                            if (shopNameInput.trim().isBlank()) {
                                errorMessage = validationMsg
                            } else {
                                onComplete(shopNameInput.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "get_started_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BizMate LK • Sath Creation",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
