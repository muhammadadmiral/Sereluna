package com.android.capstone.sereluna.data.utils

import android.net.http.HttpException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun String?.getTimeAgoFormat(): String {
    if (this.isNullOrEmpty()) return "Unknown"
    val formats = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    val sdf = SimpleDateFormat(formats, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }
    val pastTime = sdf.parse(this)?.time ?: return "Unknown"
    val diffs = System.currentTimeMillis() - pastTime

    val oneMin = 60_000L
    val oneHour = 60 * oneMin
    val oneDay = 24 * oneHour
    val oneMonth = 30 * oneDay
    val oneYear = 365 * oneDay

    return when {
        diffs >= oneYear -> "${diffs / oneYear} years ago"
        diffs >= oneMonth -> "${diffs / oneMonth} months ago"
        diffs >= oneDay -> "${diffs / oneDay} days ago"
        diffs >= oneHour -> "${diffs / oneHour} hours ago"
        diffs >= oneMin -> "${diffs / oneMin} min ago"
        else -> "Just now"
    }

  //  fun String?.generateToken() = "Bearer $this"

   // fun HttpException.getErrorMessage(): String {
  //      val message = response()?.errorBody()?.string().toString()
  //      return JSONObject(message).getString("message")
 //   }
}