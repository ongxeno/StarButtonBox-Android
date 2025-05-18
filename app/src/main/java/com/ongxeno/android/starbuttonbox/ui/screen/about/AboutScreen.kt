package com.ongxeno.android.starbuttonbox.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ongxeno.android.starbuttonbox.R
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val appName = "StarButtonBox"
    val appVersion = "1.0.0"
    val licenseName = "GNU General Public License v3.0"
    val gplLink = "https://www.gnu.org/licenses/gpl-3.0.html"
    val githubRepoLink = "https://github.com/ongxeno/starbuttonbox-android"
    val authorName = "OngXeno"

    val highContrastTextColor = MaterialTheme.colorScheme.onBackground
    val mediumContrastTextColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About $appName") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "$appName Logo",
                modifier = Modifier
                    .size(96.dp)
                    .padding(top = 8.dp, bottom = 16.dp)
            )
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = highContrastTextColor,
                modifier = Modifier.padding(top = 0.dp)
            )
            Text(
                text = "Version $appVersion",
                style = MaterialTheme.typography.titleMedium,
                color = mediumContrastTextColor,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AboutSectionTitle("License Information", color = highContrastTextColor)

            val annotatedLicenseString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = highContrastTextColor)) {
                    append("This application is free software; you can redistribute it and/or modify it under the terms of the ")
                }
                pushStringAnnotation(tag = "LICENSE_LINK", annotation = gplLink)
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append(licenseName)
                }
                pop()
                withStyle(style = SpanStyle(color = highContrastTextColor)) {
                    append(".")
                }
            }

            ClickableText(
                text = annotatedLicenseString,
                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Start),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                onClick = { offset ->
                    annotatedLicenseString.getStringAnnotations(tag = "LICENSE_LINK", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            )

            Text(
                text = "This program is distributed in the hope that it will be useful, " +
                       "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
                       "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the " +
                       "GNU General Public License for more details.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                color = mediumContrastTextColor,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            AboutSectionTitle("Source Code & Contributions", color = highContrastTextColor)

            val annotatedRepoString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = highContrastTextColor)) {
                    append("View the source code, report issues, or contribute on ")
                }
                pushStringAnnotation(tag = "REPO_LINK", annotation = githubRepoLink)
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append("GitHub")
                }
                pop()
                withStyle(style = SpanStyle(color = highContrastTextColor)) {
                    append(".")
                }
            }

            ClickableText(
                text = annotatedRepoString,
                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Start),
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                onClick = { offset ->
                    annotatedRepoString.getStringAnnotations(tag = "REPO_LINK", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} $authorName",
                style = MaterialTheme.typography.bodySmall,
                color = mediumContrastTextColor,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun AboutSectionTitle(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 8.dp),
        textAlign = TextAlign.Start
    )
}

@Preview(showBackground = true, name = "About Screen Preview")
@Composable
fun AboutScreenPreview() {
    StarButtonBoxTheme {
        AboutScreen(onNavigateBack = {})
    }
}
