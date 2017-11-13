package nostalgia.framework.remote.wifi;

import android.util.Pair;
import android.view.KeyEvent;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import nostalgia.framework.remote.wifi.WifiControllerServer.PACKET_TYPE;
import nostalgia.framework.utils.NLog;

public class WifiControllerClient {
    public static final int PACKET_SIZE = 32;
    private static final String TAG = "WifiControllerClient";
    ByteBuffer byteBuffer = ByteBuffer.allocate(PACKET_SIZE);
    long lastSendTimestamp = 0;
    private InetAddress ip;
    private int keysStates = 0;
    private int port;
    private SenderThread thread;

    public WifiControllerClient(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
        byteBuffer.clear();
        keysStates = 0;
    }

    public synchronized void sendControllerAndroidKeyEvent(KeyEvent event) {
        thread.sendAndroidKeyEventForce(event);
    }


    public synchronized void sendControllerTextEvent(String text) {
        thread.sendTextEventForce(text);
    }


    public synchronized void sendControllerCommandEvent(int command, int param0, int param1) {
        thread.sendCommandEventForce(command, param0, param1);
    }


    public synchronized void sendControllerEmulatorKeyEvent(final int action, final int keyCode) {
        if (action == 1) {
            keysStates = keysStates | (1 << keyCode);
        } else if (action == 0) {
            keysStates = keysStates & ~(1 << keyCode);
        }
        thread.forceSend();
    }

    private void sendKeyStates(final int port) {
        try {
            lastSendTimestamp = System.currentTimeMillis();
            NLog.i(TAG, "send new event:" +
                    Integer.toBinaryString(keysStates) + " port:" + port + " ip:" + ip);
            byteBuffer.clear();
            byteBuffer.putInt(0, PACKET_TYPE.EMULATOR_KEY_PACKET.ordinal());
            byteBuffer.putInt(4, port);
            byteBuffer.putInt(8, keysStates);
            DatagramPacket sendPacket =
                    new DatagramPacket(byteBuffer.array(), PACKET_SIZE, ip, WifiControllerServer.SERVER_PORT);
            DatagramSocket clientSocket = null;

            try {
                clientSocket = new DatagramSocket();
                clientSocket.send(sendPacket);

            } finally {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            }

        } catch (Exception e) {
            NLog.e(TAG, "", e);
        }
    }

    private void sendAndroidKeyEvent(final KeyEvent event) {
        try {
            NLog.i(TAG, "send new event:" + event + " ip:" + ip);
            byteBuffer.clear();
            byteBuffer.putInt(0, PACKET_TYPE.ANDROID_KEY_PACKET.ordinal());
            byteBuffer.putInt(4, event.getKeyCode());
            byteBuffer.putInt(8, event.getAction());
            DatagramPacket sendPacket =
                    new DatagramPacket(byteBuffer.array(), PACKET_SIZE, ip, WifiControllerServer.SERVER_PORT);
            DatagramSocket clientSocket = null;

            try {
                clientSocket = new DatagramSocket();
                clientSocket.send(sendPacket);

            } finally {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            }

        } catch (Exception e) {
            NLog.e(TAG, "", e);
        }
    }

    private void sendTextEvent(final String text) {
        try {
            NLog.i(TAG, "send new text event:" + text + " ip:" + ip);
            byteBuffer.clear();
            byte[] textData = text.getBytes();
            byteBuffer.putInt(0, PACKET_TYPE.TEXT_PACKET.ordinal());
            byteBuffer.putInt(4, textData.length);
            DatagramSocket clientSocket = null;

            try {
                clientSocket = new DatagramSocket();
                DatagramPacket sendPacket =
                        new DatagramPacket(byteBuffer.array(), PACKET_SIZE, ip, WifiControllerServer.SERVER_PORT);
                clientSocket.send(sendPacket);
                sendPacket =
                        new DatagramPacket(textData, textData.length, ip, WifiControllerServer.SERVER_PORT);
                clientSocket.send(sendPacket);

            } finally {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            }

        } catch (Exception e) {
            NLog.e(TAG, "", e);
        }
    }

    private void sendCommandEvent(int command, int param0, int param1) {
        try {
            NLog.i(TAG, "send command event:" + command + " " + param0 + " " + param1);
            byteBuffer.clear();
            byteBuffer.putInt(0, PACKET_TYPE.COMMAND_PACKET.ordinal());
            byteBuffer.putInt(4, command);
            byteBuffer.putInt(8, param0);
            byteBuffer.putInt(12, param1);
            DatagramSocket clientSocket = null;

            try {
                clientSocket = new DatagramSocket();
                DatagramPacket sendPacket =
                        new DatagramPacket(byteBuffer.array(), PACKET_SIZE, ip, WifiControllerServer.SERVER_PORT);
                clientSocket.send(sendPacket);
            } finally {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            }
        } catch (Exception e) {
            NLog.e(TAG, "", e);
        }
    }

    public void onResume() {
        if (thread != null) {
            thread.finish();
        }
        thread = new SenderThread();
        thread.start();
    }

    public void onPause() {
        if (thread != null) {
            thread.finish();
        }
    }

    public void onStop() {
        if (thread != null) {
            thread.finish();
        }
    }

    private class SenderThread extends Thread {

        private final int DELAY = 20;
        private LinkedList<KeyEvent> keyFifo = new LinkedList<>();
        private String textEvent = null;
        private Pair<Integer, Pair<Integer, Integer>> commandEvent = null;
        private int counter;
        private boolean needsSend = false;
        private volatile boolean running = true;

        @Override
        public void run() {
            NLog.d(TAG, "Wifi client thread start");

            while (running) {
                if (needsSend) {
                    synchronized (keyFifo) {
                        KeyEvent event = keyFifo.poll();
                        while (event != null) {
                            sendAndroidKeyEvent(event);
                            event = keyFifo.poll();
                        }
                    }
                    sendKeyStates(port);
                    if (textEvent != null) {
                        synchronized (textEvent) {
                            sendTextEvent(textEvent);
                            textEvent = null;
                        }
                    }
                    if (commandEvent != null) {
                        synchronized (commandEvent) {
                            Pair<Integer, Integer> params = commandEvent.second;
                            sendCommandEvent(commandEvent.first, params.first, params.second);
                            commandEvent = null;
                        }
                    }
                    needsSend = false;
                    counter = 0;
                } else {
                    counter += DELAY;
                }
                if (counter >= 300) {
                    needsSend = true;
                }
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException ignored) {
                }
            }
            NLog.d(TAG, "Wifi client thread stop");
        }

        public void forceSend() {
            needsSend = true;
        }

        public void finish() {
            running = false;
        }

        public void sendAndroidKeyEventForce(KeyEvent event) {
            synchronized (keyFifo) {
                keyFifo.add(event);
            }
            needsSend = true;
        }

        public void sendTextEventForce(String event) {
            textEvent = event;
            needsSend = true;
        }

        public void sendCommandEventForce(int command, int param0, int param1) {
            commandEvent = new Pair<>(command, new Pair<>(param0, param1));
            needsSend = true;
        }
    }

}
