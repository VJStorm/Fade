package com.sysu.pro.fade.home.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.sysu.pro.fade.Const;
import com.sysu.pro.fade.MainActivity;
import com.sysu.pro.fade.R;
import com.sysu.pro.fade.beans.Note;
import com.sysu.pro.fade.beans.SimpleResponse;
import com.sysu.pro.fade.beans.User;
import com.sysu.pro.fade.home.activity.DetailActivity;
import com.sysu.pro.fade.home.event.itemChangeEvent;
import com.sysu.pro.fade.service.NoteService;
import com.sysu.pro.fade.utils.RetrofitUtil;
import com.sysu.pro.fade.utils.TimeUtil;

import org.greenrobot.eventbus.EventBus;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import retrofit2.Retrofit;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by LaiXiancheng on 2017/8/2.
 * 信息流中item的ViewHolder的基类，实现了三种（仅图片，仅文字，图片加文字）item的基础（共同）功能
 */

abstract public class HomeBaseViewHolder extends RecyclerView.ViewHolder {
	TextView tvName, tvBody;    //name为用户名，body为正文 这里不写private是有原因的

	private ImageView userAvatar;
	private TextView tvHeadAction;
	private ImageView ivHeadAction;
	private TextView tvCount;
	private TextView tvAtUser;
	private ClickableProgressBar clickableProgressBar;
	private int position;
	MainActivity context;

	public HomeBaseViewHolder(View itemView) {
		super(itemView);
		userAvatar = (ImageView) itemView.findViewById(R.id.civ_avatar);
		tvName = (TextView) itemView.findViewById(R.id.tv_name);
		tvBody = (TextView) itemView.findViewById(R.id.tv_title);
		tvCount = (TextView) itemView.findViewById(R.id.tv_comment_add_count);
		tvAtUser = (TextView) itemView.findViewById(R.id.tv_original_author);
		ivHeadAction = (ImageView) itemView.findViewById(R.id.iv_head_action);
		tvHeadAction = (TextView) itemView.findViewById(R.id.tv_head_action);
		clickableProgressBar = (ClickableProgressBar) itemView.findViewById(R.id.clickable_progressbar);
	}

	public void bindView(final MainActivity context, List<Note> data, int position) {
		this.position = position;
		this.context = context;
		Note bean = data.get(position);


		/* ********* 设置界面 ***********/
		checkAndSetCurUser(context, bean);
		tvName.setText(bean.getNickname());
		setActionIfNecessary(bean);
		setCommentAndAddCountText(context, bean);
		setTimeLeftTextAndProgress(context, bean);
		Glide.with(context)
				.load(Const.BASE_IP+bean.getHead_image_url())
				.fitCenter()
				.dontAnimate()
				.into(userAvatar);

		/* ********* 设置监听器 ***********/
		setGoToDetailClickListener(context, bean);
		setAddOrMinusListener(context, bean);
		setCommentListener(context, bean);

	}

	/**
	 * 判断并设置头部的续秒或减秒
	 * @param bean
	 */
	private void setActionIfNecessary(Note bean) {
		if (bean.getType() == 1){
			Glide.with(context).load(R.drawable.add).into(ivHeadAction);
			//ivHeadAction.setImageResource(R.drawable.add);
			tvHeadAction.setText(R.string.add_time);
			tvAtUser.setVisibility(View.VISIBLE);
			tvAtUser.setText(context.getString(R.string.at_user, bean.getOrigin().getNickname()));
		}
		else if (bean.getType() == 2){
			Glide.with(context).load(R.drawable.minus).into(ivHeadAction);
			//ivHeadAction.setImageResource(R.drawable.minus);
			tvHeadAction.setText(R.string.minus_time);
			tvAtUser.setVisibility(View.VISIBLE);
			tvAtUser.setText(context.getString(R.string.at_user, bean.getOrigin().getNickname()));
		}
		else{
			ivHeadAction.setImageDrawable(null);
			tvHeadAction.setText("");
			tvAtUser.setVisibility(View.GONE);
		}
	}

	/**
	 * 检查当前帖子是不是用户自己的，是的话，
	 * 用用户自己的名字和头像（应对用户修改自己信息，回来后帖子信息没变的情况）
	 */
	private void checkAndSetCurUser(MainActivity context, Note bean) {
		User curUser = context.getCurrentUser();
		if (bean.getUser_id().equals(curUser.getUser_id())){
			bean.setHead_image_url(curUser.getHead_image_url());
			bean.setNickname(curUser.getNickname());
		}
	}

	private void setGoToDetailClickListener(final Context context, final Note bean) {
		itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						((Activity) context).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								startDetailsActivity(context, bean);
							}
						});

					}
				}).start();
			}
		});
	}


	/**
	 * 续秒或者减秒
	 */
	private void setAddOrMinusListener(final MainActivity context, final Note bean) {
		final User curUser = context.getCurrentUser();
		clickableProgressBar.setAddClickListener(new ClickableProgressBar.onAddClickListener() {
			@Override
			public void onClick() {
				Note note = getNewNote(context, bean);
				note.setType(1); // 1表示 续秒
				sendAddOrMinusToServer(note, curUser, bean);
				clickableProgressBar.showCommentButton(1);
				bean.setAction(1);
				int curProgress = clickableProgressBar.getProgress();
				int maxProgress = clickableProgressBar.getMaxProgress();
				clickableProgressBar.setProgress(Math.min(curProgress+5, maxProgress));
			}
		});
		clickableProgressBar.setMinusClickListener(new ClickableProgressBar.onMinusClickListener() {
			@Override
			public void onClick() {
				Note note = getNewNote(context, bean);
				note.setType(2); // 2表示 减秒
				sendAddOrMinusToServer(note, curUser, bean);
				clickableProgressBar.showCommentButton(0);
				bean.setAction(2);
				int curProgress = clickableProgressBar.getProgress();
				int halfProgress = clickableProgressBar.getMaxProgress() / 2;
				clickableProgressBar.setProgress(Math.max(curProgress-5, halfProgress));
			}
		});
	}

	private void setCommentListener(final MainActivity context, final Note bean) {
		if (bean.getAction() == 1)
			clickableProgressBar.showCommentButton(1);
		else if (bean.getAction() == 2)
			clickableProgressBar.showCommentButton(0);
		else if (bean.getAction() == 0)
			clickableProgressBar.hideCommentButton();
		clickableProgressBar.setCommentClickListener(new ClickableProgressBar.onCommentClickListener() {
			@Override
			public void onClick() {
				Intent intent = new Intent(context, DetailActivity.class);
				if (bean.getType() != 0)	//非原贴的话，传入的是其原贴的id
					intent.putExtra(Const.NOTE_ID,bean.getOrigin().getNote_id());
				else
					intent.putExtra(Const.NOTE_ID,bean.getNote_id());
				intent.putExtra(Const.IS_COMMENT, true);
				intent.putExtra(Const.COMMENT_NUM, bean.getComment_num());
				intent.putExtra(Const.COMMENT_ENTITY, bean);
				context.startActivity(intent);
			}
		});
	}

	/**
	 * 将包装好的note对象发到服务器，并更新本地的界面
	 * @param note 准备发往服务器的note
	 * @param curUser 当前用户
	 * @param bean 当前holder对应的bean
	 */
	private void sendAddOrMinusToServer(Note note, User curUser, final Note bean) {
		Retrofit retrofit = RetrofitUtil.createRetrofit(Const.BASE_IP, curUser.getTokenModel());
		NoteService noteService = retrofit.create(NoteService.class);
		noteService
				.changeSecond(JSON.toJSONString(note))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Subscriber<SimpleResponse>() {
					@Override
					public void onCompleted() {}

					@Override
					public void onError(Throwable e) {
						//TODO:帖子死掉
					}

					@Override
					public void onNext(SimpleResponse simpleResponse) {
						if (simpleResponse.getErr() == null){
							//USELESS!! Integer newNoteId = (Integer) simpleResponse.getExtra().get("note_id");
							Integer comment_num = (Integer) simpleResponse.getExtra().get("comment_num");    //评论数量
							Integer sub_num = (Integer) simpleResponse.getExtra().get("sub_num");    //评论数量
							Integer add_num = (Integer) simpleResponse.getExtra().get("add_num");    //评论数量
							Long fetchTime = (Long) simpleResponse.getExtra().get("fetchTime");
							bean.setComment_num(comment_num);
							bean.setSub_num(sub_num);
							bean.setAdd_num(add_num);
							bean.setFetchTime(fetchTime);
							EventBus.getDefault().post(new itemChangeEvent(position));
						}
					}
				});
	}

	@NonNull
	private Note getNewNote(MainActivity context, Note bean) {
		User curUser = context.getCurrentUser();
		Note note = new Note();
		note.setNickname(curUser.getNickname());
		note.setUser_id(curUser.getUser_id());
		note.setNote_content(bean.getNote_content());
		note.setTarget_id(bean.getNote_id());
		note.setHead_image_url(curUser.getHead_image_url());
		return note;
	}


	private void setTimeLeftTextAndProgress(Context context, Note bean) {
		Date dateNow = new Date(bean.getFetchTime());
		Date datePost = TimeUtil.getTimeDate(bean.getPost_time());
		//floor是为了防止最后半秒的计算结果就为0,也就是保证了时间真正耗尽之后计算结果才为0
		long minuteLeft = (long) (Const.HOME_NODE_DEFAULT_LIFE
				+ 5 * bean.getAdd_num()
				- bean.getSub_num()
				- Math.floor(((double) (dateNow.getTime() - datePost.getTime())) / (1000 * 60)));
		String sTimeLeft;
		if (minuteLeft < 60)
			sTimeLeft = String.valueOf(minuteLeft) + "分钟";
		else if (minuteLeft < 1440)
			sTimeLeft = String.valueOf(Math.round(((double) minuteLeft) / 60)) + "小时";
		else
			sTimeLeft = String.valueOf(Math.round(((double) minuteLeft) / 1440)) + "天";

		clickableProgressBar.setTimeText(context.getString(R.string.time_left_text, sTimeLeft));

		if (minuteLeft < 60){
			int halfProgress = clickableProgressBar.getMaxProgress() / 2;
			clickableProgressBar.setProgress((int)Math.max((halfProgress+(5.0/6)*minuteLeft), halfProgress));
		}
		else
			clickableProgressBar.setProgress(clickableProgressBar.getMaxProgress());
	}

	/**
	 * 评论数量，续秒数量显示
	 * @param context
	 * @param bean
	 */
	private void setCommentAndAddCountText(Context context, Note bean) {
		DecimalFormat decimalFormat = new DecimalFormat(",###");
		String sAddCount = decimalFormat.format(bean.getAdd_num());
		String addCntText = context.getString(R.string.add_count_text, sAddCount);
		String sCommentCount = decimalFormat.format(bean.getComment_num());
		String commentCntText = context.getString(R.string.comment_count_text, sCommentCount);
		tvCount.setText(commentCntText + "   "+addCntText);
	}

	/**
	 * 跳转到详情页
	 * @param bean 当前贴的信息
	 */
	private void startDetailsActivity(Context context, Note bean) {
		Intent intent = new Intent(context, DetailActivity.class);
		if (bean.getType() != 0)	//非原贴的话，传入的是其原贴的id
			intent.putExtra(Const.NOTE_ID,bean.getOrigin().getNote_id());
		else
			intent.putExtra(Const.NOTE_ID,bean.getNote_id());
		intent.putExtra(Const.IS_COMMENT, false);
		intent.putExtra(Const.COMMENT_NUM, bean.getComment_num());
		intent.putExtra(Const.COMMENT_ENTITY, bean);
		context.startActivity(intent);
	}


}
