package com.ap.directoriopersonajes;

import android.app.Activity;
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
    private static final int PAGINATION_THRESHOLD = 3;

    private final List<CharacterDto> characterList = new ArrayList<>();

    private DragonBallApiService apiService;
    private CharacterAdapter characterAdapter;
    private LinearLayoutManager layoutManager;
    private RecyclerView recyclerViewCharacters;
    private ProgressBar progressBarCharacters;
    private LinearLayout layoutBottomLoading;
    private LinearLayout layoutStateMessage;
    private TextView textViewStateMessage;
    private Button buttonRetry;
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
        textViewStateMessage = findViewById(R.id.textViewStateMessage);
        buttonRetry = findViewById(R.id.buttonRetry);
    }

    private void setupRecyclerView() {
        characterAdapter = new CharacterAdapter(character -> {
            String name = getValueOrDefault(character.getName(), getString(R.string.unknown_character));
            showToast(getString(R.string.selected_character_format, name));
        });

        layoutManager = new LinearLayoutManager(this);
        recyclerViewCharacters.setLayoutManager(layoutManager);
        recyclerViewCharacters.setAdapter(characterAdapter);
        recyclerViewCharacters.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && shouldLoadNextPage()) {
                    loadCharacters(currentPage + 1);
                }
            }
        });

        buttonRetry.setOnClickListener(view -> retryInitialLoad());
    }

    private void loadCharacters() {
        loadCharacters(FIRST_PAGE);
    }

    private void loadCharacters(int page) {
        if (isLoading || page > totalPages && currentPage > 0) {
            return;
        }

        isLoading = true;
        if (page == FIRST_PAGE) {
            showInitialLoadingState();
        } else {
            showNextPageLoadingState();
        }

        activeCall = apiService.getCharacters(page);
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

    private boolean shouldLoadNextPage() {
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

        return !isLoading
                && !isLastPage
                && totalItemCount > 0
                && visibleItemCount + firstVisibleItemPosition >= totalItemCount - PAGINATION_THRESHOLD;
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
            return;
        }

        characterList.addAll(characters);
        characterAdapter.addCharacters(characters);
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

    private void retryInitialLoad() {
        if (isLoading) {
            return;
        }

        currentPage = 0;
        totalPages = FIRST_PAGE;
        isLastPage = false;
        characterList.clear();
        characterAdapter.setCharacters(characterList);
        loadCharacters(FIRST_PAGE);
    }

    private void showInitialLoadingState() {
        progressBarCharacters.setVisibility(View.VISIBLE);
        layoutBottomLoading.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.GONE);
        layoutStateMessage.setVisibility(View.GONE);
        buttonRetry.setVisibility(View.GONE);
    }

    private void showNextPageLoadingState() {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.VISIBLE);
        recyclerViewCharacters.setVisibility(View.VISIBLE);
        layoutStateMessage.setVisibility(View.GONE);
        buttonRetry.setVisibility(View.GONE);
    }

    private void hideLoadingIndicators() {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.GONE);
    }

    private void showContentState() {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.VISIBLE);
        layoutStateMessage.setVisibility(View.GONE);
        buttonRetry.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.GONE);
        textViewStateMessage.setText(R.string.empty_characters_message);
        layoutStateMessage.setVisibility(View.VISIBLE);
        buttonRetry.setVisibility(View.GONE);
    }

    private void showErrorState(String message, boolean canRetry) {
        progressBarCharacters.setVisibility(View.GONE);
        layoutBottomLoading.setVisibility(View.GONE);
        recyclerViewCharacters.setVisibility(View.GONE);
        textViewStateMessage.setText(message);
        layoutStateMessage.setVisibility(View.VISIBLE);
        buttonRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        if (activeCall != null) {
            activeCall.cancel();
        }
        super.onDestroy();
    }
}
