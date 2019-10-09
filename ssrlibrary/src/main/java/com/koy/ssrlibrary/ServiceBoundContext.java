package com.koy.ssrlibrary;
/*
 * Shadowsocks - A shadowsocks client for Android
 * Copyright (C) 2014 <max.c.lv@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                            ___====-_  _-====___
 *                      _--^^^#####//      \\#####^^^--_
 *                   _-^##########// (    ) \\##########^-_
 *                  -############//  |\^^/|  \\############-
 *                _/############//   (@::@)   \\############\_
 *               /#############((     \\//     ))#############\
 *              -###############\\    (oo)    //###############-
 *             -#################\\  / VV \  //#################-
 *            -###################\\/      \//###################-
 *           _#/|##########/\######(   /\   )######/\##########|\#_
 *           |/ |#/\#/\#/\/  \#/\##\  |  |  /##/\#/  \/\#/\#/\#| \|
 *           `  |/  V  V  `   V  \#\| |  | |/#/  V   '  V  V  \|  '
 *              `   `  `      `   / | |  | | \   '      '  '   '
 *                               (  | |  | |  )
 *                              __\ | |  | | /__
 *                             (vvv(VVV)(VVV)vvv)
 *
 *                              HERE BE DRAGONS
 *
 */

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.github.shadowsocks.aidl.IShadowsocksService;
import com.github.shadowsocks.aidl.IShadowsocksServiceCallback;
import com.koy.ssrlibrary.utils.Constants;
import com.koy.ssrlibrary.utils.VayLog;

import static com.koy.ssrlibrary.ShadowsocksApplication.app;


/**
 * @author Mygod
 */
public class ServiceBoundContext extends ContextWrapper implements IBinder.DeathRecipient {

    private static final String TAG = ServiceBoundContext.class.getSimpleName();

    private IBinder binder;
    public IShadowsocksService bgService;

    private IShadowsocksServiceCallback callback;
    private ShadowsocksServiceConnection connection;
    private boolean callbackRegistered;

    public ServiceBoundContext(Context base) {
        super(base);
    }

    public class ShadowsocksServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                binder = service;
                service.linkToDeath(ServiceBoundContext.this, 0);
                bgService = IShadowsocksService.Stub.asInterface(service);
                registerCallback();
                ServiceBoundContext.this.onServiceConnected();
            } catch (RemoteException e) {
                VayLog.e(TAG, "onServiceConnected", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unregisterCallback();
            ServiceBoundContext.this.onServiceDisconnected();
            bgService = null;
            binder = null;
        }
    }

    /**
     * register callback
     */
    public void registerCallback() {
        if (bgService != null && callback != null && !callbackRegistered) {
            try {
                bgService.registerCallback(callback);
                callbackRegistered = true;
            } catch (Exception e) {
                VayLog.e(TAG, "registerCallback", e);
            }
        }
    }

    /**
     * unregister callback
     */
    public void unregisterCallback() {
        if (bgService != null && callback != null && callbackRegistered) {
            try {
                bgService.unregisterCallback(callback);
            } catch (Exception e) {
                VayLog.e(TAG, "unregisterCallback", e);
            }
            callbackRegistered = false;
        }
    }

    protected void onServiceConnected() {
    }

    protected void onServiceDisconnected() {
    }

    @Override
    public void binderDied() {
    }

    public void attachService() {
        attachService(null);
    }

    public void attachService(IShadowsocksServiceCallback.Stub callback) {
        this.callback = callback;
        if (bgService == null) {
            Class<?> clazz;
            if (app.isNatEnabled()) {
                clazz = ShadowsocksNatService.class;
            } else {
                clazz = ShadowsocksVpnService.class;
            }

            Intent intent = new Intent(this, clazz);
            intent.setAction(Constants.Action.SERVICE);

            connection = new ShadowsocksServiceConnection();
            bindService(intent, connection, Context.BIND_AUTO_CREATE);


        }
    }

    /**
     * detach service
     */
    public void detachService() {
        unregisterCallback();
        callback = null;
        if (connection != null) {
            try {
                unbindService(connection);
            } catch (Exception e) {
                VayLog.e(TAG, "detachService", e);
            }
            connection = null;
        }

        if (binder != null) {
            binder.unlinkToDeath(this, 0);
            binder = null;
        }

        bgService = null;
    }
}
