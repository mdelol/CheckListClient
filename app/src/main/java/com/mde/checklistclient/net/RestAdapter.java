package com.mde.checklistclient.net;

import com.mde.checklistclient.net.models.Task;
import com.mde.checklistclient.net.models.UserInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestAdapter {
    private String authentificationCookie;
    private static RestAdapter instance;
    private IServerConnection connection;

    private RestAdapter() {
        Interceptor COOKIES_REQUEST_INTERCEPTOR = new Interceptor() {

            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                if (authentificationCookie != null) {
                    Request request = chain.request();
                    request = request.newBuilder()
                            .addHeader("Cookie", authentificationCookie)
                            .build();
                    return chain.proceed(request);
                }

                return chain.proceed(chain.request());
            }
        };
        OkHttpClient defaultHttpClient = new OkHttpClient.Builder().addInterceptor(COOKIES_REQUEST_INTERCEPTOR).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.0.92:5000")
                .addConverterFactory(GsonConverterFactory.create())
                .client(defaultHttpClient)
                .build();

        connection = retrofit.create(IServerConnection.class);
    }

    private static class StubRestAdapter extends RestAdapter {
        @Override
        public boolean login(String userName, String password) {
            return true;
        }

        @Override
        public List<Task> getTasks() {
            List<Task> tasks = new ArrayList<>();
            Task e = new Task();
            e.setDescription("testDescription1");
            e.setCompleted(true);
            tasks.add(e);

            Task e1 = new Task();
            e1.setDescription("testDescription2");
            e1.setCompleted(false);
            tasks.add(e1);

            Task e2 = new Task();
            e2.setDescription("testDescription2");
            e2.setCompleted(false);
            tasks.add(e2);

            return tasks;
        }
    }
    public static RestAdapter getInstance() {
        /*if (instance == null) {
            instance = new RestAdapter();
        }
        return instance;*/

        return new StubRestAdapter();
    }

    public boolean login(String userName, String password) {
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(password);
        userInfo.setName(userName);

        Call<Boolean> login = connection.login(userInfo);

        try {
            Response<Boolean> execute = login.execute();
            if (execute.body()) {
                setCookieString(execute);
                return true;
            }
            return false;

        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    public List<Task> getTasks() {
        try {
            return connection.getTasks().execute().body();
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }

    private void setCookieString(Response response) {
        authentificationCookie = response.headers().get("Set-Cookie");
    }

}
