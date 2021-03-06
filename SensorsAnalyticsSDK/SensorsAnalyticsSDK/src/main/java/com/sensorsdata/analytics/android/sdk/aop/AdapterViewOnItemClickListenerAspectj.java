package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsAdapterViewItemTrackProperties;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by 王灼洲 on 2016/11/16
 * ListView、GridView onItemClick
 */

@Aspect
public class AdapterViewOnItemClickListenerAspectj {
    private final static String TAG = AdapterViewOnItemClickListenerAspectj.class.getCanonicalName();

    @After("execution(* android.widget.AdapterView.OnItemClickListener.onItemClick(android.widget.AdapterView,android.view.View,int,long))")
    public void onItemClickAOP(final JoinPoint joinPoint) throws Throwable {
        AopThreadPool.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //闭 AutoTrack
                    if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                        return;
                    }

                    //$AppClick 被过滤
                    if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                        return;
                    }

                    //基本校验
                    if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 4) {
                        return;
                    }

                    //AdapterView
                    Object object = joinPoint.getArgs()[0];
                    if (object == null) {
                        return;
                    }

                    //View
                    View view = (View) joinPoint.getArgs()[1];
                    if (view == null) {
                        return;
                    }

                    //获取所在的 Context
                    Context context = view.getContext();
                    if (context == null) {
                        return;
                    }

                    //将 Context 转成 Activity
                    Activity activity = null;
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }

                    //Activity 被忽略
                    if (activity != null) {
                        if (SensorsDataAPI.sharedInstance().isActivityAutoTrackIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    //View Type 被忽略
                    if (AopUtil.isViewIgnored(object.getClass())) {
                        return;
                    }

                    //position
                    int position = (int) joinPoint.getArgs()[2];

                    JSONObject properties = new JSONObject();

                    //View 被忽略
                    AdapterView adapterView = (AdapterView) object;

                    List<Class> mIgnoredViewTypeList = SensorsDataAPI.sharedInstance().getIgnoredViewTypeList();
                    if (mIgnoredViewTypeList != null) {
                        if (adapterView instanceof ListView) {
                            properties.put(AopConstants.ELEMENT_TYPE, "ListView");
                            if (AopUtil.isViewIgnored(ListView.class)) {
                                return;
                            }
                        } else if (adapterView instanceof GridView) {
                            properties.put(AopConstants.ELEMENT_TYPE, "GridView");
                            if (AopUtil.isViewIgnored(GridView.class)) {
                                return;
                            }
                        }
                    }

                    //扩展属性
                    Adapter adapter = adapterView.getAdapter();
                    if (adapter != null && adapter instanceof SensorsAdapterViewItemTrackProperties) {
                        try {
                            SensorsAdapterViewItemTrackProperties objectProperties = (SensorsAdapterViewItemTrackProperties) adapter;
                            JSONObject jsonObject = objectProperties.getSensorsItemTrackProperties(position);
                            if (jsonObject != null) {
                                AopUtil.mergeJSONObject(jsonObject, properties);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    //Activity 名称和页面标题
                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    //点击的 position
                    properties.put(AopConstants.ELEMENT_POSITION, String.valueOf(position));
//                    properties.put(AopConstants.ELEMENT_ACTION, "onItemClick");

                    //获取 View 自定义属性
                    JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
                    if (p != null) {
                        AopUtil.mergeJSONObject(p, properties);
                    }

                    SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    SALog.i(TAG, " AdapterView.OnItemClickListener.onItemClick AOP ERROR: " + e.getMessage());
                }
            }
        });
    }

    @After("execution(* android.widget.AdapterView.OnItemLongClickListener.onItemLongClick(..))")
    public void onItemLongClickMethod(JoinPoint joinPoint) throws Throwable {

    }
}
