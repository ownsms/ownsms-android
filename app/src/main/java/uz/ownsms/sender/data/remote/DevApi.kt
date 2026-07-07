package uz.ownsms.sender.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
}
