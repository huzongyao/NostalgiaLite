package nostalgia.framework.remote.wifi;

import android.content.Context;
import android.os.Build;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.utils.NLog;
import nostalgia.framework.utils.EmuUtils;


public class WifiServerInfoTransmitter extends Thread {
    public static final int BROADCAST_PORT = 64313;
    public static final String MESSAGE_PREFIX = "EMUDROID";
    private static final String TAG = "WifiServerInfoTransmitter";
    private static final int SLEEP_TIME = 3000;
    private static final int SLEEP_TIME_AFTER_EXCEPTION = 15000;
    protected volatile boolean running = false;

    Context context;
    String sessionDescription;
    DatagramSocket serverSocket = null;


    public WifiServerInfoTransmitter(Context context, String sessionDescription) {
        this.context = context;
        this.sessionDescription = sessionDescription;
    }

    public boolean startSending() {
        if (PreferenceUtil.isWifiServerEnable(context) && EmuUtils.isWifiAvailable(context)) {
            stopSending();
            running = true;
            start();
            return true;
        } else {
            return false;
        }
    }

    public void stopSending() {
        running = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    public void run() {
        try {
            serverSocket = new DatagramSocket();
            serverSocket.setBroadcast(true);
            NLog.i(TAG, "Start sending broadcast");
            InetAddress broadcastAddress = EmuUtils.getBroadcastAddress(context);
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            byte[] sendData = (MESSAGE_PREFIX + "%" + manufacturer + " " + model +
                    "%" + EmuUtils.getDeviceType(context).name() +
                    "%" + sessionDescription + "%").getBytes();
            int counter = 0;

            while (running) {
                NLog.i(TAG, "send broadcast " + (counter++) + " to " + broadcastAddress);
                try {
                    DatagramPacket sendPacket =
                            new DatagramPacket(sendData, sendData.length, broadcastAddress, BROADCAST_PORT);
                    serverSocket.send(sendPacket);
                } catch (Exception e) {
                    try {
                        Thread.sleep(SLEEP_TIME_AFTER_EXCEPTION);
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    NLog.e(TAG, "wtf", e);
                }
            }
            NLog.i(TAG, "Stop sending");
        } catch (Exception e) {
            NLog.e(TAG, "", e);
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
}
