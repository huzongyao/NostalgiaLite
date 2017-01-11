package nostalgia.framework.ui.remotecontroller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import nostalgia.framework.R;
import nostalgia.framework.remote.wifi.WifiServerInfoReceiver.DetectionResult;

public class ServerSelectAdapter extends ArrayAdapter<DetectionResult> {


    public ServerSelectAdapter(Context context, List<DetectionResult> objs) {
        super(context, R.layout.row_server_select_item, objs);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_server_select_item,
                    null);
        }

        DetectionResult item = getItem(position);
        TextView label1 = (TextView) convertView
                .findViewById(R.id.row_select_server_label1);
        label1.setText(item.desc);
        TextView label2 = (TextView) convertView
                .findViewById(R.id.row_select_server_label2);
        label2.setText(item.sessionDescription);
        label2.setVisibility(item.sessionDescription.equals("") ? View.GONE
                : View.VISIBLE);
        ImageView icon = (ImageView) convertView
                .findViewById(R.id.row_select_server_icon);

        switch (item.type) {
            case mobile:
                icon.setImageResource(R.drawable.ic_mobile);
                break;
            case tablet:
                icon.setImageResource(R.drawable.ic_tablet);
                break;
            case tv:
                icon.setImageResource(R.drawable.ic_tv);
                break;
        }

        return convertView;
    }

}
