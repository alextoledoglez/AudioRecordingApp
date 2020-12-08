package com.example.audiorecordingapp

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*

class AudioListAdapter(private val data: ArrayList<String>) :
    RecyclerView.Adapter<AudioListAdapter.ViewHolder>(), MediaPlayer.OnPreparedListener {

    private var audioListAdapter: AudioListAdapter = this
    private var mediaPlayer: MediaPlayer = MediaPlayer()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false) as View
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioDataSource = data[position]
        val textView = holder.itemView.findViewById(R.id.audio_data_source) as TextView
        textView.text = audioDataSource
        val mediaButton = holder.itemView.findViewById(R.id.media_button) as ImageButton
        mediaButton.setImageResource(updateImageResource())
        mediaButton.setOnClickListener { mediaActionFrom(audioDataSource) }
        val deleteButton = holder.itemView.findViewById(R.id.delete_button) as ImageButton
        deleteButton.setOnClickListener {
            val deleted = File(audioDataSource).delete()
            if (deleted) {
                data.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, data.size)
            }
        }
    }

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
                    reset()
                    setDataSource(audioDataSource)
                    setOnPreparedListener(audioListAdapter)
                    prepareAsync()
                }
            } catch (exception: Exception) {
                print(exception.stackTrace)
            }
        }
    }

    override fun onPrepared(player: MediaPlayer) {
        player.start()
    }
}