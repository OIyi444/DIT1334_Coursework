package com.name.ccf.UI.Second;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.name.ccf.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChartFragment extends Fragment {

    private Spinner spinnerFilter;
    private ImageButton btnRefresh;

    private LinearLayout chartCategoryContainer, chartTagContainer;
    private TextView textNoCategoryData, textNoTagData;

    private LinearLayout categorySection, tagSection;

    private String currentFilter = "All";
    private AppDatabase db;

    private final String[] categories = {"Rice", "Noodle", "Roti", "Dessert", "Snack"};
    private final String[] tags = {"Healthy", "Spicy", "Oil", "Salty", "Sweet", "Crispy"};

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

        db = AppDatabase.getInstance(requireContext());

        setupFilterSpinner(view);
        setupRefreshButton();

        loadCharts();
    }

    private void setupFilterSpinner(View view) {
        String[] filters = {"All", "Category", "Tag"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                filters
        );
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                currentFilter = filters[position];
                loadCharts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        LinearLayout dropdownArea = view.findViewById(R.id.dropdown_area);
        dropdownArea.setOnClickListener(v -> spinnerFilter.performClick());
    }

    private void setupRefreshButton() {
        btnRefresh.setOnClickListener(v -> loadCharts());
    }

    // ==================== 主加载逻辑 ==================== //

    private void loadCharts() {
        new Thread(() -> {
            List<Feedback> feedbackList = db.feedbackDao().getAllFeedback();

            requireActivity().runOnUiThread(() -> {
                chartCategoryContainer.removeAllViews();
                chartTagContainer.removeAllViews();
                textNoCategoryData.setVisibility(View.GONE);
                textNoTagData.setVisibility(View.GONE);
                categorySection.setVisibility(View.GONE);
                tagSection.setVisibility(View.GONE);
            });

            if (feedbackList == null || feedbackList.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    textNoCategoryData.setVisibility(View.VISIBLE);
                    textNoTagData.setVisibility(View.VISIBLE);
                    categorySection.setVisibility(View.VISIBLE);
                    tagSection.setVisibility(View.VISIBLE);
                });
                return;
            }

            switch (currentFilter) {
                case "All":
                    showAllCharts(feedbackList);
                    break;
                case "Category":
                    showCategoryCharts(feedbackList);
                    break;
                case "Tag":
                    showTagCharts(feedbackList);
                    break;
            }

        }).start();
    }

    // ==================== 图表显示逻辑 ==================== //

    private void showAllCharts(List<Feedback> feedbackList) {
        Map<String, Float> avgCategory = calculateAverage(feedbackList, true);
        Map<String, Float> avgTag = calculateAverage(feedbackList, false);

        requireActivity().runOnUiThread(() -> {
            chartCategoryContainer.removeAllViews();
            chartTagContainer.removeAllViews();

            if (!avgCategory.isEmpty()) {
                chartCategoryContainer.addView(createBarChart(avgCategory, "Category Average Rating"));
            } else {
                textNoCategoryData.setVisibility(View.VISIBLE);
            }

            if (!avgTag.isEmpty()) {
                chartTagContainer.addView(createBarChart(avgTag, "Tag Average Rating"));
            } else {
                textNoTagData.setVisibility(View.VISIBLE);
            }

            categorySection.setVisibility(View.VISIBLE);
            tagSection.setVisibility(View.VISIBLE);
        });
    }

    private void showCategoryCharts(List<Feedback> feedbackList) {
        Map<String, List<Feedback>> byCategory = feedbackList.stream()
                .filter(f -> f.category != null)
                .collect(Collectors.groupingBy(f -> f.category));

        requireActivity().runOnUiThread(() -> {
            chartCategoryContainer.removeAllViews();
            for (String c : categories) {
                List<Feedback> list = byCategory.getOrDefault(c, new ArrayList<>());
                Map<String, Float> top5 = getTop5ByItem(list);
                if (!top5.isEmpty()) {
                    chartCategoryContainer.addView(createBarChart(top5, c + " Top 5"));
                }
            }
            if (chartCategoryContainer.getChildCount() == 0)
                textNoCategoryData.setVisibility(View.VISIBLE);

            categorySection.setVisibility(View.VISIBLE);
            tagSection.setVisibility(View.GONE);
        });
    }

    private void showTagCharts(List<Feedback> feedbackList) {
        Map<String, List<Feedback>> byTag = feedbackList.stream()
                .filter(f -> f.tag != null)
                .collect(Collectors.groupingBy(f -> f.tag));

        requireActivity().runOnUiThread(() -> {
            chartTagContainer.removeAllViews();
            for (String t : tags) {
                List<Feedback> list = byTag.getOrDefault(t, new ArrayList<>());
                Map<String, Float> top5 = getTop5ByItem(list);
                if (!top5.isEmpty()) {
                    chartTagContainer.addView(createBarChart(top5, t + " Top 5"));
                }
            }
            if (chartTagContainer.getChildCount() == 0)
                textNoTagData.setVisibility(View.VISIBLE);

            tagSection.setVisibility(View.VISIBLE);
            categorySection.setVisibility(View.GONE);
        });
    }

    // ==================== 工具函数 ==================== //

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
            labels.add(e.getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, title);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chart.setData(barData);
        chart.getDescription().setEnabled(false);

        // ✅ X轴标签水平显示 + 留出间距
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(0f); // ✅ 不倾斜
        xAxis.setLabelCount(labels.size());
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setYOffset(12f);
        xAxis.setAvoidFirstLastClipping(true); // ✅ 避免文字挤在一起

        chart.getAxisLeft().setGranularity(1f);
        chart.getAxisLeft().setTextSize(12f);
        chart.getAxisRight().setEnabled(false);
        chart.setFitBars(true);
        chart.animateY(800);
        chart.setExtraBottomOffset(18f);

        Legend legend = chart.getLegend();
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        chart.setMinimumHeight(500);
        chart.setPadding(0, 0, 0, 16);
        return chart;
    }
}
