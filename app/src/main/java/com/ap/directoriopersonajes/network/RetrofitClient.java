package com.ap.directoriopersonajes.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static final String BASE_URL = "https://dragonball-api.com/api/";

    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private RetrofitClient() {
    }

    public static DragonBallApiService getApiService() {
        return retrofit.create(DragonBallApiService.class);
    }
}
