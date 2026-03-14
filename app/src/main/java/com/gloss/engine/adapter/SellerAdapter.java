package com.glass.engine.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.glass.engine.R;
import com.glass.engine.model.Seller;

import java.util.List;

public class SellerAdapter extends RecyclerView.Adapter<SellerAdapter.VH> {

    private final List<Seller> sellers;

    public SellerAdapter(List<Seller> sellers) {
        this.sellers = sellers;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seller, parent, false));
    }

@Override
public void onBindViewHolder(@NonNull VH h, int p) {
    Seller s = sellers.get(p);

    h.name.setText(s.name);
    h.country.setText("Country: " + s.country);
    h.telegram.setText("Telegram: @" + s.telegram);
    h.website.setText("Website: " + s.website);

    Glide.with(h.itemView) // ✅ FIX
            .load(s.photo)
            .placeholder(R.drawable.ic_seller_avatar)
            .error(R.drawable.ic_seller_avatar)
            .into(h.photo);

    h.telegram.setOnClickListener(v ->
            open(v, "https://t.me/" + s.telegram));

    h.website.setOnClickListener(v ->
            open(v, s.website));
}


    @Override
    public int getItemCount() {
        return sellers.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView name, country, telegram, website;

        VH(View v) {
            super(v);
            photo = v.findViewById(R.id.seller_photo);
            name = v.findViewById(R.id.seller_name);
            country = v.findViewById(R.id.seller_country);
            telegram = v.findViewById(R.id.seller_telegram);
            website = v.findViewById(R.id.seller_website);
        }
    }

    private void open(View v, String url) {
        v.getContext().startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
