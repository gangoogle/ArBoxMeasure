package com.rms.boxmeasure

//import abak.tr.com.boxedverticalseekbar.BoxedVertical
//import abak.tr.com.boxedverticalseekbar.BoxedVertical.OnValuesChangeListener
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.Point as ArPoint
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.CameraStream
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.sceneform.scene.await
import android.widget.ImageView
import com.hjq.shape.layout.ShapeLinearLayout
import com.hjq.shape.view.ShapeTextView
import kotlin.math.abs
import kotlin.math.sqrt


/**
 * AR 测量首页
 */
class ArMeasureActivity : AppCompatActivity() {

    /**
     * 启用深度信息
     */
    private var mDepthModeEnabled = false

    private lateinit var mArFragment: ArFragment

    // Views
    private lateinit var tvWidth: TextView
    private lateinit var tvLength: TextView
    private lateinit var tvHeight: TextView
    private lateinit var ivBoxStep: ImageView
    private lateinit var tvBoxStepHint: ShapeTextView
    private lateinit var skHeightControl: android.widget.SeekBar
    private lateinit var llHeightControlContainer: ShapeLinearLayout
    private lateinit var tvHeightValue: TextView
    private lateinit var tvDetectedHeight: ShapeTextView
    private lateinit var btCaptureHeight: ShapeLinearLayout
    private lateinit var tvHeightHint: ShapeTextView
    private lateinit var btReturn: ShapeLinearLayout
    private lateinit var btRay: ShapeLinearLayout
    private lateinit var ivRayImg: ImageView
    private lateinit var btSure: ShapeLinearLayout
    private lateinit var ivFpsTarget: ImageView

    //开始节点
    private lateinit var mStartNode: AnchorNode

    //锚点列表
    private var mAnchorList = arrayListOf<AnchorInfoBean>()

    private var mAndyRenderableCube: ModelRenderable? = null

    //高
    private var mHeight: Double = 0.0

    //高度锚点
    private var mHeightAnchorNode: AnchorNode? = null

    //高度节点
    private var mHeightAndyNode: TransformableNode? = null

    //高度节点的文本UI
    private var mHeightNodeTextView: TextView? = null

    //检测到的高度值
    private var mDetectedHeight: Double? = null

    //是否检测到有效高度
    private var mIsHeightDetected: Boolean = false

    //稳定检测到的高度候选值
    private var mHeightCandidate: Double? = null

    //稳定帧数
    private var mHeightCandidateStableFrames: Int = 0

    //候选高度短暂丢失的帧数
    private var mHeightCandidateMissingFrames: Int = 0

    //长
    private var mLength: Double = 0.0

    //宽
    private var mWidth: Double = 0.0

    private val mSceneView: ArSceneView by lazy {
        mArFragment.arSceneView
    }

    private val mScene: Scene by lazy {
        mSceneView.scene
    }

    private val TAG = "BoxArCore"

    //SeekBar值到高度(米)的转换系数: seekBarValue = height(cm) * 2
    private val SEEKBAR_TO_HEIGHT_FACTOR = 2

    //高度识别稳定帧数
    private val HEIGHT_STABLE_FRAME_COUNT = 4

    //短暂丢帧时保留识别结果，避免准星轻微抖动立即失效
    private val HEIGHT_MISSING_FRAME_TOLERANCE = 6

    //高度候选波动容忍值(米)
    private val HEIGHT_STABLE_TOLERANCE_METERS = 0.015

    //第三个底角允许的直角偏差
    private val RIGHT_ANGLE_TOLERANCE_DEGREES = 15.0

    //有效 hit 距离范围
    private val MIN_HIT_DISTANCE_METERS = 0.12f
    private val MAX_HIT_DISTANCE_METERS = 5.0f

    //高度识别距离阈值
    private val MIN_HEIGHT_EDGE_DISTANCE_METERS = 0.025f
    private val MAX_HEIGHT_EDGE_DISTANCE_METERS = 0.08f
    private val HEIGHT_EDGE_DISTANCE_RATIO = 0.18f
    private val MIN_TOP_SURFACE_MARGIN_METERS = 0.03f
    private val MAX_TOP_SURFACE_MARGIN_METERS = 0.10f
    private val TOP_SURFACE_MARGIN_RATIO = 0.12f

    //避免每帧重复刷新相同 UI
    private var mLastRayHasTarget: Boolean? = null
    private var mLastHeightUiState: String? = null

    //结束节点列表
    private val mEndNodeArray = arrayListOf<Node>()

    //线段节点列表
    private val mLineNodeArray = arrayListOf<Node>()

    //线段sphere模型
    private val mSphereNodeArray = arrayListOf<Node>()

    //开始节点列表
    private val mStartNodeArray = arrayListOf<Node>()

    //屏幕尺寸
    var mScreenSize = Point();


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (mAnchorList.size == 0) {
            finish()
        } else {
            backPoint()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_measure)

        // 初始化 Views
        mArFragment = (supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment)
        tvWidth = findViewById(R.id.tv_width)
        tvLength = findViewById(R.id.tv_length)
        tvHeight = findViewById(R.id.tv_height)
        ivBoxStep = findViewById(R.id.iv_box_step)
        tvBoxStepHint = findViewById(R.id.tv_box_step_hint)
        skHeightControl = findViewById(R.id.sk_height_control)
        llHeightControlContainer = findViewById(R.id.ll_height_control_container)
        tvHeightValue = findViewById(R.id.tv_height_value)
        tvDetectedHeight = findViewById(R.id.tv_detected_height)
        btCaptureHeight = findViewById(R.id.bt_capture_height)
        tvHeightHint = findViewById(R.id.tv_height_hint)
        btReturn = findViewById(R.id.bt_return)
        btRay = findViewById(R.id.bt_ray)
        ivRayImg = findViewById(R.id.iv_ray_img)
        btSure = findViewById(R.id.bt_sure)
        ivFpsTarget = findViewById(R.id.iv_fps_target)

        //获取屏幕尺寸
        val display = windowManager.defaultDisplay
        display.getRealSize(mScreenSize)
        setUiListener()
        initConfig()
        lifecycleScope.launchWhenCreated {
            loadModels()
        }
        updateSizeUI()
        setArListener()
    }

    /**
     * 回退节点
     */
    private fun backPoint() {
        while (mAnchorList.size > 0) {
            if (mAnchorList.size == 1) {
                mAnchorList.forEach { it.anchor.detach() }
                mAnchorList.clear()
                mLineNodeArray.clear()
                mSphereNodeArray.clear()
                mStartNodeArray.clear()
                mEndNodeArray.clear()
                mScene.removeChild(mStartNode)
            } else if (mAnchorList.size == 2) {
                mAnchorList.removeAt(mAnchorList.size - 1).anchor.detach()
                val index = mStartNodeArray.size - 1
                mSphereNodeArray[index].removeChild(mLineNodeArray.removeAt(index))
                mEndNodeArray[index].removeChild(mSphereNodeArray.removeAt(index + 1))
                mScene.removeChild(mStartNodeArray.removeAt(index))
                mScene.removeChild(mEndNodeArray.removeAt(index))
            } else if (mAnchorList.size == 3) {
                mAnchorList.removeAt(mAnchorList.size - 1).anchor.detach()
                val index = mStartNodeArray.size - 1
                mSphereNodeArray[index].removeChild(mLineNodeArray.removeAt(index))
                mEndNodeArray[index].removeChild(mSphereNodeArray.removeAt(index + 1))
                mScene.removeChild(mStartNodeArray.removeAt(index))
                mScene.removeChild(mEndNodeArray.removeAt(index))
                mHeightAnchorNode?.removeChild(mHeightAndyNode)
                mScene.removeChild(mHeightAnchorNode)
            }
        }
        mWidth = 0.0
        mHeight = 0.0
        mLength = 0.0
        resetHeightDetectionState()
        skHeightControl.progress = 0
        updateSizeUI()
    }

    /**
     * 绘制线
     */
    private fun drawLine(firstAnchor: Anchor, secondAnchor: Anchor, length: Double) {
        val firstAnchorNode = AnchorNode(firstAnchor)
        mStartNodeArray.add(firstAnchorNode)
        val secondAnchorNode = AnchorNode(secondAnchor)
        mEndNodeArray.add(secondAnchorNode)
        firstAnchorNode.setParent(mScene)
        secondAnchorNode.setParent(mScene)
        MaterialFactory.makeOpaqueWithColor(
            this@ArMeasureActivity,
            com.google.ar.sceneform.rendering.Color(Color.parseColor("#19ba75"))
        ).thenAccept { material ->
            val sphere = ShapeFactory.makeSphere(0.01f, Vector3(0.0f, 0.0f, 0.0f), material)
            mSphereNodeArray.add(Node().apply {
                parent = secondAnchorNode
                localPosition = Vector3.zero()
                renderable = sphere
            })
        }
        val firstWorldPosition = firstAnchorNode.worldPosition
        val secondWorldPosition = secondAnchorNode.worldPosition
        val difference = Vector3.subtract(firstWorldPosition, secondWorldPosition)
        val directionFromTopToBottom = difference.normalized()
        val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
        MaterialFactory.makeOpaqueWithColor(
            this@ArMeasureActivity,
            com.google.ar.sceneform.rendering.Color(Color.parseColor("#ea373e"))
        ).thenAccept { material ->
            val lineMode = ShapeFactory.makeCube(
                Vector3(0.005f, 0.005f, difference.length()), Vector3.zero(), material
            )
            val lineNode = Node().apply {
                setParent(firstAnchorNode)
                renderable = lineMode
                worldPosition = Vector3.add(firstWorldPosition, secondWorldPosition).scaled(0.5f)
                worldRotation = rotationFromAToB
            }
            mLineNodeArray.add(lineNode)
            ViewRenderable.builder().setView(this@ArMeasureActivity, R.layout.renderable_text)
                .build().thenAccept { it ->
                    (it.view as TextView).text = "${String.format("%.1f", length * 100)}CM"
                    it.isShadowCaster = false
                    FaceToCameraNode().apply {
                        setParent(lineNode)
                        localRotation = Quaternion.axisAngle(Vector3(0f, 0.5f, 0f), 90f)
                        localPosition = Vector3(0f, 0.02f, 0f)
                        renderable = it
                    }
                }
        }
    }

    /**
     * 设置配置
     */
    private fun initConfig() {
        mArFragment.apply {
            setOnViewCreatedListener { arSceneView ->
                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)
                updateDepthRendering(arSceneView, false)
            }
            setOnSessionConfigurationListener { session, config ->
                config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED)
                config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL)
                config.setFocusMode(Config.FocusMode.AUTO)
                config.setLightEstimationMode(Config.LightEstimationMode.DISABLED)
                mDepthModeEnabled = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                config.depthMode = if (mDepthModeEnabled) {
                    Config.DepthMode.AUTOMATIC
                } else {
                    Config.DepthMode.DISABLED
                }
                updateDepthRendering(mSceneView, mDepthModeEnabled)
            }

        }
    }


    /**
     * 计算三个向量的夹角
     */
    private fun isAngleCloseTo90Degrees(poseC: Pose, poseA: Pose, poseB: Pose): Boolean {
        var vectorCB = Vector3(poseC.tx() - poseB.tx(), 0.0f, poseC.tz() - poseB.tz())
        var vectorAB = Vector3(poseA.tx() - poseB.tx(), 0.0f, poseA.tz() - poseB.tz())

        if (vectorCB.length() <= 0.001f || vectorAB.length() <= 0.001f) {
            return false
        }

        vectorCB = vectorCB.normalized()
        vectorAB = vectorAB.normalized()

        val angleCos = abs(Vector3.dot(vectorAB, vectorCB))
        val thresholdCos = Math.sin(Math.toRadians(RIGHT_ANGLE_TOLERANCE_DEGREES)).toFloat()
        return angleCos <= thresholdCos
    }


    /**
     * 加载模型
     */
    private suspend fun loadModels() {
        mAndyRenderableCube =
            ModelRenderable.builder().setSource(this, Uri.parse("models/cubito3.glb"))
                .setIsFilamentGltf(true).await()
    }

    /**
     * 点击平面
     */
    private fun onTap(hitResult: HitResult, plane: Plane) {
        Log.d(TAG, "类型 ${hitResult.trackable}")
        if (mAnchorList.size >= 3) {
            return
        }
        val anchor = hitResult.createAnchor()
        if (mAnchorList.size == 2 && !isAngleCloseTo90Degrees(
                poseC = anchor.pose,
                poseA = mAnchorList[0].anchor.pose,
                poseB = mAnchorList[1].anchor.pose
            )
        ) {
            anchor.detach()
            Toast.makeText(this, "第三个底角需与前两点接近直角", Toast.LENGTH_SHORT).show()
            return
        }
        val anchorInfoBean = AnchorInfoBean("", anchor, 0.0)
        mAnchorList.add(anchorInfoBean)
        if (mAnchorList.size > 1) {
            val endAnchor = mAnchorList[mAnchorList.size - 1].anchor
            val startAnchor = mAnchorList[mAnchorList.size - 2].anchor
            val startPose = endAnchor.pose
            val endPose = startAnchor.pose
            val dx = startPose.tx() - endPose.tx()
            val dy = startPose.ty() - endPose.ty()
            val dz = startPose.tz() - endPose.tz()
            anchorInfoBean.length = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())
            if (mAnchorList.size == 2) {
                mWidth = anchorInfoBean.length
            } else if (mAnchorList.size == 3) {
                mLength = anchorInfoBean.length
            }
            updateSizeUI()
            drawLine(startAnchor, endAnchor, anchorInfoBean.length)
            if (mAnchorList.size == 3) {
                //测量高度 创建高度测量
                val anchorNode = AnchorNode(startAnchor)
                anchorNode.localScale = Vector3(0.1f, 0.01f, 0.1f)
                anchorNode.parent = mArFragment.getArSceneView().getScene()
                mHeightAnchorNode = anchorNode
                // Create the transformable andy and add it to the anchor.
                mHeightAndyNode = TransformableNode(mArFragment.getTransformationSystem())
                mHeightAndyNode!!.parent = anchorNode
                mHeightAndyNode!!.renderable = mAndyRenderableCube
                mHeightAndyNode!!.select()
                mHeightAndyNode!!.scaleController.isEnabled = false
                mHeightAndyNode!!.translationController.isEnabled = true
                ViewRenderable.builder().setView(this@ArMeasureActivity, R.layout.renderable_text)
                    .build().thenAccept { it ->
                        mHeightNodeTextView = it.view as TextView
                        mHeightNodeTextView?.text = "${formatHeight(mHeight)}CM"
                        it.isShadowCaster = false
                        FaceToCameraNode().apply {
                            setParent(mEndNodeArray[mEndNodeArray.size - 2])
                            localRotation = Quaternion.axisAngle(Vector3(0f, 0.5f, 0f), 90f)
                            localPosition = Vector3(0.05f, 0.02f, 0.1f)
                            renderable = it
                        }
                    }
            }
        } else {
            mStartNode = AnchorNode(anchor)
            mStartNode.setParent(mArFragment.arSceneView.scene)
            MaterialFactory.makeOpaqueWithColor(
                this@ArMeasureActivity,
                com.google.ar.sceneform.rendering.Color(Color.parseColor("#19ba75"))
            ).thenAccept { material ->
                val sphere = ShapeFactory.makeSphere(0.01f, Vector3.zero(), material)
                mSphereNodeArray.add(Node().apply {
                    setParent(mStartNode)
                    localPosition = Vector3.zero()
                    renderable = sphere
                })
            }
            updateSizeUI()
        }
    }

    /**
     * 发送hit
     */
    private fun sendHit() {
        val frame = mSceneView.arFrame ?: return
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            return
        }
        val hits = getCenterHits(frame)
        if (mAnchorList.size < 3) {
            hits.firstOrNull { hit ->
                val trackable = hit.trackable
                trackable is Plane && isValidPlaneHit(hit, trackable)
            }?.let { hit ->
                onTap(hit, hit.trackable as Plane)
            }
            return
        }
        (findBestHeightCandidate(hits) ?: mDetectedHeight)?.let { heightMeters ->
            skHeightControl.progress = (heightMeters * 100 * SEEKBAR_TO_HEIGHT_FACTOR).toInt()
        }
    }


    /**
     * 第三个点位距离前两个点相连线段的长度
     */
    fun calculateDistanceToSegment(pointA: Pose, pointB: Pose, pointC: Pose): Float {
        val vectorAC = Vector3(pointC.tx() - pointA.tx(), 0.0f, pointC.tz() - pointA.tz())
        val vectorAB = Vector3(pointB.tx() - pointA.tx(), 0.0f, pointB.tz() - pointA.tz())

        val lengthAB = sqrt(vectorAB.x * vectorAB.x + vectorAB.z * vectorAB.z)
        if (lengthAB <= 0.0001f) {
            return Float.MAX_VALUE
        }

        val dotProduct = vectorAC.x * vectorAB.x + vectorAC.z * vectorAB.z
        val normalizedDotProduct = dotProduct / (lengthAB * lengthAB)
        val clampedNormalizedDotProduct = normalizedDotProduct.coerceIn(0.0f, 1.0f)

        val pointD = Vector3(
            pointA.tx() + clampedNormalizedDotProduct * vectorAB.x,
            0.0f,
            pointA.tz() + clampedNormalizedDotProduct * vectorAB.z
        )

        val vectorDC = Vector3(pointC.tx() - pointD.x, 0.0f, pointC.tz() - pointD.z)
        return sqrt(vectorDC.x * vectorDC.x + vectorDC.z * vectorDC.z)
    }

    /**
     * 检查高度点
     */
    private fun getHeightCandidate(hit: HitResult): Double? {
        Log.d(TAG, "sendHit:point ${hit.hitPose}")
        if (mAnchorList.size < 3 || !isValidHitDistance(hit) || hit.trackable.trackingState != TrackingState.TRACKING) {
            return null
        }
        val hitPoseHeight = hit.hitPose
        val poseA = mAnchorList[0].anchor.pose
        val poseB = mAnchorList[1].anchor.pose
        val poseC = mAnchorList[2].anchor.pose
        val dy = hitPoseHeight.ty() - poseB.ty()
        val distanceAB = calculateDistanceToSegment(poseA, poseB, hitPoseHeight)
        val distanceBC = calculateDistanceToSegment(poseC, poseB, hitPoseHeight)
        val threshold = getHeightEdgeThreshold()
        Log.d(
            TAG,
            "distance  to poseAB  ${distanceAB}"
        )
        Log.d(
            TAG,
            "distance  to poseBC  ${distanceBC}"
        )
        if (dy <= 0f) {
            return null
        }
        if (!isWithinTopSurfaceProjection(hitPoseHeight)) {
            return null
        }
        if (distanceAB <= threshold || distanceBC <= threshold) {
            return dy.toDouble()
        }
        return null
    }


    /**
     * 设置监听
     */
    private fun setArListener() {
        //发射射线
        btRay.setOnClickListener {
            sendHit()
        }
        btRay.postDelayed({
            mScene.addOnUpdateListener {
                runOnUiThread {
                    onSceneUpdate()

                }
            }
        }, 500)
        mArFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->

        }
    }

    /**
     * 场景更新
     */
    private fun onSceneUpdate() {
        val frame = mSceneView.arFrame
        if (frame == null || frame.camera.trackingState != TrackingState.TRACKING) {
            if (mAnchorList.size >= 3) {
                resetHeightDetectionState()
            }
            updateHeightDetectionUI()
            updateRayUi(false)
            return
        }
        val hits = getCenterHits(frame)
        val rayHasTarget = if (mAnchorList.size < 3) {
            hits.any { hit ->
                val trackable = hit.trackable
                trackable is Plane && isValidPlaneHit(hit, trackable)
            }
        } else {
            val candidateHeight = findBestHeightCandidate(hits)
            updateStableHeightCandidate(candidateHeight)
        }
        if (mAnchorList.size < 3) {
            resetHeightDetectionState()
        }
        updateHeightDetectionUI()
        updateRayUi(rayHasTarget)
    }

    /**
     * 设置UI监听
     */
    private fun setUiListener() {
        btReturn.setOnClickListener {
            backPoint()
        }
        btSure.setOnClickListener {
            //保存
            if (mWidth > 0 && mLength > 0 && mHeight > 0) {
                val intent = Intent()
                intent.putExtra("width", mWidth)
                intent.putExtra("length", mLength)
                intent.putExtra("height", mHeight)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "请先测量尺寸", Toast.LENGTH_SHORT).show()
            }
        }
        btCaptureHeight.setOnClickListener {
            // Capture the detected height
            if (mIsHeightDetected && mDetectedHeight != null) {
                val heightCm = mDetectedHeight!! * 100
                skHeightControl.progress = (heightCm * SEEKBAR_TO_HEIGHT_FACTOR).toInt()
                Toast.makeText(this, "已捕获高度: ${formatHeight(mDetectedHeight!!)} CM", Toast.LENGTH_SHORT).show()
            }
        }

        // SeekBar 监听器
        skHeightControl.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val upDistance = progress.toFloat() / SEEKBAR_TO_HEIGHT_FACTOR
                mHeight = upDistance / 100.0
                mHeightNodeTextView?.text = "${formatHeight(mHeight)}CM"
                tvHeightValue.text = String.format("%.1f", upDistance)
                updateSizeUI()
                mHeightAnchorNode?.localScale = Vector3(0.1f, upDistance / 10f, 0.1f)
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
            }
        })
    }

    /**
     * 修改尺寸UI
     */
    @SuppressLint("SetTextI18n")
    private fun updateSizeUI() {
        tvWidth.text = "宽：${String.format("%.1f", mWidth * 100)}CM"
        tvLength.text = "长：${String.format("%.1f", mLength * 100)}CM"
        tvHeight.text = "高：${formatHeight(mHeight)}CM"
        if (mAnchorList.size == 0) {
            btReturn.visibility = View.INVISIBLE
        } else {
            btReturn.visibility = View.VISIBLE
        }
        if (mAnchorList.size >= 3) {
            btSure.visibility = View.VISIBLE
            llHeightControlContainer.visibility = View.VISIBLE
        } else {
            btSure.visibility = View.INVISIBLE
            llHeightControlContainer.visibility = View.INVISIBLE
        }
        when (mAnchorList.size) {
            0 -> {
                Glide.with(this).load(R.mipmap.ic_box_step).into(ivBoxStep)
                tvBoxStepHint.text = "瞄准第一个底角"
            }

            1 -> {
                Glide.with(this).load(R.mipmap.ic_box_step_2).into(ivBoxStep)
                tvBoxStepHint.text = "瞄准第二个底角"
            }

            2 -> {
                Glide.with(this).load(R.mipmap.ic_box_step_3).into(ivBoxStep)
                tvBoxStepHint.text = "瞄准第三个底角"
            }

            3 -> {
                Glide.with(this).load(R.mipmap.ic_box_step_4).into(ivBoxStep)
                tvBoxStepHint.text = "定位箱体高度"
            }

        }
    }

    /**
     * 更新高度检测UI反馈
     */
    private fun updateHeightDetectionUI() {
        if (mAnchorList.size < 3) {
            if (mLastHeightUiState == "hidden") {
                return
            }
            mLastHeightUiState = "hidden"
            tvDetectedHeight.visibility = View.GONE
            btCaptureHeight.visibility = View.GONE
            tvHeightHint.visibility = View.GONE
            return
        }

        val currentState = if (mIsHeightDetected && mDetectedHeight != null) {
            "detected:${formatHeight(mDetectedHeight!!)}"
        } else {
            "searching"
        }
        if (mLastHeightUiState == currentState) {
            return
        }
        mLastHeightUiState = currentState

        tvDetectedHeight.visibility = View.VISIBLE
        btCaptureHeight.visibility = View.VISIBLE
        tvHeightHint.visibility = View.VISIBLE

        if (mIsHeightDetected && mDetectedHeight != null) {
            val heightCm = formatHeight(mDetectedHeight!!)
            tvDetectedHeight.text = "检测高度: $heightCm CM"
            tvHeightHint.text = "已检测到高度 ${heightCm} CM，点击捕获"
            btCaptureHeight.isEnabled = true
            btCaptureHeight.alpha = 1.0f
            btCaptureHeight.shapeDrawableBuilder.apply {
                solidColor = Color.parseColor("#66de7b")
                intoBackground()
            }
        } else {
            // No valid height detected
            tvDetectedHeight.text = "检测高度: -- CM"
            tvHeightHint.text = "请对准箱体顶部或使用滑动条"
            btCaptureHeight.isEnabled = false
            btCaptureHeight.alpha = 0.5f
            btCaptureHeight.shapeDrawableBuilder.apply {
                solidColor = Color.parseColor("#8b8b8c")
                intoBackground()
            }
        }
    }

    private fun updateDepthRendering(sceneView: ArSceneView, enabled: Boolean) {
        sceneView.cameraStream.depthOcclusionMode = if (enabled) {
            CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_ENABLED
        } else {
            CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_DISABLED
        }
    }

    private fun getCenterHits(frame: Frame): List<HitResult> {
        return frame.hitTest(mScreenSize.x / 2f, mScreenSize.y / 2f)
    }

    private fun isValidHitDistance(hit: HitResult): Boolean {
        return hit.distance >= MIN_HIT_DISTANCE_METERS && hit.distance <= MAX_HIT_DISTANCE_METERS
    }

    private fun isValidPlaneHit(hit: HitResult, plane: Plane): Boolean {
        return plane.trackingState == TrackingState.TRACKING &&
            isValidHitDistance(hit) &&
            plane.isPoseInPolygon(hit.hitPose)
    }

    private fun getHeightEdgeThreshold(): Float {
        val referenceEdge = listOf(mWidth, mLength)
            .filter { it > 0.0 }
            .minOrNull()
            ?.toFloat()
            ?: 0.20f
        return (referenceEdge * HEIGHT_EDGE_DISTANCE_RATIO)
            .coerceIn(MIN_HEIGHT_EDGE_DISTANCE_METERS, MAX_HEIGHT_EDGE_DISTANCE_METERS)
    }

    private fun getTopSurfaceProjectionMargin(): Float {
        val referenceEdge = listOf(mWidth, mLength)
            .filter { it > 0.0 }
            .minOrNull()
            ?.toFloat()
            ?: 0.20f
        return (referenceEdge * TOP_SURFACE_MARGIN_RATIO)
            .coerceIn(MIN_TOP_SURFACE_MARGIN_METERS, MAX_TOP_SURFACE_MARGIN_METERS)
    }

    private fun isWithinTopSurfaceProjection(candidatePose: Pose): Boolean {
        if (mAnchorList.size < 3) {
            return false
        }
        val cornerPose = mAnchorList[1].anchor.pose
        val widthPose = mAnchorList[0].anchor.pose
        val lengthPose = mAnchorList[2].anchor.pose

        val widthVector = Vector3(
            widthPose.tx() - cornerPose.tx(),
            0.0f,
            widthPose.tz() - cornerPose.tz()
        )
        val lengthVector = Vector3(
            lengthPose.tx() - cornerPose.tx(),
            0.0f,
            lengthPose.tz() - cornerPose.tz()
        )
        val candidateVector = Vector3(
            candidatePose.tx() - cornerPose.tx(),
            0.0f,
            candidatePose.tz() - cornerPose.tz()
        )

        val widthLength = widthVector.length()
        val lengthLength = lengthVector.length()
        if (widthLength <= 0.001f || lengthLength <= 0.001f) {
            return false
        }

        val widthOffset = Vector3.dot(candidateVector, widthVector.normalized())
        val lengthOffset = Vector3.dot(candidateVector, lengthVector.normalized())
        val margin = getTopSurfaceProjectionMargin()

        return widthOffset in -margin..(widthLength + margin) &&
            lengthOffset in -margin..(lengthLength + margin)
    }

    private fun findBestHeightCandidate(hits: List<HitResult>): Double? {
        var pointCandidate: Double? = null
        hits.forEach { hit ->
            val trackable = hit.trackable
            when (trackable) {
                is DepthPoint -> {
                    val candidate = getHeightCandidate(hit)
                    if (candidate != null) {
                        return candidate
                    }
                }

                is ArPoint -> {
                    if (pointCandidate == null) {
                        pointCandidate = getHeightCandidate(hit)
                    }
                }
            }
        }
        return pointCandidate
    }

    private fun updateStableHeightCandidate(candidateHeight: Double?): Boolean {
        if (candidateHeight == null) {
            if (mHeightCandidate == null && mDetectedHeight == null) {
                resetHeightDetectionState()
                return false
            }
            mHeightCandidateMissingFrames += 1
            if (mHeightCandidateMissingFrames >= HEIGHT_MISSING_FRAME_TOLERANCE) {
                resetHeightDetectionState()
                return false
            }
            return true
        }
        mHeightCandidateMissingFrames = 0
        val currentCandidate = mHeightCandidate
        if (currentCandidate == null || abs(currentCandidate - candidateHeight) > HEIGHT_STABLE_TOLERANCE_METERS) {
            mHeightCandidate = candidateHeight
            mHeightCandidateStableFrames = 1
            mDetectedHeight = null
            mIsHeightDetected = false
            return true
        }
        val nextFrameCount = mHeightCandidateStableFrames + 1
        mHeightCandidate =
            ((currentCandidate * mHeightCandidateStableFrames) + candidateHeight) / nextFrameCount
        mHeightCandidateStableFrames = nextFrameCount
        if (mHeightCandidateStableFrames >= HEIGHT_STABLE_FRAME_COUNT) {
            mDetectedHeight = mHeightCandidate
            mIsHeightDetected = true
        }
        return true
    }

    private fun resetHeightDetectionState() {
        mDetectedHeight = null
        mIsHeightDetected = false
        mHeightCandidate = null
        mHeightCandidateStableFrames = 0
        mHeightCandidateMissingFrames = 0
    }

    private fun updateRayUi(hasTarget: Boolean) {
        if (mLastRayHasTarget == hasTarget) {
            return
        }
        mLastRayHasTarget = hasTarget
        if (hasTarget) {
            ivFpsTarget.setColorFilter(Color.parseColor("#66de7b"))
            btRay.shapeDrawableBuilder.apply {
                solidColor = Color.parseColor("#fcfcfc")
                intoBackground()
            }
            ivRayImg.setColorFilter(Color.parseColor("#8b8b8c"))
        } else {
            ivFpsTarget.setColorFilter(Color.parseColor("#f1f3f3"))
            btRay.shapeDrawableBuilder.apply {
                solidColor = Color.parseColor("#8b8b8c")
                intoBackground()
            }
            ivRayImg.setColorFilter(Color.parseColor("#fcfcfc"))
        }
    }

    /**
     * 格式化高度值(米转为厘米，保留一位小数)
     */
    private fun formatHeight(heightInMeters: Double): String {
        return String.format("%.1f", heightInMeters * 100)
    }

}
