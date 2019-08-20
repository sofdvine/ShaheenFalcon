package com.sid.shaheenfalcon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tonyodev.fetch2.Download;

import java.io.File;
import java.util.ArrayList;

public class DownloadItemAdapter extends RecyclerView.Adapter<DownloadItemAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Download> downloads;

    public DownloadItemAdapter(Context context, ArrayList<Download> downloads) {
        this.context = context;
        this.downloads = downloads;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_manager_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvFileName.setText(new File(downloads.get(position).getFile()).getName());
        String text = Gonona.textBytes(downloads.get(position).getDownloaded()) + "/" + Gonona.textBytes(downloads.get(position).getTotal()) + "";
        if(downloads.get(position).getDownloaded() < downloads.get(position).getTotal()){
            text += " @" + Gonona.textBytes(downloads.get(position).getDownloadedBytesPerSecond()) + "/sec " + Gonona.textMiliseconds(downloads.get(position).getEtaInMilliSeconds());
        }
        holder.tvDownloadProgress.setText(text);
        holder.pbDownloadProgress.setProgress(downloads.get(position).getProgress());
    }

    @Override
    public int getItemCount() {
        return this.downloads.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView tvFileName, tvDownloadProgress;
        public ProgressBar pbDownloadProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvFileName = (TextView) itemView.findViewById(R.id.download_manager_list_item_filename);
            tvDownloadProgress = (TextView) itemView.findViewById(R.id.download_manager_list_item_progresstext);
            pbDownloadProgress = (ProgressBar) itemView.findViewById(R.id.download_manager_list_item_progressbar);

        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
