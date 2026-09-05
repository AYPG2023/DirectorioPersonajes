package com.ap.directoriopersonajes;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.directoriopersonajes.adapter.CharacterAdapter;
import com.ap.directoriopersonajes.dto.CharacterDto;
import com.ap.directoriopersonajes.dto.CharacterResponseDto;
import com.ap.directoriopersonajes.network.DragonBallApiService;
import com.ap.directoriopersonajes.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private final List<CharacterDto> characterList = new ArrayList<>();

    private DragonBallApiService apiService;
    private CharacterAdapter characterAdapter;
    private RecyclerView recyclerViewCharacters;
    private ProgressBar progressBarCharacters;
    private TextView textViewStateMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupRecyclerView();
        apiService = RetrofitClient.getApiService();
        loadCharacters();
    }

    private void bindViews() {
        recyclerViewCharacters = findViewById(R.id.recyclerViewCharacters);
        progressBarCharacters = findViewById(R.id.progressBarCharacters);
        textViewStateMessage = findViewById(R.id.textViewStateMessage);
    }

    private void setupRecyclerView() {
        characterAdapter = new CharacterAdapter();
        characterAdapter.setCharacters(characterList);

        recyclerViewCharacters.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCharacters.setAdapter(characterAdapter);
    }

    private void loadCharacters() {
        showLoadingState();

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
                if (characters.isEmpty()) {
                    showEmptyState();
                    return;
                }

                characterList.clear();
                characterList.addAll(characters);
                characterAdapter.setCharacters(characterList);
                showContentState();

                logCharacters(characters);
                showToast(getString(R.string.characters_received_format, characters.size()));
            }

            @Override
            public void onFailure(Call<CharacterResponseDto> call, Throwable throwable) {
                Log.e(TAG, getString(R.string.connection_error_message), throwable);
                showErrorState(getString(R.string.connection_error_message));
                showToast(getString(R.string.connection_error_message));
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
        String message = getString(R.string.http_error_format, statusCode);
        showErrorState(message);
        showToast(message);
    }

    private void handleEmptyResponse() {
        Log.w(TAG, "The API returned an empty response body or items list");
        showEmptyState();
        showToast(getString(R.string.empty_characters_message));
    }

    private void showLoadingState() {
        progressBarCharacters.setVisibility(View.VISIBLE);
        recyclerViewCharacters.setVisibility(View.GONE);
        textViewStateMessage.setVisibility(View.GONE);
    }

    private void showContentState() {
        progressBarCharacters.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.VISIBLE);
        textViewStateMessage.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        progressBarCharacters.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.GONE);
        textViewStateMessage.setText(R.string.empty_characters_message);
        textViewStateMessage.setVisibility(View.VISIBLE);
    }

    private void showErrorState(String message) {
        progressBarCharacters.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.GONE);
        textViewStateMessage.setText(message);
        textViewStateMessage.setVisibility(View.VISIBLE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
