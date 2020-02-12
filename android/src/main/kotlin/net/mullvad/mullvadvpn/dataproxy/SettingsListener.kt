package net.mullvad.mullvadvpn.dataproxy

import net.mullvad.mullvadvpn.model.RelaySettings
import net.mullvad.mullvadvpn.model.Settings
import net.mullvad.mullvadvpn.service.MullvadDaemon
import net.mullvad.talpid.util.EventNotifier

class SettingsListener(val daemon: MullvadDaemon) {
    private val settingsNotifier: EventNotifier<Settings> = EventNotifier(daemon.getSettings())

    private val listenerId = daemon.onSettingsChange.subscribe { maybeSettings ->
        maybeSettings?.let { settings -> handleNewSettings(settings) }
    }

    var settings: Settings? = null
        private set

    var onAccountNumberChange: ((String?) -> Unit)? = null
        set(value) {
            synchronized(this) {
                field = value
                value?.invoke(settings?.accountToken)
            }
        }

    var onAllowLanChange: ((Boolean) -> Unit)? = null
        set(value) {
            synchronized(this) {
                field = value

                settings?.let { safeSettings ->
                    value?.invoke(safeSettings.allowLan)
                }
            }
        }

    var onRelaySettingsChange: ((RelaySettings?) -> Unit)? = null
        set(value) {
            synchronized(this) {
                field = value
                value?.invoke(settings?.relaySettings)
            }
        }

    fun onDestroy() {
        if (listenerId != -1) {
            daemon.onSettingsChange.unsubscribe(listenerId)
        }
    }

    fun subscribe(listener: (Settings) -> Unit): Int {
        return settingsNotifier.subscribe(listener)
    }

    fun unsubscribe(id: Int) {
        settingsNotifier.unsubscribe(id)
    }

    private fun handleNewSettings(newSettings: Settings) {
        synchronized(this) {
            if (settings?.accountToken != newSettings.accountToken) {
                onAccountNumberChange?.invoke(newSettings.accountToken)
            }

            if (settings?.relaySettings != newSettings.relaySettings) {
                onRelaySettingsChange?.invoke(newSettings.relaySettings)
            }

            settingsNotifier.notify(newSettings)
            settings = newSettings
        }
    }
}
