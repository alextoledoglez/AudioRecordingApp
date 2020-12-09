package com.example.audiorecordingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity(), AudioRecorderView.RecordingListener {

    companion object {
        private const val REQUEST_PERMISSION = 200
    }

    private var audioRecorder: AudioRecorder? = null
    private var audioRecorderView: AudioRecorderView? = null
    private var audioFiles: ArrayList<AudioFile> = ArrayList<AudioFile>()

    private var recyclerView: RecyclerView? = null
    private var recyclerAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerLayoutManager: RecyclerView.LayoutManager? = null

    private var audioPath: String? = null
    private var audioFile: AudioFile? = null

    private var permissionGranted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        audioRecorderView = AudioRecorderView()
        audioRecorderView?.initView(findViewById<View>(R.id.main_frame) as FrameLayout)
        audioRecorderView?.setRecordingListener(this)

        recyclerAdapter = AudioListAdapter(audioFiles)
        recyclerLayoutManager = LinearLayoutManager(this)
        recyclerView = findViewById<RecyclerView>(R.id.audios_recycler_view).apply {
            adapter = recyclerAdapter
            layoutManager = recyclerLayoutManager
        }

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION)
        audioPath = "${externalCacheDir?.absolutePath}/"
        audioPath?.let {
            val directory = File(it)
            val files: Array<File>? = directory.listFiles()
            files?.forEach { file -> addAudioFileDto(AudioFile(file.name, file.path)) }
        }

        audioRecorderView?.getMessageView()?.requestFocus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val requestCodeMatch = (requestCode == REQUEST_PERMISSION)
        permissionGranted = (requestCodeMatch && grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
        if (!permissionGranted) finish()
    }

    override fun onStop() {
        super.onStop()
        audioRecorder?.release()
        audioRecorder = null
    }

    override fun onRecordingStarted() {
        showToastText(getString(R.string.recording_started))
        val fileName = "AUD_" + Calendar.getInstance().timeInMillis.toString() + ".3gp"
        val filePath = audioPath + fileName
        audioFile = AudioFile(fileName, filePath)
        audioRecorder = AudioRecorder(filePath)
        audioRecorder?.startRecording()
    }

    override fun onRecordingLocked() {
        showToastText(getString(R.string.recording_locked))
    }

    override fun onRecordingCompleted() {
        showToastText(getString(R.string.recording_complete))
        audioFile?.let {
            addAudioFileDto(audioFile = it)
            audioRecorder?.stopRecording()
        }
    }

    override fun onRecordingCanceled() {
        showToastText(getString(R.string.recording_cancelled))
        audioRecorder?.stopRecording()
    }


    private fun addAudioFileDto(audioFile: AudioFile) {
        val noneMatch = audioFiles.none { it.name == audioFile.name }
        if (noneMatch) {
            audioFiles.add(audioFile)
            recyclerAdapter?.notifyDataSetChanged()
        }
    }

    private fun showToastText(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}