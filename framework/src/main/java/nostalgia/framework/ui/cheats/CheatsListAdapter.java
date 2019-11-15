package nostalgia.framework.ui.cheats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import nostalgia.framework.R;

public class CheatsListAdapter extends ArrayAdapter<Cheat> {

    LayoutInflater inflater;
    CheatsActivity cheatsActivity;

    public CheatsListAdapter(CheatsActivity context, List<Cheat> objects) {
        super(context, 0, objects);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.cheatsActivity = context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        TextView chars;
        TextView desc;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_cheat_list_item, null);
            chars = convertView.findViewById(R.id.row_cheat_chars);
            desc = convertView.findViewById(R.id.row_cheat_desc);
        } else {
            chars = convertView.findViewById(R.id.row_cheat_chars);
            desc = convertView.findViewById(R.id.row_cheat_desc);
        }
        Cheat cheat = getItem(position);
        CheckBox enable = convertView.findViewById(R.id.row_cheat_enable);
        ImageButton edit = convertView.findViewById(R.id.row_cheat_edit);
        ImageButton remove = convertView.findViewById(R.id.row_cheat_remove);
        chars.setText(cheat.chars);
        desc.setText(cheat.desc);
        enable.setChecked(cheat.enable);
        edit.setOnClickListener(v -> cheatsActivity.editCheat(position));
        remove.setOnClickListener(v -> cheatsActivity.removeCheat(position));
        enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Cheat cheat1 = getItem(position);
            cheat1.enable = isChecked;
            cheatsActivity.saveCheats();
        });
        return convertView;
    }

}
