package com.onetwodevs.anysend.ui.callback;

import com.onetwodevs.anysend.object.NetworkDevice;

/**
 * created by: 12 Devs
 * date: 16/04/18 03:18
 */
public interface NetworkDeviceSelectedListener
{
    boolean onNetworkDeviceSelected(NetworkDevice networkDevice, NetworkDevice.Connection connection);

    boolean isListenerEffective();
}
