package com.example.audiorecordingapp

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.view.animation.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs


class AudioRecorderView {

    companion object {
        private const val DEFAULT_COORDINATE_VALUE = 0F
        private const val MID_COORDINATE_VALUE = 0.6F
        private const val MAX_COORDINATE_VALUE = 1F
        private const val DIRECTION_OFF_SET = DEFAULT_COORDINATE_VALUE
    }

    enum class UserBehaviour {
        CANCELING, LOCKING, NONE
    }

    enum class RecordingBehaviour {
        CANCELED, LOCKED, LOCK_DONE, RELEASED
    }

    interface RecordingListener {
        fun onRecordingStarted()
        fun onRecordingLocked()
        fun onRecordingCompleted()
        fun onRecordingCanceled()
    }

    private var imageViewAudio: View? = null
    private var imageViewLockArrow: View? = null
    private var imageViewLock: View? = null
    private var imageViewMic: View? = null
    private var dustin: View? = null
    private var dustinCover: View? = null
    private var imageViewStop: View? = null
    private var imageViewSend: View? = null
    private var layoutDustin: View? = null
    private var layoutMessage: View? = null
    private var layoutSlideCancel: View? = null
    private var layoutLock: View? = null
    private var layoutEffect1: View? = null
    private var layoutEffect2: View? = null

    private var editTextMessage: EditText? = null

    private var timeText: TextView? = null
    private var textViewSlide: TextView? = null

    private var stop: ImageView? = null
    private var audio: ImageView? = null
    private var send: ImageView? = null

    private var animBlink: Animation? = null
    private var animJump: Animation? = null
    private var animJumpFast: Animation? = null

    private var isDeleting = false
    private var stopTrackingAction = false
    private var handler: Handler? = null

    private var audioTotalTime = 0
    private var timerTask: TimerTask? = null
    private var audioTimer: Timer? = null
    private var timeFormatter: SimpleDateFormat? = null

    private var lastX = DEFAULT_COORDINATE_VALUE
    private var lastY = DEFAULT_COORDINATE_VALUE
    private var firstX = DEFAULT_COORDINATE_VALUE
    private var firstY = DEFAULT_COORDINATE_VALUE

    private var duration = 100L

    private var cancelOffset = DEFAULT_COORDINATE_VALUE
    private var lockOffset = DEFAULT_COORDINATE_VALUE

    private var dp = DEFAULT_COORDINATE_VALUE
    private var isLocked = false
    private var userBehaviour = UserBehaviour.NONE
    private var recordingListener: RecordingListener? = null

    var isLayoutDirectionRightToLeft = false

    var screenWidth = 0
    var screenHeight = 0

    private var context: Context? = null

    fun initView(view: ViewGroup?) {
        if (view == null) {
            return
        }
        context = view.context
        view.removeAllViews()
        view.addView(LayoutInflater.from(view.context).inflate(R.layout.recorder_view, null))
        timeFormatter = SimpleDateFormat("m:ss", Locale.getDefault())
        val displayMetrics = view.context.resources.displayMetrics
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
        isLayoutDirectionRightToLeft = false
        editTextMessage = view.findViewById(R.id.editTextMessage)
        send = view.findViewById(R.id.imageSend)
        stop = view.findViewById(R.id.imageStop)
        audio = view.findViewById(R.id.imageAudio)
        imageViewAudio = view.findViewById(R.id.imageViewAudio)
        imageViewStop = view.findViewById(R.id.imageViewStop)
        imageViewSend = view.findViewById(R.id.imageViewSend)
        imageViewLock = view.findViewById(R.id.imageViewLock)
        imageViewLockArrow = view.findViewById(R.id.imageViewLockArrow)
        layoutDustin = view.findViewById(R.id.layoutDustin)
        layoutMessage = view.findViewById(R.id.layoutMessage)
        textViewSlide = view.findViewById(R.id.textViewSlide)
        timeText = view.findViewById(R.id.textViewTime)
        layoutSlideCancel = view.findViewById(R.id.layoutSlideCancel)
        layoutEffect2 = view.findViewById(R.id.layoutEffect2)
        layoutEffect1 = view.findViewById(R.id.layoutEffect1)
        layoutLock = view.findViewById(R.id.layoutLock)
        imageViewMic = view.findViewById(R.id.imageViewMic)
        dustin = view.findViewById(R.id.dustin)
        dustinCover = view.findViewById(R.id.dustin_cover)
        handler = Handler(Looper.getMainLooper())
        dp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            view.context.resources.displayMetrics
        )
        animBlink = AnimationUtils.loadAnimation(
            view.context,
            R.anim.blink
        )
        animJump = AnimationUtils.loadAnimation(
            view.context,
            R.anim.jump
        )
        animJumpFast = AnimationUtils.loadAnimation(
            view.context,
            R.anim.jump_fast
        )
        setupRecording()
    }

    fun setRecordingListener(recordingListener: RecordingListener?) {
        this.recordingListener = recordingListener
    }

    fun getMessageView(): EditText? {
        return editTextMessage
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRecording() {
        imageViewSend?.animate()?.setXYScales(DEFAULT_COORDINATE_VALUE)
            ?.setDuration(duration)
            ?.setInterpolator(LinearInterpolator())?.start()

        editTextMessage!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString().trim { it <= ' ' }.isEmpty()) {
                    if (imageViewSend?.visibility !== View.GONE) {
                        imageViewSend?.visibility = View.GONE
                        imageViewSend?.animate()?.setXYScales(DEFAULT_COORDINATE_VALUE)
                            ?.setDuration(duration)
                            ?.setInterpolator(LinearInterpolator())?.start()
                    }
                } else {
                    if (imageViewSend?.visibility !== View.VISIBLE && !isLocked) {
                        imageViewSend?.visibility = View.VISIBLE
                        imageViewSend?.animate()?.setXYScales(MAX_COORDINATE_VALUE)
                            ?.setDuration(duration)
                            ?.setInterpolator(LinearInterpolator())?.start()
                    }
                }
            }
        })

        imageViewAudio?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                if (isDeleting) {
                    return true
                }
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    cancelOffset = (screenWidth / 2.8).toFloat()
                    lockOffset = (screenWidth / 2.5).toFloat()
                    if (firstX == DEFAULT_COORDINATE_VALUE) {
                        firstX = motionEvent.rawX
                    }
                    if (firstY == DEFAULT_COORDINATE_VALUE) {
                        firstY = motionEvent.rawY
                    }
                    startRecord()
                } else if (motionEvent.action == MotionEvent.ACTION_UP
                    || motionEvent.action == MotionEvent.ACTION_CANCEL
                ) {
                    if (motionEvent.action == MotionEvent.ACTION_UP) {
                        stopRecording(RecordingBehaviour.RELEASED)
                    }
                } else if (motionEvent.action == MotionEvent.ACTION_MOVE) {
                    if (stopTrackingAction) {
                        return true
                    }
                    var direction = UserBehaviour.NONE
                    val motionX = abs(firstX - motionEvent.rawX)
                    val motionY: Float = abs(firstY - motionEvent.rawY)
                    if (if (isLayoutDirectionRightToLeft) motionX > DIRECTION_OFF_SET && lastX > firstX && lastY > firstY else motionX > DIRECTION_OFF_SET && lastX < firstX && lastY < firstY) {
                        if (if (isLayoutDirectionRightToLeft) motionX > motionY && lastX > firstX else motionX > motionY && lastX < firstX) {
                            direction = UserBehaviour.CANCELING
                        } else if (motionY > motionX && lastY < firstY) {
                            direction = UserBehaviour.LOCKING
                        }
                    } else if (if (isLayoutDirectionRightToLeft) motionX > motionY && motionX > DIRECTION_OFF_SET && lastX > firstX else motionX > motionY && motionX > DIRECTION_OFF_SET && lastX < firstX) {
                        direction = UserBehaviour.CANCELING
                    } else if (motionY > motionX && motionY > DIRECTION_OFF_SET && lastY < firstY) {
                        direction = UserBehaviour.LOCKING
                    }
                    if (direction == UserBehaviour.CANCELING) {
                        if (userBehaviour == UserBehaviour.NONE || motionEvent.rawY + (imageViewAudio?.width
                                ?.div(2)!!) > firstY
                        ) {
                            userBehaviour = UserBehaviour.CANCELING
                        }
                        if (userBehaviour == UserBehaviour.CANCELING) {
                            translateX(-(firstX - motionEvent.rawX))
                        }
                    } else if (direction == UserBehaviour.LOCKING) {
                        if (userBehaviour == UserBehaviour.NONE || motionEvent.rawX + (imageViewAudio?.width
                                ?.div(2)!!) > firstX
                        ) {
                            userBehaviour = UserBehaviour.LOCKING
                        }
                        if (userBehaviour == UserBehaviour.LOCKING) {
                            translateY(-(firstY - motionEvent.rawY))
                        }
                    }
                    lastX = motionEvent.rawX
                    lastY = motionEvent.rawY
                }
                view.onTouchEvent(motionEvent)
                return true
            }
        })

        imageViewStop?.setOnClickListener {
            isLocked = false
            stopRecording(RecordingBehaviour.LOCK_DONE)
        }
    }

    private fun translateY(y: Float) {
        if (y < -lockOffset) {
            locked()
            imageViewAudio?.translationY = DEFAULT_COORDINATE_VALUE
            return
        }
        if (layoutLock?.visibility !== View.VISIBLE) {
            layoutLock?.visibility = View.VISIBLE
        }
        imageViewAudio?.translationY = y
        layoutLock?.translationY = y / 2
        imageViewAudio?.translationX = DEFAULT_COORDINATE_VALUE
    }

    private fun translateX(x: Float) {
        if (if (isLayoutDirectionRightToLeft) x > cancelOffset else x < -cancelOffset) {
            canceled()
            imageViewAudio?.translationX = DEFAULT_COORDINATE_VALUE
            layoutSlideCancel?.translationX = DEFAULT_COORDINATE_VALUE
            return
        }
        imageViewAudio?.translationX = x
        layoutSlideCancel?.translationX = x
        layoutLock?.translationY = DEFAULT_COORDINATE_VALUE
        imageViewAudio?.translationY = DEFAULT_COORDINATE_VALUE
        imageViewMic?.width?.div(2)?.let {
            if (abs(x) < it) {
                if (layoutLock?.visibility !== View.VISIBLE) {
                    layoutLock?.visibility = View.VISIBLE
                }
            } else {
                if (layoutLock?.visibility !== View.GONE) {
                    layoutLock?.visibility = View.GONE
                }
            }
        }
    }

    private fun locked() {
        stopTrackingAction = true
        stopRecording(RecordingBehaviour.LOCKED)
        isLocked = true
    }

    private fun canceled() {
        stopTrackingAction = true
        stopRecording(RecordingBehaviour.CANCELED)
    }

    private fun stopRecording(recordingBehaviour: RecordingBehaviour) {
        stopTrackingAction = true

        firstX = DEFAULT_COORDINATE_VALUE
        firstY = DEFAULT_COORDINATE_VALUE
        lastX = DEFAULT_COORDINATE_VALUE
        lastY = DEFAULT_COORDINATE_VALUE

        userBehaviour = UserBehaviour.NONE
        imageViewAudio?.animate()?.setXYScales(MAX_COORDINATE_VALUE)
            ?.translationX(DEFAULT_COORDINATE_VALUE)
            ?.translationY(DEFAULT_COORDINATE_VALUE)
            ?.setDuration(duration)?.setInterpolator(LinearInterpolator())?.start()
        layoutSlideCancel?.translationX = DEFAULT_COORDINATE_VALUE
        layoutSlideCancel?.visibility = View.GONE
        layoutLock?.visibility = View.GONE
        layoutLock?.translationY = DEFAULT_COORDINATE_VALUE
        imageViewLockArrow?.clearAnimation()
        imageViewLock?.clearAnimation()
        if (isLocked) {
            return
        }
        if (recordingBehaviour == RecordingBehaviour.LOCKED) {
            imageViewStop?.visibility = View.VISIBLE
            if (recordingListener != null) recordingListener!!.onRecordingLocked()
        } else if (recordingBehaviour == RecordingBehaviour.CANCELED) {
            timeText!!.clearAnimation()
            timeText!!.visibility = View.INVISIBLE
            imageViewMic?.visibility = View.INVISIBLE
            imageViewStop?.visibility = View.GONE
            layoutEffect2?.visibility = View.GONE
            layoutEffect1?.visibility = View.GONE
            timerTask?.cancel()
            delete()
            if (recordingListener != null) recordingListener!!.onRecordingCanceled()
        } else if (recordingBehaviour == RecordingBehaviour.RELEASED || recordingBehaviour == RecordingBehaviour.LOCK_DONE) {
            timeText?.clearAnimation()
            timeText?.visibility = View.INVISIBLE
            imageViewMic?.visibility = View.INVISIBLE
            editTextMessage!!.visibility = View.VISIBLE
            imageViewStop?.visibility = View.GONE
            editTextMessage?.requestFocus()
            layoutEffect2?.visibility = View.GONE
            layoutEffect1?.visibility = View.GONE
            timerTask?.cancel()
            if (recordingListener != null) recordingListener!!.onRecordingCompleted()
        }
    }

    private fun startRecord() {
        if (recordingListener != null) recordingListener!!.onRecordingStarted()
        stopTrackingAction = false
        editTextMessage!!.visibility = View.INVISIBLE
        imageViewAudio?.animate()?.setXYScales(MAX_COORDINATE_VALUE)
            ?.setDuration(200)
            ?.setInterpolator(OvershootInterpolator())?.start()
        timeText!!.visibility = View.VISIBLE
        layoutLock?.visibility = View.VISIBLE
        layoutSlideCancel?.visibility = View.VISIBLE
        imageViewMic?.visibility = View.VISIBLE
        layoutEffect2?.visibility = View.VISIBLE
        layoutEffect1?.visibility = View.VISIBLE
        timeText?.startAnimation(animBlink)
        imageViewLockArrow?.clearAnimation()
        imageViewLock?.clearAnimation()
        imageViewLockArrow?.startAnimation(animJumpFast)
        imageViewLock?.startAnimation(animJump)
        if (audioTimer == null) {
            audioTimer = Timer()
            timeFormatter?.timeZone = TimeZone.getTimeZone("UTC")
        }
        timerTask = object : TimerTask() {
            override fun run() {
                handler?.post {
                    timeText?.text = timeFormatter?.format(Date(audioTotalTime * 1000L))
                    audioTotalTime++
                }
            }
        }
        audioTotalTime = 0
        audioTimer?.schedule(timerTask, 0, 1000)
    }

    private fun delete() {
        imageViewMic?.visibility = View.VISIBLE
        imageViewMic?.rotation = DEFAULT_COORDINATE_VALUE
        isDeleting = true
        imageViewAudio?.isEnabled = false
        handler?.postDelayed({
            isDeleting = false
            imageViewAudio?.isEnabled = true
        }, 1250)
        imageViewMic?.animate()?.translationY(-dp * 150)?.rotation(180F)?.scaleXBy(
            MID_COORDINATE_VALUE
        )
            ?.scaleYBy(MID_COORDINATE_VALUE)
            ?.setDuration(500)?.setInterpolator(
                DecelerateInterpolator()
            )?.setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    val displacement: Float = if (isLayoutDirectionRightToLeft) {
                        dp * 40
                    } else {
                        -dp * 40
                    }
                    dustin?.translationX = displacement
                    dustinCover?.translationX = displacement
                    dustinCover?.animate()?.translationX(DEFAULT_COORDINATE_VALUE)?.rotation(-120f)
                        ?.setDuration(350)
                        ?.setInterpolator(
                            DecelerateInterpolator()
                        )?.start()
                    dustin?.animate()?.translationX(DEFAULT_COORDINATE_VALUE)?.setDuration(350)
                        ?.setInterpolator(DecelerateInterpolator())
                        ?.setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {
                                dustin?.visibility = View.VISIBLE
                                dustinCover?.visibility = View.VISIBLE
                            }

                            override fun onAnimationEnd(animation: Animator) {}
                            override fun onAnimationCancel(animation: Animator) {}
                            override fun onAnimationRepeat(animation: Animator) {}
                        })?.start()
                }

                override fun onAnimationEnd(animation: Animator) {
                    imageViewMic?.animate()?.translationY(DEFAULT_COORDINATE_VALUE)
                        ?.setXYScales(MAX_COORDINATE_VALUE)
                        ?.setDuration(350)
                        ?.setInterpolator(LinearInterpolator())?.setListener(
                            object : Animator.AnimatorListener {
                                override fun onAnimationStart(animation: Animator) {}
                                override fun onAnimationEnd(animation: Animator) {
                                    imageViewMic?.visibility = View.INVISIBLE
                                    imageViewMic?.rotation = DEFAULT_COORDINATE_VALUE
                                    val displacement: Float = if (isLayoutDirectionRightToLeft) {
                                        dp * 40
                                    } else {
                                        -dp * 40
                                    }
                                    dustinCover?.animate()?.rotation(DEFAULT_COORDINATE_VALUE)
                                        ?.setDuration(150)
                                        ?.setStartDelay(50)
                                        ?.start()
                                    dustin?.animate()?.translationX(displacement)?.setDuration(200)
                                        ?.setStartDelay(250)?.setInterpolator(
                                            DecelerateInterpolator()
                                        )?.start()
                                    dustinCover?.animate()?.translationX(displacement)
                                        ?.setDuration(200)
                                        ?.setStartDelay(250)?.setInterpolator(
                                            DecelerateInterpolator()
                                        )?.setListener(object : Animator.AnimatorListener {
                                            override fun onAnimationStart(animation: Animator) {}
                                            override fun onAnimationEnd(animation: Animator) {
                                                editTextMessage!!.visibility = View.VISIBLE
                                                editTextMessage!!.requestFocus()
                                            }

                                            override fun onAnimationCancel(animation: Animator) {}
                                            override fun onAnimationRepeat(animation: Animator) {}
                                        })?.start()
                                }

                                override fun onAnimationCancel(animation: Animator) {}
                                override fun onAnimationRepeat(animation: Animator) {}
                            }
                        )?.start()
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })?.start()
    }

    private fun ViewPropertyAnimator?.setXYScales(coordinateScalesValue: Float): ViewPropertyAnimator? {
        return this?.scaleX(coordinateScalesValue)?.scaleY(coordinateScalesValue)
    }
}