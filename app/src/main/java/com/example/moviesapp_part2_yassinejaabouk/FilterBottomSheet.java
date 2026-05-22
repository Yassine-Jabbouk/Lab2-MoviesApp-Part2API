package com.example.moviesapp_part2_yassinejaabouk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFilterApplied(String sortBy, float minRating, int genreId);
    }

    private FilterListener listener;
    private String currentSort = "popularity.desc";
    private float currentMinRating = 0f;
    private int currentGenreId = -1;

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    public void setCurrentFilters(String sortBy, float minRating, int genreId) {
        this.currentSort = sortBy;
        this.currentMinRating = minRating;
        this.currentGenreId = genreId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false);

        ChipGroup chipGroupSort = view.findViewById(R.id.chipGroupSort);
        ChipGroup chipGroupGenre = view.findViewById(R.id.chipGroupGenre);
        SeekBar seekBarRating = view.findViewById(R.id.seekBarRating);
        TextView tvRatingValue = view.findViewById(R.id.tvRatingValue);
        MaterialButton btnApply = view.findViewById(R.id.btnApply);
        MaterialButton btnReset = view.findViewById(R.id.btnReset);

        // Set current sort
        switch (currentSort) {
            case "vote_average.desc": chipGroupSort.check(R.id.chipRating); break;
            case "release_date.desc": chipGroupSort.check(R.id.chipNewest); break;
            default: chipGroupSort.check(R.id.chipPopularity); break;
        }

        // Set current genre
        switch (currentGenreId) {
            case 28: chipGroupGenre.check(R.id.chipAction); break;
            case 35: chipGroupGenre.check(R.id.chipComedy); break;
            case 18: chipGroupGenre.check(R.id.chipDrama); break;
            case 27: chipGroupGenre.check(R.id.chipHorror); break;
            case 878: chipGroupGenre.check(R.id.chipSciFi); break;
            case 10749: chipGroupGenre.check(R.id.chipRomance); break;
            case 53: chipGroupGenre.check(R.id.chipThriller); break;
            case 16: chipGroupGenre.check(R.id.chipAnimation); break;
            case 99: chipGroupGenre.check(R.id.chipDocumentary); break;
            default: chipGroupGenre.check(R.id.chipGenreAll); break;
        }

        // Set current rating
        seekBarRating.setProgress((int) currentMinRating);
        tvRatingValue.setText(currentMinRating == 0 ? "Any" : (int) currentMinRating + "+");

        seekBarRating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRatingValue.setText(progress == 0 ? "Any" : progress + "+");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnApply.setOnClickListener(v -> {
            // Get sort
            int sortChecked = chipGroupSort.getCheckedChipId();
            String sortBy;
            if (sortChecked == R.id.chipRating) sortBy = "vote_average.desc";
            else if (sortChecked == R.id.chipNewest) sortBy = "release_date.desc";
            else sortBy = "popularity.desc";

            // Get genre
            int genreChecked = chipGroupGenre.getCheckedChipId();
            int genreId;
            if (genreChecked == R.id.chipAction) genreId = 28;
            else if (genreChecked == R.id.chipComedy) genreId = 35;
            else if (genreChecked == R.id.chipDrama) genreId = 18;
            else if (genreChecked == R.id.chipHorror) genreId = 27;
            else if (genreChecked == R.id.chipSciFi) genreId = 878;
            else if (genreChecked == R.id.chipRomance) genreId = 10749;
            else if (genreChecked == R.id.chipThriller) genreId = 53;
            else if (genreChecked == R.id.chipAnimation) genreId = 16;
            else if (genreChecked == R.id.chipDocumentary) genreId = 99;
            else genreId = -1;

            float minRating = seekBarRating.getProgress();
            if (listener != null) listener.onFilterApplied(sortBy, minRating, genreId);
            dismiss();
        });

        btnReset.setOnClickListener(v -> {
            chipGroupSort.check(R.id.chipPopularity);
            chipGroupGenre.check(R.id.chipGenreAll);
            seekBarRating.setProgress(0);
            tvRatingValue.setText("Any");
            if (listener != null) listener.onFilterApplied("popularity.desc", 0f, -1);
            dismiss();
        });

        return view;
    }
}