package com.example.audiorecordingapp

import android.media.MediaRecorder
import java.io.IOException

class AudioRecorder(outputFileName: String) : MediaRecorder() {

    private val recorder: MediaRecorder = MediaRecorder()

    init {
        recorder.setAudioSource(AudioSource.MIC)
        recorder.setOutputFormat(OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(AudioEncoder.AMR_NB)
        recorder.setOutputFile(outputFileName)
    }

    fun startRecording() {
        try {
            recorder.prepare()
        } catch (exception: IOException) {
            print(exception.stackTrace)
        }
        recorder.start()
    }

    fun stopRecording() {
        recorder.stop()
        recorder.release()
    }
}