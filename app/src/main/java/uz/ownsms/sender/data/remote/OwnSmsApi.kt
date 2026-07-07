package uz.ownsms.sender.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface OwnSmsApi {

    @POST("api/v1/device/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    /** Long-poll: server holds ~30s. Returns 200 with jobs, or 204 (empty) when nothing is queued. */
    @GET("api/v1/device/poll")
    suspend fun poll(): Response<PollResponse>

    @POST("api/v1/device/jobs/{id}/status")
    suspend fun reportStatus(@Path("id") id: Long, @Body body: StatusReport): StatusResponse

    @POST("api/v1/device/heartbeat")
    suspend fun heartbeat(): HeartbeatResponse

    @GET("api/v1/device/config")
    suspend fun config(): DeviceConfig

    @POST("api/v1/register")
    suspend fun signup(@Body body: SignupRequest): Provisioned

    @POST("api/v1/register/pair")
    suspend fun pair(@Body body: PairRequest): Provisioned

    @POST("api/v1/devices/pairing-code")
    suspend fun pairingCode(): PairingCodeResult
}
