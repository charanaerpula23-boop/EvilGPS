package com.charanhyper.tech.greydailer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclaimerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Disclaimer & Info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Legal Notice
            SectionCard(
                title = "Legal Notice",
                titleColor = MaterialTheme.colorScheme.primary,
                content = "This app is provided \"as is\" under the MIT License. " +
                    "The developer is NOT responsible for any misuse, legal consequences, " +
                    "or damages arising from the use of this application.\n\n" +
                    "Mock location spoofing may violate the terms of service of certain apps " +
                    "and could be illegal in some jurisdictions. Use at your own risk and " +
                    "only for legitimate testing or development purposes."
            )

            // Technical Notes
            SectionCard(
                title = "Technical Notes",
                titleColor = MaterialTheme.colorScheme.primary,
                content = "\u2022 Route generation depends on the public OSRM endpoint " +
                    "(router.project-osrm.org). Availability is not guaranteed.\n\n" +
                    "\u2022 Location search uses Android's built-in Geocoder " +
                    "(Google Play Services). Falls back to Nominatim if unavailable.\n\n" +
                    "\u2022 Active mock sessions are persisted and automatically restored " +
                    "by the foreground service.\n\n" +
                    "\u2022 The internal package namespace is " +
                    "com.charanhyper.tech.greydailer (original project name retained).\n\n" +
                    "\u2022 Map tiles are loaded from public tile servers. Data usage " +
                    "depends on zoom level and area viewed."
            )

            // Cautions
            SectionCard(
                title = "Cautions",
                titleColor = MaterialTheme.colorScheme.error,
                content = "\u2022 Do NOT use this app to cheat in games, deceive " +
                    "ride-sharing services, or commit fraud.\n\n" +
                    "\u2022 Some banking and payment apps may stop if" +
                    "Developer options are ON and this app is detected\n\n" +
                    "\u2022 Always disable mock locations when you are done testing.\n\n" +
                    "\u2022 Using mock locations while driving navigation apps may " +
                    "cause dangerous route recalculations.\n\n" +
                    "\u2022 The developer assumes zero liability for any consequences."
            )

            // License
            SectionCard(
                title = "License",
                titleColor = MaterialTheme.colorScheme.primary,
                content = "MIT License\n\n" +
                    "Copyright \u00a9 2026 Charan\n\n" +
                    "Permission is hereby granted, free of charge, to any person obtaining " +
                    "a copy of this software to deal in the Software without restriction, " +
                    "including without limitation the rights to use, copy, modify, merge, " +
                    "publish, distribute, sublicense, and/or sell copies of the Software.\n\n" +
                    "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND."
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
