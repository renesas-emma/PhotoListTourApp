package cx.fam.liuxu.photolist;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.GridView;

import org.json.JSONException;
import org.json.JSONObject;

public class DirectoryList2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_list2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Intent intent = getIntent();
        try {
            JSONObject content = new JSONObject(intent.getStringExtra("content"));
            String path="Server:" + content.getString("path");
            //item.setText(path);
            String size = content.getString("size");
            String from = content.getString("from");
            toolbar.setTitle(path + "from:to" + from + size);
            GridView view = (GridView)findViewById(R.id.grid);
            PhotoListAdapter adapter = new PhotoListAdapter(this, 0, view, content);
            view.setAdapter(adapter);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
