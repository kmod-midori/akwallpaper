package moe.reimu.ak.wallpaper

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.utils.ScreenUtils
import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.AnimationStateData
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.SkeletonBinary
import com.esotericsoftware.spine.SkeletonRenderer
import com.esotericsoftware.spine.utils.TwoColorPolygonBatch
import kotlin.random.Random


open class AkWallpaper : ApplicationAdapter() {
    val camera = OrthographicCamera()
    val backgroundCamera = OrthographicCamera()

    private lateinit var polygonBatch: TwoColorPolygonBatch
    private lateinit var spriteBatch: SpriteBatch

    private val backgroundLock = Object()
    private var backgroundTexture: Texture? = null
    private var backgroundTextureRegion: TextureRegion? = null

    private lateinit var skelRenderer: SkeletonRenderer

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

    private var screenWidth = 0.0f
    private var screenHeight = 0.0f

    private var initialized = false
    private var paused = false

    private var enqueuedCharacterName: String? = null

    var rng = Random(System.currentTimeMillis())

    override fun create() {
        polygonBatch = TwoColorPolygonBatch()
        spriteBatch = SpriteBatch()

        camera.update()
        backgroundCamera.update()

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

        skelRenderer.premultipliedAlpha = !Gdx.files.internal("$character/no_premul").exists()

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
        animState = AnimationState(animStateData).apply {
            addListener(object : AnimationState.AnimationStateAdapter() {
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

            setAnimation(0, "Idle", true)
        }

        runningTime = 0.0f
        lastSpecialTime = 0.0f
    }

    fun loadBackground(name: String) {
        synchronized(backgroundLock) {
            backgroundTextureRegion = null
            backgroundTexture?.dispose()
            backgroundTexture = null

            backgroundTexture = Texture(Gdx.files.internal("background/$name.jpg"))
            backgroundTextureRegion = TextureRegion(backgroundTexture)
        }

        updateTexRegion()
    }

    fun applySavedCamera(zoom: Float, translateX: Float, translateY: Float) {
        camera.zoom = zoom
        currentZoom = zoom
        camera.position.x = translateX
        camera.position.y = translateY
        camera.update()
    }

    private fun setAnimationThenIdle(name: String) {
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

        spriteBatch.projectionMatrix.set(backgroundCamera.combined)

        synchronized(backgroundLock) {
            backgroundTextureRegion?.let {
                spriteBatch.begin()
                spriteBatch.draw(it, 0f, 0f, screenWidth, screenHeight)
                spriteBatch.end()
            }
        }

        polygonBatch.projectionMatrix.set(camera.combined)
        polygonBatch.begin()
        skelRenderer.draw(polygonBatch, skeleton!!)
        polygonBatch.end()
    }

    override fun dispose() {
        polygonBatch.dispose()
        spriteBatch.dispose()
        backgroundTexture?.dispose()
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
        screenHeight = Gdx.graphics.height.toFloat()
        screenWidth = Gdx.graphics.width.toFloat()

        camera.viewportHeight = screenHeight
        camera.viewportWidth = screenWidth
        camera.update()

        backgroundCamera.setToOrtho(false, screenWidth, screenHeight)

        updateTexRegion()
    }

    private fun updateTexRegion() {
        synchronized(backgroundLock) {
            val tex = backgroundTexture
            val texRegion = backgroundTextureRegion

            if (tex != null && texRegion != null) {
                val screenAspectRatio = screenWidth / screenHeight
                val imageAspectRatio = tex.width.toFloat() / tex.height

                if (screenAspectRatio > imageAspectRatio) {
                    val imageHeight = (tex.width.toFloat() / screenAspectRatio).toInt()
                    // The screen is wider than the image, so the image's height should match the screen's height
                    texRegion.setRegion(0, (tex.height - imageHeight) / 2, tex.width, imageHeight)
                } else {
                    val imageWidth = (tex.height.toFloat() * screenAspectRatio).toInt()
                    // The screen is taller than the image, so the image's width should match the screen's width
                    texRegion.setRegion((tex.width - imageWidth) / 2, 0, imageWidth, tex.height)
                }
            }
        }
    }

    companion object {
        const val TAG = "AkWallpaperApp"
    }
}