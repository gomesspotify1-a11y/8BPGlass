package com.glass.engine.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glass.engine.R;
import com.glass.engine.adapter.SellerAdapter;
import com.glass.engine.model.Seller;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SellersFragment extends Fragment {

    private static final String SELLER_JSON_URL =
            "https://keypanel.tech/sellers.json";

    private RecyclerView recyclerView;
    private SellerAdapter adapter;

    // 🔹 Original data
    private final List<Seller> allSellers = new ArrayList<>();
    // 🔹 Filtered data shown in RecyclerView
    private final List<Seller> sellerList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_sellers, container, false);

        // RecyclerView setup
        recyclerView = v.findViewById(R.id.seller_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        adapter = new SellerAdapter(sellerList);
        recyclerView.setAdapter(adapter);

        // Search
        EditText searchBox = v.findViewById(R.id.search_boxx);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSellers(s.toString(), null);
            }
        });

        // Filter buttons
        

        loadSellers();
        return v;
    }

    private void loadSellers() {
        new Thread(() -> {
            try {
                URL url = new URL(SELLER_JSON_URL);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);

                BufferedReader r = new BufferedReader(
                        new InputStreamReader(c.getInputStream())
                );

                StringBuilder json = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    json.append(line);
                }

                JSONArray sellers = new JSONObject(json.toString())
                        .getJSONArray("sellers");

                allSellers.clear();
                sellerList.clear();

                for (int i = 0; i < sellers.length(); i++) {
                    JSONObject s = sellers.getJSONObject(i);
                    if (!s.optBoolean("enabled", true)) continue;

                    Seller seller = new Seller(
                            s.getString("id"),
                            s.getString("photo"),
                            s.getString("name"),
                            s.getString("country"),
                            s.getString("telegram"),
                            s.getString("website")
                    );

                    allSellers.add(seller);
                    sellerList.add(seller);
                }

                if (isAdded()) {
                    requireActivity().runOnUiThread(adapter::notifyDataSetChanged);
                }

            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Failed to load sellers",
                                    Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void filterSellers(String query, String country) {
        sellerList.clear();

        for (Seller s : allSellers) {

            boolean matchesSearch =
                    query == null || query.isEmpty()
                            || s.name.toLowerCase().contains(query.toLowerCase())
                            || s.country.toLowerCase().contains(query.toLowerCase());

            boolean matchesCountry =
                    country == null || country.equals("all")
                            || s.country.equalsIgnoreCase(country);

            if (matchesSearch && matchesCountry) {
                sellerList.add(s);
            }
        }

        adapter.notifyDataSetChanged();
    }
}
