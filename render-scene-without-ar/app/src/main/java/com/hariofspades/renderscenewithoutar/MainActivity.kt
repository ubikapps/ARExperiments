package com.hariofspades.renderscenewithoutar

import android.net.Uri
import android.os.Bundle
import android.support.animation.FlingAnimation
import android.support.animation.FloatValueHolder
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent.*
import android.view.VelocityTracker
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.android.synthetic.main.activity_main.*

/**
 * created by Hari Vignesh Jayapalan
 *
 * MainActivity, where you can place your 3D models and display it for the phones which does not
 * support ARCore. Refer the blog for further usecase and implementation assistance
 *
 */
class MainActivity : AppCompatActivity() {

    lateinit var scene: Scene

    lateinit var cupCakeNode: Node
    private var distToModel: Float = 0.0f

    var angle: Float = 0.0f
    var oldX: Float = 0.0f
    var oldY: Float = 0.0f
    var velocityTracker: VelocityTracker = VelocityTracker.obtain()
    var fling : FlingAnimation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scene = sceneView.scene // get current scene
        renderObject(Uri.parse("cupcake.sfb")) // Render the object
    }

    /**
     * load the 3D model in the space
     * @param parse URI of the model, imported using Sceneform plugin
     */
    private fun renderObject(parse: Uri) {
        ModelRenderable.builder()
                .setSource(this, parse)
                .build()
                .thenAccept {
                    addNodeToScene(it)
                    addTouchListener()
                }
                .exceptionally {
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(it.message)
                            .setTitle("error!")
                    val dialog = builder.create()
                    dialog.show()
                    return@exceptionally null
                }

    }

    /**
     * Adds a node to the current scene
     * @param model - rendered model
     */
    private fun addNodeToScene(model: ModelRenderable?) {

        model?.let {
            cupCakeNode = Node().apply {
                setParent(scene)
                localPosition = Vector3(0f, 0f, -1f)
                localScale = Vector3(3f, 3f, 3f)
                name = "Cupcake"
                renderable = it
            }


            scene.addChild(cupCakeNode)
        }
    }

    private fun addTouchListener(){
        distToModel = Vector3.subtract(scene.camera.worldPosition, cupCakeNode.worldPosition).length() * 10f

        scene.setOnTouchListener { hitTestResult, motionEvent ->
            when(motionEvent.action) {
                ACTION_DOWN -> {
                    fling?.cancel()
                }
                ACTION_MOVE -> {
                    fling = FlingAnimation(FloatValueHolder(motionEvent.x))
                    velocityTracker.addMovement(motionEvent)
                    val touchX = motionEvent.x
                    val touchY = motionEvent.y

                    val deltaX = touchX - oldX
                    val deltaY = touchY - oldY
                    oldX = touchX
                    oldY = touchY
                    angle += Math.atan((deltaX / distToModel).toDouble()).toFloat()

                    cupCakeNode.localRotation = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), angle)
                }
                ACTION_CANCEL ->{
                    fling?.cancel()
                    velocityTracker.recycle()
                }
                ACTION_UP ->{
                    velocityTracker.computeCurrentVelocity(1000)
                    fling?.apply {
                        setStartVelocity(velocityTracker.xVelocity)
                        friction = 1.1f
                        addUpdateListener { dynamicAnimation, value, velocity ->
                            angle += Math.atan((value / distToModel).toDouble()).toFloat()
                            angle += Math.atan((value / distToModel).toDouble()).toFloat()

                            cupCakeNode.localRotation = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), angle)
                        }
                        addEndListener {dynamicAnimation, cancelled: Boolean, value: Float, velocity: Float ->
                            angle += value
                        }
                    }
                    fling?.start()

//                    velocityTracker.recycle()
                }
            }
            true
        }
    }

    override fun onPause() {
        super.onPause()
        sceneView.pause()
    }

    override fun onResume() {
        super.onResume()
        sceneView.resume()
    }

}
