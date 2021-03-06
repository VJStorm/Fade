package com.sysu.pro.fade.my.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.footer.ClassicsFooter;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.sysu.pro.fade.Const;
import com.sysu.pro.fade.MainActivity;
import com.sysu.pro.fade.R;
import com.sysu.pro.fade.beans.User;
import com.sysu.pro.fade.beans.UserQuery;
import com.sysu.pro.fade.home.activity.OtherActivity;
import com.sysu.pro.fade.home.adapter.CommonAdapter;
import com.sysu.pro.fade.my.Event.DoubleFade;
import com.sysu.pro.fade.my.Event.NewNumber;
import com.sysu.pro.fade.service.UserService;
import com.sysu.pro.fade.utils.RetrofitUtil;
import com.sysu.pro.fade.utils.UserUtil;

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
 * Created by 12194 on 2018/2/27.
 */

public class ConcernFragment extends Fragment {

    private User myself;
    private Integer userId;
    private String start;
    private View rootView;
    private Retrofit retrofit;
    private RefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private CommonAdapter<User> adapter;
    private List<User> concern = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;

    public ConcernFragment() {

    }

    public static ConcernFragment newInstance(int userId) {
        final ConcernFragment f = new ConcernFragment();
        final Bundle args = new Bundle();
        args.putInt("USER_ID", userId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_forward, container, false);
        myself = new UserUtil(getActivity()).getUer();
        userId = getArguments() != null ? getArguments().getInt("USER_ID") : null;
        start = "0";
        retrofit = RetrofitUtil.createRetrofit(Const.BASE_IP, myself.getTokenModel());
        setupView();
        getData(true);
        return rootView;
    }

    private void getData(final Boolean isRefresh) {
        if (isRefresh) start = "0";
        UserService service = retrofit.create(UserService.class);
        service.getConcerns(userId.toString(), myself.getUser_id().toString(),start)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<UserQuery>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        swipeRefreshLayout.setRefreshing(false);
                        Log.d("GetFansErr", "onError: " + e.toString());
                    }

                    @Override
                    public void onNext(UserQuery userQuery) {
                        refreshLayout.finishLoadmore();
                        swipeRefreshLayout.setRefreshing(false);
                        if (isRefresh) concern.clear();
                        int addSize = userQuery.getList().size();
                        concern.addAll(userQuery.getList());
                        Log.d("Check", "fans: "+userQuery.getList().size());
                        start = userQuery.getStart().toString();
                        adapter.notifyDataSetChanged();
                        if (addSize < 20) {
                            refreshLayout.setEnableLoadmore(false);
                        } else {
                            refreshLayout.setEnableLoadmore(true);
                        }

                        //add by vj 2018.5.10
                        //下面通知主页进行数量更新,这是获取到小于20的情况
                        if (getActivity().getClass() == MainActivity.class) {
                            if (addSize % 20 != 0 || addSize == 0) {
                                EventBus.getDefault().post(new NewNumber(-1, -1, -1, concern.size()));
                            } else {
                                EventBus.getDefault().post(new NewNumber(-1, -1, -1, -1));
                            }
                        }
                    }
                });
    }

    private void setupView() {
        recyclerView = rootView.findViewById(R.id.fragment_forward_recycler_view);
        recyclerView.setBackgroundColor(getResources().getColor(R.color.fans_gray));
        adapter = new CommonAdapter<User>(concern) {
            @Override
            public int getLayoutId(int ViewType) {
                return R.layout.item_fans_fragment;
            }

            @Override
            public void convert(ViewHolder holder, final User data, int position) {
                if (position == 0) {
                    holder.setWidgetVisibility(R.id.fans_divide_line, View.GONE);
                }
                holder.setCircleImage(R.id.fans_head, Const.BASE_IP+data.getHead_image_url());
                holder.onWidgetClick(R.id.fans_root_view, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getContext(), OtherActivity.class);
                        i.putExtra(Const.USER_ID, data.getUser_id());
                        startActivity(i);
                    }
                });
                holder.setText(R.id.fans_name, data.getNickname());
                holder.setText(R.id.fans_signature, data.getSummary());
                /*
                * 2018.4.25 根据当前需求，关注页不显示是否关注
                */
//                if (data.getIsConcern() == 1) {
//                    holder.setWidgetVisibility(R.id.fans_concern_ok, View.VISIBLE);
//                } else {
//                    holder.setWidgetVisibility(R.id.fans_concern, View.VISIBLE);
//                }
            }
        };
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        refreshLayout = rootView.findViewById(R.id.fragment_forward_refresh_layout);
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setEnableAutoLoadmore(false);
        refreshLayout.setRefreshFooter(new ClassicsFooter(getContext()));
        refreshLayout.setOnLoadmoreListener(new OnLoadmoreListener() {
            @Override
            public void onLoadmore(RefreshLayout refreshlayout) {
                getData(false);
            }
        });

        swipeRefreshLayout = rootView.findViewById(R.id.fans_and_concern_refresh_layout);
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setColorSchemeResources(R.color.light_blue);
        //下拉刷新，重新获取关注列表
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        swipeRefreshLayout.setRefreshing(true);
                        getData(true);
                    }
                });
    }

    private void scrollToTOP(){
        recyclerView.smoothScrollToPosition(0);
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
