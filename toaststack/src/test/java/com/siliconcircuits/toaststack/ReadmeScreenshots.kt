package com.siliconcircuits.toaststack

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode

/**
 * Makes the README images in docs/images. Skipped in normal runs. Run with
 * ./gradlew :toaststack:testDebugUnitTest --tests "*ReadmeScreenshots*" -PrecordScreenshots
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h740dp-xhdpi")
class ReadmeScreenshots {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val reset = ToastStackReset()

    private val out = File("../docs/images")

    @Before
    fun onlyWhenAsked() {
        assumeTrue(System.getProperty("recordScreenshots") == "true")
        out.mkdirs()
    }

    // A brand palette with a green primary, so Success reads as success with
    // AppTheme colors: Info uses the blue secondary, Warning the amber tertiary.
    private val light = lightColorScheme(
        primary = Color(0xFF2E6B3F), onPrimary = Color.White,
        primaryContainer = Color(0xFFC6F0CC), onPrimaryContainer = Color(0xFF00210B),
        secondaryContainer = Color(0xFFD5E3FF), onSecondaryContainer = Color(0xFF001B3C),
        tertiaryContainer = Color(0xFFFFE08F), onTertiaryContainer = Color(0xFF241A00),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        surface = Color(0xFFF7F8F4), onSurface = Color(0xFF1A1C19),
        surfaceVariant = Color(0xFFDEE5DA), onSurfaceVariant = Color(0xFF424940),
        inverseSurface = Color(0xFF2F312D), inverseOnSurface = Color(0xFFF0F1EC),
    )
    private val dark = darkColorScheme(
        primary = Color(0xFF93D7A0), onPrimary = Color(0xFF003916),
        primaryContainer = Color(0xFF14522A), onPrimaryContainer = Color(0xFFC6F0CC),
        secondaryContainer = Color(0xFF1F477D), onSecondaryContainer = Color(0xFFD5E3FF),
        tertiaryContainer = Color(0xFF574500), onTertiaryContainer = Color(0xFFFFE08F),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        surface = Color(0xFF111410), onSurface = Color(0xFFE1E3DD),
        surfaceVariant = Color(0xFF424940), onSurfaceVariant = Color(0xFFC2C9BE),
        inverseSurface = Color(0xFFE1E3DD), inverseOnSurface = Color(0xFF2F312D),
    )

    /** A plain sample screen behind the toasts. */
    @Composable
    private fun SampleScreen() {
        val colors = MaterialTheme.colorScheme
        Column(Modifier.fillMaxSize().background(colors.surface)) {
            Box(Modifier.fillMaxWidth().height(64.dp).background(colors.surface).padding(horizontal = 20.dp)) {
                Text(
                    "Inbox",
                    modifier = Modifier.align(Alignment.CenterStart),
                    color = colors.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            listOf("Priya Nair", "Arjun Mehta", "Team Standup", "Design Review", "Rahul Verma", "Weekly Report", "Sneha Iyer", "Release Notes")
                .forEachIndexed { index, name ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(if (index % 2 == 0) colors.primaryContainer else colors.secondaryContainer),
                        ) {
                            Text(
                                name.take(1),
                                modifier = Modifier.align(Alignment.Center),
                                color = colors.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(name, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Box(Modifier.width(180.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.surfaceVariant))
                        }
                    }
                }
        }
    }

    /** Renders [scene] in light and dark with [show] run on the state, and saves both. */
    private fun capture(
        name: String,
        colorSource: ToastColorSource = ToastColorSource.AppTheme,
        withScreen: Boolean = false,
        lightOnly: Boolean = false,
        show: (ToastStackState) -> Unit,
    ) {
        this.withScreen = withScreen
        val schemes = if (lightOnly) listOf("light" to light) else listOf("light" to light, "dark" to dark)
        for ((suffix, scheme) in schemes) {
            val bitmap = render(scheme, colorSource, cropToToasts = !withScreen, show)
            File(out, "$name-$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /**
     * Copies the window with PixelCopy, which in Robolectric's hardware mode
     * draws the card shadows. With [cropToToasts], the image ends below the
     * lowest card.
     */
    private fun snapshot(cropToToasts: Boolean = false): Bitmap {
        composeTestRule.waitForIdle()
        val activity = composeTestRule.activity
        val view = activity.window.decorView
        val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        var done = false
        PixelCopy.request(activity.window, full, { done = true }, Handler(Looper.getMainLooper()))
        while (!done) shadowOf(Looper.getMainLooper()).idle()
        if (!cropToToasts) return full
        val bottom = composeTestRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .fetchSemanticsNodes().maxOf { it.boundsInWindow.bottom }
        val height = (bottom + 24 * activity.resources.displayMetrics.density).toInt().coerceAtMost(full.height)
        return Bitmap.createBitmap(full, 0, 0, full.width, height)
    }

    private var renders = 0

    private var withScreen = false

    private fun render(scheme: ColorScheme, colorSource: ToastColorSource, cropToToasts: Boolean, show: (ToastStackState) -> Unit): Bitmap {
        val state = ToastStackState(maxVisible = 6)
        if (renders++ == 0) {
            composeTestRule.setContent { Scene(scheme, colorSource, state) }
        }
        sceneState.value = Triple(scheme, colorSource, state)
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { show(state) }
        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.waitForIdle()
        return snapshot(cropToToasts)
    }

    private val sceneState = androidx.compose.runtime.mutableStateOf<Triple<ColorScheme, ToastColorSource, ToastStackState>?>(null)

    @Composable
    private fun Scene(scheme: ColorScheme, colorSource: ToastColorSource, first: ToastStackState) {
        val (currentScheme, currentSource, state) = sceneState.value ?: Triple(scheme, colorSource, first)
        MaterialTheme(colorScheme = currentScheme) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                if (withScreen) SampleScreen()
                androidx.compose.runtime.key(state) {
                    ToastStackHost(
                        state = state,
                        tag = "readme",
                        colorSource = currentSource,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }

    @Test
    fun types() = capture("types") { state ->
        state.success("Your changes were saved", title = "Saved", duration = ToastDuration.Indefinite)
        state.error("Check your connection and try again", title = "Upload failed", duration = ToastDuration.Indefinite)
        state.warning("Only 120 MB of storage left", title = "Storage low", duration = ToastDuration.Indefinite)
        state.info("A new version is ready to install", title = "Update available", duration = ToastDuration.Indefinite)
        state.show("Message archived", duration = ToastDuration.Indefinite)
    }

    @Test
    fun library() = capture("library", ToastColorSource.Library) { state ->
        state.success("Your changes were saved", title = "Saved", duration = ToastDuration.Indefinite)
        state.error("Check your connection and try again", title = "Upload failed", duration = ToastDuration.Indefinite)
        state.warning("Only 120 MB of storage left", title = "Storage low", duration = ToastDuration.Indefinite)
        state.info("A new version is ready to install", title = "Update available", duration = ToastDuration.Indefinite)
    }

    @Test
    fun actions() = capture("actions") { state ->
        state.show("Conversation deleted", duration = ToastDuration.Indefinite)
            .withAction("Undo") {}
        state.loading("Uploading photos", title = "Backup").updateProgress(0.6f, "6 of 10")
        state.error("Couldn't send message", duration = ToastDuration.Indefinite)
            .withAction("Retry") {}
        state.show("Photo shared with the team", type = ToastType.Success, duration = ToastDuration.Indefinite, showCloseButton = true)
            .withAction("View") {}
    }

    // Landscape, so the corner cards do not overlap.
    @Test
    @Config(qualifiers = "w740dp-h360dp-xhdpi")
    fun positions() = capture("positions", withScreen = true, lightOnly = true) { state ->
        state.info("Top start", duration = ToastDuration.Indefinite, position = ToastPosition.TopStart)
        state.success("Top end", duration = ToastDuration.Indefinite, position = ToastPosition.TopEnd)
        state.show("Center", duration = ToastDuration.Indefinite, position = ToastPosition.Center)
        state.warning("Bottom start", duration = ToastDuration.Indefinite, position = ToastPosition.BottomStart)
        state.error("Bottom end", duration = ToastDuration.Indefinite, position = ToastPosition.BottomEnd)
    }

    @Test
    fun custom() = capture("custom") { state ->
        state.showCustom(duration = ToastDuration.Indefinite, showCloseButton = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                    Text("P", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Priya Nair", color = MaterialTheme.colorScheme.inverseOnSurface, fontWeight = FontWeight.SemiBold)
                    Text("Sent you a photo", color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
        }
    }

    /** An animated GIF: three toasts slide in one after another, then one is swiped away. */
    @Test
    fun demo() {
        val state = ToastStackState(maxVisible = 6)
        withScreen = true
        sceneState.value = Triple(light, ToastColorSource.AppTheme, state)
        composeTestRule.setContent { Scene(light, ToastColorSource.AppTheme, state) }
        composeTestRule.mainClock.autoAdvance = false
        val frames = mutableListOf<Bitmap>()
        // The top half of the screen, where the toasts are.
        fun frames(count: Int, stepMillis: Long) = repeat(count) {
            composeTestRule.mainClock.advanceTimeBy(stepMillis)
            val full = snapshot()
            frames += Bitmap.createBitmap(full, 0, 0, full.width, full.height / 2)
        }
        frames(4, 120)
        composeTestRule.runOnIdle { state.success("Your changes were saved", title = "Saved", duration = ToastDuration.Indefinite) }
        frames(8, 60)
        composeTestRule.runOnIdle { state.info("A new version is ready", title = "Update available", duration = ToastDuration.Indefinite) }
        frames(8, 60)
        composeTestRule.runOnIdle {
            state.loading("Uploading photos", title = "Backup").updateProgress(0.4f, "4 of 10")
        }
        frames(8, 60)
        composeTestRule.runOnIdle { state.toasts.lastOrNull()?.let { state.updateToastProgress(it.id, 0.9f, "9 of 10") } }
        frames(6, 120)
        composeTestRule.runOnIdle { state.toasts.firstOrNull()?.let { state.dismiss(it.id) } }
        frames(10, 60)
        frames(4, 200)
        writeGif(frames, File(out, "demo.gif"), delayCentis = 6)
        // One frame from the middle, to check the capture by eye.
        File("build/demo-frame.png").outputStream().use { frames[frames.size / 2].compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Writes [frames] as a looping GIF, at half size to keep the file small. */
    private fun writeGif(frames: List<Bitmap>, file: File, delayCentis: Int) {
        val writer = ImageIO.getImageWritersByFormatName("gif").next()
        file.delete()
        ImageIO.createImageOutputStream(file).use { stream ->
            writer.output = stream
            writer.prepareWriteSequence(null)
            frames.forEach { frame ->
                val small = Bitmap.createScaledBitmap(frame, frame.width / 2, frame.height / 2, true)
                val image = BufferedImage(small.width, small.height, BufferedImage.TYPE_INT_RGB)
                val pixels = IntArray(small.width * small.height)
                small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
                image.setRGB(0, 0, small.width, small.height, pixels, 0, small.width)
                val type = ImageTypeSpecifier.createFromRenderedImage(image)
                val metadata = writer.getDefaultImageMetadata(type, null)
                val format = metadata.nativeMetadataFormatName
                val root = metadata.getAsTree(format) as IIOMetadataNode
                fun child(name: String) = (0 until root.length).map { root.item(it) }
                    .firstOrNull { it.nodeName == name } as IIOMetadataNode? ?: IIOMetadataNode(name).also { root.appendChild(it) }
                child("GraphicControlExtension").apply {
                    setAttribute("disposalMethod", "none")
                    setAttribute("userInputFlag", "FALSE")
                    setAttribute("transparentColorFlag", "FALSE")
                    setAttribute("delayTime", delayCentis.toString())
                    setAttribute("transparentColorIndex", "0")
                }
                child("ApplicationExtensions").appendChild(
                    IIOMetadataNode("ApplicationExtension").apply {
                        setAttribute("applicationID", "NETSCAPE")
                        setAttribute("authenticationCode", "2.0")
                        userObject = byteArrayOf(1, 0, 0) // loop forever
                    },
                )
                metadata.setFromTree(format, root)
                writer.writeToSequence(IIOImage(image, null, metadata), null)
            }
            writer.endWriteSequence()
        }
    }
}
