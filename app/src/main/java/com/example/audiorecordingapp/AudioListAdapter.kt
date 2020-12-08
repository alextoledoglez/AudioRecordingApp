package com.example.audiorecordingapp

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class AudioListAdapter(private val data: ArrayList<String>) :
    RecyclerView.Adapter<AudioListAdapter.ViewHolder>() {

    private var mediaPlayer: MediaPlayer = MediaPlayer()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false) as View
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioDataSource = data[position]
        val textView = holder.itemView.findViewById(R.id.audio_data_source) as TextView
        textView.text = audioDataSource
        val mediaButton = holder.itemView.findViewById(R.id.media_button) as ImageButton
        mediaButton.setImageResource(updateImageResource())
        mediaButton.setOnClickListener { mediaActionFrom(audioDataSource) }
        val deleteButton = holder.itemView.findViewById(R.id.delete_button) as ImageButton
        deleteButton.setOnClickListener { }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = data.size

    private fun updateImageResource(): Int {
        return if (mediaPlayer.isPlaying) {
            R.drawable.ic_media_stop
        } else {
            R.drawable.ic_media_play
        }
    }

    private fun mediaActionFrom(audioDataSource: String) {
        mediaPlayer.apply {
            try {
                if (isPlaying) {
                    release()
                } else {
                    setDataSource(audioDataSource)
                    prepare()
                    start()
                }
            } catch (exception: Exception) {
                print(exception.stackTrace)
            }
        }
    }
}