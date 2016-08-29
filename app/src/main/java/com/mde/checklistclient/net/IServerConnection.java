package com.mde.checklistclient.net;

import com.mde.checklistclient.net.models.Task;
import com.mde.checklistclient.net.models.UserInfo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface IServerConnection {
    @POST("/api/auth")
    Call<Boolean> login(@Body UserInfo userInfo);

    @GET("/api/values")
    Call<List<Task>> getTasks();
}
