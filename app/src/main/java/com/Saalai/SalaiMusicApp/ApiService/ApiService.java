package com.Saalai.SalaiMusicApp.ApiService;

import com.Saalai.SalaiMusicApp.Models.NavigationResponse;
import com.Saalai.SalaiMusicApp.Response.CatchUpChannelDetailsResponse;
import com.Saalai.SalaiMusicApp.Response.CatchUpResponse;
import com.Saalai.SalaiMusicApp.Response.ContactUsResponse;
import com.Saalai.SalaiMusicApp.Response.DashboardResponse;
import com.Saalai.SalaiMusicApp.Response.EnquiryResponse;
import com.Saalai.SalaiMusicApp.Response.LiveTvResponse;


import com.Saalai.SalaiMusicApp.Response.OtpResponse;
import com.Saalai.SalaiMusicApp.Response.RadioResponse;
import com.Saalai.SalaiMusicApp.Response.RegisterResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    @FormUrlEncoded
    @POST("checkRegister")
    Call<RegisterResponse> checkRegister(
            @Field("grant_type") String grantType,
            @Field("client_id") String clientId,
            @Field("userCountry") String userCountry,
            @Field("userMobile") String userMobile,
            @Field("deviceID") String deviceID,
            @Field("mobileType") String mobileType,
            @Field("device_token") String deviceToken,
            @Field("name") String name,
            @Field("referalCode") String referalCode
    );

    @FormUrlEncoded
    @POST("secure/numberVerification")
    Call<OtpResponse> verifyOtp(
            @Header("Authorization") String authToken,
            @Field("verificationCode") String verificationCode
    );

    @POST("secure/getDashboardList")
    Call<DashboardResponse> getDashboardList(
            @Header("Authorization") String authToken
    );

    @FormUrlEncoded
    @POST("secure/getLiveTvList")
    Call<LiveTvResponse> getLiveTvList(
            @Header("Authorization") String authToken,
            @Field("offset") String offset,
            @Field("count") String count
    );

    @FormUrlEncoded
    @POST("secure/getRadioList")
    Call<RadioResponse> getRadioList(
            @Header("Authorization") String authToken,
            @Field("channelId") String channelId,
            @Field("offset") String offset,
            @Field("count") String count
    );

    @POST("secure/getMovieDashboadList")
    Call<ResponseBody> getMovieDashboard
            (@Header("Authorization") String token
            );


    @FormUrlEncoded
    @POST("secure/getMovieDetails")
    Call<ResponseBody> getMovieDetails(
            @Header("Authorization") String token,
            @Field("channelId") String movieId
    );

    @POST("secure/getTvShowDashboadList")
    Call<ResponseBody> getTvShowDashboard(
            @Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("secure/getCategoryMovieList")
    Call<ResponseBody> getMovieList(
            @Header("Authorization") String token,
            @Field("categoryId") int categoryId,
            @Field("offset") int offset,
            @Field("count") int count
    );

    @FormUrlEncoded
    @POST("secure/getTvShowEpisodeList")
    Call<ResponseBody> getTvShowEpisodeList(
            @Header("Authorization") String token,
            @Field("channelId") String channelId,
            @Field("episodeId") String episodeId,
            @Field("offset") String offset,
            @Field("count") String count
    );


    @FormUrlEncoded
    @POST("secure/getTvShowList")
    Call<ResponseBody> getTvShowList(
            @Header("Authorization") String token,
            @Field("categoryId") int categoryId,
            @Field("offset") int offset,
            @Field("count") int count
    );


    @FormUrlEncoded
    @POST("secure/getCatchupChannelList")
    Call<CatchUpResponse> getCatchUpChannelList(
            @Header("Authorization") String authToken,
            @Field("offset") String offset,
            @Field("count") String count
    );

    @FormUrlEncoded
    @POST("secure/getCatchupChannelDetails")
    Call<CatchUpChannelDetailsResponse> getCatchUpChannelDetails(
            @Header("Authorization") String authToken,
            @Field("channelId") int channelId
    );


    @FormUrlEncoded
    @POST("secure/getLatestMovieList")
    Call<ResponseBody> getLatestMovieList(
            @Header("Authorization") String token,
            @Field("offset") int offset,
            @Field("count") int count
    );

    @FormUrlEncoded
    @POST("secure/getLatestTvShowList")
    Call<ResponseBody> getLatestTvShowList(
            @Header("Authorization") String token,
            @Field("offset") int offset,
            @Field("count") int count
    );


    @POST("secure/logout")
    Call<ResponseBody> logout(
            @Header("Authorization") String authToken
    );

    @FormUrlEncoded
    @POST("secure/updateStreamTime")
    Call<ResponseBody> updateStreamTime(
            @Header("Authorization") String authToken,
            @Field("channelId") String channelId,
            @Field("type") String type,
            @Field("time") String time
    );

    @POST("countryList")
    Call<ResponseBody> getCountryList();

    @POST("help")
    Call<ResponseBody> getHelp();

    @POST("getTermsAndConditions") Call<ResponseBody> getTermsAndConditions();

    // Add to ApiService.java
    @POST("secure/appMenuList")
    Call<NavigationResponse> getNavigationMenu(
            @Header("Authorization") String authToken);


    // Add this method to your ApiService interface:
    @FormUrlEncoded
    @POST("secure/reSendVerificationCode")
    Call<OtpResponse> resendVerificationCode(
            @Header("Authorization") String authToken);

    // In your ApiService interface
    @POST("contactUs")
    Call<ContactUsResponse> getContactUs();

    // In ApiService.java, add this method:
    @POST("secure/sendEnquiry")
    Call<EnquiryResponse> sendEnquiry(
            @Header("Authorization") String authToken);
}
