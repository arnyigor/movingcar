package com.arny.movingcar

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scroll_vert.post {
            val centerX  = moving_view.getCenterX()
            val centerY  = moving_view.getCenterY()
            scroll_vert.scrollTo(centerX, centerY)
            scroll_hor.scrollTo(centerX, centerY)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_clear -> {
                moving_view.clear()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
