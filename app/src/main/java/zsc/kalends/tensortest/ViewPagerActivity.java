package zsc.kalends.tensortest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

public class ViewPagerActivity extends AppCompatActivity implements TextView.OnClickListener{

    private ImageView[] imageViews;
    private ViewPager viewPager;
    private ImageView imageView;
    private ViewGroup group;
    private TextView textView;
    private ArrayList<View> pageview;

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_caption);
        viewPager = findViewById(R.id.viewPager);
        LayoutInflater factory = LayoutInflater.from(ViewPagerActivity.this);
        final View textEntryView = factory.inflate(R.layout.page3, null);
        textView = textEntryView.findViewById(R.id.btn_start);
        if (textView != null) {
            Log.e("ViewPager", textView.getText().toString());
            textView.setOnClickListener(this);
        }

        LayoutInflater inflater = getLayoutInflater();
        View view1 = inflater.inflate(R.layout.page1, null);
        View view2 = inflater.inflate(R.layout.page2, null);
        View view3 = inflater.inflate(R.layout.page3, null);

        pageview = new ArrayList<>();
        pageview.add(view1);
        pageview.add(view2);
        pageview.add(view3);

        group = findViewById(R.id.viewGroup);
        imageViews = new ImageView[pageview.size()];
        for (int i = 0; i < pageview.size(); i++) {
            imageView = new ImageView(ViewPagerActivity.this);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(20, 20));
            imageView.setPadding(20, 0, 20, 0);
            imageViews[i] = imageView;

            if (0 == i) {
                imageViews[i].setBackgroundResource(R.drawable.page_indicator_focused);
            } else {
                imageViews[i].setBackgroundResource(R.drawable.page_indicator_unfocused);
            }

            group.addView(imageViews[i]);
        }

        viewPager.setAdapter(mPagerAdapter);
        viewPager.setOnPageChangeListener(new GuidePageChangeListener());

    }

    PagerAdapter mPagerAdapter = new PagerAdapter() {
        @Override
        public int getCount() {
            return pageview.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        public void destroyItem(@NonNull View view, int index, @NonNull Object object) {
            ((ViewPager)view).removeView(pageview.get(index));
        }

        public Object instantiateItem(@NonNull View view, int index) {
            ((ViewPager)view).addView(pageview.get(index));
            return pageview.get(index);
        }
    };

    @Override
    public void onClick(View view) {
        startActivity(new Intent(ViewPagerActivity.this, MainActivity.class));
        finish();
    }

    class GuidePageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            for (int i = 0; i < imageViews.length; i++) {
                if (position == i) {
                    imageViews[position].setBackgroundResource(R.drawable.page_indicator_focused);
                } else {
                    imageViews[i].setBackgroundResource(R.drawable.page_indicator_unfocused);
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}
