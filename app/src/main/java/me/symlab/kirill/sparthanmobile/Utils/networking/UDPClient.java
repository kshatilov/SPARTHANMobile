package me.symlab.kirill.sparthanmobile.Utils.networking;

import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import me.symlab.kirill.sparthanmobile.Utils.EasyCallable;

public class UDPClient implements Client {

    private static final String TAG = "udpClient";
    private String ip;
    private final static int port = 90;
    private final static int IN_PACKAGE_MAX_SIZE = 25;

    @Override
    public void init(String ip, int port) {
        this.ip = ip;
    }

    @Override
    public void send(JSONArray data, EasyCallable<String> successCallback, Runnable failCallback) {
        new Thread(new ClientSendAndListen(ip, port, data.toString(), successCallback)).start();
    }

    public static class ClientSendAndListen implements Runnable {

        private final int port;
        private final String ip;
        private final String data;
        private final EasyCallable<String> successCallback;

        public ClientSendAndListen(String ip, int port, String data, EasyCallable<String> successCallback) {
            this.ip = ip;
            this.port = port;
            this.data = data;
            this.successCallback = successCallback;
        }

        @Override
        public void run() {
            DatagramSocket udpSocket = null;
            try {
                udpSocket = new DatagramSocket();
                InetAddress serverAddr = InetAddress.getByName(ip);
                String _data = data + "_" + String.valueOf(SystemClock.uptimeMillis());
                byte[] buf = _data.getBytes();
                DatagramPacket outPacket = new DatagramPacket(buf, buf.length, serverAddr, port);
                udpSocket.send(outPacket);
                byte[] message = new byte[IN_PACKAGE_MAX_SIZE];
                DatagramPacket inPacket = new DatagramPacket(message, message.length);
                udpSocket.setSoTimeout(10000);
                udpSocket.receive(inPacket);
                String text = new String(message, 0, inPacket.getLength());
                String[] s = text.split("_");
                String gesture = s[0];
                Long start = Long.valueOf(s[1]);
                long end = SystemClock.uptimeMillis();
                Log.d(TAG, "UDP time SPARTHAN: " + (end - start));
                successCallback.accept(gesture);
            } catch (IOException e) {
                Log.d(TAG, "UDP time SPARTHAN: " + "LOST");
            } finally {
                udpSocket.close();
            }
        }
    }
}
