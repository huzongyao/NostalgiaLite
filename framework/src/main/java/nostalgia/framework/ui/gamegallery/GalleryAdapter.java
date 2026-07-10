package nostalgia.framework.ui.gamegallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import android.widget.CheckBox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import nostalgia.framework.R;

/**
 * 游戏列表适配器。
 * <p>
 * 实现 SectionIndexer 接口，支持快速滚动定位。
 * 支持按名称、插入日期、游玩次数、最近游玩四种排序方式，
 * 以及名称过滤和多选模式。
 * </p>
 */
public class GalleryAdapter extends BaseAdapter implements SectionIndexer {

    /** 按名称字母排序 */
    public static final int SORT_BY_NAME_ALPHA = 0;
    /** 按插入日期排序 */
    public static final int SORT_BY_INSERT_DATE = 1;
    /** 按游玩次数排序 */
    public static final int SORT_BY_MOST_PLAYED = 2;
    /** 按最近游玩时间排序 */
    public static final int SORT_BY_LAST_PLAYED = 3;

    private HashMap<Character, Integer> alphaIndexer = new HashMap<>();
    private String filter = "";
    private Character[] sections;
    private LayoutInflater inflater;
    private Context context;
    private int mainColor;
    private ArrayList<GameDescription> games = new ArrayList<>();
    private ArrayList<RowItem> filterGames = new ArrayList<>();
    private int sumRuns = 0;
    private int sortType = SORT_BY_NAME_ALPHA;
    private boolean isMultiSelectMode = false;
    private HashSet<String> selectedGameIds = new HashSet<>();

    private Comparator<GameDescription> nameComparator = (lhs, rhs) -> {
        return lhs.getSortName().compareTo(rhs.getSortName());
    };

    private Comparator<GameDescription> insertDateComparator = (lhs, rhs) ->
            Long.compare(rhs.insertTime, lhs.insertTime);

    private Comparator<GameDescription> lastPlayedDateComparator = (lhs, rhs) -> {
        long dif = lhs.lastGameTime - rhs.lastGameTime;
        if (dif == 0) {
            return 0;
        } else if (dif < 0) {
            return 1;
        } else {
            return -1;
        }
    };
    private Comparator<GameDescription> playedCountComparator = (lhs, rhs) ->
            -lhs.runCount + rhs.runCount;

    public GalleryAdapter(Context context) {
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mainColor = context.getResources().getColor(R.color.main_color);
    }

    @Override
    public int getCount() {
        return filterGames.size();
    }

    @Override
    public Object getItem(int position) {
        return filterGames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RowItem item = filterGames.get(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_game_list, null);
        }
        GameDescription game = item.game;
        
        TextView name = convertView.findViewById(R.id.row_game_item_name);
        TextView size = convertView.findViewById(R.id.row_game_item_size);
        TextView runs = convertView.findViewById(R.id.row_game_item_runs);
        TextView lastPlayed = convertView.findViewById(R.id.row_game_item_last_played);
        TextView ext = convertView.findViewById(R.id.row_game_item_ext);
        ImageView arrowIcon = convertView.findViewById(R.id.game_item_arrow);
        ProgressBar runIndicator = convertView.findViewById(R.id.row_game_item_progressBar);
        CheckBox checkBox = convertView.findViewById(R.id.game_item_checkbox);
        
        runIndicator.setMax(sumRuns);
        
        name.setText(game.getCleanName());
        name.setTextColor(mainColor);
        
        size.setText(game.getFileSizeFormatted());
        
        runs.setText(String.format(context.getString(R.string.gallery_play_count), game.runCount));
        
        if (game.lastGameTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            lastPlayed.setText(String.format(context.getString(R.string.gallery_last_played), 
                    sdf.format(new Date(game.lastGameTime))));
        } else {
            lastPlayed.setText(context.getString(R.string.gallery_never_played));
        }
        
        String extension = getFileExtension(game.name);
        ext.setText(extension.toUpperCase());
        
        arrowIcon.setImageResource(R.drawable.ic_next_arrow);
        arrowIcon.clearAnimation();
        
        // 处理多选模式
        if (isMultiSelectMode) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(selectedGameIds.contains(game.checksum));
            arrowIcon.setVisibility(View.GONE);
        } else {
            checkBox.setVisibility(View.GONE);
            arrowIcon.setVisibility(View.VISIBLE);
        }
        
        return convertView;
    }
    
    /** 获取文件扩展名 */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /** 设置过滤关键字 */
    public void setFilter(String filter) {
        this.filter = filter.toLowerCase();
        filterGames();
    }

    /** 设置游戏列表（替换） */
    public void setGames(ArrayList<GameDescription> games) {
        this.games = new ArrayList<>(games);
        filterGames();
    }

    /** 添加新游戏（去重），返回总数 */
    public int addGames(ArrayList<GameDescription> newGames) {
        for (GameDescription game : newGames) {
            if (!games.contains(game)) {
                games.add(game);
            }
        }
        filterGames();
        return games.size();
    }
    
    /** 获取游戏列表副本 */
    public ArrayList<GameDescription> getGames() {
        return new ArrayList<>(games);
    }

    /** 过滤和排序游戏列表 */
    private void filterGames() {
        filterGames.clear();
        switch (sortType) {
            case SORT_BY_NAME_ALPHA:
                Collections.sort(games, nameComparator);
                break;
            case SORT_BY_INSERT_DATE:
                Collections.sort(games, insertDateComparator);
                break;
            case SORT_BY_MOST_PLAYED:
                Collections.sort(games, playedCountComparator);
                break;
            case SORT_BY_LAST_PLAYED:
                Collections.sort(games, lastPlayedDateComparator);
                break;
        }
        String containsFilter = " " + filter;
        sumRuns = 0;
        for (GameDescription game : games) {
            if (game == null || game.getCleanName() == null || game.getCleanName().isEmpty()) {
                continue;
            }
            sumRuns = game.runCount > sumRuns ? game.runCount : sumRuns;
            String name = game.getCleanName().toLowerCase();
            boolean secondCondition = true;
            if (sortType == SORT_BY_LAST_PLAYED || sortType == SORT_BY_MOST_PLAYED) {
                secondCondition = game.lastGameTime != 0;
            }
            if ((name.startsWith(filter) || name.contains(containsFilter)) && secondCondition) {
                RowItem item = new RowItem();
                item.game = game;
                item.firstLetter = name.charAt(0);
                filterGames.add(item);
            }
        }

        alphaIndexer.clear();
        if (sortType == SORT_BY_NAME_ALPHA) {
            for (int i = 0; i < filterGames.size(); i++) {
                RowItem item = filterGames.get(i);
                char ch = item.firstLetter;
                if (!alphaIndexer.containsKey(ch)) {
                    alphaIndexer.put(ch, i);
                }
            }
        }
        super.notifyDataSetChanged();
    }

    /** 设置排序类型 */
    public void setSortType(int sortType) {
        this.sortType = sortType;
        filterGames();
    }

    @Override
    public int getPositionForSection(int section) {
        try {
            Character ch = Character.toLowerCase(sections[section]);
            Integer pos = alphaIndexer.get(ch);
            if (pos == null) {
                return 0;
            } else {
                return pos;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    @Override
    public int getSectionForPosition(int position) {
        RowItem item = (RowItem) getItem(position);
        char ch = Character.toUpperCase(item.firstLetter);
        for (int i = 0; i < sections.length; i++) {
            Character ch1 = sections[i];
            if (ch1.equals(ch)) {
                return i;
            }
        }
        return 1;
    }

    @Override
    public Object[] getSections() {
        Set<Character> keyset = alphaIndexer.keySet();
        sections = new Character[keyset.size()];
        keyset.toArray(sections);
        Arrays.sort(sections, Character::compareTo);
        for (int i = 0; i < sections.length; i++)
            sections[i] = Character.toUpperCase(sections[i]);
        return sections;
    }

    @Override
    public void notifyDataSetChanged() {
        filterGames();
    }

    /** 列表行数据项 */
    public class RowItem {
        GameDescription game;
        char firstLetter;
    }
    
    // 多选模式相关方法
    public void setMultiSelectMode(boolean isMultiSelect) {
        boolean wasMultiSelect = this.isMultiSelectMode;
        this.isMultiSelectMode = isMultiSelect;
        if (!isMultiSelect && wasMultiSelect) {
            selectedGameIds.clear();
        }
        super.notifyDataSetChanged();
    }
    
    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }
    
    public void toggleSelection(GameDescription game) {
        if (selectedGameIds.contains(game.checksum)) {
            selectedGameIds.remove(game.checksum);
        } else {
            selectedGameIds.add(game.checksum);
        }
        super.notifyDataSetChanged();
    }
    
    public HashSet<String> getSelectedGameIds() {
        return new HashSet<>(selectedGameIds);
    }
    
    public ArrayList<GameDescription> getSelectedGames() {
        ArrayList<GameDescription> result = new ArrayList<>();
        for (GameDescription game : games) {
            if (selectedGameIds.contains(game.checksum)) {
                result.add(game);
            }
        }
        return result;
    }
    
    public void selectAll() {
        for (GameDescription game : games) {
            selectedGameIds.add(game.checksum);
        }
        super.notifyDataSetChanged();
    }
    
    public void clearSelection() {
        selectedGameIds.clear();
        super.notifyDataSetChanged();
    }
    
    public int getSelectedCount() {
        return selectedGameIds.size();
    }

}
