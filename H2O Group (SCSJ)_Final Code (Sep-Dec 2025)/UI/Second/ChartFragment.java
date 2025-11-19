package com.name.ccf.UI.Second;

// (All your original imports remain unchanged)
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast; // ⬅️ (Add this import)

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.Data.Repository.FeedbackRepository;
import com.name.ccf.R;
import com.name.ccf.UI.Main.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors; // ⬅️ (Add this import)
import java.util.stream.Collectors;

public class ChartFragment extends Fragment {

    private Spinner spinnerFilter;
    private ImageButton btnRefresh;
    private LinearLayout chartCategoryContainer, chartTagContainer;
    private TextView textNoCategoryData, textNoTagData;
    private LinearLayout categorySection, tagSection;
    private LinearLayout dropdownArea;

    private String currentFilter;
    private AppDatabase db;
    private String userType = "guest";

    private final String[] categories = {
            "Rice / Noodles", "Western", "Malay", "Chinese", "Indian / Mamak",
            "Snacks / Bakery", "Desserts", "Beverages", "Vegetarian", "Others"
    };
    private final String[] tags = {"Healthy", "Spicy", "Oily", "Salty", "Sweet", "Crispy"};

    public ChartFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerFilter = view.findViewById(R.id.spinner_filter);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        categorySection = view.findViewById(R.id.category_chart_section);
        tagSection = view.findViewById(R.id.tag_chart_section);
        chartCategoryContainer = view.findViewById(R.id.chart_category_container);
        chartTagContainer = view.findViewById(R.id.chart_tag_container);
        textNoCategoryData = view.findViewById(R.id.text_no_category_data);
        textNoTagData = view.findViewById(R.id.text_no_tag_data);
        dropdownArea = view.findViewById(R.id.dropdown_area);

        db = AppDatabase.getInstance(requireContext());
        currentFilter = getString(R.string.chart_filter_all);

        loadUserDataFromActivity();
        setupFilterSpinner(view);

        // --- GEMINI FIX: Refresh button logic is now changed ---
        setupRefreshButton();
        // --- End of GEMINI FIX ---

        // --- GEMINI FIX: Initial load logic is now changed ---
        loadChartsFromLocalDb(); // Renamed for clarity
        // --- End of GEMINI FIX ---
    }

    // (Your loadUserDataFromActivity method remains unchanged)
    private void loadUserDataFromActivity() {
        if (getActivity() instanceof SecondaryActivity) {
            SecondaryActivity activity = (SecondaryActivity) getActivity();
            userType = activity.getUserType();
            if (userType == null || userType.isEmpty()) userType = "guest";
        } else {
            userType = "guest";
        }
    }

    // (Your setupFilterSpinner method remains unchanged)
    private void setupFilterSpinner(View view) {
        String[] filters = {
                getString(R.string.chart_filter_all),
                getString(R.string.chart_filter_category),
                getString(R.string.chart_filter_tag)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_selected,
                filters
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                currentFilter = filters[position];
                // --- GEMINI FIX: Must call updateCharts, not loadCharts ---
                // We re-process the existing data, not reload it
                // loadCharts(); // <-- Old incorrect line

                // We need to re-read from the DB to apply the new filter logic
                loadChartsFromLocalDb();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        dropdownArea.setOnClickListener(v -> spinnerFilter.performClick());
    }

    // --- GEMINI FIX: Refresh button logic is REPLACED ---
    /**
     * Sets up the refresh button.
     * Clicking it will now trigger a full sync from Firebase.
     */
    private void setupRefreshButton() {
        btnRefresh.setOnClickListener(v -> refreshDataFromFirebase());
    }

    /**
     * [NEW] This is the *correct* refresh logic.
     * It fetches ALL data from Firebase, deletes the local cache,
     * inserts the new data, and then re-draws the charts.
     */
    private void refreshDataFromFirebase() {
        // Prevent action if a guest somehow clicks it
        if (userType.equals("guest") || getContext() == null) {
            return;
        }

        // Disable button and show toast
        btnRefresh.setEnabled(false);
        // (Using hardcoded string to avoid R.string.toast_refreshing compilation error)
        Toast.makeText(requireContext(), "Refreshing...", Toast.LENGTH_SHORT).show();

        FeedbackRepository repo = new FeedbackRepository();

        // 1. Fetch all feedback (async)
        repo.fetchAllFeedback(firebaseList -> {
            // 2. We are now on the Main Thread (callback).
            //    We must use a new background thread for DB I/O.
            Executors.newSingleThreadExecutor().execute(() -> {
                if (db == null) db = AppDatabase.getInstance(requireContext());

                // 3. Delete all old data
                db.feedbackDao().deleteAll();

                // 4. Insert all new data
                db.feedbackDao().insertAll(firebaseList);

                // 5. Go back to the Main Thread to update UI
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateCharts(firebaseList); // Redraw UI with fresh data
                        btnRefresh.setEnabled(true); // Re-enable button
                        if (getContext() != null) {
                            // (Using hardcoded string to avoid R.string.toast_refresh_complete compilation error)
                            Toast.makeText(getContext(), "Refresh complete", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Just in case fragment is detached
                    btnRefresh.setEnabled(true);
                }
            });
        });
    }
    // --- End of GEMINI FIX ---

    // --- GEMINI FIX: Initial load logic is REPLACED ---
    /**
     * [CHANGED] This method now ONLY loads feedback from the local Room database.
     * It trusts that SecondaryActivity has already synced data from Firebase.
     */
    private void loadChartsFromLocalDb() {
        if (userType.equals("guest")) {
            // (Original Guest Logic)
            dropdownArea.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.GONE);
            categorySection.setVisibility(View.VISIBLE);
            tagSection.setVisibility(View.GONE);
            chartCategoryContainer.removeAllViews();
            textNoCategoryData.setText(R.string.toast_guest_charts);
            textNoCategoryData.setVisibility(View.VISIBLE);
            return;
        }

        // --- GEMINI FIX: Only load from local Room DB ---
        // It trusts that SecondaryActivity has already synced the data.
        // (All Firebase logic has been removed from here).
        new Thread(() -> {
            if (db == null) db = AppDatabase.getInstance(requireContext());
            List<Feedback> localData = db.feedbackDao().getAllFeedback();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateCharts(localData));
            }
        }).start();
        // --- End of GEMINI FIX ---
    }
    // --- End of GEMINI FIX ---


    // ==================== UI 更新 (No Change) ==================== //
    // (All your chart generation methods: updateCharts, showAllCharts,
    // showCategoryCharts, showTagCharts, calculateAverage,
    // getTop5ByItem, createBarChart... remain unchanged)

    private void updateCharts(List<Feedback> feedbackList) {
        if (feedbackList == null || feedbackList.isEmpty()) {
            chartCategoryContainer.removeAllViews();
            chartTagContainer.removeAllViews();
            textNoCategoryData.setVisibility(View.VISIBLE);
            textNoTagData.setVisibility(View.VISIBLE);
            categorySection.setVisibility(View.VISIBLE);
            tagSection.setVisibility(View.VISIBLE);
            return;
        }

        if (currentFilter.equals(getString(R.string.chart_filter_all))) {
            showAllCharts(feedbackList);
        } else if (currentFilter.equals(getString(R.string.chart_filter_category))) {
            showCategoryCharts(feedbackList);
        } else if (currentFilter.equals(getString(R.string.chart_filter_tag))) {
            showTagCharts(feedbackList);
        }
    }

    private void showAllCharts(List<Feedback> feedbackList) {
        Map<String, Float> avgCategory = calculateAverage(feedbackList, true);
        Map<String, Float> avgTag = calculateAverage(feedbackList, false);

        chartCategoryContainer.removeAllViews();
        chartTagContainer.removeAllViews();
        textNoCategoryData.setVisibility(View.GONE);
        textNoTagData.setVisibility(View.GONE);

        if (!avgCategory.isEmpty()) {
            chartCategoryContainer.addView(createBarChart(avgCategory, getString(R.string.chart_title_avg_category)));
        } else textNoCategoryData.setVisibility(View.VISIBLE);

        if (!avgTag.isEmpty()) {
            chartTagContainer.addView(createBarChart(avgTag, getString(R.string.chart_title_avg_tag)));
        } else textNoTagData.setVisibility(View.VISIBLE);

        categorySection.setVisibility(View.VISIBLE);
        tagSection.setVisibility(View.VISIBLE);
    }

    private void showCategoryCharts(List<Feedback> feedbackList) {
        Map<String, List<Feedback>> byCategory = feedbackList.stream()
                .filter(f -> f.category != null)
                .collect(Collectors.groupingBy(f -> f.category));
        Map<String, Map<String, Float>> chartDataMap = new HashMap<>();
        for (String c : categories) {
            List<Feedback> list = byCategory.getOrDefault(c, new ArrayList<>());
            Map<String, Float> top5 = getTop5ByItem(list);
            if (!top5.isEmpty()) chartDataMap.put(c, top5);
        }
        chartCategoryContainer.removeAllViews();
        textNoCategoryData.setVisibility(View.GONE);
        if (chartDataMap.isEmpty()) textNoCategoryData.setVisibility(View.VISIBLE);
        else chartDataMap.forEach((name, data) ->
                chartCategoryContainer.addView(createBarChart(data, getString(R.string.chart_title_top5, name)))
        );
        categorySection.setVisibility(View.VISIBLE);
        tagSection.setVisibility(View.GONE);
    }

    private void showTagCharts(List<Feedback> feedbackList) {
        Map<String, List<Feedback>> byTag = feedbackList.stream()
                .filter(f -> f.tag != null)
                .collect(Collectors.groupingBy(f -> f.tag));
        Map<String, Map<String, Float>> chartDataMap = new HashMap<>();
        for (String t : tags) {
            List<Feedback> list = byTag.getOrDefault(t, new ArrayList<>());
            Map<String, Float> top5 = getTop5ByItem(list);
            if (!top5.isEmpty()) chartDataMap.put(t, top5);
        }
        chartTagContainer.removeAllViews();
        textNoTagData.setVisibility(View.GONE);
        if (chartDataMap.isEmpty()) textNoTagData.setVisibility(View.VISIBLE);
        else chartDataMap.forEach((name, data) ->
                chartTagContainer.addView(createBarChart(data, getString(R.string.chart_title_top5, name)))
        );
        tagSection.setVisibility(View.VISIBLE);
        categorySection.setVisibility(View.GONE);
    }

    private Map<String, Float> calculateAverage(List<Feedback> list, boolean byCategory) {
        Map<String, List<Float>> groupMap = new HashMap<>();
        for (Feedback f : list) {
            String key = byCategory ? f.category : f.tag;
            if (key == null || key.isEmpty()) continue;
            groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(f.rating);
        }
        Map<String, Float> result = new HashMap<>();
        for (Map.Entry<String, List<Float>> e : groupMap.entrySet()) {
            float avg = (float) e.getValue().stream().mapToDouble(Float::doubleValue).average().orElse(0);
            result.put(e.getKey(), avg);
        }
        return result;
    }

    private Map<String, Float> getTop5ByItem(List<Feedback> list) {
        Map<String, List<Float>> group = new HashMap<>();
        for (Feedback f : list) {
            if (f.dishname == null) continue;
            group.computeIfAbsent(f.dishname, k -> new ArrayList<>()).add(f.rating);
        }
        Map<String, Float> avg = new HashMap<>();
        for (Map.Entry<String, List<Float>> e : group.entrySet()) {
            float avgScore = (float) e.getValue().stream().mapToDouble(Float::doubleValue).average().orElse(0);
            avg.put(e.getKey(), avgScore);
        }
        return avg.entrySet().stream()
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private BarChart createBarChart(Map<String, Float> data, String title) {
        BarChart chart = new BarChart(requireContext());
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Float> e : data.entrySet()) {
            entries.add(new BarEntry(i++, e.getValue()));
            labels.add(e.getKey().replace(" / ", "/\n"));
        }
        BarDataSet dataSet = new BarDataSet(entries, title);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chart.setData(barData);
        chart.getDescription().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(0f);
        xAxis.setLabelCount(labels.size());
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        chart.getAxisLeft().setGranularity(1f);
        chart.getAxisLeft().setTextSize(12f);
        chart.getAxisRight().setEnabled(false);
        chart.setFitBars(true);
        chart.animateY(800);
        chart.setExtraBottomOffset(30f);
        Legend legend = chart.getLegend();
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.setMinimumHeight(500);
        chart.setPadding(0, 0, 0, 0);
        int fixedHeightInPixels = 600;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                fixedHeightInPixels // Use the fixed height (使用这个固定高度)
        );
        params.setMargins(0, 0, 0, 48); // Add bottom margin
        chart.setLayoutParams(params);
        return chart;
    }
}