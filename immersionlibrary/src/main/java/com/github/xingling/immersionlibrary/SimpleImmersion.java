package com.github.xingling.immersionlibrary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class SimpleImmersion {
    private BarParams mBarParams;
    private BarConfig mConfig;

    private Activity mActivity;
    private Window mWindow;
    private ViewGroup mDecorView;
    private ViewGroup mContentView;

    private static final String NAVIGATIONBAR_IS_MIN = "navigationbar_is_min";

    private SimpleImmersion(Activity activity) {
        WeakReference<Activity> activityWeakReference = new WeakReference<>(activity);
        mActivity = activityWeakReference.get();
        mWindow = mActivity.getWindow();
        initParams();
    }

    private SimpleImmersion(Fragment fragment) {
        this(fragment.getActivity(), fragment);
    }

    private SimpleImmersion(Activity activity, Fragment fragment) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity不能为空!!!");
        }
        WeakReference<Activity> activityWeakReference = new WeakReference<>(activity);
        WeakReference<Fragment> fragmentWeakReference = new WeakReference<>(fragment);
        mActivity = activityWeakReference.get();
        mWindow = mActivity.getWindow();
        initParams();
    }

    /**
     * 初始化沉浸式默认参数
     * Init params.
     */
    private void initParams() {
        mDecorView = (ViewGroup) mWindow.getDecorView();
        mContentView = mDecorView.findViewById(android.R.id.content);
        mConfig = new BarConfig(mActivity);
        mBarParams = new BarParams();
    }

    /**
     * 初始化Activity
     * With immersion bar.
     *
     * @param activity the activity
     */
    public static SimpleImmersion with(@NonNull Activity activity) {
        if (activity == null)
            throw new IllegalArgumentException("Activity不能为null");
        return new SimpleImmersion(activity);
    }

    /**
     * 调用该方法必须保证加载Fragment的Activity先初始化,已过时，使用with(Activity activity, Fragment fragment)方法
     * With immersion bar.
     *
     * @param fragment the fragment
     */
    public static SimpleImmersion with(@NonNull Fragment fragment) {
        if (fragment == null)
            throw new IllegalArgumentException("Fragment不能为null");
        return new SimpleImmersion(fragment);
    }

    public static SimpleImmersion with(@NonNull Activity activity, @NonNull Fragment fragment) {
        if (activity == null)
            throw new IllegalArgumentException("Activity不能为null");
        if (fragment == null)
            throw new IllegalArgumentException("Fragment不能为null");
        return new SimpleImmersion(activity, fragment);
    }

    /**
     * 透明状态栏，默认透明
     */
    public SimpleImmersion transparentStatusBar() {
        mBarParams.statusBarColor = Color.TRANSPARENT;
        return this;
    }

    /**
     * 透明导航栏，默认黑色
     */
    public SimpleImmersion transparentNavigationBar() {
        mBarParams.navigationBarColor = Color.TRANSPARENT;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        mBarParams.fullScreen = true;
        return this;
    }

    /**
     * 透明状态栏和导航栏
     */
    public SimpleImmersion transparentBar() {
        mBarParams.statusBarColor = Color.TRANSPARENT;
        mBarParams.navigationBarColor = Color.TRANSPARENT;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        mBarParams.fullScreen = true;
        return this;
    }

    public SimpleImmersion statusBarColor(@ColorRes int statusBarColor) {
        return this.statusBarColorInt(ContextCompat.getColor(mActivity, statusBarColor));
    }

    public SimpleImmersion statusBarColorInt(@ColorInt int statusBarColor) {
        mBarParams.statusBarColor = statusBarColor;
        return this;
    }

    public SimpleImmersion navigationBarColor(@ColorRes int navigationBarColor) {
        return this.navigationBarColorInt(ContextCompat.getColor(mActivity, navigationBarColor));
    }

    public SimpleImmersion navigationBarColorInt(@ColorInt int navigationBarColor) {
        mBarParams.navigationBarColor = navigationBarColor;
        mBarParams.navigationBarColorTemp = mBarParams.navigationBarColor;
        return this;
    }

    public SimpleImmersion fullScreen(boolean isFullScreen) {
        mBarParams.fullScreen = isFullScreen;
        return this;
    }

    public SimpleImmersion statusBarDarkFont(boolean isDarkFont) {
        return statusBarDarkFont(isDarkFont, 0f);
    }

    public SimpleImmersion statusBarDarkFont(boolean isDarkFont, @FloatRange(from = 0f, to = 1f) float statusAlpha) {
        mBarParams.darkFont = isDarkFont;
        if (!isDarkFont)
            mBarParams.flymeOSStatusBarFontColor = 0;
        if (OSUtils.isSupportStatusBarDarkFont()) {
            mBarParams.statusBarAlpha = 0;
        } else {
            mBarParams.statusBarAlpha = statusAlpha;
        }
        return this;
    }

    public SimpleImmersion fitsSystemWindows(boolean fits) {
        mBarParams.fits = fits;
        return this;
    }

    public SimpleImmersion titleBar(View view) {
        if (view == null) {
            throw new IllegalArgumentException("View参数不能为空");
        }
        return titleBar(view, true);
    }

    public SimpleImmersion titleBar(View view, boolean statusBarFlag) {
        if (view == null) {
            throw new IllegalArgumentException("View参数不能为空");
        }
        mBarParams.titleBarView = view;
        mBarParams.statusBarFlag = statusBarFlag;
        setTitleBar();
        return this;
    }

    private void setTitleBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mBarParams.titleBarView != null) {
            final ViewGroup.LayoutParams layoutParams = mBarParams.titleBarView.getLayoutParams();
            if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT ||
                    layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                mBarParams.titleBarView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mBarParams.titleBarView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (mBarParams.titleBarHeight == 0)
                            mBarParams.titleBarHeight = mBarParams.titleBarView.getHeight() + mConfig.getStatusBarHeight();
                        if (mBarParams.titleBarPaddingTopHeight == 0)
                            mBarParams.titleBarPaddingTopHeight = mBarParams.titleBarView.getPaddingTop()
                                    + mConfig.getStatusBarHeight();
                        layoutParams.height = mBarParams.titleBarHeight;
                        mBarParams.titleBarView.setPadding(mBarParams.titleBarView.getPaddingLeft(),
                                mBarParams.titleBarPaddingTopHeight,
                                mBarParams.titleBarView.getPaddingRight(),
                                mBarParams.titleBarView.getPaddingBottom());
                        mBarParams.titleBarView.setLayoutParams(layoutParams);
                    }
                });
            } else {
                if (mBarParams.titleBarHeight == 0)
                    mBarParams.titleBarHeight = layoutParams.height + mConfig.getStatusBarHeight();
                if (mBarParams.titleBarPaddingTopHeight == 0)
                    mBarParams.titleBarPaddingTopHeight = mBarParams.titleBarView.getPaddingTop()
                            + mConfig.getStatusBarHeight();
                layoutParams.height = mBarParams.titleBarHeight;
                mBarParams.titleBarView.setPadding(mBarParams.titleBarView.getPaddingLeft(),
                        mBarParams.titleBarPaddingTopHeight,
                        mBarParams.titleBarView.getPaddingRight(),
                        mBarParams.titleBarView.getPaddingBottom());
                mBarParams.titleBarView.setLayoutParams(layoutParams);
            }
        }
    }

    /**
     * 通过上面配置后初始化后方可成功调用
     */
    public void init() {
        initBar();   //初始化沉浸式
        setStatusBarView();  //通过状态栏高度动态设置状态栏布局
        transformView();  //变色view
        keyboardEnable();  //解决软键盘与底部输入框冲突问题
        registerEMUI3_x();  //解决华为emui3.1或者3.0导航栏手动隐藏的问题
    }

    /**
     * 初始化状态栏和导航栏
     */
    private void initBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;  //防止系统栏隐藏时内容区域大小发生变化
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !OSUtils.isEMUI3_1()) {
                uiFlags = initBarAboveLOLLIPOP(uiFlags); //初始化5.0以上，包含5.0
                uiFlags = setStatusBarDarkFont(uiFlags); //android 6.0以上设置状态栏字体为暗色
                supportActionBar();
            } else {
                initBarBelowLOLLIPOP(); //初始化5.0以下，4.4以上沉浸式
                solveNavigation();  //解决android4.4有导航栏的情况下，activity底部被导航栏遮挡的问题和android 5.0以下解决状态栏和布局重叠问题
            }
            uiFlags = hideBar(uiFlags);  //隐藏状态栏或者导航栏
            mWindow.getDecorView().setSystemUiVisibility(uiFlags);
        }
        if (OSUtils.isMIUI6Later())
            setMIUIStatusBarDarkFont(mWindow, mBarParams.darkFont);         //修改miui状态栏字体颜色
        if (OSUtils.isFlymeOS4Later()) {          // 修改Flyme OS状态栏字体颜色
            if (mBarParams.flymeOSStatusBarFontColor != 0) {
                FlymeOSStatusBarFontUtils.setStatusBarDarkIcon(mActivity, mBarParams.flymeOSStatusBarFontColor);
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    FlymeOSStatusBarFontUtils.setStatusBarDarkIcon(mActivity, mBarParams.darkFont);
            }
        }
    }

    /**
     * 通过状态栏高度动态设置状态栏布局
     */
    private void setStatusBarView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mBarParams.statusBarViewByHeight != null) {
            ViewGroup.LayoutParams params = mBarParams.statusBarViewByHeight.getLayoutParams();
            params.height = mConfig.getStatusBarHeight();
            mBarParams.statusBarViewByHeight.setLayoutParams(params);
        }
    }

    /**
     * 变色view
     * <p>
     * Transform view.
     */
    private void transformView() {
        if (mBarParams.viewMap.size() != 0) {
            Set<Map.Entry<View, Map<Integer, Integer>>> entrySet = mBarParams.viewMap.entrySet();
            for (Map.Entry<View, Map<Integer, Integer>> entry : entrySet) {
                View view = entry.getKey();
                Map<Integer, Integer> map = entry.getValue();
                Integer colorBefore = mBarParams.statusBarColor;
                Integer colorAfter = mBarParams.statusBarColorTransform;
                for (Map.Entry<Integer, Integer> integerEntry : map.entrySet()) {
                    colorBefore = integerEntry.getKey();
                    colorAfter = integerEntry.getValue();
                }
                if (view != null) {
                    if (Math.abs(mBarParams.viewAlpha - 0.0f) == 0)
                        view.setBackgroundColor(ColorUtils.blendARGB(colorBefore, colorAfter, mBarParams.statusBarAlpha));
                    else
                        view.setBackgroundColor(ColorUtils.blendARGB(colorBefore, colorAfter, mBarParams.viewAlpha));
                }
            }
        }
    }

    /**
     * 解决底部输入框与软键盘问题
     * Keyboard enable.
     */
    private void keyboardEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (mBarParams.keyboardPatch == null) {
                mBarParams.keyboardPatch = KeyboardPatch.patch(mActivity, mWindow);
            }
            mBarParams.keyboardPatch.setBarParams(mBarParams);
            if (mBarParams.keyboardEnable) {  //解决软键盘与底部输入框冲突问题
                mBarParams.keyboardPatch.enable(mBarParams.keyboardMode);
            } else {
                mBarParams.keyboardPatch.disable(mBarParams.keyboardMode);
            }
        }
    }

    /**
     * 注册emui3.x导航栏监听函数
     * Register emui 3 x.
     */
    private void registerEMUI3_x() {
        if ((OSUtils.isEMUI3_1() || OSUtils.isEMUI3_0()) && mConfig.hasNavigtionBar()
                && mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
            if (mBarParams.navigationStatusObserver == null && mBarParams.navigationBarView != null) {
                mBarParams.navigationStatusObserver = new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        int navigationBarIsMin = Settings.System.getInt(mActivity.getContentResolver(),
                                NAVIGATIONBAR_IS_MIN, 0);
                        if (navigationBarIsMin == 1) {
                            //导航键隐藏了
                            mBarParams.navigationBarView.setVisibility(View.GONE);
                            mContentView.setPadding(0, mContentView.getPaddingTop(), 0, 0);
                        } else {
                            //导航键显示了
                            mBarParams.navigationBarView.setVisibility(View.VISIBLE);
                            if (!mBarParams.systemWindows) {
                                if (mConfig.isNavigationAtBottom())
                                    mContentView.setPadding(0, mContentView.getPaddingTop(), 0, mConfig.getNavigationBarHeight());
                                else
                                    mContentView.setPadding(0, mContentView.getPaddingTop(), mConfig.getNavigationBarWidth(), 0);
                            } else
                                mContentView.setPadding(0, mContentView.getPaddingTop(), 0, 0);
                        }
                    }
                };
            }
            mActivity.getContentResolver().registerContentObserver(Settings.System.getUriFor
                    (NAVIGATIONBAR_IS_MIN), true, mBarParams.navigationStatusObserver);
        }
    }

    /**
     * 初始化android 5.0以上状态栏和导航栏
     *
     * @param uiFlags the ui flags
     * @return the int
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int initBarAboveLOLLIPOP(int uiFlags) {
        uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;  //Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态栏遮住。
        if (mBarParams.fullScreen && mBarParams.navigationBarEnable) {
            uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION; //Activity全屏显示，但导航栏不会被隐藏覆盖，导航栏依然可见，Activity底部布局部分会被导航栏遮住。
        }
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (mConfig.hasNavigtionBar()) {  //判断是否存在导航栏
            mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);  //需要设置这个才能设置状态栏颜色
        if (mBarParams.statusBarFlag)
            mWindow.setStatusBarColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                    mBarParams.statusBarColorTransform, mBarParams.statusBarAlpha));  //设置状态栏颜色
        else
            mWindow.setStatusBarColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                    Color.TRANSPARENT, mBarParams.statusBarAlpha));  //设置状态栏颜色
        if (mBarParams.navigationBarEnable)
            mWindow.setNavigationBarColor(ColorUtils.blendARGB(mBarParams.navigationBarColor,
                    mBarParams.navigationBarColorTransform, mBarParams.navigationBarAlpha));  //设置导航栏颜色
        return uiFlags;
    }

    /**
     * 初始化android 4.4和emui3.1状态栏和导航栏
     */
    private void initBarBelowLOLLIPOP() {
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//透明状态栏
        setupStatusBarView(); //创建一个假的状态栏
        if (mConfig.hasNavigtionBar()) {  //判断是否存在导航栏，是否禁止设置导航栏
            if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable)
                mWindow.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);//透明导航栏，设置这个，如果有导航栏，底部布局会被导航栏遮住
            else
                mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            setupNavBarView();   //创建一个假的导航栏
        }
    }

    /**
     * 设置一个可以自定义颜色的状态栏
     */
    private void setupStatusBarView() {
        if (mBarParams.statusBarView == null) {
            mBarParams.statusBarView = new View(mActivity);
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                mConfig.getStatusBarHeight());
        params.gravity = Gravity.TOP;
        mBarParams.statusBarView.setLayoutParams(params);
        if (mBarParams.statusBarFlag)
            mBarParams.statusBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                    mBarParams.statusBarColorTransform, mBarParams.statusBarAlpha));
        else
            mBarParams.statusBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.statusBarColor,
                    Color.TRANSPARENT, mBarParams.statusBarAlpha));
        mBarParams.statusBarView.setVisibility(View.VISIBLE);
        ViewGroup viewGroup = (ViewGroup) mBarParams.statusBarView.getParent();
        if (viewGroup != null)
            viewGroup.removeView(mBarParams.statusBarView);
        mDecorView.addView(mBarParams.statusBarView);
    }

    /**
     * 设置一个可以自定义颜色的导航栏
     */
    private void setupNavBarView() {
        if (mBarParams.navigationBarView == null) {
            mBarParams.navigationBarView = new View(mActivity);
        }
        FrameLayout.LayoutParams params;
        if (mConfig.isNavigationAtBottom()) {
            params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mConfig.getNavigationBarHeight());
            params.gravity = Gravity.BOTTOM;
        } else {
            params = new FrameLayout.LayoutParams(mConfig.getNavigationBarWidth(), FrameLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.END;
        }
        mBarParams.navigationBarView.setLayoutParams(params);
        if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
            if (!mBarParams.fullScreen && (mBarParams.navigationBarColorTransform == Color.TRANSPARENT)) {
                mBarParams.navigationBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.navigationBarColor,
                        Color.BLACK, mBarParams.navigationBarAlpha));
            } else {
                mBarParams.navigationBarView.setBackgroundColor(ColorUtils.blendARGB(mBarParams.navigationBarColor,
                        mBarParams.navigationBarColorTransform, mBarParams.navigationBarAlpha));
            }
        } else
            mBarParams.navigationBarView.setBackgroundColor(Color.TRANSPARENT);
        mBarParams.navigationBarView.setVisibility(View.VISIBLE);
        ViewGroup viewGroup = (ViewGroup) mBarParams.navigationBarView.getParent();
        if (viewGroup != null)
            viewGroup.removeView(mBarParams.navigationBarView);
        mDecorView.addView(mBarParams.navigationBarView);
    }

    /**
     * Sets status bar dark font.
     * 设置状态栏字体颜色，android6.0以上
     */
    private int setStatusBarDarkFont(int uiFlags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mBarParams.darkFont) {
            return uiFlags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            return uiFlags;
        }
    }

    /**
     * 支持actionBar的界面
     * Support action bar.
     */
    private void supportActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !OSUtils.isEMUI3_1()) {
            for (int i = 0, count = mContentView.getChildCount(); i < count; i++) {
                View childView = mContentView.getChildAt(i);
                if (childView instanceof ViewGroup) {
                    mBarParams.systemWindows = childView.getFitsSystemWindows();
                    if (mBarParams.systemWindows) {
                        mContentView.setPadding(0, 0, 0, 0);
                        return;
                    }
                }
            }
            if (mBarParams.isSupportActionBar) {
                mContentView.setPadding(0, mConfig.getStatusBarHeight() + mConfig.getActionBarHeight(), 0, 0);
            } else {
                if (mBarParams.fits)
                    mContentView.setPadding(0, mConfig.getStatusBarHeight(), 0, 0);
                else
                    mContentView.setPadding(0, 0, 0, 0);
            }
        }
    }

    /**
     * 解决安卓4.4和EMUI3.1导航栏与状态栏的问题，以及系统属性fitsSystemWindows的坑
     */
    private void solveNavigation() {
        for (int i = 0, count = mContentView.getChildCount(); i < count; i++) {
            View childView = mContentView.getChildAt(i);
            if (childView instanceof ViewGroup) {
                if (childView instanceof DrawerLayout) {
                    View childAt1 = ((DrawerLayout) childView).getChildAt(0);
                    if (childAt1 != null) {
                        mBarParams.systemWindows = childAt1.getFitsSystemWindows();
                        if (mBarParams.systemWindows) {
                            mContentView.setPadding(0, 0, 0, 0);
                            return;
                        }
                    }
                } else {
                    mBarParams.systemWindows = childView.getFitsSystemWindows();
                    if (mBarParams.systemWindows) {
                        mContentView.setPadding(0, 0, 0, 0);
                        return;
                    }
                }
            }

        }
        // 解决android4.4有导航栏的情况下，activity底部被导航栏遮挡的问题
        if (mConfig.hasNavigtionBar() && !mBarParams.fullScreenTemp && !mBarParams.fullScreen) {
            if (mConfig.isNavigationAtBottom()) { //判断导航栏是否在底部
                if (!mBarParams.isSupportActionBar) { //判断是否支持actionBar
                    if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
                        if (mBarParams.fits)
                            mContentView.setPadding(0, mConfig.getStatusBarHeight(),
                                    0, mConfig.getNavigationBarHeight()); //有导航栏，获得rootView的根节点，然后设置距离底部的padding值为导航栏的高度值
                        else
                            mContentView.setPadding(0, 0, 0, mConfig.getNavigationBarHeight());
                    } else {
                        if (mBarParams.fits)
                            mContentView.setPadding(0, mConfig.getStatusBarHeight(), 0, 0);
                        else
                            mContentView.setPadding(0, 0, 0, 0);
                    }
                } else {
                    //支持有actionBar的界面
                    if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable)
                        mContentView.setPadding(0, mConfig.getStatusBarHeight() +
                                mConfig.getActionBarHeight() + 10, 0, mConfig.getNavigationBarHeight());
                    else
                        mContentView.setPadding(0, mConfig.getStatusBarHeight() +
                                mConfig.getActionBarHeight() + 10, 0, 0);
                }
            } else {
                if (!mBarParams.isSupportActionBar) {
                    if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable) {
                        if (mBarParams.fits)
                            mContentView.setPadding(0, mConfig.getStatusBarHeight(),
                                    mConfig.getNavigationBarWidth(), 0); //不在底部，设置距离右边的padding值为导航栏的宽度值
                        else
                            mContentView.setPadding(0, 0, mConfig.getNavigationBarWidth(), 0);
                    } else {
                        if (mBarParams.fits)
                            mContentView.setPadding(0, mConfig.getStatusBarHeight(), 0, 0);
                        else
                            mContentView.setPadding(0, 0, 0, 0);
                    }
                } else {
                    //支持有actionBar的界面
                    if (mBarParams.navigationBarEnable && mBarParams.navigationBarWithKitkatEnable)
                        mContentView.setPadding(0, mConfig.getStatusBarHeight() +
                                mConfig.getActionBarHeight() + 10, mConfig.getNavigationBarWidth(), 0);
                    else
                        mContentView.setPadding(0, mConfig.getStatusBarHeight() +
                                mConfig.getActionBarHeight() + 10, 0, 0);
                }
            }
        } else {
            if (!mBarParams.isSupportActionBar) {
                if (mBarParams.fits)
                    mContentView.setPadding(0, mConfig.getStatusBarHeight(), 0, 0);
                else
                    mContentView.setPadding(0, 0, 0, 0);
            } else {
                //支持有actionBar的界面
                mContentView.setPadding(0, mConfig.getStatusBarHeight() + mConfig.getActionBarHeight() + 10, 0, 0);
            }
        }
    }

    /**
     * Hide bar.
     * 隐藏或显示状态栏和导航栏。
     *
     * @param uiFlags the ui flags
     * @return the int
     */

    private int hideBar(int uiFlags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            switch (mBarParams.barHide) {
                case FLAG_HIDE_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.INVISIBLE;
                    break;
                case FLAG_HIDE_STATUS_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.INVISIBLE;
                    break;
                case FLAG_HIDE_NAVIGATION_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                    break;
                case FLAG_SHOW_BAR:
                    uiFlags |= View.SYSTEM_UI_FLAG_VISIBLE;
                    break;
            }
        }
        return uiFlags | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    }

    /**
     * 设置状态栏字体图标为深色，需要MIUIV6以上
     *
     * @return boolean 成功执行返回true
     */
    private void setMIUIStatusBarDarkFont(Window window, boolean darkFont) {
        if (window != null) {
            Class clazz = window.getClass();
            try {
                int darkModeFlag;
                Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
                if (darkFont) {
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag);//状态栏透明且黑色字体
                } else {
                    extraFlagField.invoke(window, 0, darkModeFlag);//清除黑色字体
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public BarParams getBarParams() {
        return mBarParams;
    }
}
