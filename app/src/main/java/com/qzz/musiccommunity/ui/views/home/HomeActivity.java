package com.qzz.musiccommunity.ui.views.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.Service.MusicPlayerService;
import com.qzz.musiccommunity.instance.MusicManager;
import com.qzz.musiccommunity.model.BannerItem;
import com.qzz.musiccommunity.model.HorizontalCardItem;
import com.qzz.musiccommunity.model.OneColumnItem;
import com.qzz.musiccommunity.model.TwoColumnItem;
import com.qzz.musiccommunity.model.iface.ListItem;
import com.qzz.musiccommunity.network.ApiService;
import com.qzz.musiccommunity.network.DataConverter;
import com.qzz.musiccommunity.network.RetrofitClient;
import com.qzz.musiccommunity.network.dto.BaseResponse;
import com.qzz.musiccommunity.network.dto.ModuleConfig;
import com.qzz.musiccommunity.database.dto.MusicInfo;
import com.qzz.musiccommunity.network.dto.PagedData;
import com.qzz.musiccommunity.ui.common.BottomMusicPlayerView;
import com.qzz.musiccommunity.ui.common.musicList.MusicPlaylistDialog;
import com.qzz.musiccommunity.ui.views.MusicPlayer.MusicPlayerActivity;
import com.qzz.musiccommunity.ui.views.MusicPlayer.iface.OnMusicItemClickListener;
import com.qzz.musiccommunity.ui.views.home.adapter.MultiTypeAdapter;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements OnMusicItemClickListener,MusicPlaylistDialog.OnPlaylistActionListener {

    private static final String TAG = "HomeActivity";

    // UI组件
    private RecyclerView recyclerViewSwipe;
    private MultiTypeAdapter adapter;
    private SmartRefreshLayout swipeRefreshLayout;

    // 底部播放器
    private ViewStub stubBottomPlayer;
    private BottomMusicPlayerView bottomMusicPlayerView;
    private boolean isBottomPlayerInflated = false; // 标记ViewStub是否已经inflate

    // 网络和数据
    private ApiService apiService;
    private List<ListItem> currentData = new ArrayList<>();

    // 分页参数
    private int currentPage = 1;
    private final int pageSize = 10;
    private boolean isLoading = false;

    // Handler for UI operations
    private Handler mainHandler;

    // Network call tracking
    private Call<BaseResponse<PagedData<ModuleConfig>>> currentCall;

    private MusicPlayerService musicService;
    private boolean isMusicServiceBound = false;

    // 添加 ServiceConnection 对象
    private ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            musicService = binder.getService();
            isMusicServiceBound = true;
            Log.d(TAG, "已连接到音乐播放服务");

            // 服务连接后更新底部播放器
            if (bottomMusicPlayerView != null && isBottomPlayerInflated) {
                bottomMusicPlayerView.updatePlayerView();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isMusicServiceBound = false;
            Log.d(TAG, "与音乐播放服务断开连接");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.d(TAG, "Activity创建开始");

        try {
            initializeComponents();

            // 绑定音乐播放服务
            bindMusicService();

            // 延迟加载初始数据，确保UI完全初始化
            mainHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    Log.d(TAG, "开始初始数据加载（刷新模式）");
                    loadHomePageData(true); // 改为 true，按刷新处理
                }
            }, 100);

        } catch (Exception e) {
            Log.e(TAG, "Activity初始化失败", e);
            showError("初始化失败，请重启应用");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - 检查并更新底部播放器显示状态");
        // 修正2：从其他Activity返回时判断BottomMusicPlayerView显示
        checkAndUpdateBottomPlayerVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - 检查并更新底部播放器显示状态");
        // 修正2：从其他Activity返回时判断BottomMusicPlayerView显示
        checkAndUpdateBottomPlayerVisibility();

        // 注册播放状态变化监听器
        if (bottomMusicPlayerView != null && isBottomPlayerInflated) {
            bottomMusicPlayerView.registerPlaybackStateChangeListener();
            bottomMusicPlayerView.updatePlayerView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause - 移除播放状态变化监听器");
        // 移除播放状态变化监听器
        if (bottomMusicPlayerView != null && isBottomPlayerInflated) {
            bottomMusicPlayerView.unregisterPlaybackStateChangeListener();
        }
    }

    /**
     * 绑定音乐播放服务
     */
    private void bindMusicService() {
        if (!isMusicServiceBound) {
            Intent intent = new Intent(this, MusicPlayerService.class);
            // 同时启动服务，确保服务不会因未启动而绑定失败
            startService(intent);
            boolean bound = bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "尝试绑定音乐服务，结果: " + bound);
        }
    }

    /**
     * 解绑音乐播放服务
     */
    private void unbindMusicService() {
        if (isMusicServiceBound) {
            unbindService(musicServiceConnection);
            isMusicServiceBound = false;
            Log.d(TAG, "解绑音乐服务");
        }
    }



    /**
     * 初始化所有组件
     */
    private void initializeComponents() {
        Log.d(TAG, "开始初始化组件");

        // 初始化Handler
        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化View
        initViews();

        // 初始化网络服务
        initNetworkService();

        // 设置RecyclerView
        setupRecyclerView();

        // 设置刷新布局
        setupRefreshLayout();

        // 修正：初始化底部音乐播放器（但不立即显示）
        initBottomMusicPlayerStub();

        Log.d(TAG, "组件初始化完成");
    }

    /**
     * 初始化View组件
     */
    private void initViews() {
        try {
            recyclerViewSwipe = findViewById(R.id.recycler_view_swipe);
            swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
            stubBottomPlayer = findViewById(R.id.stub_bottom_player);

            if (recyclerViewSwipe == null) {
                throw new IllegalStateException("RecyclerView未找到，请检查布局文件");
            }

            if (swipeRefreshLayout == null) {
                throw new IllegalStateException("SmartRefreshLayout未找到，请检查布局文件");
            }

            Log.d(TAG, "View初始化成功");

        } catch (Exception e) {
            Log.e(TAG, "View初始化失败", e);
            throw e;
        }
    }

    /**
     * 初始化网络服务
     */
    private void initNetworkService() {
        try {
            apiService = RetrofitClient.getInstance().getApiService();
            if (apiService == null) {
                throw new IllegalStateException("ApiService初始化失败");
            }
            Log.d(TAG, "网络服务初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "网络服务初始化失败", e);
            showError("网络服务初始化失败");
            throw e;
        }
    }

    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        try {
            // 打印数据详情，确保数据读取正确
            printDataDetails(currentData);

            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerViewSwipe.setLayoutManager(layoutManager);

            // 创建适配器
            adapter = new MultiTypeAdapter(this, currentData, this);

            recyclerViewSwipe.setAdapter(adapter);

            Log.d(TAG, "RecyclerView设置完成，初始适配器ItemCount: " + adapter.getItemCount());

        } catch (Exception e) {
            Log.e(TAG, "RecyclerView设置失败", e);
            throw e;
        }
    }

    /**
     * 打印currentData中的所有数据，用于调试和验证数据读取是否正确
     * @param data 要打印的数据列表
     */
    private void printDataDetails(List<ListItem> data) {
        if (data == null) {
            Log.e(TAG, "数据列表为空");
            return;
        }

        Log.d(TAG, "======== 数据列表详情 ========");
        Log.d(TAG, "总项目数: " + data.size());

        for (int i = 0; i < data.size(); i++) {
            ListItem item = data.get(i);
            Log.d(TAG, "项目 #" + i + " ----");

            switch (item.getItemType()) {
                case ListItem.TYPE_BANNER:
                    printBannerItemDetails((BannerItem) item);
                    break;

                case ListItem.TYPE_HORIZONTAL_CARD:
                    printHorizontalCardDetails((HorizontalCardItem) item);
                    break;

                case ListItem.TYPE_ONE_COLUMN:
                    printOneColumnDetails((OneColumnItem) item);
                    break;

                case ListItem.TYPE_TWO_COLUMN:
                    printTwoColumnDetails((TwoColumnItem) item);
                    break;

                default:
                    Log.d(TAG, "未知类型: " + item.getItemType());
            }
        }

        Log.d(TAG, "======== 数据列表详情结束 ========");
    }

    /**
     * 打印BannerItem的详细信息
     * @param bannerItem 要打印的BannerItem对象
     */
    private void printBannerItemDetails(BannerItem bannerItem) {
        Log.d(TAG, "类型: 轮播图");
        Log.d(TAG, "模块ID: " + bannerItem.getModuleId());
        Log.d(TAG, "标题: " + bannerItem.getTitle());

        printMusicList(bannerItem.getMusicList());
    }

    /**
     * 打印HorizontalCardItem的详细信息
     * @param horizontalCardItem 要打印的HorizontalCardItem对象
     */
    private void printHorizontalCardDetails(HorizontalCardItem horizontalCardItem) {
        Log.d(TAG, "类型: 横向卡片");
        Log.d(TAG, "模块ID: " + horizontalCardItem.getModuleId());
        Log.d(TAG, "标题: " + horizontalCardItem.getTitle());

        printMusicList(horizontalCardItem.getMusicList());
    }

    /**
     * 打印OneColumnItem的详细信息
     * @param oneColumnItem 要打印的OneColumnItem对象
     */
    private void printOneColumnDetails(OneColumnItem oneColumnItem) {
        Log.d(TAG, "类型: 单列布局");
        Log.d(TAG, "模块ID: " + oneColumnItem.getModuleId());
        Log.d(TAG, "标题: " + oneColumnItem.getTitle());

        printMusicList(oneColumnItem.getMusicList());
    }

    /**
     * 打印TwoColumnItem的详细信息
     * @param twoColumnItem 要打印的TwoColumnItem对象
     */
    private void printTwoColumnDetails(TwoColumnItem twoColumnItem) {
        Log.d(TAG, "类型: 双列布局");
        Log.d(TAG, "模块ID: " + twoColumnItem.getModuleId());
        Log.d(TAG, "标题: " + twoColumnItem.getTitle());

        printMusicList(twoColumnItem.getMusicList());
    }

    /**
     * 打印音乐列表的详细信息
     * @param musicList 要打印的音乐列表
     */
    private void printMusicList(List<MusicInfo> musicList) {
        if (musicList != null) {
            Log.d(TAG, "音乐列表数量: " + musicList.size());

            for (int j = 0; j < musicList.size(); j++) {
                MusicInfo music = musicList.get(j);
                Log.d(TAG, "  音乐 #" + j + ":");
                Log.d(TAG, "    ID: " + music.getId());
                Log.d(TAG, "    名称: " + music.getMusicName());
                Log.d(TAG, "    作者: " + music.getAuthor());
                Log.d(TAG, "    封面URL: " + music.getCoverUrl());
                Log.d(TAG, "    音乐URL: " + music.getMusicUrl());
                Log.d(TAG, "    歌词URL: " + music.getLyricUrl());
            }
        } else {
            Log.d(TAG, "音乐列表为空");
        }
    }

    /**
     * 优化后的设置刷新布局
     */
    private void setupRefreshLayout() {
        try {
            // 设置下拉刷新
            swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
                @Override
                public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                    Log.d(TAG, "用户触发下拉刷新");
                    // 重置无更多数据状态
                    refreshLayout.resetNoMoreData();
                    loadHomePageData(true);
                }
            });

            // 设置上拉加载更多
            swipeRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
                @Override
                public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                    Log.d(TAG, "用户触发上拉加载更多");
                    loadMoreData();
                }
            });

            // 优化配置参数
            swipeRefreshLayout.setEnableRefresh(true);
            swipeRefreshLayout.setEnableLoadMore(true);
            swipeRefreshLayout.setEnableAutoLoadMore(true);
            swipeRefreshLayout.setEnableOverScrollBounce(false); // 禁用过度滚动
            swipeRefreshLayout.setEnableOverScrollDrag(false);   // 禁用过度拖拽

            // 设置延迟参数避免状态冲突
            swipeRefreshLayout.setReboundDuration(300);

            Log.d(TAG, "刷新布局设置完成");

        } catch (Exception e) {
            Log.e(TAG, "刷新布局设置失败", e);
            throw e;
        }
    }

    /**
     * 修正后的网络请求方法
     */
    private void loadHomePageData(boolean isRefresh) {
        if (isLoading || isFinishing() || isDestroyed()) {
            Log.w(TAG, "数据正在加载中或Activity状态异常，跳过请求");
            finishRefreshAndLoadMore();
            return;
        }

        if (apiService == null) {
            showError("网络服务未初始化");
            finishRefreshAndLoadMore();
            return;
        }

        isLoading = true;

        if (isRefresh) {
            currentPage = 1;
            Log.d(TAG, "刷新数据，重置页码到1");
        } else {
            Log.d(TAG, "加载第" + currentPage + "页数据");
        }

        cancelCurrentRequest();

        currentCall = apiService.getHomePage(currentPage, pageSize);
        currentCall.enqueue(new Callback<BaseResponse<PagedData<ModuleConfig>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<PagedData<ModuleConfig>>> call,
                                   @NonNull Response<BaseResponse<PagedData<ModuleConfig>>> response) {

                if (isFinishing() || isDestroyed()) {
                    Log.w(TAG, "Activity已销毁，忽略网络响应");
                    return;
                }

                isLoading = false;
                currentCall = null;

                // 使用runOnUiThread替代mainHandler.post
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    // 先完成刷新/加载状态
                    finishRefreshAndLoadMore();

                    // 延迟处理响应，确保UI状态稳定
                    mainHandler.postDelayed(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            handleNetworkResponse(response, isRefresh);
                        }
                    }, 100);
                });
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<PagedData<ModuleConfig>>> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    Log.w(TAG, "Activity已销毁，忽略网络失败回调");
                    return;
                }

                isLoading = false;
                currentCall = null;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    finishRefreshAndLoadMore();

                    if (!call.isCanceled()) {
                        Log.e(TAG, "网络请求失败", t);
                        showError("网络请求失败: " + getErrorMessage(t));
                    }
                });
            }
        });
    }

    /**
     * 处理网络响应
     */
    private void handleNetworkResponse(Response<BaseResponse<PagedData<ModuleConfig>>> response, boolean isRefresh) {
        try {
            if (response.isSuccessful() && response.body() != null) {
                BaseResponse<PagedData<ModuleConfig>> baseResponse = response.body();

                if (baseResponse.getCode() == 200 && baseResponse.getData() != null) {
                    PagedData<ModuleConfig> pagedData = baseResponse.getData();
                    List<ModuleConfig> moduleConfigs = pagedData.getRecords();

                    if (moduleConfigs != null && !moduleConfigs.isEmpty()) {
                        processSuccessfulData(moduleConfigs, pagedData, isRefresh);
                    } else {
                        processEmptyData(isRefresh);
                    }
                } else {
                    String errorMsg = baseResponse.getMsg() != null ? baseResponse.getMsg() : "未知错误";
                    showError("服务器返回错误: " + errorMsg);
                    Log.e(TAG, "服务器错误，code: " + baseResponse.getCode() + ", msg: " + errorMsg);
                }
            } else {
                showError("网络请求失败，响应码: " + response.code());
                Log.e(TAG, "HTTP错误，响应码: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "数据处理失败", e);
            showError("数据解析失败");
        }
    }

    /**
     * 处理成功的数据
     */
    private void processSuccessfulData(List<ModuleConfig> moduleConfigs, PagedData<ModuleConfig> pagedData, boolean isRefresh) {
        try {
            // 转换数据
            List<ListItem> newItems = DataConverter.convertToListItems(moduleConfigs);
            Log.d(TAG, "转换得到 " + newItems.size() + " 个数据项");

            // 修正：对于初始加载或刷新，使用刷新逻辑
            if (isRefresh || currentData.isEmpty()) {
                // 刷新：替换所有数据
                currentData.clear();
                currentData.addAll(newItems);
                updateAdapter();
                Log.d(TAG, "刷新完成，当前数据总数: " + currentData.size());
            } else {
                // 加载更多：追加数据
                int oldSize = currentData.size();
                currentData.addAll(newItems);
                notifyAdapterItemsInserted(oldSize, newItems.size());
                Log.d(TAG, "加载更多完成，新增 " + newItems.size() + " 项，总数: " + currentData.size());
            }

            // 打印详细数据用于调试
            printDataDetails(currentData);

            // 检查是否还有更多数据
            checkNoMoreData(pagedData);

        } catch (Exception e) {
            Log.e(TAG, "处理成功数据时发生错误", e);
            showError("数据处理失败");
        }
    }

    /**
     * 处理空数据
     */
    private void processEmptyData(boolean isRefresh) {
        if (isRefresh || currentData.isEmpty()) {
            currentData.clear();
            updateAdapter();
            showToast("暂无数据");
        } else {
            setNoMoreData();
        }
        Log.d(TAG, "处理空数据，isRefresh: " + isRefresh);
    }

    /**
     * 辅助方法：完成刷新或加载更多状态
     */
    private void finishRefreshAndLoadMore() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.finishRefresh();
            swipeRefreshLayout.finishLoadMore();
        }
    }

    /**
     * 辅助方法：加载更多数据
     */
    private void loadMoreData() {
        currentPage++;
        loadHomePageData(false);
    }

    /**
     * 辅助方法：更新适配器
     */
    private void updateAdapter() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 辅助方法：通知适配器插入新项
     */
    private void notifyAdapterItemsInserted(int startPosition, int itemCount) {
        if (adapter != null) {
            adapter.notifyItemRangeInserted(startPosition, itemCount);
        }
    }

    /**
     * 辅助方法：设置没有更多数据
     */
    private void setNoMoreData() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.finishLoadMoreWithNoMoreData();
        }
    }

    /**
     * 辅助方法：检查是否还有更多数据
     */
    private void checkNoMoreData(PagedData<ModuleConfig> pagedData) {
        if (pagedData.getCurrent() * pagedData.getSize() >= pagedData.getTotal()) {
            setNoMoreData();
        } else {
            swipeRefreshLayout.setEnableLoadMore(true);
        }
    }

    /**
     * 辅助方法：取消当前网络请求
     */
    private void cancelCurrentRequest() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
            Log.d(TAG, "取消了之前的网络请求");
        }
    }

    /**
     * 辅助方法：显示错误信息
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * 辅助方法：显示Toast
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 辅助方法：获取错误信息
     */
    private String getErrorMessage(Throwable t) {
        if (t instanceof java.net.SocketTimeoutException) {
            return "连接超时";
        } else if (t instanceof java.net.UnknownHostException) {
            return "无法连接到服务器，请检查网络";
        } else if (t instanceof retrofit2.HttpException) {
            return "HTTP错误: " + ((retrofit2.HttpException) t).code();
        } else {
            return t.getMessage() != null ? t.getMessage() : "未知错误";
        }
    }

    @Override
    protected void onDestroy() {
        if (bottomMusicPlayerView != null) {
            bottomMusicPlayerView.unbindMusicService();
        }
        cancelCurrentRequest();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
        Log.d(TAG, "Activity销毁");
    }

    /**
     * 实现 OnMusicItemClickListener 接口
     * 当音乐项被点击时调用
     * @param musicInfo 被点击的音乐信息
     * @param position 被点击的音乐在列表中的位置
     */
    @Override
    public void onItemClick(MusicInfo musicInfo, int position) {
        Log.d(TAG, "点击了音乐: " + musicInfo.getMusicName() + ", 位置: " + position);
        // 将点击的音乐添加到播放列表的开头并设置为当前播放
        MusicManager.getInstance(this).addAndReorderPlaylist(musicInfo);
        // 使用回调方法跳转到音乐播放页面
        openMusicPlayerActivity();

        // 添加歌时判断BottomMusicPlayerView显示
        checkAndUpdateBottomPlayerVisibility();
    }

    @Override
    public void onPlayButtonClick(MusicInfo musicInfo, int position) {
        Log.d(TAG, "点击了播放按钮: " + musicInfo.getMusicName() + ", 位置: " + position);
        // 将点击的音乐添加到播放列表的开头并设置为当前播放
        MusicManager.getInstance(this).addToPlaylist(musicInfo);

        // 修正1：添加歌时判断BottomMusicPlayerView显示
        checkAndUpdateBottomPlayerVisibility();
    }

    /**
     * 实现MusicPlaylistDialog.OnPlaylistActionListener接口，
     * 以便MusicPlaylistDialog可以与Activity通信
     */
    @Override
    public void onPlayMusicFromPlaylist(int position) {
        // 从播放列表中播放指定位置的音乐
        if (musicService != null) {
            musicService.playAtPosition(position);
        }
    }
    @Override
    public void onPlaylistChanged() {
        // 播放列表变化时更新UI
        if (bottomMusicPlayerView != null) {
            bottomMusicPlayerView.updatePlayerView();
        }
    }
    @Override
    public void onPlayModeChanged(MusicPlayerService.PlayMode playMode) {
        // 播放模式变化时通知服务
        if (musicService != null) {
            musicService.setPlayMode(playMode);
        }
    }
    @Override
    public MusicPlayerService.PlayMode getCurrentPlayMode() {
        // 获取当前播放模式
        if (musicService != null) {
            return musicService.getPlayMode();
        }
        return MusicPlayerService.PlayMode.SEQUENCE; // 默认顺序播放
    }

    /**
     * 修正1：初始化底部播放器ViewStub，但不立即inflate
     */
    private void initBottomMusicPlayerStub() {
        try {
            if (stubBottomPlayer == null) {
                Log.e(TAG, "ViewStub未找到");
                return;
            }

            // 不立即inflate，等需要显示时再inflate
            isBottomPlayerInflated = false;
            Log.d(TAG, "底部播放器ViewStub初始化成功");

        } catch (Exception e) {
            Log.e(TAG, "底部播放器ViewStub初始化失败", e);
        }
    }

    /**
     * 修正1和修正2：检查并更新底部播放器的显示状态
     * 根据当前播放状态决定是否显示BottomMusicPlayerView
     */
    private void checkAndUpdateBottomPlayerVisibility() {
        try {
            MusicManager musicManager = MusicManager.getInstance(this);
            // 检查播放列表是否为空
            boolean hasPlaylist = musicManager.hasPlaylist();
            Log.d(TAG, "检查播放器显示状态 - hasPlaylist: " + hasPlaylist +
                    ", isInflated: " + isBottomPlayerInflated);
            if (hasPlaylist) {
                // 有播放列表，需要显示底部播放器
                if (!isBottomPlayerInflated) {
                    // ViewStub还没有inflate，现在inflate
                    inflateBottomMusicPlayer();
                } else if (bottomMusicPlayerView != null) {
                    // 已经inflate，只需要更新视图并确保可见
                    bottomMusicPlayerView.setVisibility(View.VISIBLE);
                    bottomMusicPlayerView.updatePlayerView();
                }
            } else {
                // 空列表时隐藏BottomMusicPlayerView
                if (isBottomPlayerInflated && bottomMusicPlayerView != null) {
                    bottomMusicPlayerView.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查底部播放器显示状态时出错", e);
        }
    }

    // 使用回调方法实现音乐播放页面跳转
    private void openMusicPlayerActivity() {
        // 添加防重复跳转判断
        if (isFinishing() || isDestroyed()) {
            Log.d(TAG, "Activity已销毁，忽略页面跳转");
            return;
        }

        // 释放资源，避免内存泄漏
        if (bottomMusicPlayerView != null) {
            bottomMusicPlayerView.pauseUpdateTasks();
        }

        // 设置跳转标志
        Intent intent = new Intent(this, MusicPlayerActivity.class);
        // 添加标志以清除任务栈中可能存在的相同Activity实例
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    /**
     * 显示播放列表对话框
     */
    private void showPlaylistDialog() {
        MusicPlaylistDialog playlistDialog = MusicPlaylistDialog.newInstance();
        playlistDialog.show(getSupportFragmentManager(), "MusicPlaylistDialog");
    }


    /**
     * inflate底部播放器ViewStub
     */
    private void inflateBottomMusicPlayer() {
        try {
            if (stubBottomPlayer != null && !isBottomPlayerInflated) {
                // inflate ViewStub，返回的是BottomMusicPlayerView实例
                View inflatedView = stubBottomPlayer.inflate();
                // 正确获取BottomMusicPlayerView
                bottomMusicPlayerView = (BottomMusicPlayerView) inflatedView;
                isBottomPlayerInflated = true;
                if (bottomMusicPlayerView != null) {
                    // 设置回调监听器 - 需要在BottomMusicPlayerView中添加此方法
                    bottomMusicPlayerView.setOnBottomPlayerActionListener(new BottomMusicPlayerView.OnBottomPlayerActionListener() {
                        @Override
                        public void onBottomPlayerClick() {
                            // 点击播放器主体时跳转到播放页面
                            openMusicPlayerActivity();
                        }
                        @Override
                        public void onPlaylistButtonClick() {
                            // 点击播放列表按钮时显示播放列表对话框
                            showPlaylistDialog();
                        }
                    });

                    // 更新播放器状态
                    bottomMusicPlayerView.updatePlayerView();
                    bottomMusicPlayerView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "底部播放器inflate并显示成功");
                } else {
                    Log.e(TAG, "底部播放器inflate后仍然为null");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "inflate底部播放器失败", e);
        }
    }
}
