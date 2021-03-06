package com.example.bartek.googlemaps1.DetailsActivities;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.example.bartek.googlemaps1.R;

public class DetailsActivity extends AppCompatActivity {
    private static final String TAG = "DetailsActivity";
    public static final String IntentTag = "userid";
    private FragmentPagerAdapter fragmentPagerAdapter;
    private static int userID;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userID = getIntent().getIntExtra(IntentTag, 0);
        setContentView(R.layout.activity_details);
        ViewPager viewPager = (ViewPager) findViewById(R.id.vpPager);
        fragmentPagerAdapter = new detailsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private static class detailsPagerAdapter extends FragmentPagerAdapter{
        private static int NUM_ITEMS = 2;

        public detailsPagerAdapter(android.support.v4.app.FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    return DetailsInfoActivity.newInstance(userID);
                case 1:
                    return DetailsMapActivity.newInstance(userID);
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case 0:
                    return "Info";
                case 1:
                    return "Map";
                default: return "Info";
            }
        }
    }
}
