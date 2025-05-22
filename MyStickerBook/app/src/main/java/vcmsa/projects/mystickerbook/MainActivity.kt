package vcmsa.projects.mystickerbook

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue // For ServerValue.TIMESTAMP
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database // RTDB specific import
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
class MainActivity : AppCompatActivity() {

    private lateinit var imageViewPreview: ImageView
    private lateinit var buttonSelectImage: Button
    private lateinit var editTextStickerName: EditText
    private lateinit var editTextStickerCategory: EditText
    private lateinit var buttonUploadSticker: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var rvSticker: RecyclerView

    private var selectedImageUri: Uri? = null

    private lateinit var storage: FirebaseStorage
    private lateinit var database: DatabaseReference
    private var stickerAdapter: StickerAdapter? = null
    private var stickersListener: ValueEventListener? = null

    private val TAG = "MainActivityStickersRTDB"

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data.let { uri ->
                    selectedImageUri = uri
                    imageViewPreview.setImageURI(uri)
                }
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openGallary()
            } else {
                Toast.makeText(this, "Permission denied to read media files", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        imageViewPreview = findViewById(R.id.imageViewPreview)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        editTextStickerName = findViewById(R.id.editTextStickerName)
        editTextStickerCategory = findViewById(R.id.ediTextStickerCategory)
        buttonUploadSticker = findViewById(R.id.buttonUploadSticker)
        progressBar = findViewById(R.id.progressBar)
        rvSticker = findViewById(R.id.rvSticker)

        storage = Firebase.storage
        database = Firebase.database.reference.child("stickers")

        setupRecyclerView()

        buttonSelectImage.setOnClickListener {
            checkPermissionAndOpenGallery()
        }
        buttonUploadSticker.setOnClickListener {
            uploadStickerData()
        }
    }

    private fun setupRecyclerView() {
        stickerAdapter = StickerAdapter(mutableListOf())
        rvSticker.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = stickerAdapter
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        when {
            ContextCompat.checkSelfPermission(
                this, permission
            )
                    == PackageManager.PERMISSION_GRANTED -> {
                openGallary()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(
                    this,
                    "Permission is needed to select images",
                    Toast.LENGTH_SHORT
                )
                    .show()
                requestPermissionLauncher.launch(permission)
            }

            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallary() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)

    }

    private fun uploadStickerData() {
        val stickerName = editTextStickerName.text.toString().trim()
        val stickerCategory = editTextStickerCategory.text.toString().trim()

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image. dumbass", Toast.LENGTH_SHORT).show()
            return
        }
        if (stickerName.isEmpty()) {
            editTextStickerName.error = "name cannot be empty dumbass"
            return
        }
        if (stickerCategory.isEmpty()) {
            editTextStickerCategory.error = "category cannot be empty dumbass"
            return
        }
        progressBar.visibility = View.VISIBLE
        buttonUploadSticker.isEnabled = false

        lifecycleScope.launch {
            try {
                val imageFileName =
                    "stickers_rtdb/${UUID.randomUUID()} ${System.currentTimeMillis()}.jpg"
                val storageRef = storage.reference.child(imageFileName)
                val imageUrl = storageRef.downloadUrl.await().toString()
                Log.d(TAG, "Image uploaded to Storage $imageUrl")
                val stickerid = database.push().key!!
                if (stickerid != null) {
                    throw Exception("Couldn't get push key for sticker. RTDB error")
                }
                val stickerData = Sticker(
                    name = stickerName,
                    category = stickerCategory,
                    imageUrl = imageUrl,
                    uploadedAt = ServerValue.TIMESTAMP
                )

                database.child(stickerid).setValue(stickerData).await()
                Log.d(TAG, "metadata saved to RTDB with ID: $stickerid")

                Toast.makeText(
                    this@MainActivity,
                    "Sticker uploaded successfully",
                    Toast.LENGTH_SHORT
                ).show()

                editTextStickerName.text.clear()
                editTextStickerCategory.text.clear()
                imageViewPreview.setImageResource(R.drawable.ic_launcher_background)
                selectedImageUri = null
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading sticker", e)
                Toast.makeText(
                    this@MainActivity,
                    "upload failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                progressBar.visibility = View.GONE
                buttonUploadSticker.isEnabled = true
            }
        }
    }

    private fun attatchStickersListner() {
        if (stickersListener == null) {
            progressBar.visibility = View.VISIBLE
            stickersListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stickerlist = mutableListOf<Sticker>()
                    for (stickerSnapshot in snapshot.children) {
                        val sticker = stickerSnapshot.getValue(Sticker::class.java)
                        sticker?.let {
                            it.id = stickerSnapshot.key
                                ?: ""
                            stickerlist.add(it)
                        }
                    }
                    stickerlist.sortByDescending { it.getUploadedAtLong() }

                    stickerAdapter?.setStickers(stickerlist)
                    progressBar.visibility = View.GONE
                    Log.d(TAG, "Sticker list updated${stickerlist.size} item found")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching stickers", error.toException())
                    Toast.makeText(
                        baseContext,
                        "Error to load stickers: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressBar.visibility = View.GONE
                }
            }
            database.addValueEventListener(stickersListener!!)
        }
    }
    private fun detatchStickersListener() {
        stickersListener?.let {
            database.removeEventListener(it)
            stickersListener = null
            Log.d(TAG, "Sticker listener detatched")
        }
    }
    override  fun onStart() {
        super.onStart()
        attatchStickersListner()
    }
    override fun onStop() {
        super.onStop()
        detatchStickersListener()
    }
}


