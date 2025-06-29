package com.qzz.musiccommunity.network;

import com.qzz.musiccommunity.network.dto.BaseResponse;
import com.qzz.musiccommunity.network.dto.ModuleConfig;
import com.qzz.musiccommunity.network.dto.PagedData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("music/homePage")
    Call<BaseResponse<PagedData<ModuleConfig>>> getHomePage(
            @Query("current") int current,
            @Query("size") int size
    );
}
