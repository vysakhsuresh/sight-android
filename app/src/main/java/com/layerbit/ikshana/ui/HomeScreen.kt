package com.layerbit.ikshana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.layerbit.ikshana.R
import com.layerbit.ikshana.engine.gesture.Gesture
import com.layerbit.ikshana.engine.gesture.Gestures
import com.layerbit.ikshana.engine.mode.Event
import com.layerbit.ikshana.engine.mode.Mode
import com.layerbit.ikshana.text.Labels
import com.layerbit.ikshana.Session

/**
 * The home screen is the camera, in the mode last used (brief, section 4). The whole picture is
 * one control: tap pauses or resumes the voice, swipe down repeats, up goes to the next line,
 * left and right change mode. With TalkBack on, the same four are the picture's custom actions,
 * and its double-tap is pause, so nothing depends on the system letting a gesture through.
 *
 * The six modes sit at the bottom in two rows of three, not one row of six: at the moto's 411 dp
 * a row of six is 68 dp a button and "Translate" at 18 sp does not fit without shrinking, and a
 * grid never shrinks a label (Stave's lesson). Each is still reachable by TalkBack in one swipe.
 */
@Composable
fun HomeScreen(
    session: Session,
    camera: @Composable (Modifier) -> Unit,
    onAllowCamera: () -> Unit,
    onPickPicture: () -> Unit
) {
    val state = session.state
    var settingsOpen by remember { mutableStateOf(false) }
    val activeName = stringResource(Labels.mode(state.active))

    Box(Modifier.fillMaxSize().background(Ink.Background)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                CameraSurface(session, activeName, camera)
                if (state.active == Mode.HELP) HelpDoors(onAllowCamera, onPickPicture)
                session.shown?.let { Banner(it) }
                SettingsButton(Modifier.align(Alignment.TopEnd)) { settingsOpen = true }
            }
            ModeGrid(session)
        }
        if (settingsOpen) SettingsPanel(session) { settingsOpen = false }
    }
}

@Composable
private fun CameraSurface(session: Session, activeName: String, camera: @Composable (Modifier) -> Unit) {
    val density = LocalDensity.current.density
    // pointerInput(Unit) keeps its first lambdas; read the session through updated state (Stave).
    val s by rememberUpdatedState(session)
    val repeat = stringResource(R.string.action_repeat)
    val next = stringResource(R.string.action_next_mode)
    val previous = stringResource(R.string.action_previous_mode)
    val pause = stringResource(R.string.action_pause)
    val description = stringResource(R.string.camera_view, activeName)
    val paused = stringResource(R.string.paused)
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .border(3.dp, Ink.Iris, RoundedCornerShape(18.dp))
            .semantics {
                contentDescription = description
                if (session.paused) stateDescription = paused
                onClick(label = pause) { s.togglePause(); true }
                customActions = listOf(
                    CustomAccessibilityAction(repeat) { s.repeat(); true },
                    CustomAccessibilityAction(next) { s.stepMode(1); true },
                    CustomAccessibilityAction(previous) { s.stepMode(-1); true }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var last = down
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        last = change
                        if (!change.pressed) break
                    }
                    val g = Gestures.classify(
                        last.position.x - down.position.x, last.position.y - down.position.y,
                        last.uptimeMillis - down.uptimeMillis, density
                    )
                    when (g) {
                        Gesture.TAP -> s.togglePause()
                        Gesture.SWIPE_DOWN -> s.repeat()
                        Gesture.SWIPE_UP -> s.next()
                        Gesture.SWIPE_LEFT -> s.stepMode(1)
                        Gesture.SWIPE_RIGHT -> s.stepMode(-1)
                        Gesture.NONE -> Unit
                    }
                }
            }
    ) {
        camera(Modifier.fillMaxSize())
        if (session.paused) {
            Text(
                paused,
                Modifier.align(Alignment.Center).background(Ink.Background.copy(alpha = 0.8f), RoundedCornerShape(14.dp)).padding(16.dp),
                style = MaterialTheme.typography.titleMedium, color = Ink.Text
            )
        }
    }
}

@Composable
private fun HelpDoors(onAllowCamera: () -> Unit, onPickPicture: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Ink.Background.copy(alpha = 0.92f), RoundedCornerShape(18.dp)).padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.say_no_camera), style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
        Spacer(Modifier.height(24.dp))
        BigButton(stringResource(R.string.door_allow_camera), primary = true, onClick = onAllowCamera)
        Spacer(Modifier.height(16.dp))
        BigButton(stringResource(R.string.door_pick_picture), primary = false, onClick = onPickPicture)
    }
}

@Composable
private fun Banner(text: String) {
    // The line the voice could not say, and where to get a voice: with no TTS engine this banner
    // and the earcon's haptic twin are all the app has left (docs/FALLBACKS.md).
    Text(
        text + "\n\n" + stringResource(R.string.no_voice),
        Modifier.fillMaxWidth().padding(top = 76.dp).background(Ink.Surface, RoundedCornerShape(14.dp)).padding(16.dp),
        style = MaterialTheme.typography.titleMedium, color = Ink.Text
    )
}

@Composable
private fun BigButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = Target)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) Ink.Iris else Ink.Surface)
            .border(1.dp, if (primary) Ink.Iris else Ink.BorderStrong, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (primary) Ink.OnIris else Ink.Text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SettingsButton(modifier: Modifier, onClick: () -> Unit) {
    val name = stringResource(R.string.settings)
    Box(
        modifier.padding(8.dp).size(Target).clip(RoundedCornerShape(14.dp))
            .background(Ink.Surface.copy(alpha = 0.85f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center
    ) {
        // Three bars, drawn in text so there is no icon library to declare: the name is on the node.
        // Seen in the moto's dump on 12 Nov: without clearing, the glyph was read after the name.
        Text("≡", Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.headlineMedium, color = Ink.Text)
    }
}

@Composable
private fun ModeGrid(session: Session) {
    val on = stringResource(R.string.mode_state_on)
    val off = stringResource(R.string.mode_state_off)
    val notHere = stringResource(R.string.mode_state_not_here)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in Mode.row.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (mode in row) {
                    val selected = session.state.wanted == mode
                    val active = session.state.active == mode
                    val runs = session.runsAsItself(mode)
                    val state = when { active -> on; !runs -> notHere; else -> off }
                    Box(
                        Modifier.weight(1f).heightIn(min = Target)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) Ink.Iris else Ink.Surface)
                            .border(1.dp, if (selected) Ink.Iris else Ink.Border, RoundedCornerShape(14.dp))
                            // Selectable, not clickable: the tint is the only mark of the chosen mode.
                            .selectable(selected = selected, role = Role.Tab) { session.send(Event.Choose(mode)) }
                            .semantics { stateDescription = state },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(Labels.mode(mode)),
                            style = MaterialTheme.typography.labelLarge,
                            color = when { active -> Ink.OnIris; runs -> Ink.Text; else -> Ink.Muted },
                            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(session: Session, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).safeDrawingPadding().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().background(Ink.Surface, RoundedCornerShape(18.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge, color = Ink.Text)
            SwitchRow(stringResource(R.string.settings_earcons), session.earcons) { session.switchEarcons(it) }
            SwitchRow(stringResource(R.string.settings_haptics), session.haptics) { session.switchHaptics(it) }
            ChoiceRow(stringResource(R.string.settings_reading), stringResource(Labels.reading(session.readLanguagesShown))) { session.nextReadLanguages() }
            BigButton(stringResource(R.string.settings_close), primary = true, onClick = onClose)
        }
    }
}

/** A setting with several values: a tap moves to the next, and the row's text says which is on. */
@Composable
private fun ChoiceRow(label: String, value: String, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Target).clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onNext)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Ink.Iris)
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val on = stringResource(R.string.mode_state_on)
    val off = stringResource(R.string.mode_state_off)
    Row(
        Modifier.fillMaxWidth().heightIn(min = Target).clip(RoundedCornerShape(14.dp))
            .toggleable(value = value, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Ink.Text)
        Box(
            Modifier.size(width = 64.dp, height = 36.dp).clip(RoundedCornerShape(18.dp))
                .background(if (value) Ink.Iris else Ink.BorderStrong),
            contentAlignment = if (value) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.padding(4.dp).size(28.dp).clip(RoundedCornerShape(14.dp)).background(if (value) Ink.OnIris else Ink.Text))
        }
        Spacer(Modifier.size(8.dp))
        // Shown for the sighted half; the switch's own state is what TalkBack reads, once.
        Text(if (value) on else off, Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.bodyMedium, color = Ink.Muted)
    }
}
