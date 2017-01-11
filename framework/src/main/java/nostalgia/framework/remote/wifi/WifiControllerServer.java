package nostalgia.framework.remote.wifi;

import android.util.SparseIntArray;
import android.view.KeyEvent;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import nostalgia.framework.remote.ControllerEventSource;
import nostalgia.framework.remote.ControllerKeyEvent;
import nostalgia.framework.remote.OnControllerEventListener;
import nostalgia.framework.utils.NLog;

public class WifiControllerServer implements ControllerEventSource {
    public static final int SERVER_PORT = 53216;
    private static final String TAG = "remote.wifi.WifiControllerServer";
    private static WifiControllerServer instance;
    String sessionDescription;
    private ServerThread serverThread;
    private OnControllerEventListener listener;

    private WifiControllerServer(String sessionDescription) {
        this.sessionDescription = sessionDescription;
    }

    public static WifiControllerServer getInstance(String sessionDescription) {
        if (instance == null) {
            instance = new WifiControllerServer(sessionDescription);
        }

        instance.sessionDescription = sessionDescription;
        return instance;
    }

    public void setSessionDescription(String s) {
        sessionDescription = s;
    }

    @Override
    public void setControllerEventListener(OnControllerEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void onResume() {
        if (serverThread != null) {
            serverThread.stopListening();
        }

        serverThread = instance.new ServerThread();
        serverThread.startListening();
    }

    @Override
    public void onPause() {
        if (serverThread != null)
            serverThread.stopListening();
    }

    public enum PACKET_TYPE {
        PING_PACKET, EMULATOR_KEY_PACKET, ANDROID_KEY_PACKET, TEXT_PACKET, COMMAND_PACKET
    }

    private class ServerThread extends Thread {
        protected AtomicBoolean running = new AtomicBoolean(false);
        DatagramSocket serverSocket = null;
        private byte[] buffer = new byte[WifiControllerClient.PACKET_SIZE];
        private Object listenerLock = new Object();

        public void startListening() {
            running.set(true);
            start();
        }

        public void stopListening() {
            running.set(false);

            if (serverSocket != null) {
                serverSocket.close();
            }
        }

        public void run() {
            NLog.d(TAG, "Starting remote controller SERVER THREAD " + getName());
            SparseIntArray lastKeyStates = new SparseIntArray();

            try {
                DatagramChannel channel = DatagramChannel.open();
                serverSocket = channel.socket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(SERVER_PORT));
                serverSocket.setSoTimeout(0);
                NLog.i(TAG, "Start listening on");

                while (running.get()) {
                    DatagramPacket packet = new DatagramPacket(buffer,
                            WifiControllerClient.PACKET_SIZE);

                    try {
                        serverSocket.receive(packet);
                        byte[] data = packet.getData();
                        ByteBuffer bb = ByteBuffer.wrap(data);
                        PACKET_TYPE packetType = PACKET_TYPE.values()[bb
                                .getInt(0)];

                        switch (packetType) {
                            case PING_PACKET:
                                DatagramPacket responsePacket = new DatagramPacket(
                                        new byte[]{1, 1, 1, 1}, 4,
                                        packet.getAddress(), packet.getPort());
                                NLog.i(TAG, "sending response packet");
                                serverSocket.send(responsePacket);
                                break;
                            case EMULATOR_KEY_PACKET: {
                                int port = bb.getInt(4);
                                int keyStates = bb.getInt(8);
                                int lastKeyState = lastKeyStates.get(port);

                                if (lastKeyState != keyStates) {
                                    lastKeyStates.put(port, keyStates);
                                    int mask = 1;

                                    for (int i = 0; i < 32; i++) {
                                        int s = keyStates & mask;
                                        int l = lastKeyState & mask;

                                        if (s != l) {
                                            ControllerKeyEvent res = new ControllerKeyEvent();
                                            res.action = s == mask ? 0 : 1;
                                            res.keyCode = i;
                                            res.port = port;

                                            synchronized (listenerLock) {
                                                if (listener != null) {
                                                    listener.onControllerEmulatorKeyEvent(res);
                                                }
                                            }
                                        }

                                        mask = (mask << 1);
                                    }
                                }

                                break;
                            }
                            case ANDROID_KEY_PACKET: {
                                int keyCode = bb.getInt(4);
                                int keyAction = bb.getInt(8);
                                KeyEvent event = new KeyEvent(keyAction, keyCode);

                                synchronized (listenerLock) {
                                    if (listener != null) {
                                        listener.onControllerAndroidKeyEvent(event);
                                    }
                                }

                                break;
                            }
                            case TEXT_PACKET: {
                                int len = bb.getInt(4);
                                byte[] txtbuffer = new byte[len];
                                DatagramPacket txtpacket = new DatagramPacket(
                                        txtbuffer, len);
                                serverSocket.receive(txtpacket);
                                byte[] arr = txtpacket.getData();
                                String txt = new String(arr, 0, len);

                                synchronized (listenerLock) {
                                    if (listener != null) {
                                        listener.onControllerTextEvent(txt);
                                    }
                                }

                                break;
                            }
                            case COMMAND_PACKET: {
                                int command = bb.getInt(4);
                                int param1 = bb.getInt(8);
                                int param2 = bb.getInt(12);

                                synchronized (listenerLock) {
                                    if (listener != null) {
                                        listener.onControllerCommandEvent(command,
                                                param1, param2);
                                    }
                                }

                                break;
                            }
                        }

                    } catch (SocketTimeoutException e) {
                        NLog.e(TAG, "timeout");

                    } catch (SocketException e) {
                        if (running.get()) {
                            NLog.e(TAG, "socket close", e);
                        }

                    } catch (ArrayIndexOutOfBoundsException e) {
                        NLog.e(TAG, "Non supported packet", e);

                    } catch (Exception e) {
                        NLog.e(TAG, "", e);
                    }
                }

                NLog.d(TAG, "Stopping remote controller SERVER THREAD "
                        + getName());

            } catch (Exception e) {
                NLog.e(TAG, "Error: SERVER STOPPED " + getName());
                NLog.e(TAG, "", e);

            } finally {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            }
        }
    }

}
