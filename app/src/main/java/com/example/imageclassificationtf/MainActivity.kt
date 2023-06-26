package com.example.imageclassificationtf

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.imageclassificationtf.databinding.ActivityMainBinding
import com.example.imageclassificationtf.ml.BirdsModel
import org.tensorflow.lite.support.image.TensorImage

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private val GALLERY_REQUESST_CODE=113

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCaptureImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
                takePicturePreview.launch(null)
            }else{
                requestPermissions.launch(android.Manifest.permission.CAMERA)
            }
        }

        binding.btnLoadImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                val intent=Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type="image/*"
                val mimeTypes= arrayOf("image/jpeg","image/jpg","image/png")
                intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes)
                intent.flags=Intent.FLAG_GRANT_READ_URI_PERMISSION
                onResult.launch(intent)
            }else{
                requestPermissions.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }



        binding.tvOutputValue.setOnClickListener {
            val intent=Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${binding.tvOutputValue.text}"))
            startActivity(intent)
        }
    }

    private val takePicturePreview= registerForActivityResult(ActivityResultContracts.TakePicturePreview()){ bitmap->
        if (bitmap!=null){
            binding.ivPlaceholder.setImageBitmap(bitmap)
            outPutGenerator(bitmap)
        }
    }

    private val requestPermissions=registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted->
        if (isGranted){
            takePicturePreview.launch(null)
        }else{
            Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val onResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onResultReceived(GALLERY_REQUESST_CODE,result)
    }
    private fun outPutGenerator(bitmap: Bitmap) {
        val model = BirdsModel.newInstance(this)
        val newBitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true)
        val ttfImage = TensorImage.fromBitmap(newBitmap)
        val outputs = model.process(ttfImage).probabilityAsCategoryList.apply {
            sortByDescending {
                it.score
            }
        }
            val highestProbability = outputs[0]
            binding.tvOutputValue.text=highestProbability.label
    }

    private fun onResultReceived(requestCode:Int, result: androidx.activity.result.ActivityResult?){
        when(requestCode){
            GALLERY_REQUESST_CODE->{
                if(result?.resultCode== Activity.RESULT_OK){
                    result.data?.data.let {uri->
                        val bitmap=BitmapFactory.decodeStream(contentResolver.openInputStream(uri!!))
                        binding.ivPlaceholder.setImageBitmap(bitmap)
                        outPutGenerator(bitmap)
                    }
                }else{
                    Toast.makeText(this, "error selecting image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}