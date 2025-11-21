package com.example.neuroshelf.domain

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceRecognition(private val context: Context, modelPath: String = "facenet_int8.tflite") {
    private val interpreter: Interpreter
    private val inputSize = 112 // típicamente 112 para MobileFaceNet o 160/112 para FaceNet

    init {
        val model = loadModelFile(context, modelPath)
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(ctx: Context, filename: String): MappedByteBuffer {
        val assetFileDescriptor = ctx.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun getEmbedding(bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val buffer = convertBitmapToFloatBuffer(scaled)
        val output = Array(1) { FloatArray(128) } // embedding length (adjust según modelo)
        interpreter.run(buffer, output)
        val emb = output[0]
        return l2Normalize(emb)
    }

    private fun convertBitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        imgData.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val v = intValues[pixel++]
                // normalizar entre -1..1 common for facenet
                imgData.putFloat(((v shr 16 and 0xFF) - 127.5f) / 128f)
                imgData.putFloat(((v shr 8 and 0xFF) - 127.5f) / 128f)
                imgData.putFloat(((v and 0xFF) - 127.5f) / 128f)
            }
        }
        imgData.rewind()
        return imgData
    }

    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (v in embedding) sum += v * v
        val norm = sqrt(sum)
        val out = FloatArray(embedding.size)
        for (i in embedding.indices) out[i] = embedding[i] / norm
        return out
    }

    fun compare(emb1: FloatArray, emb2: FloatArray): Float {
        // cosine similarity
        var dot = 0f
        for (i in emb1.indices) dot += emb1[i] * emb2[i]
        return dot // higher = more similar; threshold ~ 0.5-0.8 depending on model
    }
}
