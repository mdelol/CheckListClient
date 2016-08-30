package com.mde.checklistclient;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ListView;

import com.mde.checklistclient.net.RestAdapter;
import com.mde.checklistclient.net.models.Task;

import java.util.List;

public class ListActivity extends AppCompatActivity {
    private Context context;
    private RecyclerView mMainListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        context = getApplicationContext();
        mMainListView = (RecyclerView) findViewById(R.id.mainListView);
        mMainListView.setLayoutManager(new LinearLayoutManager(context));
        new UserLoginTask().execute();
    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        private List<Task> mTasks;

        @Override
        protected Boolean doInBackground(Void... voids) {
            mTasks = RestAdapter.getInstance().getTasks();
            return mTasks != null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if (aBoolean) {
                mMainListView.setAdapter(new ElementAdapter(context, mTasks));
            }
        }
    }
}
