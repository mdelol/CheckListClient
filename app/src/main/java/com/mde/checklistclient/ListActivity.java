package com.mde.checklistclient;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.mde.checklistclient.net.RestAdapter;
import com.mde.checklistclient.net.models.Task;

import java.util.List;

public class ListActivity extends Activity {
    private Context context;
    private ListView mMainListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        context = getApplicationContext();
        mMainListView = (ListView) findViewById(R.id.mainListView);

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