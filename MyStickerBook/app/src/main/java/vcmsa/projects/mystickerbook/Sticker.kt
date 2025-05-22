package vcmsa.projects.mystickerbook

import com.google.firebase.database.Exclude

data class Sticker(
    @get:Exclude
    var id: String="",
    val name: String="",
    val category: String="",
    val imageUrl: String="",
    val uploadedAt: Any?=null
) {
    @Exclude
    fun getUploadedAtLong(): Long {
        return (uploadedAt as? Long) ?: 0L
    }

}