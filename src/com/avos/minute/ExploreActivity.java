package com.avos.minute;

import com.example.videokitsample.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;

public class ExploreActivity extends FragmentActivity {
    private static final String TAG = ExploreActivity.class.getSimpleName();

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);
        ActionBar bar = getActionBar();
        bar.setTitle("Explore");
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setDisplayShowHomeEnabled(true);
        
        mViewPager = (ViewPager)findViewById(R.id.pager);
//        ActionBar.Tab tabA = (ActionBar.Tab)this.findViewById(R.id.tab1);
//        ActionBar.Tab tabB = (ActionBar.Tab)this.findViewById(R.id.tab2);
//        ActionBar.Tab tabC = (ActionBar.Tab)this.findViewById(R.id.tab3);
//        tabA.setTabListener(new MyTabsListener());
//        tabB.setTabListener(new MyTabsListener());
//        tabC.setTabListener(new MyTabsListener());
//        bar.addTab(tabA);
//        bar.addTab(tabB);
//        bar.addTab(tabC);
        
        mTabsAdapter = new TabsAdapter(this, mViewPager);
//        mTabsAdapter.addTab(tabA.setText("Tab 1"),
//        Tab1.class, null);
//        mTabsAdapter.addTab(tabB.setText("Tab 2"),
//                Tab2.class, null);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
//        MenuItem shoot = menu.add("Shoot");
//        shoot.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }
    protected class MyTabsListener implements ActionBar.TabListener {
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub

        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub

        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub

        }        
    }
}
