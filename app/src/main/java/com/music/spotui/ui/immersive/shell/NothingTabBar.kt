package com.music.spotui.ui.immersive.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.look.rememberReduceMotion
import com.music.spotui.ui.navigation.Routes

/**
 * How far the Search circle stands above the bar it belongs to.
 *
 * Measured off the reference images at the phone's density: the reference bar is 160 px tall and the
 * circle — the same 160 px across — sits with its centre 50 px above the bar's centre, so its top is
 * 50 px above the bar's top. That is 22 dp here, against a bar that is [TabBarContentHeight] tall.
 */
internal val NothingSearchRaise = 22.dp

/**
 * The Nothing look's tab bar: **Home | Search | Your Library**, with Search on a circle of its own in
 * the middle, raised above the two pills.
 *
 * Nothing else about the bar is new. The pills and the circle are the Immersive glass
 * ([immersiveGlassSurface]), and each pill is a [FloatingTabBar] holding one tab, so the lifted
 * indicator, its spring, the haptics and the tap behaviour are the Immersive ones rather than a
 * second implementation that would drift from it. The ink follows the look: the selected tab in
 * [Immersive.Accent] (the Nothing red), the rest at 65% white.
 *
 * When Search is the selected tab neither pill shows an indicator — the same way Immersive's pill
 * disappears while its standalone tab is selected — and only the magnifier takes the accent.
 *
 * The bar reports a height of [NothingSearchRaise] plus [TabBarContentHeight], because the circle
 * overhangs the top. The chrome stacks the mini player above whatever height this returns, which is
 * what puts the mini player just over the circle.
 */
@Composable
internal fun NothingTabBarContent(
    selectedTab: String,
    onTabClick: (Routes) -> Unit,
    modifier: Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val pillShape = remember { RoundedCornerShape(percent = 50) }
    // A factory, not a value: every surface gets its own glass.
    val pillGlass: @Composable () -> Modifier = { Modifier.immersiveGlassSurface(pillShape) }
    val circleGlass: @Composable () -> Modifier = { Modifier.immersiveGlassSurface(CircleShape) }

    Box(modifier.padding(top = NothingSearchRaise)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            NothingTabPill(Routes.Home, selectedTab, onTabClick, reduceMotion, pillGlass, Modifier.weight(1f))
            NothingSearchCircle(
                selectedTab = selectedTab,
                onTabClick = onTabClick,
                glass = circleGlass,
                modifier = Modifier.offset(y = -NothingSearchRaise),
            )
            NothingTabPill(Routes.Library, selectedTab, onTabClick, reduceMotion, pillGlass, Modifier.weight(1f))
        }
    }
}

/** One side of the bar: a glass pill holding a single tab, with the Immersive indicator and ink. */
@Composable
private fun NothingTabPill(
    item: Routes,
    selectedTab: String,
    onTabClick: (Routes) -> Unit,
    reduceMotion: Boolean,
    glass: @Composable () -> Modifier,
    modifier: Modifier,
) {
    val selected = selectedTab == item.route
    val tint = if (selected) Immersive.Accent else Immersive.Text.copy(alpha = 0.65f)
    FloatingTabBar(
        selectedTabKey = selectedTab,
        modifier = modifier.height(TabBarContentHeight),
        tabBarContentModifier = glass,
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = Color.Transparent,
            indicatorColor = Color.White.copy(alpha = 0.12f),
        ),
        sizes = FloatingTabBarDefaults.sizes(
            tabBarContentPadding = PaddingValues(PILL_INSET),
            tabExpandedContentPadding = PaddingValues(vertical = TAB_VERTICAL_PADDING),
        ),
        reduceMotion = reduceMotion,
        contentKey = selectedTab,
    ) {
        tab(
            key = item.route,
            icon = { TabIcon(item, tint) },
            title = {
                Text(
                    text = tabLabel(item),
                    style = TabLabel,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = TAB_ICON_LABEL_GAP),
                )
            },
            onClick = { onTabClick(item) },
        )
    }
}

/** The middle circle: the standalone Search tab, raised, with no label of its own. */
@Composable
private fun NothingSearchCircle(
    selectedTab: String,
    onTabClick: (Routes) -> Unit,
    glass: @Composable () -> Modifier,
    modifier: Modifier,
) {
    val item = Routes.Search
    val selected = selectedTab == item.route
    val tint = if (selected) Immersive.Accent else Immersive.Text.copy(alpha = 0.65f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(TabBarContentHeight)
            .background(Color.Transparent, CircleShape)
            .then(glass())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = tabLabel(item),
                onClick = { onTabClick(item) },
            ),
    ) {
        TabIcon(item, tint)
    }
}

/** The tab's glyph, at the bar's own size, naming itself for TalkBack on the standalone tab. */
@Composable
private fun TabIcon(item: Routes, tint: Color) {
    Icon(
        painter = painterResource(item.icon),
        contentDescription = if (item == Routes.Search) tabLabel(item) else null,
        tint = tint,
        modifier = Modifier.size(25.dp),
    )
}
