package uz.ownsms.sender.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** The developer-facing API (`/api/v1/messages`, `/api/v1/device`), authenticated with the account's api_key. */
interface DevApi {
    @POST("api/v1/messages")
    suspend fun sendMessage(@Body body: SendMessageReq): SentMessage

    @GET("api/v1/messages")
    suspend fun listMessages(
        @Query("status") status: String? = null,
        @Query("before") before: Long? = null,
        @Query("limit") limit: Int = 50,
    ): MessagesPage

    @GET("api/v1/device")
    suspend fun deviceStatus(): DeviceStatus

    // --- campaigns ---
    @POST("api/v1/campaigns")
    suspend fun createCampaign(@Body body: CreateCampaignReq): CampaignCreated

    @GET("api/v1/campaigns/{id}")
    suspend fun campaign(@Path("id") id: String): CampaignDetail

    /** action ∈ pause | resume | cancel */
    @POST("api/v1/campaigns/{id}/{action}")
    suspend fun campaignAction(@Path("id") id: String, @Path("action") action: String): CampaignDetail

    // --- account management ---
    @GET("api/v1/keys")
    suspend fun keys(): KeysList

    @POST("api/v1/keys")
    suspend fun createKey(@Body body: CreateKeyReq): ApiKeyInfo

    @POST("api/v1/keys/{id}/revoke")
    suspend fun revokeKey(@Path("id") id: Long): ApiKeyInfo

    @GET("api/v1/webhook")
    suspend fun webhook(): WebhookConfig

    @PUT("api/v1/webhook")
    suspend fun putWebhook(@Body body: WebhookPut): WebhookConfig

    @GET("api/v1/devices")
    suspend fun devices(): DevicesList

    /** action ∈ activate | deactivate */
    @POST("api/v1/devices/{id}/{action}")
    suspend fun deviceAction(@Path("id") id: Long, @Path("action") action: String): DeviceInfo

    @GET("api/v1/audit")
    suspend fun audit(): AuditList
}
