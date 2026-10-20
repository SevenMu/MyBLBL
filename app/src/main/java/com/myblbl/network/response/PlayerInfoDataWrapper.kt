package com.myblbl.network.response

import com.google.gson.annotations.SerializedName
import com.myblbl.model.dm.DmMaskInfo
import com.myblbl.model.interaction.InteractionInfo
import com.myblbl.model.player.ViewPoint
import com.myblbl.model.subtitle.SubtitleWrapper
import java.io.Serializable

data class PlayerInfoDataWrapper(
    @SerializedName("interaction")
    val interaction: InteractionInfo? = null,
    @SerializedName("subtitle")
    val subtitle: SubtitleWrapper? = null,
    @SerializedName("dm_mask")
    val dmMask: DmMaskInfo? = null,
    @SerializedName("view_points")
    val viewPoints: List<ViewPoint>? = null
) : Serializable
