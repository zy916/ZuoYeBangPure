package com.zuoyebang.pure;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class ZuoYeBangPureHook extends XposedModule {
    private static final String TAG = "ZuoYeBangPure";
    private static final String TARGET = "com.baidu.homework";

    // Build variant switch: when true the bottom bar keeps only "home" and "me".
    private static final boolean SIMPLIFY_TABS = false;

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET.equals(param.getPackageName())) return;
        Log.i(TAG, "ZuoYeBangPure loaded: " + param.getPackageName());
        install(this, param.getClassLoader());
    }

    private static void install(XposedInterface xposed, ClassLoader cl) {
        blockAds(xposed, cl);
        blockAdSdk(xposed, cl);
        blockUpdate(xposed, cl);
        blockVideoWindow(xposed, cl);
        blockExplainLabel(xposed, cl);
        logWebActions(xposed, cl);
        hideViews(xposed, cl);
        injectExplainJs(xposed, cl);
        if (SIMPLIFY_TABS) simplifyBottomTabs(xposed, cl);
        Log.i(TAG, "ZuoYeBangPure install done");
    }

    private static void blockAds(XposedInterface xposed, ClassLoader cl) {
        // Central adx ad loader in the app
        ReflectUtils.hookAllMethodsDefault(xposed, "com.baidu.homework.adx.AdManager", cl, "load", null);
        ReflectUtils.hookAllMethodsDefault(xposed, "com.baidu.homework.adx.AdManager_Impl", cl, "load", null);
        ReflectUtils.hookAllMethodsDefault(xposed, "com.baidu.homework.adx.AdManager_Impl", cl, "from", null);
        Log.i(TAG, "ad manager blocked");
    }

    private static void blockAdSdk(XposedInterface xposed, ClassLoader cl) {
        block(xposed, cl, "com.bytedance.sdk.openadsdk.TTAdNativeImpl",
                "loadFeedAd", "loadSplashAd", "loadDrawFeedAd", "loadBannerExpressAd",
                "loadNativeExpressAd", "loadStream", "loadRewardVideoAd", "loadFullScreenVideoAd");
        block(xposed, cl, "com.qq.e.ads.splash.SplashAD", "fetchAndShowIn", "fetchFullScreenAndShowIn", "fetchAdOnly", "showAd");
        block(xposed, cl, "com.qq.e.ads.interstitial.InterstitialAD", "loadAd", "show");
        block(xposed, cl, "com.qq.e.ads.nativ.NativeUnifiedAD", "loadData");
        block(xposed, cl, "com.qq.e.ads.nativ.NativeExpressAD", "loadAD");
        block(xposed, cl, "com.qq.e.ads.banner2.UnifiedBannerView", "fetchAd");
        block(xposed, cl, "com.baidu.mobads.sdk.api.SplashAd", "loadAd", "showAd");
        block(xposed, cl, "com.baidu.mobads.sdk.api.NativeAd", "loadAd", "showAd");
        block(xposed, cl, "com.baidu.mobads.sdk.api.NativeUnifiedAd", "loadAd", "showAd");
        block(xposed, cl, "com.baidu.mobads.sdk.api.BannerAd", "loadAd", "showAd");
        block(xposed, cl, "com.kwad.sdk.api.KsSplashScreenAd", "load", "show");
        block(xposed, cl, "com.kwad.sdk.api.KsNativeAd", "load", "show");
        block(xposed, cl, "com.kwad.sdk.api.KsInterstitialAd", "load", "show");
        block(xposed, cl, "com.kwad.sdk.api.KsFeedAd", "load", "show");
        Log.i(TAG, "ad sdk blocked");
    }

    private static void block(XposedInterface xposed, ClassLoader cl, String className, String... methods) {
        for (String method : methods) {
            ReflectUtils.hookAllMethodsDefault(xposed, className, cl, method, null);
        }
    }

    private static void blockUpdate(XposedInterface xposed, ClassLoader cl) {
        int hits = 0;
        // Update-check entry point: version check, download dispatch and dialog routing all go through d3 a-f
        for (String method : new String[]{"a", "b", "c", "d", "e", "f"}) {
            hits += ReflectUtils.hookAllMethodsDefault(xposed, "com.baidu.homework.common.utils.d3", cl, method, null);
        }
        // Upgrade dialog bodies
        hits += ReflectUtils.hookAllMethodsDefault(xposed, "com.baidu.homework.dialog.biz.upgrade.a", cl, "e", null);
        hits += ReflectUtils.hookAllMethodsDefault(xposed, "com.baidu.homework.dialog.biz.upgrade.AppUpgradeDialogTask", cl, "G", Boolean.FALSE);
        hits += ReflectUtils.hookAllMethodsDefault(xposed, "com.baidu.homeworkhd.activity.index.fragment.home.dialog.AppUpgradeDialogHD", cl, "showDialog", null);
        // H5-triggered update checks / download / apply
        for (String action : new String[]{
                "com.baidu.homework.activity.web.actions.UpdateCheckAction",
                "com.baidu.homework.activity.web.actions.CheckNewVersionAction",
                "com.baidu.homework.activity.web.actions.NewVersionCheckAction",
                "com.baidu.homework.activity.web.actions.ApplyUpdateAction",
                "com.baidu.homework.activity.web.actions.LiveShowForceUpgradeView",
                "com.baidu.homework.activity.web.actions.LiveLoadUpgradeDataAndShowUpdateView"}) {
            hits += ReflectUtils.hookAllMethodsDefault(xposed, action, cl, "onAction", null);
        }
        Log.i(TAG, "update dialogs blocked, hooks=" + hits);
    }

    private static void blockVideoWindow(XposedInterface xposed, ClassLoader cl) {
        // The two H5 actions that create the small floating explanation card
        String[] actions = new String[]{
                "com.baidu.homework.activity.web.actions.SearchResultShowWindowWebAction",
                "com.baidu.homework.activity.web.actions.ZybSearchTransResultShowWindowAction",
                "com.baidu.homework.activity.web.actions.PlayVideoHintWebAction",
                "com.baidu.homework.activity.web.actions.SearchResultAnnotateAction",
                "com.baidu.homework.activity.web.actions.AnnotateLoadSuccessAction"};
        int hits = 0;
        for (String action : actions) {
            int n = ReflectUtils.hookAllMethodsDefault(xposed, action, cl, "onAction", null);
            hits += n;
            Log.i(TAG, "action " + action.substring(action.lastIndexOf('.') + 1) + " hooks=" + n);
        }

        // Only these two actions create the floating WebViewDialog; A1 is its sole factory.
        int factory = ReflectUtils.hookAllMethodsDefault(xposed,
                "com.baidu.homework.activity.search.core.i", cl, "A1", null);
        Log.i(TAG, "video window blocked: actions=" + hits + ", factory=" + factory);
    }

    private static void logWebActions(XposedInterface xposed, ClassLoader cl) {
        ReflectUtils.hookAllMethods(xposed, "com.baidu.homework.common.ui.widget.HybridWebView", cl, "dispatchAction", chain -> {
            try {
                String action = String.valueOf(chain.getArg(1));
                if (isVideoAction(action)) Log.i(TAG, "dispatchAction: " + action);
            } catch (Throwable ignored) {
            }
            return chain.proceed();
        });
        ReflectUtils.hookAllMethods(xposed, "com.baidu.homework.common.ui.widget.HybridWebView", cl, "dispatchActionToListeners", chain -> {
            try {
                String action = String.valueOf(chain.getArg(0));
                if (isVideoAction(action)) Log.i(TAG, "dispatchActionToListeners: " + action);
            } catch (Throwable ignored) {
            }
            return chain.proceed();
        });
    }

    private static boolean isVideoAction(String action) {
        if (action == null) return false;
        String a = action.toLowerCase();
        return a.contains("annotate") || a.contains("video") || a.contains("window")
                || a.contains("explain") || a.contains("play") || a.contains("show");
    }

    private static final String HIDE_EXPLAIN_JS =
            "(function(){"
          + "if(window.__zybPureHide)return;window.__zybPureHide=1;"
          + "function hide(el){try{var p=el;for(var i=0;i<10&&p&&p!==document.body;i++){var s=getComputedStyle(p),r=p.getBoundingClientRect();if(s&&(s.position==='fixed'||s.position==='absolute')&&r.width>=40&&r.height>=20){p.style.setProperty('display','none','important');p.style.setProperty('visibility','hidden','important');p.style.setProperty('pointer-events','none','important');return;}p=p.parentElement;}el.style.setProperty('display','none','important');}catch(e){}}"
          + "function chk(el){try{if(!el||el.nodeType!==1)return;var t=el.textContent||'';if(t.indexOf('\u8bb2\u89e3\u4e2d')>=0&&t.length<=40){hide(el);}}catch(e){}}"
          + "function scan(n){try{if(!n)return;if(n.nodeType===1){chk(n);if(n.querySelectorAll){var a=n.querySelectorAll('*');for(var i=0;i<a.length;i++)chk(a[i]);}}}catch(e){}}"
          + "function start(){try{scan(document.body);new MutationObserver(function(ms){for(var i=0;i<ms.length;i++){var m=ms[i];if(m.type==='childList'){for(var j=0;j<m.addedNodes.length;j++)scan(m.addedNodes[j]);}}}).observe(document.documentElement,{childList:true,subtree:true});}catch(e){}}"
          + "if(document.readyState==='loading'){document.addEventListener('DOMContentLoaded',start,true);}else{start();}"
          + "})();";

    private static void injectExplainJs(XposedInterface xposed, ClassLoader cl) {
        XposedInterface.Hooker loadHooker = chain -> {
            Object result = chain.proceed();
            try {
                Object self = chain.getThisObject();
                if (self instanceof View && isTargetView((View) self)) {
                    final View view = (View) self;
                    view.postDelayed(() -> evaluateJs(view, cl), 30L);
                    view.postDelayed(() -> evaluateJs(view, cl), 500L);
                }
            } catch (Throwable ignored) {
            }
            return result;
        };
        int hits = 0;
        for (String method : new String[]{"loadUrl", "loadData", "loadDataWithBaseURL"}) {
            hits += ReflectUtils.hookAllMethods(xposed, "com.baidu.homework.common.ui.widget.HybridWebView", cl, method, loadHooker);
            hits += ReflectUtils.hookAllMethods(xposed, "com.zuoyebang.common.web.WebView", cl, method, loadHooker);
        }
        XposedInterface.Hooker pageHooker = chain -> {
            Object result = chain.proceed();
            try {
                Object arg = chain.getArg(0);
                if (arg instanceof View && isTargetView((View) arg)) {
                    final View view = (View) arg;
                    view.postDelayed(() -> evaluateJs(view, cl), 30L);
                    view.postDelayed(() -> evaluateJs(view, cl), 500L);
                }
            } catch (Throwable ignored) {
            }
            return result;
        };
        hits += ReflectUtils.hookAllMethods(xposed, "com.baidu.homework.common.ui.widget.HybridWebView$d", cl, "c", pageHooker);
        hits += ReflectUtils.hookAllMethods(xposed, "com.baidu.homework.common.ui.widget.HybridWebView$d", cl, "d", pageHooker);
        Log.i(TAG, "js inject hooks=" + hits);
    }

    private static void evaluateJs(View view, ClassLoader cl) {
        try {
            Class<?> callbackClass = Class.forName("com.zuoyebang.common.web.l", false, cl);
            Method method = ReflectUtils.findMethod(view.getClass(), "evaluateJavascript", String.class, callbackClass);
            if (method != null) {
                method.invoke(view, HIDE_EXPLAIN_JS, null);
            } else {
                Log.w(TAG, "evaluateJs method not found: " + view.getClass().getName());
            }
        } catch (Throwable t) {
            Log.w(TAG, "evaluateJs failed: " + t);
        }
    }

    private static void blockExplainLabel(XposedInterface xposed, ClassLoader cl) {
        // Native "explaining" pill and loading animation drawn over the scanned image
        int n = 0;
        for (String m : new String[]{"processLabel", "processLoadingAnim",
                "createNewLabel", "createNewLoading", "updateLabelVisibility"}) {
            n += ReflectUtils.hookAllMethodsDefault(xposed,
                    "com.baidu.homework.activity.search.correct.ExplainLabelContainer", cl, m, null);
        }
        for (String m : new String[]{"handleExplainLabel", "showOrHideLabel"}) {
            n += ReflectUtils.hookAllMethodsDefault(xposed,
                    "com.baidu.homework.activity.search.correct.ImageDecorFuseContainer", cl, m, null);
        }
        Log.i(TAG, "explain label blocked hooks=" + n);
    }

    private static void hideViews(XposedInterface xposed, ClassLoader cl) {
        try {
            Method setVisibility = View.class.getDeclaredMethod("setVisibility", int.class);
            xposed.hook(setVisibility).intercept(chain -> {
                View view = (View) chain.getThisObject();
                if (isBlockedView(view) && isTargetView(view)) {
                    try {
                        Log.i(TAG, "blocked visibility: " + view.getClass().getName() + " -> " + chain.getArg(0));
                    } catch (Throwable ignored) {
                    }
                    return chain.proceed(new Object[]{View.GONE});
                }
                return chain.proceed();
            });
        } catch (Throwable t) {
            Log.w(TAG, "visibility hook failed: " + t);
        }

        try {
            for (Method addView : ViewGroup.class.getDeclaredMethods()) {
                if (!"addView".equals(addView.getName())) continue;
                xposed.hook(addView).intercept(chain -> {
                    Object arg = chain.getArg(0);
                    if (arg instanceof View) {
                        View child = (View) arg;
                        if (isBlockedView(child) && isTargetView(child)) {
                            child.setVisibility(View.GONE);
                            ViewGroup.LayoutParams lp = child.getLayoutParams();
                            if (lp != null) {
                                lp.width = 0;
                                lp.height = 0;
                                child.setLayoutParams(lp);
                            }
                            Log.i(TAG, "blocked view: " + child.getClass().getName());
                        }
                    }
                    return chain.proceed();
                });
            }
        } catch (Throwable t) {
            Log.w(TAG, "addView hook failed: " + t);
        }
        Log.i(TAG, "view blockers installed");
    }

    private static boolean isTargetView(View view) {
        if (view == null) return false;
        Context ctx = view.getContext();
        for (int i = 0; ctx != null && i < 12; i++) {
            try {
                if (TARGET.equals(ctx.getPackageName())) return true;
            } catch (Throwable ignored) {
            }
            if (!(ctx instanceof ContextWrapper)) break;
            Context base = ((ContextWrapper) ctx).getBaseContext();
            if (base == ctx) break;
            ctx = base;
        }
        return false;
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, Boolean> BLOCKED_CACHE =
            new java.util.concurrent.ConcurrentHashMap<String, Boolean>();

    private static boolean isBlockedView(View view) {
        String name = view.getClass().getName().toLowerCase();
        Boolean cached = BLOCKED_CACHE.get(name);
        if (cached != null) return cached.booleanValue();
        boolean blocked = computeBlocked(name);
        BLOCKED_CACHE.put(name, Boolean.valueOf(blocked));
        return blocked;
    }

    private static boolean computeBlocked(String name) {
        return name.contains("dialogupdateview")
                || name.contains("appupgradedialog")
                || name.contains("adlargeimgdialog")
                || name.contains("advideocontroller")
                || name.contains("commonwebadview")
                || name.contains("livewebadview")
                || name.contains("floatadview")
                || name.contains("singlequestion.webviewdialog")
                || name.contains("explainlabelcontainer")
                || name.contains("adx") && name.contains("view");
    }

    // ---- Bottom tab simplification (variant) ----

    private static void simplifyBottomTabs(XposedInterface xposed, ClassLoader cl) {
        XposedInterface.Hooker resumeHooker = chain -> {
            Object result = chain.proceed();
            try {
                scheduleFilterFromActivity(chain.getThisObject(), 0L);
                scheduleFilterFromActivity(chain.getThisObject(), 500L);
                scheduleFilterFromActivity(chain.getThisObject(), 1500L);
            } catch (Throwable ignored) {
            }
            return result;
        };
        int n = ReflectUtils.hookAllMethods(xposed, "com.baidu.homework.activity.index.IndexActivity", cl, "onResume", resumeHooker);

        XposedInterface.Hooker fHooker = chain -> {
            Object result = chain.proceed();
            try {
                scheduleFilterTabs(chain.getThisObject(), 0L);
                scheduleFilterTabs(chain.getThisObject(), 400L);
            } catch (Throwable ignored) {
            }
            return result;
        };
        n += ReflectUtils.hookAllMethods(xposed, "com.baidu.homework.activity.index.IndexActivity$f", cl, "f", fHooker);
        n += ReflectUtils.hookAllMethods(xposed, "com.baidu.homework.activity.index.IndexActivity$f", cl, "g", fHooker);
        Log.i(TAG, "tab simplify hooks=" + n);
    }

    private static void scheduleFilterFromActivity(Object activity, long delay) {
        try {
            Field bf = activity.getClass().getDeclaredField("b");
            bf.setAccessible(true);
            Object f = bf.get(activity);
            if (f != null) scheduleFilterTabs(f, delay);
        } catch (Throwable ignored) {
        }
    }

    private static void scheduleFilterTabs(Object f, long delay) {
        try {
            Field bf = f.getClass().getDeclaredField("b");
            bf.setAccessible(true);
            View bar = (View) bf.get(f);
            if (bar != null) bar.postDelayed(() -> filterBottomBar(f), delay);
        } catch (Throwable ignored) {
        }
    }

    private static View tabView(Object e) {
        if (e == null) return null;
        try {
            Field af = e.getClass().getDeclaredField("a");
            af.setAccessible(true);
            Object v = af.get(e);
            if (v instanceof View) return (View) v;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void ensureVisible(View v, View parent) {
        int guard = 0;
        while (v != null && v != parent && guard++ < 30) {
            v.setVisibility(View.VISIBLE);
            Object p = v.getParent();
            v = (p instanceof View) ? (View) p : null;
        }
    }

    private static boolean containsView(View container, View target) {
        if (container == null || target == null) return false;
        View v = target;
        int guard = 0;
        while (v != null && guard++ < 60) {
            if (v == container) return true;
            Object p = v.getParent();
            v = (p instanceof View) ? (View) p : null;
        }
        return false;
    }

    private static void applyWeight(View v, View bar) {
        if (v == null) return;
        View direct = v;
        int guard = 0;
        while (direct.getParent() instanceof View && direct.getParent() != bar && guard++ < 30) {
            direct = (View) direct.getParent();
        }
        ViewGroup.LayoutParams lp = direct.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) lp).weight = 1f;
            lp.width = 0;
            direct.setLayoutParams(lp);
        }
    }

    private static void hideTab(View v) {
        if (v == null) return;
        v.setVisibility(View.GONE);
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null) {
            lp.width = 0;
            lp.height = 0;
            v.setLayoutParams(lp);
        }
    }

    private static void filterBottomBar(Object f) {
        try {
            Class<?> fc = f.getClass();
            Field barF = fc.getDeclaredField("b");
            barF.setAccessible(true);
            LinearLayout bar = (LinearLayout) barF.get(f);
            Field actF = fc.getDeclaredField("i");
            actF.setAccessible(true);
            Object activity = actF.get(f);
            if (bar == null || activity == null) return;

            Field aF = fc.getDeclaredField("a");
            aF.setAccessible(true);
            Object[] arr = (Object[]) aF.get(f);
            if (arr == null || arr.length < 5) {
                Log.w(TAG, "tabs arr short");
                return;
            }

            // Tab order is fixed: 0=home, 4=me (see IndexActivity$f constructor).
            View homeView = tabView(arr[0]);
            View meView = tabView(arr[4]);

            if (homeView == null || meView == null) {
                Log.w(TAG, "home/me view missing, skip filter");
                return;
            }

            for (int i = 0; i < arr.length; i++) {
                View v = tabView(arr[i]);
                if (v == null) continue;
                if (v == homeView || v == meView) {
                    ensureVisible(v, bar);
                    applyWeight(v, bar);
                } else {
                    hideTab(v);
                }
            }

            int childCount = bar.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = bar.getChildAt(i);
                boolean keep = containsView(child, homeView) || containsView(child, meView);
                if (keep) {
                    child.setVisibility(View.VISIBLE);
                    ViewGroup.LayoutParams lp = child.getLayoutParams();
                    if (lp instanceof LinearLayout.LayoutParams) {
                        ((LinearLayout.LayoutParams) lp).weight = 1f;
                        lp.width = 0;
                        child.setLayoutParams(lp);
                    }
                    continue;
                }
                boolean holdsHiddenTab = false;
                for (int j = 0; j < arr.length; j++) {
                    View v = tabView(arr[j]);
                    if (v == null || v == homeView || v == meView) continue;
                    if (containsView(child, v)) {
                        holdsHiddenTab = true;
                        break;
                    }
                }
                if (holdsHiddenTab) hideTab(child);
            }

            float sum = 0f;
            for (int i = 0; i < bar.getChildCount(); i++) {
                View child = bar.getChildAt(i);
                if (child.getVisibility() == View.GONE) continue;
                ViewGroup.LayoutParams lp = child.getLayoutParams();
                if (lp instanceof LinearLayout.LayoutParams) {
                    sum += ((LinearLayout.LayoutParams) lp).weight;
                }
            }
            if (sum > 0f) bar.setWeightSum(sum);
            bar.requestLayout();

            switchToHomeIfHidden(activity);
        } catch (Throwable t) {
            Log.w(TAG, "filter tabs failed: " + t);
        }
    }

    private static void switchToHomeIfHidden(Object activity) {
        if (activity == null) return;
        try {
            Field lF = activity.getClass().getDeclaredField("l");
            lF.setAccessible(true);
            Object c0 = lF.get(activity);
            if (c0 == null) return;
            Method md = c0.getClass().getMethod("d", String.class);
            int homeIdx = (Integer) md.invoke(c0, "home");
            int meIdx = (Integer) md.invoke(c0, "me");
            Field tabHostF = activity.getClass().getDeclaredField("a");
            tabHostF.setAccessible(true);
            Object tabHost = tabHostF.get(activity);
            if (tabHost == null) return;
            Method getCur = tabHost.getClass().getMethod("getCurrentTab");
            int cur = (Integer) getCur.invoke(tabHost);
            if (cur == homeIdx || cur == meIdx) return;
            Method q0 = activity.getClass().getMethod("Q0", int.class);
            q0.invoke(activity, homeIdx);
            Log.i(TAG, "hidden tab " + cur + " -> home " + homeIdx);
        } catch (Throwable ignored) {
        }
    }
}
