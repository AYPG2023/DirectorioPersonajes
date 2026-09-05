package com.ap.directoriopersonajes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.directoriopersonajes.R;
import com.ap.directoriopersonajes.dto.CharacterDto;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    private final List<CharacterDto> characters = new ArrayList<>();
    private final OnCharacterClickListener onCharacterClickListener;

    public CharacterAdapter(OnCharacterClickListener onCharacterClickListener) {
        this.onCharacterClickListener = onCharacterClickListener;
    }

    @NonNull
    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_character, parent, false);
        return new CharacterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacterViewHolder holder, int position) {
        CharacterDto character = characters.get(position);
        holder.bind(character, onCharacterClickListener);
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }

    public void setCharacters(List<CharacterDto> newCharacters) {
        characters.clear();
        if (newCharacters != null) {
            characters.addAll(newCharacters);
        }
        notifyDataSetChanged();
    }

    public void addCharacters(List<CharacterDto> newCharacters) {
        if (newCharacters != null) {
            characters.addAll(newCharacters);
            notifyDataSetChanged();
        }
    }

    public interface OnCharacterClickListener {
        void onCharacterClick(CharacterDto character);
    }

    public static class CharacterViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageViewCharacter;
        private final TextView textViewCharacterName;
        private final TextView textViewCharacterSubtitle;
        private final TextView textViewCharacterKi;

        public CharacterViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCharacter = itemView.findViewById(R.id.imageViewCharacter);
            textViewCharacterName = itemView.findViewById(R.id.textViewCharacterName);
            textViewCharacterSubtitle = itemView.findViewById(R.id.textViewCharacterSubtitle);
            textViewCharacterKi = itemView.findViewById(R.id.textViewCharacterKi);
        }

        public void bind(CharacterDto character, OnCharacterClickListener onCharacterClickListener) {
            String name = getValueOrDefault(
                    character != null ? character.getName() : null,
                    itemView.getContext().getString(R.string.unknown_character)
            );
            String race = getValueOrDefault(
                    character != null ? character.getRace() : null,
                    itemView.getContext().getString(R.string.unknown_race)
            );
            String gender = getValueOrDefault(
                    character != null ? character.getGender() : null,
                    itemView.getContext().getString(R.string.unknown_gender)
            );
            String ki = getValueOrDefault(
                    character != null ? character.getKi() : null,
                    itemView.getContext().getString(R.string.unavailable_ki)
            );

            textViewCharacterName.setText(name);
            textViewCharacterSubtitle.setText(
                    itemView.getContext().getString(R.string.character_subtitle_format, race, gender)
            );
            textViewCharacterKi.setText(itemView.getContext().getString(R.string.ki_format, ki));
            imageViewCharacter.setContentDescription(
                    itemView.getContext().getString(R.string.character_image_content_description, name)
            );

            Glide.with(itemView)
                    .load(character != null ? character.getImage() : null)
                    .placeholder(R.drawable.ic_character_placeholder)
                    .error(R.drawable.ic_character_error)
                    .fitCenter()
                    .into(imageViewCharacter);

            itemView.setOnClickListener(view -> {
                if (onCharacterClickListener != null && character != null) {
                    onCharacterClickListener.onCharacterClick(character);
                }
            });
        }

        private String getValueOrDefault(String value, String defaultValue) {
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return value;
        }
    }
}
