package com.qzz.musiccommunity.ui.views.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qzz.musiccommunity.R;
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

public class HomeActivity extends AppCompatActivity implements OnMusicItemClickListener {

    private static final String TAG = "HomeActivity";

    // UI组件
    private RecyclerView recyclerViewSwipe;
    private MultiTypeAdapter adapter;
    private SmartRefreshLayout swipeRefreshLayout;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.d(TAG, "Activity创建开始");

        try {
            initializeComponents();

            // 延迟加载初始数据，确保UI完全初始化
            // 修正1：初始加载应该按刷新处理
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

        Log.d(TAG, "组件初始化完成");
    }

    /**
     * 初始化View组件
     */
    private void initViews() {
        try {
            recyclerViewSwipe = findViewById(R.id.recycler_view_swipe);
            swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

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
     * 修正2：处理成功的数据 - 完善逻辑
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

            // 打印适配器状态
            printAdapterState();

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
     * 加载更多数据
     */
    private void loadMoreData() {
        if (isLoading) {
            swipeRefreshLayout.finishLoadMore();
            return;
        }

        currentPage++;
        Log.d(TAG, "准备加载第 " + currentPage + " 页数据");
        loadHomePageData(false);
    }

    /**
     * 修正3：更新适配器 - 确保正确通知数据变化
     */
    private void updateAdapter() {
        if (adapter != null) {
            Log.d(TAG, "更新适配器前 - ItemCount: " + adapter.getItemCount() + ", 数据大小: " + currentData.size());

            adapter.setItems(currentData);

            Log.d(TAG, "更新适配器后 - ItemCount: " + adapter.getItemCount() + ", 数据大小: " + currentData.size());

            // 如果适配器的setItems方法没有自动调用notifyDataSetChanged，手动调用
            // adapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "适配器为null，无法更新数据");
        }
    }

    /**
     * 修正4：通知适配器新增数据 - 优化实现
     */
    private void notifyAdapterItemsInserted(int positionStart, int itemCount) {
        if (adapter != null) {
            Log.d(TAG, "通知适配器插入数据前 - ItemCount: " + adapter.getItemCount());

            adapter.setItems(currentData);

            // 推荐使用 notifyItemRangeInserted 而不是完全刷新
            // 这样性能更好且动画效果更流畅
            // adapter.notifyItemRangeInserted(positionStart, itemCount);

            Log.d(TAG, "通知适配器插入数据后 - 位置: " + positionStart + ", 数量: " + itemCount + ", ItemCount: " + adapter.getItemCount());
        } else {
            Log.e(TAG, "适配器为null，无法插入数据");
        }
    }

    /**
     * 新增：打印适配器状态，用于调试
     */
    private void printAdapterState() {
        if (adapter != null) {
            Log.d(TAG, "========适配器状态========");
            Log.d(TAG, "适配器项目数: " + adapter.getItemCount());
            Log.d(TAG, "数据列表大小: " + currentData.size());

            for (int i = 0; i < Math.min(currentData.size(), 5); i++) {
                ListItem item = currentData.get(i);
                Log.d(TAG, "项目 " + i + ": " + getItemTypeName(item.getItemType()));
            }
            if (currentData.size() > 5) {
                Log.d(TAG, "... 还有 " + (currentData.size() - 5) + " 个项目");
            }
            Log.d(TAG, "========================");
        } else {
            Log.e(TAG, "适配器为null");
        }
    }

    /**
     * 新增：获取项目类型名称，用于调试
     */
    private String getItemTypeName(int itemType) {
        switch (itemType) {
            case ListItem.TYPE_BANNER: return "BANNER";
            case ListItem.TYPE_HORIZONTAL_CARD: return "HORIZONTAL_CARD";
            case ListItem.TYPE_ONE_COLUMN: return "ONE_COLUMN";
            case ListItem.TYPE_TWO_COLUMN: return "TWO_COLUMN";
            default: return "UNKNOWN(" + itemType + ")";
        }
    }

    /**
     * 修正后的检查是否还有更多数据
     */
    private void checkNoMoreData(PagedData<ModuleConfig> pagedData) {
        if (pagedData == null || isFinishing() || isDestroyed()) {
            return;
        }

        try {
            boolean noMoreData = false;

            if (pagedData.getTotal() > 0 && currentData.size() >= pagedData.getTotal()) {
                noMoreData = true;
                Log.d(TAG, "已加载所有数据，当前: " + currentData.size() + ", 总数: " + pagedData.getTotal());
            } else if (pagedData.getPages() > 0 && currentPage >= pagedData.getPages()) {
                noMoreData = true;
                Log.d(TAG, "已加载所有页面，当前页: " + currentPage + ", 总页数: " + pagedData.getPages());
            }

            // 使用更长的延迟确保状态稳定
            if (noMoreData) {
                mainHandler.postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        setNoMoreData();
                    }
                }, 500); // 增加延迟时间
            }

        } catch (Exception e) {
            Log.e(TAG, "检查更多数据时发生错误", e);
        }
    }



    /**
     * 修正后的设置没有更多数据方法
     */
    private void setNoMoreData() {
        if (swipeRefreshLayout == null || isFinishing() || isDestroyed()) {
            return;
        }

        try {
            runOnUiThread(() -> {
                if (swipeRefreshLayout == null || isFinishing() || isDestroyed()) {
                    return;
                }

                // 确保刷新和加载状态都已完成再设置noMoreData
                if (!swipeRefreshLayout.isRefreshing() && !swipeRefreshLayout.isLoading()) {
                    swipeRefreshLayout.finishLoadMoreWithNoMoreData();
                    Log.d(TAG, "设置无更多数据状态");
                } else {
                    // 如果还在刷新或加载中，延迟设置
                    mainHandler.postDelayed(() -> setNoMoreData(), 300);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "设置无更多数据时发生错误", e);
        }
    }


    /**
     * 修正后的完成刷新和加载更多方法 - 避免状态冲突
     */
    private void finishRefreshAndLoadMore() {
        if (swipeRefreshLayout == null || isFinishing() || isDestroyed()) {
            return;
        }

        try {
            // 使用runOnUiThread确保在主线程执行
            runOnUiThread(() -> {
                if (swipeRefreshLayout == null || isFinishing() || isDestroyed()) {
                    return;
                }

                // 分别检查和处理刷新状态
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.finishRefresh(200, true, false); // 添加延迟参数
                    Log.d(TAG, "完成下拉刷新");
                }

                // 分别检查和处理加载更多状态
                if (swipeRefreshLayout.isLoading()) {
                    swipeRefreshLayout.finishLoadMore(200, true, false); // 添加延迟参数，不触发noMoreData
                    Log.d(TAG, "完成上拉加载");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "完成刷新状态时发生错误", e);
        }
    }



    /**
     * 取消当前网络请求
     */
    private void cancelCurrentRequest() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
            currentCall = null;
            Log.d(TAG, "已取消当前网络请求");
        }
    }

    /**
     * 获取错误信息
     */
    private String getErrorMessage(Throwable t) {
        if (t == null) {
            return "未知错误";
        }

        String message = t.getMessage();
        if (message == null || message.isEmpty()) {
            return t.getClass().getSimpleName();
        }

        return message;
    }

    // ==================== 公共方法 ====================

    /**
     * 手动刷新数据
     */
    public void refreshData() {
        Log.d(TAG, "手动刷新数据");
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.autoRefresh();
        } else {
            loadHomePageData(true);
        }
    }

    /**
     * 重置数据
     */
    public void resetData() {
        Log.d(TAG, "重置数据");

        currentData.clear();
        currentPage = 1;
        isLoading = false;

        updateAdapter();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.resetNoMoreData();
        }
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Log.e(TAG, "显示错误: " + message);
        showToast(message);
    }

    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        if (message != null && !message.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== 生命周期方法 ====================

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity onResume");
        // TODO: 后期可在此处添加数据刷新逻辑
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity onPause");
        // TODO: 后期可在此处添加暂停逻辑
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity onDestroy");

        try {
            // 取消网络请求
            cancelCurrentRequest();

            // 清理Handler
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
                mainHandler = null;
            }

            // 清理数据
            if (currentData != null) {
                currentData.clear();
            }

            // 清理适配器
            if (adapter != null) {
                // TODO: 后期实现adapter的cleanup方法
                adapter = null;
            }

            Log.d(TAG, "Activity资源清理完成");

        } catch (Exception e) {
            Log.e(TAG, "Activity销毁时清理资源失败", e);
        }
    }

    @Override
    public void onBackPressed() {
        // TODO: 后期可添加退出确认逻辑
        super.onBackPressed();
    }

    @Override
    public void onPlayButtonClick(MusicInfo musicInfo, int position) {
        // 处理播放按钮点击事件，启动MusicPlayerActivity
        musicInfo.printDetailedInfo();
        Log.d(TAG, "播放添加按钮点击: " + musicInfo.getMusicName() + ", 位置: " + position);


        // 启动MusicPlayerActivity并传递当前音乐信息
//        startMusicPlayerActivityWithMusic(musicInfo);
        MusicManager musicManager = MusicManager.getInstance();
        musicManager.addToPlaylist(musicInfo);

        Toast.makeText(this, musicInfo.getMusicName()+"已添加到播放列表", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onItemClick(MusicInfo musicInfo, int position) {
        // 处理整个item点击事件，启动MusicPlayerActivity
        musicInfo.printDetailedInfo();
        Log.d(TAG, "Item点击: " + musicInfo.getMusicName() + ", 位置: " + position);

        // 启动MusicPlayerActivity并传递当前音乐信息
        startMusicPlayerActivityWithMusic(musicInfo);
    }

    /**
     * 启动音乐播放页面并传递音乐信息
     * @param musicInfo 要播放的音乐信息
     */
    private void startMusicPlayerActivityWithMusic(MusicInfo musicInfo) {
        Intent intent = new Intent(this, MusicPlayerActivity.class);

        // 方法1：直接通过Intent传递Parcelable对象（推荐）
        intent.putExtra("MUSIC_INFO", musicInfo);

        // 方法2：使用Bundle包装数据（如果需要传递更多参数）
        Bundle bundle = new Bundle();
        bundle.putParcelable("MUSIC_INFO", musicInfo);
        bundle.putString("SOURCE_ACTIVITY", "MainActivity"); // 可选：标识来源
        bundle.putLong("CLICK_TIME", System.currentTimeMillis()); // 可选：点击时间戳
        intent.putExtras(bundle);

        startActivity(intent);
        Log.d(TAG, "已启动MusicPlayerActivity，传递音乐: " + musicInfo.getMusicName());
    }


}
