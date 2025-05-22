package vcmsa.projects.mystickerbook
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Adapter for the RecyclerView to display stickers
class StickerAdapter(private val stickers: MutableList<Sticker>) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            StickerAdapter.StickerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker, parent, false)
        // inflate the item layout
        return StickerViewHolder(view)
    }

    // binds the sticker data to the views in a ViewHolder when it's scrolled into view
    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val sticker = stickers[position]
        holder.bind(sticker) // call the bind function to set the sata for the item
    }

    override fun getItemCount(): Int = stickers.size
    fun setStickers(newStickers: List<Sticker>){
        stickers.clear()
        stickers.addAll(newStickers)
        notifyDataSetChanged()
    }
    class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val imageView: ImageView = itemView.findViewById(R.id.ImageViewStickerItem)
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewStickerNameItem)
        private val categoryTextView: TextView = itemView.findViewById(R.id.textViewStickerCategoryItem)
        fun bind(sticker: Sticker){
            nameTextView.text = sticker.name
            categoryTextView.text = "Category: ${sticker.category}"

            Glide.with(itemView.context)
                .load(sticker.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(com.google.android.material.R.drawable.mtrl_ic_error)
                .into(imageView)
        }
    }
}