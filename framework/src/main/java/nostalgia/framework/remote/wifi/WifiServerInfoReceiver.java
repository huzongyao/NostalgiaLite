package nostalgia.framework.remote.wifi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;

import nostalgia.framework.utils.NLog;
import nostalgia.framework.utils.Utils.ServerType;

@SuppressLint("HandlerLeak")
public class WifiServerInfoReceiver {

    private static final String TAG = "remote.wifi.BroadcastReceiverService";
    BroadcastReceiverThread broadcastReceiverThread;
    BroadcastReceiverListener listener;

    public void startExploring(final Context context, BroadcastReceiverListener listener) {
        this.listener = listener;
        stop();
        broadcastReceiverThread = new BroadcastReceiverThread();
        broadcastReceiverThread.startListening();
    }

    public void stop() {
        if (broadcastReceiverThread != null) {
            broadcastReceiverThread.stopListening();
        }
    }

    public interface BroadcastReceiverListener {
        public void onServerDetect(DetectionResult result);
    }

    public class DetectionResult {
        public InetAddress address;
        public String desc;
        public String sessionDescription = "";
        public SparseArray<String> slots = new SparseArray<String>();
        public ServerType type = ServerType.mobile;
        long lastDetect = 0;

        public DetectionResult(InetAddress address, String desc,
                               String sessionDescription, ServerType type) {
            this.address = address;
            this.desc = desc;
            this.sessionDescription = sessionDescription;
            this.type = type;
            lastDetect = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return desc;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DetectionResult) {
                DetectionResult oo = (DetectionResult) o;

                if (desc.equals(oo.desc) && oo.address.equals(address)) {
                    return true;

                } else {
                    return false;
                }

            } else
                return false;
        }
    }

    private class BroadcastReceiverThread extends Thread {
        protected volatile boolean running = false;
        DatagramSocket serverSocket = null;
        Handler sendHandler = new Handler() {
            public void handleMessage(Message msg) {
                listener.onServerDetect((DetectionResult) msg.obj);
            }
        };

        public void startListening() {
            running = true;
            start();
        }

        public void stopListening() {
            running = false;

            if (serverSocket != null) {
                serverSocket.close();
            }
        }

        public void run() {
            byte[] recvBuf = new byte[300];

            try {
                DatagramChannel channel = DatagramChannel.open();
                serverSocket = channel.socket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress("0.0.0.0", WifiServerInfoTransmitter.BROADCAST_PORT));
                serverSocket.setSoTimeout(0);
                NLog.i(TAG, "Start listening broadcast:" + running);

                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                        serverSocket.receive(packet);
                        NLog.i(TAG, "recive from:" + packet.getAddress().getHostAddress());
                        String data = new String(packet.getData());
                        String[] items = data.split("%");

                        if (items.length >= 3) {
                            if (items[0].equals(WifiServerInfoTransmitter.MESSAGE_PREFIX)) {
                                Message msg = new Message();
                                msg.obj = new DetectionResult(packet.getAddress(), items[1],
                                        items[3], ServerType.valueOf(items[2]));
                                sendHandler.sendMessage(msg);
                            }
                        }

                    } catch (SocketTimeoutException e) {
                        NLog.i(TAG, "timeout");
                    } catch (SocketException e) {
                        if (running) {
                            NLog.i(TAG, "socket close");
                        }
                    } catch (Exception e) {
                        NLog.e(TAG, "", e);
                    }
                }
                NLog.i(TAG, "Stop listening");
            } catch (Exception e) {
                NLog.e(TAG, "", e);
            } finally {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            }
        }

    }

}
