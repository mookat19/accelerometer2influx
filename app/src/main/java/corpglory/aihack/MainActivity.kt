package corpglory.aihack

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import io.reactivex.subjects.PublishSubject
import org.jetbrains.anko.button
import org.jetbrains.anko.editText
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout


class MainActivity : AppCompatActivity() {

    lateinit var sensorManager: SensorManager
    var customEventListener: CustomEventListener? = null

    lateinit var sensor: Sensor
    lateinit var graphanaService: GraphanaService

    lateinit var outView: TextView
    lateinit var publishSubject: PublishSubject<String>
    lateinit var retrofit: Retrofit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)




        verticalLayout {
            val addrView = editText("http://209.205.120.226:8086") {
                hint = "Name"
                textSize = 24f
            }

            button("Start / refresh") {
                textSize = 26f
                onClick {
                    startListen(addrView.text.toString())
                }
            }

            outView = textView("") {
                id = View.generateViewId()
            }
        }



        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        startListen("http://209.205.120.226:8086")
    }

    private fun startListen(addr: String) {
        retrofit = Retrofit.Builder()
                .baseUrl(addr)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()


        graphanaService = retrofit.create(GraphanaService::class.java)


        val subject = PublishSubject.create<String>()
        subject
                .observeOn(Schedulers.io())
                .subscribe {
                    val response = graphanaService.push(it).execute()
                    runOnUiThread {
                        Log.i("here", response.message())
                    }
                }

        customEventListener = CustomEventListener(this@MainActivity, outView, subject)
        if (customEventListener != null) {
            sensorManager.unregisterListener(customEventListener)
        }
        sensorManager.registerListener(customEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(customEventListener)
    }
}
