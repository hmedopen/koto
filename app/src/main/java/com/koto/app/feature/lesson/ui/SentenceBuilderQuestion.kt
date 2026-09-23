package com.koto.app.feature.lesson.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koto.app.feature.lesson.LessonSession
import com.koto.app.feature.lesson.SentenceValidation
import com.koto.app.feature.lesson.model.Answer
import com.koto.app.feature.lesson.model.JapaneseText
import com.koto.app.feature.lesson.model.LessonText
import com.koto.app.feature.lesson.model.Question
import com.koto.app.ui.theme.KotoColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.abs

/** A tile has one composition and one motion owner for its entire question. */
private class SentenceTileMotion(private val scope: CoroutineScope) {
    var position by mutableStateOf(Offset.Zero)
        private set
    private var target: Offset? = null
    private var animation: Job? = null
    private var held = false

    fun placeAt(destination: IntOffset) {
        val next = Offset(destination.x.toFloat(), destination.y.toFloat())
        if (target == next) return
        val firstPlacement = target == null
        target = next
        if (firstPlacement) position = next
        else if (!held) travelTo(next)
    }

    fun pickUp() { animation?.cancel(); held = true }
    fun dragTo(destination: Offset) { position = destination }
    fun release() { held = false; target?.let(::travelTo) }

    private fun travelTo(destination: Offset) {
        animation?.cancel()
        val start = position
        if (start == destination) return
        animation = scope.launch {
            // Interrupt from the visible position. Completion never mutates game state.
            animate(0f, 1f, animationSpec = tween(210)) { progress, _ ->
                position = start + (destination - start) * progress
            }
        }
    }
}

private data class SentenceDrag(val id: String, val originalOrder: List<String>, val destination: Int, val grabOffset: Offset)

private class SentenceBoardMeasurements {
    var geometry: SentenceBoardGeometry? = null
    var viewport: Rect = Rect.Zero
}

@Composable
internal fun ColumnScope.SentenceBuilderQuestion(
    q: Question.SentenceBuilder,
    session: LessonSession,
    speak: (JapaneseText) -> Unit,
    scrollState: ScrollState? = null,
) {
    // Keying the entire board also makes replay and a new question clean cancellation boundaries.
    key(q.id) {
        Text(q.prompt, fontSize = 23.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center, color = KotoColors.Navy,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Spacer(Modifier.height(16.dp))
        Text("Your sentence", style = MaterialTheme.typography.labelLarge, color = KotoColors.Navy,
            modifier = Modifier.fillMaxWidth())
        Text("Tap to return · Hold and drag to reorder", style = MaterialTheme.typography.bodySmall,
            color = KotoColors.QuietInk, modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 10.dp))
        SentenceBoard(q, session, speak, scrollState)
    }
}

@Composable
private fun SentenceBoard(q: Question.SentenceBuilder, session: LessonSession, speak: (JapaneseText) -> Unit,
    scrollState: ScrollState?) {
    val game = session.sentenceGame
    val scope = rememberCoroutineScope()
    val motions = remember(q.id) { q.tiles.associate { it.id to SentenceTileMotion(scope) } }
    val measurements = remember { SentenceBoardMeasurements() }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val gap = with(density) { 8.dp.roundToPx() }
    val inset = with(density) { 8.dp.roundToPx() }
    val minimumCellWidth = with(density) { (80.dp * fontScale.coerceAtLeast(1f)).roundToPx() }
    val escapeMargin = with(density) { 28.dp.toPx() }
    val edgeZone = with(density) { 48.dp.toPx() }
    val scrollSpeed = with(density) { 360.dp.toPx() }
    val reorderSlop = with(density) { 6.dp.toPx() }
    var drag by remember { mutableStateOf<SentenceDrag?>(null) }
    var dragScrollDirection by remember { mutableIntStateOf(0) }
    val selectedIds by rememberUpdatedState(game.sentenceTileIds)
    val currentDrag by rememberUpdatedState(drag)
    val previewOrder = drag?.let { moving ->
        (moving.originalOrder - moving.id).toMutableList().apply { add(moving.destination, moving.id) }
    } ?: game.sentenceTileIds
    val finishDrag: (Boolean) -> Unit = { commit ->
        drag?.let { moving ->
            if (commit && session.question?.id == q.id && !session.checked &&
                session.sentenceGame.sentenceTileIds == moving.originalOrder) {
                session.moveSentenceTile(moving.id, moving.destination)
            }
            drag = null
            dragScrollDirection = 0
            motions.getValue(moving.id).release()
        }
    }
    val latestFinishDrag by rememberUpdatedState(finishDrag)
    LaunchedEffect(session.checked, game.validationEpoch) { finishDrag(false) }

    val updateDraggedPosition: (Offset) -> Unit = { position ->
        drag?.let { moving ->
            motions.getValue(moving.id).dragTo(position)
            measurements.geometry?.let { geometry ->
                val center = position + Offset(geometry.cellWidth / 2f - inset, geometry.cellHeight / 2f)
                val destination = geometry.targetIndex(center, moving.originalOrder.size)
                if (destination != moving.destination) {
                    fun distanceTo(index: Int): Float {
                        val slot = geometry.sentenceSlot(index)
                        return (center - Offset(slot.x + geometry.cellWidth / 2f, slot.y + geometry.cellHeight / 2f)).getDistance()
                    }
                    // A small dead band keeps a finger resting on a slot boundary from chattering.
                    if (distanceTo(moving.destination) - distanceTo(destination) > reorderSlop) {
                        drag = moving.copy(destination = destination)
                    }
                }
            }
        }
    }
    val latestUpdateDraggedPosition by rememberUpdatedState(updateDraggedPosition)
    LaunchedEffect(drag?.id, dragScrollDirection) {
        val scroll = scrollState ?: return@LaunchedEffect
        if (dragScrollDirection == 0) return@LaunchedEffect
        var lastFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val elapsed = ((frame - lastFrame) / 1_000_000_000f).coerceAtMost(.032f)
            lastFrame = frame
            val moving = currentDrag ?: break
            val geometry = measurements.geometry ?: break
            val viewport = measurements.viewport
            val firstTop = geometry.sentenceTop.toFloat()
            val lastBottom = geometry.sentenceSlot(moving.originalOrder.lastIndex).y + geometry.cellHeight
            val requested = if (dragScrollDirection > 0) {
                (lastBottom - viewport.bottom).coerceIn(0f, scrollSpeed * elapsed)
            } else {
                (firstTop - viewport.top).coerceIn(-scrollSpeed * elapsed, 0f)
            }
            if (abs(requested) < .5f) break
            val consumed = scroll.scrollBy(requested)
            if (abs(consumed) < .5f) break
            // Scrolling changes board coordinates; keep the lifted tile under the stationary finger.
            latestUpdateDraggedPosition(motions.getValue(moving.id).position + Offset(0f, consumed))
        }
    }

    val activate: (String) -> Unit = { id ->
        if (drag == null && !session.checked) {
            if (id in session.sentenceGame.sentenceTileIds) session.removeSentenceTile(id)
            else session.addSentenceTile(id)
            // State responds on release; speech and motion do not gate the next input.
            (q.tiles.first { it.id == id }.text as? LessonText.Japanese)?.let { speak(it.value) }
        }
    }
    val correct = session.checked && game.validation == SentenceValidation.Correct
    val boardColor by animateColorAsState(when {
        correct -> KotoColors.CorrectWash
        game.validation == SentenceValidation.WrongTiles -> KotoColors.WrongWash
        game.validation == SentenceValidation.Missing || game.validation == SentenceValidation.Extra ||
            game.validation == SentenceValidation.WrongOrder -> KotoColors.WarningWash
        else -> KotoColors.SoftGrey
    }, tween(160), label = "Sentence surface")

    Layout(
        modifier = Modifier.fillMaxWidth().testTag("sentence_board").semantics { isTraversalGroup = true }
            .onSizeChanged { if (drag != null) latestFinishDrag(false) }
            .onGloballyPositioned { coordinates ->
                val origin = coordinates.positionInWindow()
                measurements.viewport = coordinates.boundsInWindow().translate(-origin)
            }
            .pointerInput(q.id, session.checked, minimumCellWidth) {
                if (!session.checked) detectDragGesturesAfterLongPress(
                    onDragStart = { pointer ->
                        val geometry = measurements.geometry
                        if (geometry != null && currentDrag == null) {
                            val id = selectedIds.lastOrNull { id ->
                                val position = motions.getValue(id).position
                                Rect(position.x, position.y, position.x + geometry.cellWidth,
                                    position.y + geometry.cellHeight).contains(pointer)
                            }
                            if (id != null) {
                                motions.getValue(id).pickUp()
                                drag = SentenceDrag(id, selectedIds.toList(), selectedIds.indexOf(id),
                                    pointer - motions.getValue(id).position)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        drag?.let { moving ->
                            change.consume()
                            latestUpdateDraggedPosition(change.position - moving.grabOffset)
                            val viewport = measurements.viewport
                            dragScrollDirection = when {
                                change.position.y > viewport.bottom - edgeZone -> 1
                                change.position.y < viewport.top + edgeZone -> -1
                                else -> 0
                            }
                        }
                    },
                    onDragEnd = {
                        val geometry = measurements.geometry
                        val moving = drag
                        val inside = if (geometry != null && moving != null) {
                            val center = motions.getValue(moving.id).position +
                                Offset(geometry.cellWidth / 2f - inset, geometry.cellHeight / 2f)
                            geometry.sentenceBounds.inflate(escapeMargin).contains(center)
                        } else false
                        latestFinishDrag(inside)
                    },
                    onDragCancel = { latestFinishDrag(false) },
                )
            },
        content = {
            // Noninteractive surfaces have no tile semantics. Pool holes never collapse.
            Box(Modifier.background(boardColor, RoundedCornerShape(16.dp)))
            Box(contentAlignment = Alignment.Center) {
                if (previewOrder.isEmpty()) Text("Tap words below to build your sentence",
                    color = KotoColors.QuietInk, style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            }
            SentenceFeedback(game.validation)
            Text("Word bank", style = MaterialTheme.typography.labelLarge, color = KotoColors.QuietInk,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            q.tiles.forEach { tile ->
                Box(Modifier.testTag("sentence_pool_slot_${tile.id}")
                    .background(KotoColors.SoftGrey, RoundedCornerShape(12.dp))
                    .border(1.dp, KotoColors.Hairline, RoundedCornerShape(12.dp)))
            }
            // The moving tile leaves a visible gap in the preview sentence.
            Box(Modifier.alpha(if (drag != null) 1f else 0f)
                .background(KotoColors.BlueWash, RoundedCornerShape(12.dp))
                .border(1.dp, KotoColors.BlueEdge, RoundedCornerShape(12.dp)))
            q.tiles.forEach { tile -> key(tile.id) {
                val index = previewOrder.indexOf(tile.id)
                val selected = index >= 0
                SentenceWordTile(tile, selected, index, previewOrder.size,
                    enabled = !session.checked, lifted = drag?.id == tile.id,
                    validation = if (selected) game.validation else SentenceValidation.Idle,
                    onClick = { activate(tile.id) },
                    move = { destination ->
                        if (drag == null) session.moveSentenceTile(tile.id, destination)
                    })
            } }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val innerWidth = (width - inset * 2).coerceAtLeast(1)
        val columns = ((innerWidth + gap) / (minimumCellWidth + gap)).coerceIn(1, 5)
        val cellWidth = ((innerWidth - (columns - 1) * gap) / columns).coerceAtLeast(1)
        val tileStart = 5 + q.tiles.size
        val tiles = q.tiles.indices.map { index ->
            measurables[tileStart + index].measure(Constraints.fixedWidth(cellWidth))
        }
        val cellHeight = tiles.maxOf { it.height }
        val rows = ceil(q.tiles.size.toFloat() / columns).toInt()
        val gridHeight = rows * cellHeight + (rows - 1) * gap
        val sentenceHeight = gridHeight + inset * 2
        val feedback = measurables[2].measure(Constraints.fixedWidth(width))
        val bankLabel = measurables[3].measure(Constraints.fixedWidth(width))
        val poolTop = sentenceHeight + feedback.height + bankLabel.height
        val geometry = SentenceBoardGeometry(columns, cellWidth, cellHeight, gap, inset, poolTop, q.tiles.size)
        measurements.geometry = geometry
        val backdrop = measurables[0].measure(Constraints.fixed(width, sentenceHeight))
        val hint = measurables[1].measure(Constraints.fixed(width, sentenceHeight))
        val holes = q.tiles.indices.map { measurables[4 + it].measure(Constraints.fixed(cellWidth, cellHeight)) }
        val dragHole = measurables[4 + q.tiles.size].measure(Constraints.fixed(cellWidth, cellHeight))
        layout(width, poolTop + gridHeight + inset) {
            backdrop.place(0, 0)
            hint.place(0, 0)
            feedback.place(0, sentenceHeight)
            bankLabel.place(0, sentenceHeight + feedback.height)
            holes.forEachIndexed { index, hole ->
                val slot = geometry.poolSlot(index)
                hole.place(slot.x + inset, slot.y)
            }
            drag?.let {
                val slot = geometry.sentenceSlot(it.destination)
                dragHole.place(slot.x + inset, slot.y)
            }
            q.tiles.forEachIndexed { index, tile ->
                val sentenceIndex = previewOrder.indexOf(tile.id)
                val slot = if (sentenceIndex >= 0) geometry.sentenceSlot(sentenceIndex) else geometry.poolSlot(index)
                val motion = motions.getValue(tile.id)
                motion.placeAt(IntOffset(slot.x + inset, slot.y))
                val position = motion.position
                tiles[index].placeWithLayer(position.x.roundToInt(), position.y.roundToInt(),
                    zIndex = if (drag?.id == tile.id) 2f else if (sentenceIndex >= 0) 1f else 0f)
            }
        }
    }
}

@Composable
private fun SentenceWordTile(
    tile: Answer, selected: Boolean, index: Int, count: Int, enabled: Boolean,
    lifted: Boolean, validation: SentenceValidation, onClick: () -> Unit, move: (Int) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    var pointerPressed by remember { mutableStateOf(false) }
    val semanticPressed by interaction.collectIsPressedAsState()
    val pressed = pointerPressed || semanticPressed
    val depression by animateFloatAsState(if (pressed && !lifted) 3f else 0f,
        if (pressed) snap() else tween(90), label = "Word press")
    val lift by animateFloatAsState(if (lifted) 1f else 0f, tween(120), label = "Word lift")
    val success = validation == SentenceValidation.Correct
    val warning = validation == SentenceValidation.Missing || validation == SentenceValidation.Extra ||
        validation == SentenceValidation.WrongOrder
    val wrong = validation == SentenceValidation.WrongTiles
    val face by animateColorAsState(when {
        success -> KotoColors.CorrectWash
        wrong -> KotoColors.WrongWash
        warning -> KotoColors.WarningWash
        selected -> KotoColors.LessonBlue
        else -> Color.White
    }, tween(150), label = "Word face")
    val edge = when {
        success -> KotoColors.Correct
        wrong -> KotoColors.Wrong
        warning -> KotoColors.Warning
        selected -> KotoColors.Navy
        else -> KotoColors.BlueEdge
    }
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.padding(bottom = 3.dp)
        .graphicsLayer {
            scaleX = 1f + lift * .045f; scaleY = scaleX
            translationY = -lift * 5.dp.toPx()
            shadowElevation = lift * 6.dp.toPx()
            this.shape = shape
        }
        .testTag(if (selected) "assembled_${tile.id}" else "tile_${tile.id}")
        .pointerInput(enabled) {
            if (!enabled) {
                pointerPressed = false
                return@pointerInput
            }
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    pointerPressed = event.changes.any { it.pressed }
                }
            }
        }
        .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction, indication = null,
            onClickLabel = if (selected) "Return word to bank" else "Add word to sentence", onClick = onClick)
        .semantics(mergeDescendants = true) {
            this.selected = selected
            traversalIndex = if (selected) index.toFloat() else 100f
            stateDescription = when {
                lifted -> "Moving word, position ${index + 1} of $count"
                selected -> "Word ${index + 1} of $count"
                else -> "In word bank"
            }
            if (selected && enabled) customActions = buildList {
                if (index > 0) add(CustomAccessibilityAction("Move earlier") { move(index - 1); true })
                if (index < count - 1) add(CustomAccessibilityAction("Move later") { move(index + 1); true })
            }
        }, propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().graphicsLayer { translationY = 3.dp.toPx() }.background(edge, shape))
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .graphicsLayer { translationY = depression.dp.toPx() }
            .background(face, shape).border(1.dp, edge.copy(alpha = .55f), shape)
            .padding(horizontal = 4.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides if (selected && !success && !warning && !wrong) Color.White else KotoColors.Navy) {
                ContentText(tile.text, 18.sp)
            }
        }
    }
}

@Composable
private fun SentenceFeedback(validation: SentenceValidation) {
    val message = when (validation) {
        SentenceValidation.Missing -> "The sentence is missing words."
        SentenceValidation.Extra -> "The sentence has an extra word."
        SentenceValidation.WrongOrder -> "The words are in the wrong order."
        SentenceValidation.WrongTiles -> "That sentence is not correct."
        SentenceValidation.Correct -> "Nicely done!"
        SentenceValidation.Idle -> ""
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
        // Measure the longest feedback at the user's font scale, even while idle.
        // Checking never moves the pool, and large type never clips its message.
        Text("Check your words. Return any that don’t belong.", style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).alpha(0f).clearAndSetSemantics {})
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = when (validation) {
                SentenceValidation.Correct -> KotoColors.Correct
                SentenceValidation.WrongTiles -> KotoColors.Wrong
                else -> KotoColors.Warning
            },
            modifier = Modifier.matchParentSize().testTag("sentence_feedback").semantics { liveRegion = LiveRegionMode.Polite })
    }
}
