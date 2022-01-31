package com.consultantapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AlertDialog
import com.consultantapp.R

private fun showNoInternetDialog(context: Context) {
    AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(context.getString(R.string.internet))
            .setMessage(context.getString(R.string.check_internet))
            .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                /*val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)*/
            }.show()
}

fun isConnectedToInternet(context: Context?, showAlert: Boolean): Boolean {
    val isConnected = isConnectedToInternet(context)

    return when {
        isConnected -> true
        showAlert -> {
            showNoInternetDialog(context as Context)
            false
        }
        else -> false
    }
}

private fun isConnectedToInternet(context: Context?): Boolean {
    val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            //for other device how are able to connect with Ethernet
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            //for check internet over Bluetooth
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    } else {
        val nwInfo = connectivityManager.activeNetworkInfo ?: return false
        return nwInfo.isConnected
    }
}