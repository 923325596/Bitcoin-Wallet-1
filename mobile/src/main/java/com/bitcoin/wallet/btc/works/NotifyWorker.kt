package com.bitcoin.wallet.btc.works

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.bitcoin.wallet.btc.api.BlockchainEndpoint
import com.bitcoin.wallet.btc.ui.activitys.MainActivity
import io.reactivex.Single
import javax.inject.Inject
import com.bitcoin.wallet.btc.R
import java.util.concurrent.TimeUnit


class NotifyWorker @Inject constructor(
    private val context: Application,
    private val workerParameters: WorkerParameters,
    private val api: BlockchainEndpoint
) : RxWorker(context, workerParameters) {
    override fun createWork(): Single<Result> {
        val base = workerParameters.inputData.getString("base")
        val key = workerParameters.inputData.getString("key")
        if (base.isNullOrEmpty() && key.isNullOrEmpty()) {
            return Single.just(Result.failure())
        }
        return makePriceNofity(base ?: "BTC", key ?: "")
    }

    private fun makePriceNofity(base: String, key: String): Single<Result> {
        return Single.fromObservable(api.getPriceIndexes(base, key))
            .delay(12, TimeUnit.SECONDS)
            .doOnSuccess {
                Log.d("WORK", it["USD"]?.price.toString())
                val title = "AVG PRICE: $".plus(it["USD"]?.price.toString()).plus("/BTC")
                makeStatusNotification(title, context)
            }
            .map { if (it.isNotEmpty()) Result.success() else Result.failure() }
            .onErrorReturnItem(Result.failure())
    }

    companion object {

        fun enqueue(base: String, key: String) {
            val workManager = WorkManager.getInstance()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            workManager.enqueueUniquePeriodicWork(
                base,
                ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequest.Builder(NotifyWorker::class.java, 15, TimeUnit.MINUTES)
                    .setInputData(
                        Data.Builder()
                            .putString("base", base)
                            .putString("key", key)
                            .build()
                    )
                    .setConstraints(constraints)
                    .build()
            )
        }

        fun makeStatusNotification(message: String, context: Context) {
            // Make a channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
                val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
                val importance = NotificationManager.IMPORTANCE_HIGH
                @SuppressLint("WrongConstant") val channel = NotificationChannel(CHANNEL_ID, name, importance)
                channel.description = description
                channel.enableVibration(true)
                channel.lightColor = Color.GRAY
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

                // Add the channel
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                notificationManager.createNotificationChannel(channel)
            }
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            // Create the notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(LongArray(0))

            // Show the notification
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }

        const val VERBOSE_NOTIFICATION_CHANNEL_NAME = "Verbose WorkManager Notifications"
        const val NOTIFICATION_TITLE = "Hello, See rates today on the Bitcoin Wallet"
        const val VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION =
            "Shows notifications whenever work starts"
        const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
        const val NOTIFICATION_ID = 1029
    }
}