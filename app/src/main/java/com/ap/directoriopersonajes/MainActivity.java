package com.ap.directoriopersonajes;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
    private static final int FIRST_PAGE = 1;
    private static final int PAGE_SIZE = 10;

    private final List<CharacterDto> characterList = new ArrayList<>();

    private DragonBallApiService apiService;
    private CharacterAdapter characterAdapter;
    private LinearLayoutManager layoutManager;
    private RecyclerView recyclerViewCharacters;
    private ProgressBar progressBarCharacters;
    private LinearLayout layoutBottomLoading;
    private LinearLayout layoutStateMessage;
    private TextView textViewLoadedCount;
    private TextView textViewStateMessage;
    private Button buttonRetry;
    private Button buttonLoadMore;
    private Call<CharacterResponseDto> activeCall;

    private int currentPage = 0;
    private int totalPages = FIRST_PAGE;
    private boolean isLoading = false;
    private boolean isLastPage = false;

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
        layoutBottomLoading = findViewById(R.id.layoutBottomLoading);
        layoutStateMessage = findViewById(R.id.layoutStateMessage);
        textViewLoadedCount = findViewById(R.id.textViewLoadedCount);
        textViewStateMessage = findViewById(R.id.textViewStateMessage);
        buttonRetry = findViewById(R.id.buttonRetry);
        buttonLoadMore = findViewById(R.id.buttonLoadMore);
        updateLoadedCount();
    }

    private void setupRecyclerView() {
        characterAdapter = new CharacterAdapter(character -> {
            openCharacterDetail(character);
        });

        layoutManager = new LinearLayoutManager(this);
        recyclerViewCharacters.setLayoutManager(layoutManager);
        recyclerViewCharacters.setAdapter(characterAdapter);

        buttonRetry.setOnClickListener(view -> retryInitialLoad());
        buttonLoadMore.setOnClickListener(view -> loadNextPage());
    }

    private void openCharacterDetail(CharacterDto character) {
        if (character == null) {
            return;
        }

        startActivity(CharacterDetailActivity.createIntent(
                this,
                character.getName(),
                character.getKi(),
                character.getMaxKi(),
                character.getRace(),
                character.getGender(),
                character.getDescription(),
                character.getImage(),
                character.getAffiliation()
        ));
    }

    private void loadCharacters() {
        loadCharacters(FIRST_PAGE);
    }

    private void loadCharacters(int page) {
        if (isLoading || page > totalPages && currentPage > 0) {
            return;
        }

        if (!hasInternetConnection()) {
            handleNoInternetConnection(page);
            return;
        }

        isLoading = true;
        if (page == FIRST_PAGE) {
            showInitialLoadingState();
        } else {
            showNextPageLoadingState();
        }

        activeCall = apiService.getCharacters(page, PAGE_SIZE);
        activeCall.enqueue(new Callback<CharacterResponseDto>() {
            @Override
            public void onResponse(Call<CharacterResponseDto> call, Response<CharacterResponseDto> response) {
                if (isActivityInactive()) {
                    return;
                }

                isLoading = false;
                hideLoadingIndicators();

                if (!response.isSuccessful()) {
                    handleHttpError(response.code(), page);
                    return;
                }

                CharacterResponseDto responseBody = response.body();
                if (responseBody == null || responseBody.getItems() == null) {
                    handleEmptyResponse(page);
                    return;
                }

                List<CharacterDto> characters = responseBody.getItems();
                if (characters.isEmpty()) {
                    handleEmptyResponse(page);
                    return;
                }

                updatePaginationState(responseBody, page);
                appendCharacters(characters, page);
                showContentState();

                logCharacters(characters);
                if (page == FIRST_PAGE) {
                    showToast(getString(R.string.characters_received_format, characters.size()));
                }
            }

            @Override
            public void onFailure(Call<CharacterResponseDto> call, Throwable throwable) {
                if (isActivityInactive() || call.isCanceled()) {
                    return;
                }

                isLoading = false;
                hideLoadingIndicators();
                handleConnectionError(throwable, page);
            }
        });
    }

    private void loadNextPage() {
        if (isLoading || isLastPage) {
            return;
        }
        loadCharacters(currentPage + 1);
    }

    private void updatePaginationState(CharacterResponseDto responseBody, int requestedPage) {
        currentPage = requestedPage;
        if (responseBody.getMeta() != null) {
            totalPages = responseBody.getMeta().getTotalPages();
            currentPage = responseBody.getMeta().getCurrentPage();
        }
        isLastPage = currentPage >= totalPages;
    }

    private void appendCharacters(List<CharacterDto> characters, int page) {
        if (page == FIRST_PAGE) {
            characterList.clear();
            characterList.addAll(characters);
            characterAdapter.setCharacters(characterList);
            updateLoadedCount();
            return;
        }

        characterList.addAll(characters);
        characterAdapter.addCharacters(characters);
        updateLoadedCount();
    }

    private void logCharacters(List<CharacterDto> characters) {
        for (CharacterDto character : characters) {
            Log.d(TAG, "Character: "
                    + character.getName()
                    + " | Race: " + character.getRace()
                    + " | Gender: " + character.getGender());
        }
    }

    private void handleHttpError(int statusCode, int page) {
        Log.e(TAG, "HTTP error while fetching characters. Code: " + statusCode);
        String message = getString(R.string.http_error_format, statusCode);
        if (page == FIRST_PAGE && characterList.isEmpty()) {
            showErrorState(message, true);
        } else {
            showContentState();
            showToast(getString(R.string.next_page_error_message));
        }
    }

    private void handleEmptyResponse(int page) {
        Log.w(TAG, "The API returned an empty response body or items list");
        if (page == FIRST_PAGE && characterList.isEmpty()) {
            showEmptyState();
        } else {
            isLastPage = true;
            showContentState();
        }
    }

    private void handleConnectionError(Throwable throwable, int page) {
        Log.e(TAG, getString(R.string.connection_error_message), throwable);
        if (page == FIRST_PAGE && characterList.isEmpty()) {
            showErrorState(getString(R.string.connection_error_message), true);
            return;
        }

        showContentState();
        showToast(getString(R.string.next_page_error_message));
    }

    private void handleNoInternetConnection(int page) {
        Log.w(TAG, getString(R.string.no_internet_message));
        if (page == FIRST_PAGE && characterList.isEmpty()) {
            showErrorState(getString(R.string.no_internet_message), true);
            return;
        }

        showContentState();
        showToast(getString(R.string.next_page_error_message));
    }

    private void retryInitialLoad() {
        if (isLoading) {
            return;
        }

        showToast(getString(R.string.retrying_message));
        currentPage = 0;
        totalPages = FIRST_PAGE;
        isLastPage = false;
        characterList.clear();
        characterAdapter.setCharacters(characterList);
        updateLoadedCount();
        loadCharacters(FIRST_PAGE);
    }

    private void showInitialLoadingState() {
        progressBarCharacters.setVisibility(View.VISIBLE);
        layoutBottomLoading.setVisibility(View.GONE);
        buttonLoadMore.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.GONE);
        layoutStateMessage.setVisibility(View.GONE);
        buttonRetry.setVisibility(View.GONE);
    }

    private void showNextPageLoadingState() {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.VISIBLE);
        buttonLoadMore.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.VISIBLE);
        layoutStateMessage.setVisibility(View.GONE);
        buttonRetry.setVisibility(View.GONE);
    }

    private void hideLoadingIndicators() {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.GONE);
        buttonLoadMore.setVisibility(View.GONE);
    }

    private void showContentState() {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.VISIBLE);
        layoutStateMessage.setVisibility(View.GONE);
        buttonRetry.setVisibility(View.GONE);
        buttonLoadMore.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState() {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.GONE);
        buttonLoadMore.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.GONE);
        textViewStateMessage.setText(R.string.empty_characters_message);
        layoutStateMessage.setVisibility(View.VISIBLE);
        buttonRetry.setVisibility(View.GONE);
    }

    private void showErrorState(String message, boolean canRetry) {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.GONE);
        buttonLoadMore.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.GONE);
        textViewStateMessage.setText(message);
        layoutStateMessage.setVisibility(View.VISIBLE);
        buttonRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateLoadedCount() {
        textViewLoadedCount.setText(getString(R.string.loaded_characters_format, characterList.size()));
    }

    private String getValueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private boolean isActivityInactive() {
        return isFinishing() || isDestroyed();
    }

    private boolean hasInternetConnection() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @Override
    protected void onDestroy() {
        if (activeCall != null) {
            activeCall.cancel();
        }
        super.onDestroy();
    }
}
