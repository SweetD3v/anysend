package com.onetwodevs.anysend.ui;

import com.genonbeta.android.framework.util.Interrupter;

/**
 * created by: 12 Devs
 * date: 16/04/18 22:41
 */
public interface UITask
{
    void updateTaskStarted(final Interrupter interrupter);

    void updateTaskStopped();
}
