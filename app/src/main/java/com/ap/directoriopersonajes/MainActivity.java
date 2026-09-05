package com.ap.directoriopersonajes;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.ap.directoriopersonajes.dto.CharacterDto;
import com.ap.directoriopersonajes.dto.CharacterResponseDto;
import com.ap.directoriopersonajes.network.DragonBallApiService;
import com.ap.directoriopersonajes.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private DragonBallApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiService = RetrofitClient.getApiService();
        loadCharacters();
    }

    private void loadCharacters() {
        apiService.getCharacters().enqueue(new Callback<CharacterResponseDto>() {
            @Override
            public void onResponse(Call<CharacterResponseDto> call, Response<CharacterResponseDto> response) {
                if (!response.isSuccessful()) {
                    handleHttpError(response.code());
                    return;
                }

                CharacterResponseDto responseBody = response.body();
                if (responseBody == null || responseBody.getItems() == null) {
                    handleEmptyResponse();
                    return;
                }

                List<CharacterDto> characters = responseBody.getItems();
                logCharacters(characters);
                showToast("Personajes recibidos: " + characters.size());
            }

            @Override
            public void onFailure(Call<CharacterResponseDto> call, Throwable throwable) {
                Log.e(TAG, "No fue posible conectar con la API", throwable);
                showToast("No fue posible conectar con la API");
            }
        });
    }

    private void logCharacters(List<CharacterDto> characters) {
        for (CharacterDto character : characters) {
            Log.d(TAG, "Character: "
                    + character.getName()
                    + " | Race: " + character.getRace()
                    + " | Gender: " + character.getGender());
        }
    }

    private void handleHttpError(int statusCode) {
        Log.e(TAG, "HTTP error while fetching characters. Code: " + statusCode);
        showToast("Error al obtener personajes. Codigo HTTP: " + statusCode);
    }

    private void handleEmptyResponse() {
        Log.w(TAG, "The API returned an empty response body or items list");
        showToast("La API no devolvio personajes");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
