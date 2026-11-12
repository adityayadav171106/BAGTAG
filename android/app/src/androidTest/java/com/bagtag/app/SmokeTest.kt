package com.bagtag.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Device/emulator smoke test: verifies that the app presents a usable stopped state without permissions. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun initialScreenHasPrimaryMonitoringControls() { rule.onNodeWithText("BAGTAG").assertIsDisplayed(); rule.onNodeWithText("START MONITORING").assertIsDisplayed(); rule.onNodeWithText("RECENT ACTIVITY").assertIsDisplayed() }
}
