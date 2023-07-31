package com.dzirbel.kotify.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

object KotifyTypography {
    val Default by lazy {
        FontFamily(
            Font("fonts/Noto_Sans/NotoSans-Thin.ttf", FontWeight.Thin),
            Font("fonts/Noto_Sans/NotoSans-ExtraLight.ttf", FontWeight.ExtraLight),
            Font("fonts/Noto_Sans/NotoSans-Light.ttf", FontWeight.Light),
            Font("fonts/Noto_Sans/NotoSans-Regular.ttf", FontWeight.Normal),
            Font("fonts/Noto_Sans/NotoSans-Medium.ttf", FontWeight.Medium),
            Font("fonts/Noto_Sans/NotoSans-SemiBold.ttf", FontWeight.SemiBold),
            Font("fonts/Noto_Sans/NotoSans-Bold.ttf", FontWeight.Bold),
            Font("fonts/Noto_Sans/NotoSans-ExtraBold.ttf", FontWeight.ExtraBold),
            Font("fonts/Noto_Sans/NotoSans-Black.ttf", FontWeight.Black),

            Font("fonts/Noto_Sans/NotoSans-ThinItalic.ttf", FontWeight.Thin, FontStyle.Italic),
            Font("fonts/Noto_Sans/NotoSans-ExtraLightItalic.ttf", FontWeight.ExtraLight, FontStyle.Italic),
            Font("fonts/Noto_Sans/NotoSans-LightItalic.ttf", FontWeight.Light, FontStyle.Italic),
            Font("fonts/Noto_Sans/NotoSans-Italic.ttf", FontWeight.Normal, FontStyle.Italic),
            Font("fonts/Noto_Sans/NotoSans-MediumItalic.ttf", FontWeight.Medium, FontStyle.Italic),
            Font("fonts/Noto_Sans/NotoSans-SemiBoldItalic.ttf", FontWeight.SemiBold, FontStyle.Italic),
            Font("fonts/Noto_Sans/NotoSans-BoldItalic.ttf", FontWeight.Bold, FontStyle.Italic),
            Font("fonts/Noto_Sans/NotoSans-ExtraBoldItalic.ttf", FontWeight.ExtraBold, FontStyle.Italic),
            Font("fonts/Noto_Sans/NotoSans-BlackItalic.ttf", FontWeight.Black, FontStyle.Italic),
        )
    }

    val Monospace by lazy {
        FontFamily(
            Font("fonts/Noto_Sans_Mono/NotoSansMono-Thin.ttf", FontWeight.Thin),
            Font("fonts/Noto_Sans_Mono/NotoSansMono-ExtraLight.ttf", FontWeight.ExtraLight),
            Font("fonts/Noto_Sans_Mono/NotoSansMono-Light.ttf", FontWeight.Light),
            Font("fonts/Noto_Sans_Mono/NotoSansMono-Regular.ttf", FontWeight.Normal),
            Font("fonts/Noto_Sans_Mono/NotoSansMono-Medium.ttf", FontWeight.Medium),
            Font("fonts/Noto_Sans_Mono/NotoSansMono-SemiBold.ttf", FontWeight.SemiBold),
            Font("fonts/Noto_Sans_Mono/NotoSansMono-Bold.ttf", FontWeight.Bold),
            Font("fonts/Noto_Sans_Mono/NotoSansMono-ExtraBold.ttf", FontWeight.ExtraBold),
            Font("fonts/Noto_Sans_Mono/NotoSansMono-Black.ttf", FontWeight.Black),
        )
    }
}
