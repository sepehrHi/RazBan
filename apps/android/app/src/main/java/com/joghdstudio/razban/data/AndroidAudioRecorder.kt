package com.joghdstudio.razban.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AndroidAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    fun start(outputFile: File) {
        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            Log.d("AndroidAudioRecorder", "Recording started successfully to path: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("AndroidAudioRecorder", "Failed to start recording", e)
        }
    }

    fun stop() {
        try {
            recorder?.stop()
            Log.d("AndroidAudioRecorder", "Recording stopped successfully")
        } catch (e: Exception) {
            Log.e("AndroidAudioRecorder", "Error stopping recorder", e)
        } finally {
            recorder?.release()
            recorder = null
        }
    }
}
