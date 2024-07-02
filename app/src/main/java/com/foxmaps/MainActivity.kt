package com.foxmaps

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import androidx.fragment.app.commit
import com.foxmaps.maps.presentation.MapHostFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            v.setPadding(0)
            insets.inset(0, 0, 0, 0)
        }

        if (savedInstanceState == null) {
            val fragment = MapHostFragment.newInstance()
            supportFragmentManager.commit {
                add(R.id.main, fragment)
            }
        }
    }
}
