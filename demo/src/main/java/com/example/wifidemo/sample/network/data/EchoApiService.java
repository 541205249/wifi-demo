package com.example.wifidemo.sample.network.data;

import com.example.wifidemo.sample.network.model.EchoJsonRequest;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface EchoApiService {
    @GET("get")
    Call<ResponseBody> requestGetExample(
            @Query("module") String module,
            @Query("deviceId") String deviceId,
            @Query("requestAt") String requestAt
    );

    @POST("post")
    Call<ResponseBody> requestPostJsonExample(@Body EchoJsonRequest request);

    @FormUrlEncoded
    @POST("post")
    Call<ResponseBody> requestPostFormExample(
            @Field("patientId") String patientId,
            @Field("examMode") String examMode,
            @Field("operator") String operator,
            @Field("note") String note
    );

    @Multipart
    @POST("post")
    Call<ResponseBody> requestUploadExample(
            @Part("description") RequestBody description,
            @Part("deviceId") RequestBody deviceId,
            @Part MultipartBody.Part filePart
    );
}
