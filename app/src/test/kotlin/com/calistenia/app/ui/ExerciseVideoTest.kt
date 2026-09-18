package com.calistenia.app.ui

import com.calistenia.app.data.ExerciseSeed
import com.calistenia.app.data.ExerciseVideo
import com.calistenia.app.data.ExerciseVideos
import org.junit.Assert.*
import org.junit.Test

class ExerciseVideoTest {
    @Test fun `all existing exercises have a distinct attributed demonstration`() {
        assertEquals(ExerciseSeed.exercises.map { it.id }.toSet(), ExerciseVideos.all.keys)
        assertEquals(36, ExerciseVideos.all.values.map { it.youtubeId }.toSet().size)
        ExerciseVideos.all.values.forEach {
            assertTrue(it.author.isNotBlank())
            assertTrue(it.watchUrl.startsWith("https://www.youtube.com/watch?v="))
        }
    }

    @Test fun `video identifiers cannot inject script or redirect to another host`() {
        listOf("", "../anything", "https://evil.example", "';alert(1)//").forEach { id ->
            assertThrows(IllegalArgumentException::class.java) { ExerciseVideo(id, "author") }
        }
    }

    @Test fun `embed supplies app identity and waits for user playback`() {
        val html = videoHtml(ExerciseVideos.all.getValue("wall_push_up"))
        assertTrue(html.startsWith("<!doctype html>"))
        assertTrue(html.contains("videoId: 'QpMTk21EmaM'"))
        assertTrue(html.contains("autoplay:0"))
        assertTrue(html.contains("controls:1"))
        assertTrue(html.contains("origin:'${VIDEO_ORIGIN.removeSuffix("/")}'"))
        assertTrue(html.contains("onError:"))
    }

    @Test fun `player reports readiness and video failures to native UI`() {
        val states = mutableListOf<VideoStatus>()
        val bridge = VideoStatusBridge { states.add(it) }
        bridge.ready()
        bridge.error()
        assertEquals(listOf(VideoStatus.READY, VideoStatus.ERROR), states)
    }
}
