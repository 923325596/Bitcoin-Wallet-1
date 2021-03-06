package com.bitcoin.wallet.btc.data.live

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.bitcoin.wallet.btc.BitcoinApplication
import com.bitcoin.wallet.btc.utils.Configuration

class ConfigOwnNameLiveData(application: BitcoinApplication) : LiveData<String>(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val config: Configuration = application.config

    override fun onActive() {
        config.registerOnSharedPreferenceChangeListener(this)
        value = config.ownName
    }

    override fun onInactive() {
        config.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (Configuration.PREFS_KEY_OWN_NAME == key)
            value = config.ownName
    }
}