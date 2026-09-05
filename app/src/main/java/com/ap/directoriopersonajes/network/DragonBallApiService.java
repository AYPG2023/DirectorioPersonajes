package com.ap.directoriopersonajes.network;

import com.ap.directoriopersonajes.dto.CharacterResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DragonBallApiService {

    @GET("characters")
    Call<CharacterResponseDto> getCharacters(
            @Query("page") int page,
            @Query("limit") int limit
    );
}
