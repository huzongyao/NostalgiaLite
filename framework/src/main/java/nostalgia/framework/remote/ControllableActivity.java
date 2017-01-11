package nostalgia.framework.remote;

import android.support.v7.app.AppCompatActivity;

import nostalgia.framework.remote.wifi.WifiServerInfoTransmitter;

public abstract class ControllableActivity extends AppCompatActivity {

    private WifiServerInfoTransmitter infoTransmitter;

    @Override
    protected void onResume() {
        super.onResume();
        startWifiListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopWifiListening();
    }

    protected void startWifiListening() {
        VirtualDPad.getInstance().onResume(getWindow());

        if (infoTransmitter != null) {
            infoTransmitter.stopSending();
        }

        infoTransmitter = new WifiServerInfoTransmitter(this, "");
        infoTransmitter.startSending();
    }

    protected void stopWifiListening() {
        VirtualDPad.getInstance().onPause();

        if (infoTransmitter != null) {
            infoTransmitter.stopSending();
        }
    }

}
