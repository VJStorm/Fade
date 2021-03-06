package com.sysu.pro.fade.my.fragment;

import android.app.Activity;
import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sysu.pro.fade.Const;
import com.sysu.pro.fade.MainActivity;
import com.sysu.pro.fade.R;
import com.sysu.pro.fade.beans.Note;
import com.sysu.pro.fade.beans.NoteQuery;
import com.sysu.pro.fade.beans.PersonPage;
import com.sysu.pro.fade.beans.User;
import com.sysu.pro.fade.home.adapter.NotesAdapter;
import com.sysu.pro.fade.home.animator.FadeItemAnimator;
import com.sysu.pro.fade.home.event.NoteChangeEvent;
import com.sysu.pro.fade.home.listener.EndlessRecyclerOnScrollListener;
import com.sysu.pro.fade.my.Event.DoubleFade;
import com.sysu.pro.fade.my.Event.NewNumber;
import com.sysu.pro.fade.service.NoteService;
import com.sysu.pro.fade.service.UserService;
import com.sysu.pro.fade.utils.RetrofitUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by 12194 on 2018/1/30.
 */

public class MyLiveContent {

    /*图片URL数组*/
    private List<Note> notes;//当前加载的帖子
    /*信息流适配器*/
    private NotesAdapter adapter;
    /*刷新控件*/
    private SwipeRefreshLayout swipeRefresh;
    /*上拉加载滑动监听*/
    private EndlessRecyclerOnScrollListener loadMoreScrollListener;
    /*检测是否删除的滑动监听*/
    //private JudgeRemoveOnScrollListener judgeRemoveScrollListener;
    /*列表*/
    private RecyclerView recyclerView;

    private Activity activity;
    private Context context;
    private View rootView;

    /**
     * add By 黄路 2017/8/18
     */
    private Integer start;
    private User user;           //登录用户的全部信息
    private List<Note>updateList;  //已加载帖子，用于发给服务器，更新帖子情况(每一项仅仅包含note_id 和 target_id)
    private List<Note>checkList;   //顶部下拉查询返回的帖子，根据这个来判断和更新已加载帖子的情况
    private Retrofit retrofit;
    private UserService userService;
    private NoteService noteService;
    private Boolean isEnd; //记录向下是否到了结尾
    private Boolean isLoading;

    /**
     * add by VJ 2018/4/27
     * 用于没有内容时的文案
     */
    private ImageView interesting;
    private TextView nothingLive;

    public MyLiveContent(Activity activity, final Context context, View rootView){
        this.activity = activity;
        this.context = context;
        this.rootView = rootView;
        interesting = rootView.findViewById(R.id.interesting);
        nothingLive = rootView.findViewById(R.id.nothing_live);
        //EventBus订阅
        EventBus.getDefault().register(this);
        swipeRefresh = (SwipeRefreshLayout)rootView.findViewById(R.id.swipe_refresh);
        swipeRefresh.setRefreshing(false);
        //初始化用户信息
        user = ((MainActivity) activity).getCurrentUser();
        notes = new ArrayList<>();
        updateList = new ArrayList<>();
        checkList = new ArrayList<>();
        isEnd = false;
        isLoading = true;
        initViews();
        retrofit = RetrofitUtil.createRetrofit(Const.BASE_IP,user.getTokenModel());
        userService = retrofit.create(UserService.class);
        noteService = retrofit.create(NoteService.class);
        userService.getPersonPage(user.getUser_id().toString(),user.getUser_id().toString())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<PersonPage>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("初次加载","失败");
                        e.printStackTrace();
                        setLoadingMore(false);
                        swipeRefresh.setRefreshing(false);
                    }

                    @Override
                    public void onNext(PersonPage personPage) {
                        Log.i("首次加载","成功");
                        notes.clear();
                        if(personPage.getQuery().getList() != null && personPage.getQuery().getList().size() != 0){
                            addToListTail(personPage.getQuery().getList());
                        }
                        //更新start
                        start = personPage.getQuery().getStart();
                        setLoadingMore(false);
                        swipeRefresh.setRefreshing(false);
//                        Toast.makeText(context,"加载成功",Toast.LENGTH_SHORT).show();
                        isLoading = false;
                        //add by vj
                        if (notes.size() != 0) {
                            interesting.setVisibility(View.GONE);
                            nothingLive.setVisibility(View.GONE);
                        } else {
                            interesting.setVisibility(View.VISIBLE);
                            nothingLive.setVisibility(View.VISIBLE);
                        }
                    }
                });
        start = 0;

    }

    private void initViews(){
        recyclerView = (RecyclerView) rootView.findViewById(R.id.rv_home);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new NotesAdapter((MainActivity) context, notes);
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.light_blue);
		/*刷新数据*/
        swipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshItems();
                    }
                });
        loadMoreScrollListener = new EndlessRecyclerOnScrollListener(context, layoutManager) {
            @Override
            public void onLoadMore(int currentPage) {
                    addItems();
            }
        };
        recyclerView.addOnScrollListener(loadMoreScrollListener);
        FadeItemAnimator fadeItemAnimator = new FadeItemAnimator();
        fadeItemAnimator.setRemoveDuration(400);
        fadeItemAnimator.setChangeDuration(0);//解决notifyItem时的闪屏问题
        recyclerView.setItemAnimator(fadeItemAnimator);

    }

    /**
     * 加载更多
     */
    private void addItems() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(isEnd){
//                            Toast.makeText(context,"往下没有啦",Toast.LENGTH_SHORT).show();
                            setLoadingMore(false);
                            swipeRefresh.setRefreshing(false);
                        }else {
                            //加载更多
                            isLoading = true;
                            userService.getLiveNote(user.getUser_id().toString(),user.getUser_id().toString(), start.toString())
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Subscriber<NoteQuery>() {
                                        @Override
                                        public void onCompleted() {
                                        }
                                        @Override
                                        public void onError(Throwable e) {
                                            Log.e("加载更多","失败");
                                            e.printStackTrace();
                                            setLoadingMore(false);
                                        }
                                        @Override
                                        public void onNext(NoteQuery noteQuery) {
                                            Log.i("加载更多","成功");
                                            List<Note>addList = noteQuery.getList();
                                            start = noteQuery.getStart();
                                            if(addList.size() != 0){
                                                addToListTail(noteQuery.getList());
                                                Toast.makeText(context,"加载成功",Toast.LENGTH_SHORT).show();
                                                //add by vj
                                                if (notes.size() != 0) {
                                                    interesting.setVisibility(View.GONE);
                                                    nothingLive.setVisibility(View.GONE);
                                                }
                                            }
                                            else{
                                                Toast.makeText(context,"往下没有啦",Toast.LENGTH_SHORT).show();
                                            }
                                            if(addList.size() < 10) isEnd = true;
                                            swipeRefresh.setRefreshing(false);
                                            setLoadingMore(false);
                                            isLoading = false;
                                        }
                                    });
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 下拉刷新
     */
    private void refreshItems() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadMoreScrollListener.resetPreviousTotal();
                        //顶部下拉刷新
                        swipeRefresh.setRefreshing(true);
                        Log.i("test",updateList.toString());
                        userService.getLiveNote(user.getUser_id().toString(),user.getUser_id().toString(), "0")
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<NoteQuery>() {
                                    @Override
                                    public void onCompleted() {
                                    }
                                    @Override
                                    public void onError(Throwable e) {
                                        Log.e("顶部加载","失败");
                                        e.printStackTrace();
                                        swipeRefresh.setRefreshing(false);
                                    }
                                    @Override
                                    public void onNext(NoteQuery noteQuery) {
                                        Log.i("顶部加载","成功");
                                        notes.clear();
                                        if(noteQuery.getList() != null){
                                            addToListTail(noteQuery.getList());
                                        }
                                        //add by vj
                                        if (notes.size() != 0) {
                                            interesting.setVisibility(View.GONE);
                                            nothingLive.setVisibility(View.GONE);
                                        } else {
                                            interesting.setVisibility(View.VISIBLE);
                                            nothingLive.setVisibility(View.VISIBLE);
                                        }
                                        //更新start
                                        start = noteQuery.getStart();
                                        setLoadingMore(false);
                                        swipeRefresh.setRefreshing(false);
//                                        Toast.makeText(context,"加载成功",Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
            }
        }).start();
    }

    /**
     * 设置“正在加载”是否显示
     * @param isShow 是否显示
     */
    private void setLoadingMore(boolean isShow){
        loadMoreScrollListener.loading = false;
        adapter.setLoadingMore(isShow);
    }


    private void scrollToTOP(){
        recyclerView.smoothScrollToPosition(0);
    }

    /**
     * 如果当前帖子有本机用户自己的帖子，则检查其头像和名字更新
     * 当home变为可见时调用
     */
    public void refreshIfUserChange(){
        boolean isChange = false;
        for (Note note: notes){
            if (note.getUser_id().equals(user.getUser_id())){
                if (!note.getHead_image_url().equals(user.getHead_image_url())
                        || !note.getNickname().equals(user.getNickname())){
                    note.setHead_image_url(user.getHead_image_url());
                    note.setNickname(user.getNickname());
                    isChange = true;
                }
                else{
                    isChange = false;
                    break;
                }
            }
        }
        if (isChange){
            adapter.notifyDataSetChanged();
        }
    }

    private void addToListTail(List<Note>list){
        //下翻加载数据
        for(Note note : list){
            //note.setIs_die(1);
            if(note.getComment_num() == null) note.setComment_num(0);
            if(note.getAdd_num() == null) note.setAdd_num(0);
            if(note.getSub_num() == null) note.setSub_num(0);
        }
        notes.addAll(list);
        adapter.notifyDataSetChanged();

        //add by vj 2018.5.10
        //下面通知主页进行数量更新
        if (notes.size()%10 != 0 || notes.size() == 0) {
            EventBus.getDefault().post(new NewNumber(notes.size(), -1, -1, -1));
        } else {
            EventBus.getDefault().post(new NewNumber(-1, -1, -1, -1));
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetNewNote(Note note) {
        //接收新的Note，加到头部
        note.setFetchTime(System.currentTimeMillis());
        note.setAction(0);
        if(note.getComment_num() == null) note.setComment_num(0);
        if(note.getAdd_num() == null) note.setAdd_num(0);
        if(note.getSub_num() == null) note.setSub_num(0);
        notes.add(0,note);
        Note simpleNote = new Note();
        simpleNote.setNote_id(note.getNote_id());
        simpleNote.setTarget_id(note.getTarget_id());
        updateList.add(0,simpleNote);
        adapter.notifyDataSetChanged();
    }


    /**
     * item发生变化，更新界面
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onItemChanged(NoteChangeEvent noteChangeEvent) {
        int noteId = noteChangeEvent.getOriginalNoteId();
        Note newNote = noteChangeEvent.getNote();
        for (int i = 0; i < notes.size(); i++) {
            Note tmpNote = notes.get(i);
            if (tmpNote.getOriginalId().equals(noteId)){
                if (tmpNote.isOriginalNote())
                    notes.set(i, newNote);
                else
                    tmpNote.setOrigin(newNote);
                adapter.notifyItemChanged(i);
            }
        }
    }

    /**
     * 修改用户信息，更新主界面
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public  void onGetUser(User user){
        Integer user_id = user.getUser_id();
        for(Note note : notes){
            if(note.getUser_id().equals(user_id)){
                note.setHead_image_url(Const.BASE_IP + user.getHead_image_url());
                note.setNickname(user.getNickname());
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DoubleFade msg) {
        String message = msg.getMessage();
        boolean isClicked = msg.isClick();
        if (isClicked) {
            scrollToTOP();
        }
    }
}