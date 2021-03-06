package com.sysu.pro.fade;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.sysu.pro.fade.home.activity.OtherActivity;

import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.push.RongPushClient;


/**
 * Created by LaiXiancheng on 2018/1/23.
 * Email: lxc.sysu@qq.com
 */

public class App extends Application {
	//private RefWatcher mRefWatcher;
	public static Application instance;


	@Override
	public void onCreate() {
		super.onCreate();
		RongPushClient.registerMiPush(this, "2882303761517779005", "5451777914005");
		//RongPushClient.registerHWPush(this);
		RongIM.init(this);
		setRongMsgClickListener();
		instance = this;

		//内存分析工具
		//mRefWatcher = LeakCanary.install(this);

	}

	public static Application getInstance() {
		return instance;
	}




	static String getCurProcessName(Context context) {
		int pid = android.os.Process.myPid();
		ActivityManager mActivityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager
				.getRunningAppProcesses()) {
			if (appProcess.pid == pid) {
				return appProcess.processName;
			}
		}
		return null;
	}

	public void setRongMsgClickListener(){

		if(RongContext.getInstance() != null) {
			RongContext.getInstance().setConversationClickListener(new RongIM.ConversationClickListener() {
				@Override
				public boolean onUserPortraitClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String s) {
					String userId = userInfo.getUserId();
					Intent intent = new Intent(context,OtherActivity.class);
					intent.putExtra("user_id",Integer.parseInt(userId));
					context.startActivity(intent);
					return false;
				}

				@Override
				public boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String s) {
					return false;
				}

				@Override
				public boolean onMessageClick(Context context, View view, Message message) {
					return false;
				}

				@Override
				public boolean onMessageLinkClick(Context context, String s, Message message) {
					return false;
				}

				@Override
				public boolean onMessageLongClick(Context context, View view, Message message) {
					return false;
				}
			});

			//融云消息的通知
			RongIM.setOnReceiveMessageListener(new RongIMClient.OnReceiveMessageListener() {
				@Override
				public boolean onReceived(Message message, int i) {
					return false;
				}
			});
		}
	}
}
