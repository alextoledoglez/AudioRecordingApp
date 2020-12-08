package com.example.audiorecordingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSION = 200
    }

    private var audioRecorder: AudioRecorder? = null
    private var audioFiles: ArrayList<String> = ArrayList<String>()

    private var recyclerView: RecyclerView? = null
    private var recyclerAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerLayoutManager: RecyclerView.LayoutManager? = null

    private var permissionGranted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        recyclerAdapter = AudioListAdapter(audioFiles)
        recyclerLayoutManager = LinearLayoutManager(this)
        recyclerView = findViewById<RecyclerView>(R.id.audios_recycler_view).apply {
            adapter = recyclerAdapter
            layoutManager = recyclerLayoutManager
        }

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION)
        val audioPath = "${externalCacheDir?.absolutePath}/"
        val directory = File(audioPath)
        val files: Array<File>? = directory.listFiles()
        files?.forEach { file -> addAudioFileName(fileName = file.path) }

        val recordButton = findViewById<FloatingActionButton>(R.id.fab)
        recordButton.setOnTouchListener { view, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_BUTTON_PRESS -> {

                    val audioUUID = UUID.randomUUID().toString()
                    val audioName = "_audio_record.3gp"
                    val outputAudioFileName = audioPath + audioUUID + audioName
                    addAudioFileName(outputAudioFileName)

                    audioRecorder = AudioRecorder(outputAudioFileName)
                    view.setOnLongClickListener {
                        showToastText(baseContext, getString(R.string.start_recording))
                        audioRecorder?.startRecording()
                        true
                    }

                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_BUTTON_RELEASE -> {
                    showToastText(view.context, getString(R.string.stop_recording))
                    audioRecorder?.stopRecording()
                }
            }

            false
        }
    }

    override fun onStop() {
        super.onStop()
        audioRecorder?.release()
        audioRecorder = null
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

    private fun addAudioFileName(fileName: String) {
        if (!audioFiles.contains(fileName)) {
            audioFiles.add(fileName)
            recyclerAdapter?.notifyDataSetChanged()
        }
    }

    private fun showToastText(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}