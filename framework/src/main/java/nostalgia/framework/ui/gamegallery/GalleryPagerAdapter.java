package nostalgia.framework.ui.gamegallery;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

import nostalgia.framework.R;
import nostalgia.framework.ui.gamegallery.GalleryAdapter.RowItem;
import nostalgia.framework.utils.NLog;

public class GalleryPagerAdapter extends PagerAdapter {

    public static final String EXTRA_POSITIONS = "EXTRA_POSITIONS";
    private final static int[] SORT_TYPES = new int[]{
            GalleryAdapter.SORT_BY_NAME_ALPHA,
            GalleryAdapter.SORT_BY_LAST_PLAYED,
            GalleryAdapter.SORT_BY_MOST_PLAYED,
    };
    private final String[] mTabTitles;
    private int[] yOffsets = new int[SORT_TYPES.length];
    private ListView[] lists = new ListView[SORT_TYPES.length];
    private GalleryAdapter[] listAdapters = new GalleryAdapter[SORT_TYPES.length];
    private Activity activity;
    private OnItemClickListener listener;

    public GalleryPagerAdapter(Activity activity, OnItemClickListener listener) {
        this.activity = activity;
        this.listener = listener;
        mTabTitles = activity.getResources().getStringArray(R.array.gallery_page_tab_names);
        for (int i = 0; i < SORT_TYPES.length; i++) {
            GalleryAdapter adapter = listAdapters[i] = new GalleryAdapter(activity);
            adapter.setSortType(SORT_TYPES[i]);
        }
    }

    @Override
    public int getCount() {
        return SORT_TYPES.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles[SORT_TYPES[position]];
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0.equals(arg1);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final ListView list = new ListView(activity);
        list.setCacheColorHint(0x00000000);
        list.setFastScrollEnabled(true);
        list.setSelector(R.drawable.row_game_item_list_selector);
        list.setAdapter(listAdapters[position]);
        list.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
            RowItem item = (RowItem) listAdapters[position].getItem(arg2);
            listener.onItemClick(item.game);
        });
        list.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                NLog.i("list", position + ":" + scrollState + "");
                if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    yOffsets[position] = list.getFirstVisiblePosition();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });
        list.setSelection(yOffsets[position]);
        lists[position] = list;
        container.addView(list);
        return list;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    public void setGames(ArrayList<GameDescription> games) {
        for (GalleryAdapter adapter : listAdapters) {
            adapter.setGames(new ArrayList<>(games));
        }
    }


    public int addGames(ArrayList<GameDescription> newGames) {
        int result = 0;
        for (GalleryAdapter adapter : listAdapters) {
            result = adapter.addGames(new ArrayList<>(newGames));
        }
        return result;
    }

    public void setFilter(String filter) {
        for (GalleryAdapter adapter : listAdapters) {
            adapter.setFilter(filter);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        for (int i = 0; i < SORT_TYPES.length; i++) {
            GalleryAdapter adapter = listAdapters[i];
            adapter.notifyDataSetChanged();
            if (lists[i] != null)
                lists[i].setSelection(yOffsets[i]);
        }
        super.notifyDataSetChanged();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putIntArray(EXTRA_POSITIONS, yOffsets);
    }

    public void onRestoreInstanceState(Bundle inState) {
        if (inState != null) {
            yOffsets = inState.getIntArray(EXTRA_POSITIONS);
            if (yOffsets == null)
                yOffsets = new int[mTabTitles.length];
        }
    }

    public interface OnItemClickListener {
        void onItemClick(GameDescription game);
    }

}