package com.sid.shaheenfalcon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.exoplayer2.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class RequestListActivity extends AppCompatActivity {

    protected static ArrayList<SFRequest> requests;
    private String userAgentStr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_list);
        ListView requestListView = findViewById(R.id.request_list);
        requestListView.setAdapter(new RequestListAdapter(getApplicationContext(), requests));
        registerForContextMenu(requestListView);
        if (getIntent().hasExtra("USER_AGENT")) {
            userAgentStr = getIntent().getStringExtra("USER_AGENT");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.request_list) {
            menu.add(Menu.NONE, 1, Menu.NONE, "View / Edit Request");
            menu.add(Menu.NONE, 2, Menu.NONE, "Open with default player");
            menu.add(Menu.NONE, 3, Menu.NONE, "Copy Url");
            menu.add(Menu.NONE, 4, Menu.NONE, "Open With...");
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case 1:
                break;
            case 2:
                Log.d("REQ_LIST_URL", requests.get(info.position).getUrl());
                Intent intent = new Intent(RequestListActivity.this, PlayerActivity.class);
                intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, true);
                intent.putExtra(PlayerActivity.ABR_ALGORITHM_EXTRA, PlayerActivity.ABR_ALGORITHM_DEFAULT);
                intent.putExtra("PLAYER_USER_AGENT", userAgentStr);
                intent.putExtra("EXTRA_HEADERS", new HashMap<String, String>(requests.get(info.position).getHeaders()));
                intent.setData(Uri.parse(requests.get(info.position).getUrl()));
                startActivity(intent);
                break;
            case 3:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied URL", requests.get(info.position).getUrl());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Copied", Toast.LENGTH_LONG).show();
                break;
            case 4:
                Intent intentOpenWith = new Intent(Intent.ACTION_VIEW);
                intentOpenWith.setData(Uri.parse(requests.get(info.position).getUrl()));
                startActivity(intentOpenWith);
                break;
        }
        return super.onContextItemSelected(item);
    }
}
