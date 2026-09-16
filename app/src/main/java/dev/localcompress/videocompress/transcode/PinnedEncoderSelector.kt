package dev.localcompress.videocompress.transcode

import android.media.MediaCodecInfo
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EncoderSelector
import com.google.common.collect.ImmutableList

/**
 * Forces Media3's [DefaultEncoderFactory][androidx.media3.transformer.DefaultEncoderFactory] to
 * use one specific codec instance (as picked on the codec-diagnostics screen) instead of whichever
 * one Media3 judges "best". Falls back to the normal candidate list if the pinned codec has
 * disappeared (e.g. a different device) or doesn't support the requested mime type.
 */
@OptIn(UnstableApi::class)
class PinnedEncoderSelector(private val pinnedEncoderName: String?) : EncoderSelector {
    override fun selectEncoderInfos(mimeType: String): ImmutableList<MediaCodecInfo> {
        val defaults = EncoderSelector.DEFAULT.selectEncoderInfos(mimeType)
        if (pinnedEncoderName == null) return defaults
        val pinned = defaults.filter { it.name == pinnedEncoderName }
        return if (pinned.isNotEmpty()) ImmutableList.copyOf(pinned) else defaults
    }
}
