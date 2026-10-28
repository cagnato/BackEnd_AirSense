package br.pucpr.airsense.devices

import br.pucpr.airsense.environments.Room
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "tbl_devices")
class Device(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var serialNumber: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var firmwareVersion: String = "1.0.0",

    @Column(nullable = false)
    var active: Boolean = true,

    @OneToOne
    @JoinColumn(name = "room_id", unique = true)
    var room: Room? = null
)

data class DeviceRequest(
    @field:NotBlank(message = "O número de série (MAC/Serial) é obrigatório")
    val serialNumber: String,

    @field:NotBlank(message = "O nome do dispositivo é obrigatório")
    val name: String,

    val firmwareVersion: String = "1.0.0",
    val active: Boolean = true
)

data class DeviceResponse(
    val id: Long,
    val serialNumber: String,
    val name: String,
    val firmwareVersion: String,
    val active: Boolean,
    val boundRoomId: Long?,
    val boundRoomName: String?
) {
    constructor(device: Device) : this(
        id = device.id!!,
        serialNumber = device.serialNumber,
        name = device.name,
        firmwareVersion = device.firmwareVersion,
        active = device.active,
        boundRoomId = device.room?.id,
        boundRoomName = device.room?.name
    )
}