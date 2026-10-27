/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.conciergetestapp.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme

/**
 * A horizontal carousel of simple cards that mirrors the out-of-the-box product cards — the same
 * `--product-card-*` theme values and the same image / badge / title / description / CTA layout —
 * and adds the one thing the built-in card can't do: a button sends a card action instead of
 * opening a link. Buttons are disabled while [gate] can't send.
 *
 * The row is measured at [IntrinsicSize.Max] so every card is as tall as the tallest one, and it
 * scrolls horizontally only: a card never nests a vertical scroll inside the transcript.
 *
 * Presses and their outcome live in [runtime].
 */
@Composable
fun SimpleCardContent(runtime: SimpleCardRuntime, gate: CardGate = CardGate.OPEN, modifier: Modifier = Modifier) {
    val model = runtime.model
    val style = SimpleCardTheme.current
    val pending by runtime.pendingPress.collectAsState()
    val error by runtime.error.collectAsState()
    val unconfirmed by runtime.unconfirmed.collectAsState()
    val canPress = pending == null && gate.canSend

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(style.carouselItemSpacing),
                modifier = Modifier
                    .padding(vertical = style.carouselVerticalPadding)
                    .height(IntrinsicSize.Max)
            ) {
                model.cards.forEach { card ->
                    SimpleProductCard(
                        card = card,
                        showImage = model.showImages,
                        style = style,
                        enabled = canPress && card.enabled,
                        pendingKey = pending,
                        unconfirmed = unconfirmed,
                        pressKey = { cta -> runtime.pressKey(card, cta) },
                        onPress = { cta -> runtime.select(card, cta) }
                    )
                }
                Spacer(Modifier.width(style.carouselTrailingPadding))
            }
        }

        error?.let {
            Text(
                it,
                color = ConciergeTheme.colors.error,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

/**
 * One card, laid out like the SDK's extended product card: optional image slot with a badge
 * pinned bottom-start, title, description, then the detail line and buttons anchored to the
 * bottom. Tapping the card body does what its first button does, as it does out of the box.
 */
@Composable
private fun SimpleProductCard(
    card: SimpleCard,
    showImage: Boolean,
    style: SimpleCardStyle,
    enabled: Boolean,
    pendingKey: String?,
    unconfirmed: Set<String>,
    pressKey: (SimpleCardCta) -> String,
    onPress: (SimpleCardCta) -> Unit
) {
    val primary = card.ctas.firstOrNull()
    val primaryEnabled = enabled && primary != null && pressKey(primary) !in unconfirmed
    Column(
        modifier = Modifier
            .width(style.cardWidth)
            .heightIn(min = if (showImage) style.cardMinHeight else 0.dp, max = style.cardMaxHeight)
            .fillMaxHeight()
            .clip(style.cardShape)
            .background(style.cardBackgroundColor)
            .border(1.dp, style.cardOutlineColor, style.cardShape)
            .clickable(
                enabled = primaryEnabled,
                onClickLabel = primary?.let { "${it.label}: ${card.title}" },
                role = Role.Button,
                onClick = { primary?.let(onPress) }
            )
            .alpha(if (card.enabled) 1f else style.disabledAlpha)
    ) {
        if (showImage) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = style.imageTopPadding)
                    .height(style.imageHeight)
            ) {
                SimpleCardImage(
                    url = card.imageUrl,
                    style = style,
                    modifier = Modifier.width(style.imageWidth).height(style.imageHeight)
                )
                card.badge?.let {
                    Badge(
                        text = it,
                        style = style,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = style.badgeInset, bottom = style.badgeInset)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = style.contentPadding,
                    end = style.contentPadding,
                    // The image already supplies breathing room above the text; without one the
                    // card's own padding has to, and the card-sized top gap looks like a mistake.
                    top = if (showImage) style.contentPaddingTop else style.contentPadding
                )
        ) {
            // Without an image slot the badge has nowhere to sit, so it leads the text instead.
            if (!showImage) {
                card.badge?.let {
                    Badge(it, style, Modifier.padding(bottom = style.titleSubtitleSpacing))
                }
            }
            Text(
                text = card.title,
                color = style.titleColor,
                fontSize = style.titleFontSize,
                fontWeight = style.titleFontWeight,
                lineHeight = style.titleLineHeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (card.description.isNotEmpty()) {
                Text(
                    text = card.description,
                    color = style.subtitleColor,
                    fontSize = style.subtitleFontSize,
                    fontWeight = style.subtitleFontWeight,
                    lineHeight = style.subtitleLineHeight,
                    letterSpacing = style.subtitleLetterSpacing,
                    maxLines = style.descriptionMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = style.titleSubtitleSpacing)
                )
            }
        }

        // Leftover card height above the detail line and the buttons, as in the built-in card.
        Spacer(modifier = Modifier.weight(1f))

        card.detail?.let {
            Text(
                text = it,
                color = style.detailColor,
                fontSize = style.detailFontSize,
                fontWeight = style.detailFontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = style.contentPadding, end = style.contentPadding, top = style.sectionSpacing)
            )
        }

        if (card.ctas.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(style.ctaSpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = style.contentPadding, end = style.contentPadding, top = style.ctaTopSpacing)
            ) {
                card.ctas.forEachIndexed { index, cta ->
                    CtaButton(
                        cta = cta,
                        style = style,
                        // The first button is the card's own action, so it leads; any other is
                        // outlined, the way a secondary action reads everywhere else in the chat.
                        primary = index == 0,
                        enabled = enabled && pressKey(cta) !in unconfirmed,
                        pending = pendingKey == pressKey(cta),
                        // A lone button hugs its label; two share the card evenly.
                        modifier = if (card.ctas.size > 1) Modifier.weight(1f) else Modifier.wrapContentWidth(),
                        onPress = { onPress(cta) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(style.contentPaddingBottom))
    }
}

@Composable
private fun Badge(text: String, style: SimpleCardStyle, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .wrapContentWidth(unbounded = true)
            .background(style.badgeBackgroundColor, style.badgeShape)
            .padding(horizontal = style.badgePaddingHorizontal, vertical = style.badgePaddingVertical)
    ) {
        Text(
            text = text,
            color = style.badgeTextColor,
            fontSize = style.badgeFontSize,
            fontWeight = style.badgeFontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CtaButton(
    cta: SimpleCardCta,
    style: SimpleCardStyle,
    primary: Boolean,
    enabled: Boolean,
    pending: Boolean,
    modifier: Modifier,
    onPress: () -> Unit
) {
    val label = if (pending) cta.pendingLabel else cta.label
    val outline = if (primary) Modifier else Modifier.border(style.secondaryCtaBorderWidth, style.secondaryCtaBorderColor, style.ctaShape)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(style.ctaShape)
            .background(if (primary) style.ctaBackgroundColor else style.secondaryCtaBackgroundColor)
            .then(outline)
            .clickable(enabled = enabled, onClickLabel = label, role = Role.Button, onClick = onPress)
            .padding(horizontal = style.ctaHorizontalPadding, vertical = style.ctaVerticalPadding)
    ) {
        Text(
            text = label,
            color = if (primary) style.ctaTextColor else style.secondaryCtaTextColor,
            fontSize = style.ctaFontSize,
            fontWeight = style.ctaFontWeight,
            lineHeight = style.ctaFontSize * 1.4f,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Loads [url] through the host's `LocalImageProvider`; shows a flat placeholder until ready. */
@Composable
private fun SimpleCardImage(url: String?, style: SimpleCardStyle, modifier: Modifier = Modifier) {
    CardImage(url, contentDescription = null, modifier = modifier.clip(style.imageShape).background(style.imagePlaceholderColor))
}
