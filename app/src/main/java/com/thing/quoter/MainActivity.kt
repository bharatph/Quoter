package com.thing.quoter

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.gunhansancar.changelanguageexample.helper.LocaleHelper
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.thing.quoter.model.Quote
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header_section.*
import java.io.ByteArrayOutputStream


class MainActivity : AppActivity(), View.OnClickListener, View.OnLongClickListener {

    override fun onClick(v: View) {
        when (v.id) {
            R.id.rootView -> {
                show(QuoteHelper.getQuote())
            }
            R.id.themeToggle -> {
                toggleTheme()
            }
            R.id.fontToggle -> {
                //TODO change font app globally
                toggleFont()
            }
            R.id.languageSelect -> {
                LocaleHelper.setLocale(this, "ta")
                recreate()
            }
            R.id.toggleSpeaker -> {
                speakerTextView.visibility = if (speakerTextView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        when (v!!.id) {
            R.id.rootView -> {
                if (!isDark) {
                    QuoteHelper.shouldLoadImage = true
                    toggleTheme()
                    return false
                }
                loadImage()
                return true
            }
            else -> {
                return true
            }
        }
    }

    private var objectAnimator: ObjectAnimator? = null

    fun indicateLoading(shouldLoad: Boolean, ofBackground: Boolean = true, quote: Quote? = null) {
        if (objectAnimator == null) {
            val colorFrom = ContextCompat.getColor(this, R.color.colorPrimary)
            val colorTo = ContextCompat.getColor(this, R.color.colorPrimaryDark)
            val dur: Long = 1500
            objectAnimator = ObjectAnimator.ofObject(rootView, "backgroundColor", ArgbEvaluator(), colorFrom, colorTo).apply {
                duration = dur
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = FastOutLinearInInterpolator()
            }
        }
        if (shouldLoad) {
            objectAnimator?.start()
            if (ofBackground) {
            } else {
                if (quote != null) {
                    quoteTextView.text = quote.quote
                    speakerTextView.text = quote.speaker
                }
                var anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
                anim.repeatCount = Animation.INFINITE
                anim.repeatMode = Animation.REVERSE
                quoteContainer.startAnimation(anim)
            }
        } else {
            if (ofBackground) {
                objectAnimator?.start()
                objectAnimator?.cancel()
            } else {
                quoteContainer.clearAnimation()
            }
        }
    }

    private fun loadImage() {
        indicateLoading(true)
        Picasso.get()
                .load("https://media.timeout.com/images/101759135/630/472/image.jpg")
                .resize(420, 300)
                .centerCrop()
                .into(object : Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        indicateLoading(false)

                    }

                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        indicateLoading(false)
                        rootView.background = BitmapDrawable(resources, bitmap!!)
                    }

                })
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    fun show(quote: Quote?) {
        if (quote == null) return show(resources.getString(R.string.quote_loading))
        quoteContainer.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in).apply {
            interpolator = FastOutSlowInInterpolator()
        })
        quoteTextView.text = quote.quote
        speakerTextView.text = if (quote.speaker.isEmpty()) getString(R.string.speaker_unknown) else quote.speaker
    }

    //Replacement for Toast
    fun show(message: String, speaker: String = getString(R.string.app_name)) {
        show(Quote(message, speaker))
    }

    fun onboard() {
        val onboardMsgs = resources.getStringArray(R.array.onboard_msgs)
        var i = 0
        show(onboardMsgs[i++], getString(R.string.company_name))
        rootView.setOnClickListener {
            if (i == onboardMsgs.size) {
                toggleTheme()
                isFirstTime = false
                return@setOnClickListener
            }
            show(onboardMsgs[i++], getString(R.string.company_name))
        }
    }

    fun getBitmapFromView(view: View): Bitmap {
        var b = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b)
        header.visibility = View.GONE
        view.draw(canvas)
        header.visibility = View.VISIBLE
        return b
    }


    fun getImageUri(b: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, b, "Title", "")
        return Uri.parse(path)
    }


    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            WRITE_EXT_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                    show("Cannot save until you give permissions")
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


    fun share() {

        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, getImageUri(getBitmapFromView(rootView)))
            type = "image/*"
        }
        startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.share_text)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.applyStyle(if (isDark) R.style.AppTheme else R.style.AppTheme_Light, true)
        setContentView(R.layout.activity_main)
        //
        quoteTextView.typeface = if (isFontSerif) Typeface.SERIF else Typeface.SANS_SERIF
        //

        if (isFirstTime) {
            header.visibility = View.GONE
            return onboard()
        } else {
            show(QuoteHelper.stashedQuote)
            //then register click
            rootView.setOnClickListener(this)
            //register long press
            rootView.setOnLongClickListener(this)
            //setup share fling
            val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                    if (e1?.y!! - e2?.y!! > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        share()
                    }
                    return true
                }
            })
            rootView.setOnTouchListener { _, motionEvent ->
                gestureDetector.onTouchEvent(motionEvent)
            }
            themeToggle.setOnClickListener(this)
            fontToggle.setOnClickListener(this)
            languageSelect.setOnClickListener(this)
            toggleSpeaker.setOnClickListener(this)
        }
        if (QuoteHelper.shouldLoadImage) {
            loadImage()
            QuoteHelper.shouldLoadImage = false
        }
    }
}