package com.fluffnark.motoringdashboard.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.fluffnark.motoringdashboard.R

private data class ConceptProgram(
    val id: String,
    val title: String,
    val subtitle: String,
    val program: String,
)

class ConceptMediaService : MediaBrowserServiceCompat() {
    private val programs = listOf(
        ConceptProgram("desert-modern", "Desert Modern", "Design Signal No. 1", "Morning Program"),
        ConceptProgram("walnut-brass", "Walnut & Brass", "Design Signal No. 2", "Local Library"),
        ConceptProgram("night-geometry", "Night Geometry", "Design Signal No. 3", "Evening Radio"),
    )
    private lateinit var mediaSession: MediaSessionCompat
    private var selectedIndex = 0

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "MotoringDashboard").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPrepare() = select(selectedIndex, PlaybackStateCompat.STATE_PAUSED)
                override fun onPlay() = setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                override fun onPause() = setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                override fun onStop() = setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
                override fun onSkipToNext() = select((selectedIndex + 1) % programs.size, PlaybackStateCompat.STATE_PLAYING)
                override fun onSkipToPrevious() = select((selectedIndex - 1 + programs.size) % programs.size, PlaybackStateCompat.STATE_PLAYING)
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    val index = programs.indexOfFirst { it.id == mediaId }.takeIf { it >= 0 } ?: return
                    select(index, PlaybackStateCompat.STATE_PLAYING)
                }
                override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
                    val index = programs.indexOfFirst { it.id == mediaId }.takeIf { it >= 0 } ?: return
                    select(index, PlaybackStateCompat.STATE_PAUSED)
                }
                override fun onPlayFromSearch(query: String?, extras: Bundle?) {
                    val normalized = query.orEmpty().trim()
                    val index = programs.indexOfFirst {
                        it.title.contains(normalized, ignoreCase = true) ||
                            it.program.contains(normalized, ignoreCase = true)
                    }.takeIf { it >= 0 } ?: selectedIndex
                    select(index, PlaybackStateCompat.STATE_PLAYING)
                }
            })
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
        select(0, PlaybackStateCompat.STATE_PAUSED)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot = BrowserRoot(ROOT_ID, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        if (parentId != ROOT_ID) {
            result.sendResult(mutableListOf())
            return
        }
        val iconUri = Uri.parse("android.resource://$packageName/${R.drawable.ic_launcher}")
        result.sendResult(programs.map { program ->
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(program.id)
                    .setTitle(program.title)
                    .setSubtitle(program.subtitle)
                    .setDescription(program.program)
                    .setIconUri(iconUri)
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE,
            )
        }.toMutableList())
    }

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }

    private fun select(index: Int, state: Int) {
        selectedIndex = index
        val program = programs[index]
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, program.id)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, program.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, program.subtitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, program.program)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork(index))
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artwork(index))
                .build()
        )
        setPlaybackState(state)
    }

    private fun setPlaybackState(state: Int) {
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
            PlaybackStateCompat.ACTION_PREPARE or
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f)
                .build()
        )
    }

    private fun artwork(variation: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(36, 35, 31))

        paint.color = Color.rgb(232, 215, 184)
        canvas.drawCircle(160f, 160f, 112f, paint)
        paint.color = when (variation) {
            1 -> Color.rgb(123, 128, 96)
            2 -> Color.rgb(181, 151, 89)
            else -> Color.rgb(215, 101, 59)
        }
        canvas.drawArc(RectF(64f, 64f, 256f, 256f), 205f, 235f, true, paint)
        paint.color = Color.rgb(36, 35, 31)
        paint.strokeWidth = 13f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(160f, 92f, 160f, 164f, paint)
        canvas.drawLine(160f, 164f, 218f, 202f, paint)
        return bitmap
    }

    companion object {
        private const val ROOT_ID = "motoring-root"
    }
}
