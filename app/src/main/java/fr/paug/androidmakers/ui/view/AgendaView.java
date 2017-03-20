package fr.paug.androidmakers.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import fr.paug.androidmakers.R;
import fr.paug.androidmakers.model.ScheduleSlot;

/**
 * Created by stan on 18/03/2017.
 */
public class AgendaView extends ScrollView {

    private static final long OFFSET_MS = DateUtils.HOUR_IN_MILLIS;

    private static int[] sBackgrounds = {
            R.drawable.bg_box_blue_selector,
            R.drawable.bg_box_red_selector,
            R.drawable.bg_box_green_selector
    };
    private Calendar mCalendar = Calendar.getInstance();
    private SparseArray<String> mTimeLabel = new SparseArray<>();
    private int mTimeWidth;
    private long mFirstHour;
    private Paint mLinePaint;
    private TextPaint mTextPaint;

    public AgendaView(Context context) {
        super(context);
        init();
    }

    public AgendaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AgendaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AgendaView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setAgenda(Items items, AgendaClickListener listener) {
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;

        SparseArray<List<Item>> agenda = items.getItems();
        int[] keys = new int[agenda.size()];
        for (int i = 0; i < agenda.size(); i++) {
            keys[i] = agenda.keyAt(i);

            List<Item> agendaPerTrack = agenda.get(keys[i]);
            for (Item agendaItem : agendaPerTrack) {
                start = Math.min(start, agendaItem.getStartTimestamp());
                end = Math.max(end, agendaItem.getEndTimestamp());
            }
        }

        removeAllViews();
        if (end - start > DateUtils.DAY_IN_MILLIS) {
            mFirstHour = 0;
            return; // more than a day? It's a bug
        }

        Context context = getContext();
        LinearLayout rootView = new LinearLayout(context);
        rootView.setOrientation(LinearLayout.HORIZONTAL);

        mFirstHour = getClosestHourTimestamp(start - OFFSET_MS);

        // add all agenda items

        for (int i = 0; i < agenda.size(); i++) {
            List<Item> agendaPerTrack = agenda.get(keys[i]);
            // for track 1
            FrameLayout frameLayout = new FrameLayout(context);
            for (Item agendaItem : agendaPerTrack) {
                long startDiffWithBegin = agendaItem.getStartTimestamp() - mFirstHour;
                long endDiffWithBegin = agendaItem.getEndTimestamp() - mFirstHour;
                long duration = endDiffWithBegin - startDiffWithBegin;

                TextView textView = new TextView(context);
                textView.setText(agendaItem.getTitle());
                textView.setTextColor(Color.WHITE);
                textView.getPaint().setFakeBoldText(true);
                textView.setBackgroundResource(
                        sBackgrounds[Math.abs(agendaItem.getRoomColorIndex()) % sBackgrounds.length]
                );
                textView.setOnClickListener(new ViewClickListener(listener, agendaItem));

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        getPxFromDurationMs(duration));
                lp.topMargin = getPxFromDurationMs(startDiffWithBegin);
                frameLayout.addView(textView, lp);
            }
            frameLayout.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
            ));
            rootView.addView(frameLayout);
        }
        addView(rootView);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mFirstHour > 0) {
            int y = 0;
            for (long timestamp = mFirstHour + DateUtils.HOUR_IN_MILLIS;
                 y < getScrollY() + getHeight(); timestamp += DateUtils.HOUR_IN_MILLIS) {

                y = getPxFromDurationMs(timestamp - mFirstHour);
                String label = getTimeLabel(timestamp);
                drawLineHour(canvas, y, label);
            }
        }
        super.onDraw(canvas);
    }

    private String getTimeLabel(long timestamp) {
        int hash = Long.valueOf(timestamp).hashCode();
        String label = mTimeLabel.get(hash);
        if (label == null) {
            mCalendar.setTimeInMillis(timestamp);
            label = String.format(Locale.getDefault(), "%02d:00",
                    mCalendar.get(Calendar.HOUR_OF_DAY));
        }
        return label;
    }

    private void drawLineHour(Canvas canvas, int y, String label) {
        canvas.drawLine(mTimeWidth, y, getWidth(), y, mLinePaint);

        float measureText = mTextPaint.measureText(label);
        canvas.drawText(label,
                0, label.length(),
                mTimeWidth / 2 - measureText / 2f,
                y + (mTextPaint.getTextSize() - mTextPaint.descent()) / 2,
                mTextPaint);
    }

    private long getClosestHourTimestamp(long timestamp) {
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(timestamp);
        instance.add(Calendar.HOUR, 1);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        return instance.getTimeInMillis();
    }

    private int getPxFromDurationMs(long ms) {
        float sp = 150.0f * ms / DateUtils.HOUR_IN_MILLIS;
        return getPixelFromSp(sp);
    }

    private int getPixelFromSp(float sp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getResources().getDisplayMetrics()));
    }

    private void init() {
        setWillNotDraw(false);
        setFillViewport(true);
        setClipChildren(false);

        mLinePaint = new Paint();
        mLinePaint.setColor(Color.GRAY);
        mLinePaint.setStrokeWidth(1f);

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(getPixelFromSp(14));

        mTimeWidth = getPixelFromSp(50);

        int paddingBottom = getResources().getDimensionPixelSize(R.dimen.padding);
        setPadding(mTimeWidth, paddingBottom, 0, paddingBottom);
    }

    public interface AgendaClickListener {
        void onClick(Item agendaItem);
    }

    public static class Items {
        private final String mTitle;
        private final SparseArray<List<AgendaView.Item>> mItems;

        public Items(String title, SparseArray<List<Item>> items) {
            mTitle = title;
            mItems = items;
        }

        public String getTitle() {
            return mTitle;
        }

        public SparseArray<List<Item>> getItems() {
            return mItems;
        }
    }

    public static class Item {

        private ScheduleSlot mScheduleSlot;
        private String mTitle;

        public Item(ScheduleSlot scheduleSlot, String title) {
            mScheduleSlot = scheduleSlot;
            mTitle = title;
        }

        public long getStartTimestamp() {
            return mScheduleSlot.startDate;
        }

        public long getEndTimestamp() {
            return mScheduleSlot.endDate;
        }

        public String getTitle() {
            return mTitle;
        }

        public int getRoomColorIndex() {
            return mScheduleSlot.room;
        }

        public int getSessionId() {
            return mScheduleSlot.sessionId;
        }
    }


    private static class ViewClickListener implements OnClickListener {

        private AgendaClickListener mAgendaClickListener;
        private Item mAgendaItem;

        public ViewClickListener(AgendaClickListener agendaClickListener, Item agendaItem) {
            mAgendaClickListener = agendaClickListener;
            mAgendaItem = agendaItem;
        }

        @Override
        public void onClick(View v) {
            mAgendaClickListener.onClick(mAgendaItem);
        }
    }
}
