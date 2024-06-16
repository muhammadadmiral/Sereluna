package com.android.capstone.sereluna.data.model

sealed class NotificationStatus(val status:String) {

    object Ordered: NotificationStatus("Ordered")
    object Canceled: NotificationStatus("Canceled")
    object Confirmed: NotificationStatus("Confirmed")
    object Shipped: NotificationStatus("Shipped")
    object Delivered: NotificationStatus("Delivered")
    object Returned: NotificationStatus("Returned")

}

fun getNotifStatus(status: String): NotificationStatus {
    return when (status) {
        "Ordered" -> {
            NotificationStatus.Ordered
        }
        "Canceled" -> {
            NotificationStatus.Canceled
        }
        "Confirmed" -> {
            NotificationStatus.Confirmed
        }
        "Shipped" -> {
            NotificationStatus.Shipped
        }
        "Delivered" -> {
            NotificationStatus.Delivered
        }
        else -> NotificationStatus.Returned
    }
}
