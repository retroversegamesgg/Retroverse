package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeBanner(
    modifier: Modifier = Modifier,
    images: List<Int>,           // seus R.drawable.xxx
    autoSlideDuration: Long = 3000L
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { images.size }   // <- usa sua assinatura
    )

    val scope = rememberCoroutineScope()

    // Auto slide
    LaunchedEffect(pagerState.currentPage, images.size) {
        delay(autoSlideDuration)
        val nextPage = (pagerState.currentPage + 1) % images.size
        scope.launch { pagerState.animateScrollToPage(nextPage) }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            pageSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 8.dp)
            ) { page ->
            Image(
                painter = painterResource(id = images[page]),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
            )
        }

        // Indicadores
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(images.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}
