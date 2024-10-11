package com.amazic.library.ads.call_api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("getidv2/{param}")
    Call<List<AdsModel>> callAds(@Path("param") String parameterValue);
}
