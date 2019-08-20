package com.sid.shaheenfalcon;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.mitm.CertificateSniffingMitmManager;
import org.littleshoot.proxy.mitm.RootCertificateException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public class SFProxyServer extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            HttpProxyServer server =
                    DefaultHttpProxyServer.bootstrap()
                            .withPort(2727)
                            .withManInTheMiddle(new CertificateSniffingMitmManager())
                            .withFiltersSource(new HttpFiltersSourceAdapter() {
                                public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                                    return new HttpFiltersAdapter(originalRequest) {
                                        @Override
                                        public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                            // TODO: implement your filtering here
                                            return null;
                                        }

                                        @Override
                                        public HttpObject serverToProxyResponse(HttpObject httpObject) {
                                            // TODO: implement your filtering here
                                            return httpObject;
                                        }
                                    };
                                }
                            })
                            .start();
        } catch (RootCertificateException e) {
            e.printStackTrace();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}
