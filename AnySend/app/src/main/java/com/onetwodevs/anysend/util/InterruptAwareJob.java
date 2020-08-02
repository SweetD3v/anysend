package com.onetwodevs.anysend.util;

import com.genonbeta.android.framework.util.Interrupter;

/**
 * created by: 12 Devs
 * date: 11.02.2018 19:37
 */

abstract public class InterruptAwareJob
{
    abstract protected void onRun();

    protected void run(Interrupter interrupter)
    {
        onRun();
        interrupter.removeClosers();
    }
}
