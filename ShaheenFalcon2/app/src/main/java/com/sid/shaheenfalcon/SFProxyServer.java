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

import java.util.ArrayList;
import java.util.HashMap;

import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.multipart.HttpData;

public class SFProxyServer extends Service {

    protected ArrayList<SFRequest> requests = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<SFRequest> requests = new ArrayList<SFRequest>();
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
                                            if (httpObject instanceof FullHttpRequest) {
                                                FullHttpRequest req = (FullHttpRequest) httpObject;
                                                String method = req.getMethod().toString();
                                                CompositeByteBuf buffer = (CompositeByteBuf) req.content();
                                                HashMap<String, String> headers = new HashMap<String, String>();
                                                for(String headerName : req.headers().names()){
                                                    headers.put(headerName, req.headers().get(headerName));
                                                }
                                                requests.add(new SFRequest(req.getMethod().name(), req.getUri().toString(), headers, buffer.array()));
                                            }
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
