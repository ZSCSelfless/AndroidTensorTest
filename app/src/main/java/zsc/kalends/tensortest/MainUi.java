package zsc.kalends.tensortest;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import yalantis.com.sidemenu.interfaces.Resourceble;
import yalantis.com.sidemenu.interfaces.ScreenShotable;
import yalantis.com.sidemenu.model.SlideMenuItem;
import yalantis.com.sidemenu.util.ViewAnimator;
import zsc.kalends.tensortest.fragment.CameraContentFragment;
import zsc.kalends.tensortest.fragment.UserContentFragment;

public class MainUi extends AppCompatActivity implements ViewAnimator.ViewAnimatorListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private List<SlideMenuItem> list = new ArrayList<>();
    private ViewAnimator viewAnimator;
    private LinearLayout linearLayout;
    private CameraContentFragment cameraContentFragment;
    private UserContentFragment userContentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        cameraContentFragment = (CameraContentFragment) getSupportFragmentManager().findFragmentByTag(cameraContentFragment.TAG);
        if (cameraContentFragment == null) {
            cameraContentFragment = new CameraContentFragment();
            transaction.add(R.id.fragment_camera, cameraContentFragment, CameraContentFragment.TAG);
        }
        ((FragmentTransaction) transaction).show(cameraContentFragment);
        transaction.commit();
        /*userContentFragment = new UserContentFragment();*/
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setScrimColor(Color.TRANSPARENT);
        linearLayout = findViewById(R.id.left_drawer);
        linearLayout.setOnClickListener(view -> drawerLayout.closeDrawers());

        setActionBar();
        createMenuList();
        viewAnimator = new ViewAnimator<>(this, list, cameraContentFragment,drawerLayout,this);
    }

    @Override
    public ScreenShotable onSwitch(Resourceble slideMenuItem, ScreenShotable screenShotable, int position) {
        switch (slideMenuItem.getName()) {
            case "Close":
                return screenShotable;
            case "Video":
                return cameraContentFragment;
//            case "User":
//                return userContentFragment;
            default:
                return screenShotable;
        }

    }

    @Override
    public void disableHomeButton() {
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(false);
    }

    @Override
    public void enableHomeButton() {
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        drawerLayout.closeDrawers();
    }

    @Override
    public void addViewToContainer(View view) {
        linearLayout.addView(view);
    }

    private void createMenuList() {
        SlideMenuItem menuItem0 = new SlideMenuItem("CLOSE", R.mipmap.icn_close);
        list.add(menuItem0);

        SlideMenuItem menuItem1 = new SlideMenuItem("Video", R.mipmap.icn_1);
        list.add(menuItem1);

        SlideMenuItem menuItem2 = new SlideMenuItem("Description", R.mipmap.icn_3);
        list.add(menuItem2);

        SlideMenuItem menuItem3 = new SlideMenuItem("Chat", R.mipmap.icn_4);
        list.add(menuItem3);

        SlideMenuItem menuItem4 = new SlideMenuItem("User", R.mipmap.icn_2);
        list.add(menuItem4);
    }

    private void setActionBar() {
        Toolbar tooolbar = findViewById(R.id.toolbar);
        setSupportActionBar(tooolbar);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, tooolbar, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                linearLayout.removeAllViews();
                linearLayout.invalidate();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if (slideOffset > 0.6 && linearLayout.getChildCount() == 0) {
                    viewAnimator.showMenuContent();
                }
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        drawerLayout.addDrawerListener(drawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }
}
