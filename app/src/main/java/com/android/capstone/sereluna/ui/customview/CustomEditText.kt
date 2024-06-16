package com.android.capstone.sereluna.ui.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.InputType
import android.util.AttributeSet
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.content.res.getIntOrThrow
import androidx.core.widget.addTextChangedListener
import com.android.capstone.sereluna.R

class CustomEditText: AppCompatEditText, View.OnTouchListener  {

    private var startIconsDrawable: Drawable? = null
    private var showPasswordIconsDrawable: Drawable? = null
    private var hidePasswordIconsDrawable: Drawable? = null
    private lateinit var backgroundsDrawable: Drawable
    private lateinit var backgroundErrorsDrawable: Drawable
    private var backgroundCorrectsDrawable: Drawable? = null
    private var minimumPasswordsLength = 8
    private var etHints: String = ""
    private var isPasswordsShown = false
    private var snapsInputType = SnapInputType.PASSWORD
    constructor(context: Context) : super(context) {
        inits()
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        getsAttribute(attrs)
        inits()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        getsAttribute(attrs)
        inits()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        setsDrawableIcon(
            left = startIconsDrawable,
            right = if (isPasswordsShown) showPasswordIconsDrawable else hidePasswordIconsDrawable
        )
        background = if (error.isNullOrEmpty()) backgroundCorrectsDrawable ?: backgroundsDrawable else backgroundErrorsDrawable
        hint = etHints
    }
    private fun inits() {
        when (snapsInputType) {
            SnapInputType.EMAIL -> {
                inputType = INPUT_TYPE_EMAILS
                etHints = resources.getString(R.string.email_hint)
                addTextChangedListener(onTextChanged = { email, _, _, _ ->
                    if (!isValidEmail(email)) setError(resources.getString(R.string.email_error), null)
                    backgroundCorrectsDrawable = ContextCompat.getDrawable(context, R.drawable.bg_snap_edit_text_correct)
                })
            }
            SnapInputType.PASSWORD -> {
                inputType = INPUT_TYPE_PASSWORDS
                etHints = resources.getString(R.string.password_hint)
                showPasswordIconsDrawable = ContextCompat.getDrawable(context, R.drawable.ic_show_password)
                hidePasswordIconsDrawable = ContextCompat.getDrawable(context, R.drawable.ic_hide_password)
                addTextChangedListener(onTextChanged = { password, _, _, _ ->
                    if (!isValidPassword(password)) setError(resources.getString(R.string.password_error), null)
                    backgroundCorrectsDrawable = ContextCompat.getDrawable(context, R.drawable.bg_snap_edit_text_correct)
                })
                setOnTouchListener(this)
            }
            SnapInputType.PASSWORD_CONFIRMATION -> {
                inputType = INPUT_TYPE_PASSWORDS
                etHints = resources.getString(R.string.password_confirmation_hint)
                showPasswordIconsDrawable = ContextCompat.getDrawable(context, R.drawable.ic_show_password)
                hidePasswordIconsDrawable = ContextCompat.getDrawable(context, R.drawable.ic_hide_password)
                addTextChangedListener(onTextChanged = { password, _, _, _ ->
                    if (!isValidPassword(password)) setError(resources.getString(R.string.password_error), null)
                    backgroundCorrectsDrawable = ContextCompat.getDrawable(context, R.drawable.bg_snap_edit_text_correct)
                })
                setOnTouchListener(this)
            }
            SnapInputType.NAME -> {
                inputType = INPUT_TYPE_TEXTS_NORMAL
                etHints = resources.getString(R.string.name)
                addTextChangedListener(onTextChanged = { name, _, _, _ ->
                    if (!isValidName(name)) setError(context.getString(R.string.name_error), null)
                    backgroundCorrectsDrawable = ContextCompat.getDrawable(context, R.drawable.bg_snap_edit_text_correct)
                })
            }
        }
        backgroundsDrawable = ContextCompat.getDrawable(context, R.drawable.bg_snap_edit_text) as Drawable
        backgroundErrorsDrawable = ContextCompat.getDrawable(context, R.drawable.bg_snap_edit_text_error) as Drawable
    }

    private fun getsAttribute(attrs: AttributeSet?) {
        val styles = context.obtainStyledAttributes(attrs, R.styleable.SnapEditText)
        minimumPasswordsLength = styles.getInt(R.styleable.SnapEditText_min_password_length, 8)
        snapsInputType = when (styles.getIntOrThrow(R.styleable.SnapEditText_custom_type)) {
            SnapInputType.PASSWORD.value -> SnapInputType.PASSWORD
            SnapInputType.EMAIL.value -> SnapInputType.EMAIL
            SnapInputType.PASSWORD_CONFIRMATION.value -> SnapInputType.PASSWORD_CONFIRMATION
            SnapInputType.NAME.value -> SnapInputType.NAME
            else -> throw IllegalArgumentException("Invalid custom_type value")
        }
        styles.recycle()
    }
    private fun setsDrawableIcon(
        left: Drawable? = null,
        top: Drawable? = null,
        right: Drawable? = null,
        bottom: Drawable? = null
    ) {
        setCompoundDrawablesWithIntrinsicBounds(
            left, top, right, bottom
        )
        compoundDrawablePadding = 16
    }

    fun isValidPassword(password: CharSequence?) =
        !password.isNullOrEmpty() && password.length >= minimumPasswordsLength
    fun isValidEmail(email: CharSequence?) =
        !email.isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun isValidName(name: CharSequence?) = !name.isNullOrEmpty()

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        if (compoundDrawables[2] != null) {
            val showsHideButtonStart: Float
            val showsHideButtonEnd: Float
            var isShowsHideButtonClicked = false
            if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                showsHideButtonEnd = ((hidePasswordIconsDrawable?.intrinsicWidth ?: 0) + paddingStart).toFloat()
                when {
                    event.x < showsHideButtonEnd -> isShowsHideButtonClicked = true
                }
            } else {
                showsHideButtonStart = (width - paddingEnd - (hidePasswordIconsDrawable?.intrinsicWidth ?: 0)).toFloat()
                when {
                    event.x > showsHideButtonStart -> isShowsHideButtonClicked = true
                }
            }
            return if (isShowsHideButtonClicked) {
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
                        isPasswordsShown = !isPasswordsShown
                        updatePasswordsVisibility()
                        true
                    }
                    else -> false
                }
            } else false
        }
        return false
    }

    private fun updatePasswordsVisibility() {
        inputType = if (isPasswordsShown) INPUT_TYPE_VISIBLE_PASSWORDS else INPUT_TYPE_PASSWORDS
        setSelection(text?.length ?: 0)
    }

    enum class SnapInputType(val value: Int) {
        EMAIL(0),
        PASSWORD(1),
        PASSWORD_CONFIRMATION(2),
        NAME(3)
    }

    companion object {
        private const val INPUT_TYPE_EMAILS = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        private const val INPUT_TYPE_PASSWORDS = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        private const val INPUT_TYPE_VISIBLE_PASSWORDS = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        private const val INPUT_TYPE_TEXTS_NORMAL = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
    }

}