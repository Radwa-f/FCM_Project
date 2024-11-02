package ma.example.ensaj_projet

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo: ImageView = findViewById(R.id.logo)
        val title: ImageView = findViewById(R.id.title)

        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        logo.alpha = 0f

        logo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(2000)
            .withEndAction {

                logo.postDelayed({
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }, 1000)
            }
            .start()
    }
    override fun onPause() {
        super.onPause()
        finish()
    }

}