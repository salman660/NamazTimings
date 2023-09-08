package com.apptic.namaztimings.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apptic.namaztimings.Model.PrayerTiming;
import com.apptic.namaztimings.R;

import java.util.List;

public class PrayerTimingsAdapter extends RecyclerView.Adapter<PrayerTimingsAdapter.ViewHolder> {


    private OnItemClickListener itemClickListener;

    private List<PrayerTiming> prayerTimingsList;

    public PrayerTimingsAdapter(List<PrayerTiming> prayerTimingsList) {
        this.prayerTimingsList = prayerTimingsList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.prayer_list_item, parent, false);
        return new ViewHolder(view);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PrayerTiming prayerTiming = prayerTimingsList.get(position);
        holder.prayerNameTextView.setText(prayerTiming.getName());
        holder.startTimeTextView.setText(prayerTiming.getStartTime());
        holder.endTimeTextView.setText(prayerTiming.getEndTime());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(position);
                }
            }
        });
    }



    @Override
    public int getItemCount() {
        return prayerTimingsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView prayerNameTextView;
        TextView startTimeTextView;
        TextView endTimeTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            prayerNameTextView = itemView.findViewById(R.id.prayername);
            startTimeTextView = itemView.findViewById(R.id.prayertime_starting);
            endTimeTextView = itemView.findViewById(R.id.prayertime_ending);

            // Set click listener within the ViewHolder
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(getAdapterPosition());
                    }
                }
            });
        }
    }
}
