package br.com.leituradescomplicada

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import br.com.leituradescomplicada.databinding.ActivityMainBinding
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPhotoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        selectPhotoOfGallery()
        takePhoto()
        recognizedText()
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    handleImageSelection(uri)
                }
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val photoUri = Uri.fromFile(File(currentPhotoPath))
                handleImageSelection(photoUri)
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
            }
        }

    private fun selectPhotoOfGallery() {
        binding.selectedImg.visibility = View.VISIBLE

        binding.selectImgButtonGallery.setOnClickListener {
            binding.txtChooseImg.visibility = View.GONE
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            getContent.launch(intent)
            binding.txtChooseImg.visibility = View.GONE
            binding.readTextsButton.visibility = View.VISIBLE
        }
    }

    private fun takePhoto() {
        binding.takePhotoButton.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()

        val photoURI: Uri = FileProvider.getUriForFile(
            this,
            "br.com.leituradescomplicada.fileprovider",
            photoFile
        )

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun handleImageSelection(selectedImage: Uri) {
        try {
            val source = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
            } else {
                val source = ImageDecoder.createSource(contentResolver, selectedImage)
                ImageDecoder.decodeBitmap(source)
            }
            binding.txtChooseImg.visibility = View.GONE
            binding.selectedImg.setImageBitmap(source)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearTexts() {
        binding.clearTextButton.setOnClickListener {
            binding.textToRead.text = ""
        }
    }

    private fun recognizeText(resultText: FirebaseVisionText) {
        if (resultText.textBlocks.size == 0) {
            binding.textToRead.text = "Data not found"
        }
        for (block in resultText.textBlocks) {
            val text = block.text
            binding.textToRead.text = "${binding.textToRead.text} $text"
        }
    }

    private fun recognizedText() {
        binding.readTextsButton.setOnClickListener {
            val drawable = binding.selectedImg.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                if (bitmap != null) {
                    binding.appName.visibility = View.GONE
                    val image = FirebaseVisionImage.fromBitmap(bitmap)
                    val recognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
                    clearTexts()
                    recognizer.processImage(image)
                        .addOnSuccessListener { recognizeText(it) }
                        .addOnFailureListener {
                            binding.textToRead.text = "Failed to Recognize Text"
                        }
                } else {
                    showToast("Nenhuma imagem selecionada!")
                }
            } else {
                showToast("Nenhuma imagem selecionada!")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}