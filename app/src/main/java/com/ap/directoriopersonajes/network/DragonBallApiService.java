package com.ap.directoriopersonajes.network;

import com.ap.directoriopersonajes.dto.CharacterResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;

public interface DragonBallApiService {

    @GET("characters")
    Call<CharacterResponseDto> getCharacters();
}
