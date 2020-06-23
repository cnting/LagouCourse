package com.cnting.dexclassloaderhotfix

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dalvik.system.DexClassLoader
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn.setOnClickListener {
            val jarFile = File(getExternalFilesDir(null)!!.path + File.separator + "say_hotfix.jar")
            if (!jarFile.exists()) {
                val say = SayException()
                Toast.makeText(this, say.saySomething(), Toast.LENGTH_SHORT).show()
            } else {
                val dexClassLoader = DexClassLoader(
                    jarFile.absolutePath,
                    getExternalFilesDir(null)!!.absolutePath,
                    null,
                    classLoader
                )
                DexUtil.injectDexAtFirst(dexClassLoader);
                val clazz = dexClassLoader.loadClass("com.cnting.dexclassloaderhotfix.SayHotFix")
                val iSay = clazz.newInstance() as ISay
                Toast.makeText(this, iSay.saySomething(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}