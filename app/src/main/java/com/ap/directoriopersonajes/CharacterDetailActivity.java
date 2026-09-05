package com.ap.directoriopersonajes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class CharacterDetailActivity extends Activity {

    private static final String EXTRA_NAME = "extra_name";
    private static final String EXTRA_KI = "extra_ki";
    private static final String EXTRA_MAX_KI = "extra_max_ki";
    private static final String EXTRA_RACE = "extra_race";
    private static final String EXTRA_GENDER = "extra_gender";
    private static final String EXTRA_DESCRIPTION = "extra_description";
    private static final String EXTRA_IMAGE = "extra_image";
    private static final String EXTRA_AFFILIATION = "extra_affiliation";

    public static Intent createIntent(
            Context context,
            String name,
            String ki,
            String maxKi,
            String race,
            String gender,
            String description,
            String image,
            String affiliation
    ) {
        Intent intent = new Intent(context, CharacterDetailActivity.class);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_KI, ki);
        intent.putExtra(EXTRA_MAX_KI, maxKi);
        intent.putExtra(EXTRA_RACE, race);
        intent.putExtra(EXTRA_GENDER, gender);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        intent.putExtra(EXTRA_IMAGE, image);
        intent.putExtra(EXTRA_AFFILIATION, affiliation);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_detail);

        bindCharacterDetails();
    }

    private void bindCharacterDetails() {
        TextView textViewBack = findViewById(R.id.textViewBack);
        ImageView imageViewDetailCharacter = findViewById(R.id.imageViewDetailCharacter);
        TextView textViewDetailName = findViewById(R.id.textViewDetailName);
        TextView textViewDetailSubtitle = findViewById(R.id.textViewDetailSubtitle);
        TextView textViewDetailKi = findViewById(R.id.textViewDetailKi);
        TextView textViewDetailMaxKi = findViewById(R.id.textViewDetailMaxKi);
        TextView textViewDetailAffiliation = findViewById(R.id.textViewDetailAffiliation);
        TextView textViewDetailDescription = findViewById(R.id.textViewDetailDescription);

        String name = getExtraOrDefault(EXTRA_NAME, getString(R.string.unknown_character));
        String race = getExtraOrDefault(EXTRA_RACE, getString(R.string.unknown_race));
        String gender = getExtraOrDefault(EXTRA_GENDER, getString(R.string.unknown_gender));
        String ki = getExtraOrDefault(EXTRA_KI, getString(R.string.unavailable_ki));
        String maxKi = getExtraOrDefault(EXTRA_MAX_KI, getString(R.string.unavailable_ki));
        String affiliation = getExtraOrDefault(EXTRA_AFFILIATION, getString(R.string.unknown_affiliation));
        String description = getExtraOrDefault(EXTRA_DESCRIPTION, getString(R.string.unavailable_description));
        String image = getIntent().getStringExtra(EXTRA_IMAGE);

        textViewBack.setOnClickListener(view -> finish());
        textViewDetailName.setText(name);
        textViewDetailSubtitle.setText(getString(R.string.character_subtitle_format, race, gender));
        textViewDetailKi.setText(getString(R.string.ki_format, ki));
        textViewDetailMaxKi.setText(getString(R.string.max_ki_format, maxKi));
        textViewDetailAffiliation.setText(getString(R.string.affiliation_format, affiliation));
        textViewDetailDescription.setText(description);
        imageViewDetailCharacter.setContentDescription(
                getString(R.string.character_image_content_description, name)
        );

        Glide.with(this)
                .load(image)
                .placeholder(R.drawable.ic_character_placeholder)
                .error(R.drawable.ic_character_error)
                .fitCenter()
                .into(imageViewDetailCharacter);
    }

    private String getExtraOrDefault(String key, String defaultValue) {
        String value = getIntent().getStringExtra(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
