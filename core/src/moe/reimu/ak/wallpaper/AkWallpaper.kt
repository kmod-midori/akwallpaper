package moe.reimu.ak.wallpaper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.utils.ScreenUtils
import com.esotericsoftware.spine.*
import com.esotericsoftware.spine.utils.TwoColorPolygonBatch
import kotlin.random.Random


open class AkWallpaper : ApplicationAdapter() {
    val camera = OrthographicCamera()
    lateinit var batch: TwoColorPolygonBatch

    lateinit var skelRenderer: SkeletonRenderer

    private var atlas: TextureAtlas? = null
    private var skeleton: Skeleton? = null
    private var animState: AnimationState? = null

    var currentZoom = 1.0f
    var currentTranslateX = 0.0f
    var currentTranslateY = 0.0f
    var isPreview = false
    var isPlayingInteract = false
    var runningTime = 0.0f
    var lastSpecialTime = 0.0f

    var initialized = false
    var paused = false

    var enqueuedCharacterName: String? = null

    var rng = Random(System.currentTimeMillis())

    override fun create() {
        batch = TwoColorPolygonBatch()
        camera.update()

        skelRenderer = SkeletonRenderer().apply {
            premultipliedAlpha = true
        }

        Gdx.input.inputProcessor = GestureDetector(object : GestureDetector.GestureAdapter() {
            override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
                setInteractAnimation()
                return true
            }

            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                if (isPreview) {
                    camera.translate(-deltaX * currentZoom, deltaY * currentZoom)
                    camera.update()
                    return true
                }
                return false
            }

            override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (isPreview) {
                    currentZoom = camera.zoom
                    return true
                }
                return false
            }

            override fun zoom(initialDistance: Float, distance: Float): Boolean {
                if (isPreview) {
                    camera.zoom = initialDistance / distance * currentZoom
                    camera.update()
                    return true
                }
                return false
            }
        })

        initialized = true
    }

    fun loadCharacter(character: String) {
        if (!initialized) {
            return
        }
        if (paused) {
            enqueuedCharacterName = character
            return
        }

        Gdx.app.log(TAG, "Loading character $character")

        atlas?.dispose()
        atlas = TextureAtlas(Gdx.files.internal("$character/char.atlas"))
        val skelBinary = SkeletonBinary(atlas).apply {
            scale = 1f
        }
        val skelData = skelBinary.readSkeletonData(Gdx.files.internal("$character/char.skel"))

        skeleton = Skeleton(skelData).apply {
            setPosition(0.0f, 0f)
        }

        val animStateData = AnimationStateData(skelData).apply {
            defaultMix = 0.5f
        }
        animState = AnimationState(animStateData)

        animState!!.addListener(object : AnimationState.AnimationStateAdapter() {
            override fun end(entry: AnimationState.TrackEntry?) {
                if (entry?.animation?.name == "Interact") {
                    isPlayingInteract = false
                }
            }

            override fun complete(entry: AnimationState.TrackEntry?) {
                val f = rng.nextFloat()
                if (runningTime - lastSpecialTime >= 8.0f && f < 0.3) {
                    lastSpecialTime = runningTime
                    setSpecialAnimation()
                }
            }
        })

        animState!!.setAnimation(0, "Idle", true)

        runningTime = 0.0f
        lastSpecialTime = 0.0f
    }

    fun applySavedCamera(zoom: Float, translateX: Float, translateY: Float) {
        camera.zoom = zoom
        currentZoom = zoom
        camera.position.x = translateX
        camera.position.y = translateY
        camera.update()
    }

    fun setAnimationThenIdle(name: String) {
        animState?.setAnimation(0, name, false)
        animState?.addAnimation(0, "Idle", true, 0f)
    }

    fun setSpecialAnimation() {
        setAnimationThenIdle("Special")
    }

    fun setInteractAnimation() {
        if (isPlayingInteract) {
            return
        }
        isPlayingInteract = true
        setAnimationThenIdle("Interact")
    }

    override fun render() {
        runningTime += Gdx.graphics.deltaTime
        animState?.update(Gdx.graphics.deltaTime)

        ScreenUtils.clear(0f, 0f, 0f, 1f)

        if (skeleton == null || animState == null) {
            return
        }

        animState!!.apply(skeleton)
        skeleton!!.updateWorldTransform()

        batch.projectionMatrix.set(camera.combined)

        batch.begin()
        skelRenderer.draw(batch, skeleton!!)
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        atlas?.dispose()
    }

    override fun resume() {
        Gdx.app.log(TAG, "Application resumed")
        paused = false

        if (enqueuedCharacterName != null) {
            loadCharacter(enqueuedCharacterName!!)
            enqueuedCharacterName = null
        }
    }

    override fun pause() {
        Gdx.app.log(TAG, "Application paused")
        paused = true
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportHeight = Gdx.graphics.height.toFloat()
        camera.viewportWidth = Gdx.graphics.width.toFloat()
        camera.update()
    }

    companion object {
        const val TAG = "AkWallpaperApp"
    }
}