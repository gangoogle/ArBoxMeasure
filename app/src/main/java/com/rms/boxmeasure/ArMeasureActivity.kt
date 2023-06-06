package com.rms.boxmeasure

import abak.tr.com.boxedverticalseekbar.BoxedVertical
import abak.tr.com.boxedverticalseekbar.BoxedVertical.OnValuesChangeListener
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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Trackable
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
import com.rms.boxmeasure.databinding.ActivityArMeasureBinding

/**
 * AR 测量首页
 */
class ArMeasureActivity : AppCompatActivity() {

    /**
     * 启用深度信息
     */
    private val OPEN_DEPTH = false


    private lateinit var mArFragment: ArFragment
    private lateinit var mBinding: ActivityArMeasureBinding

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

    override fun onBackPressed() {
        if (mAnchorList.size == 0) {
            finish()
        } else {
            backPoint()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView<ActivityArMeasureBinding>(
            this, R.layout.activity_ar_measure
        )
        mArFragment = (supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment)
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
                mAnchorList.clear()
                mLineNodeArray.clear()
                mSphereNodeArray.clear()
                mStartNodeArray.clear()
                mEndNodeArray.clear()
                mScene.removeChild(mStartNode)
            } else if (mAnchorList.size == 2) {
                mAnchorList.removeAt(mAnchorList.size - 1)
                val index = mStartNodeArray.size - 1
                mSphereNodeArray[index].removeChild(mLineNodeArray.removeAt(index))
                mEndNodeArray[index].removeChild(mSphereNodeArray.removeAt(index + 1))
                mScene.removeChild(mStartNodeArray.removeAt(index))
                mScene.removeChild(mEndNodeArray.removeAt(index))
            } else if (mAnchorList.size == 3) {
                mAnchorList.removeAt(mAnchorList.size - 1)
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
        mBinding.skHeightControl.value = 0
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
            mLineNodeArray.add(Node().apply {
                setParent(firstAnchorNode)
                renderable = lineMode
                worldPosition = Vector3.add(firstWorldPosition, secondWorldPosition).scaled(0.5f)
                worldRotation = rotationFromAToB
            })
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
                //启动深度信息
                if (OPEN_DEPTH) {
                    arSceneView.cameraStream.depthOcclusionMode =
                        CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_ENABLED
                }
            }
            setOnSessionConfigurationListener { session, config ->
                config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED)
                config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL)
                config.setFocusMode(Config.FocusMode.AUTO)
                //depth深度信息
                if (OPEN_DEPTH) {
                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        config.depthMode = Config.DepthMode.AUTOMATIC
                    }
                }
            }

        }
    }


    /**
     * 计算三个向量的夹角
     */
    private fun isAngleCloseTo90Degrees(poseC: Pose, poseA: Pose, poseB: Pose): Boolean {
        // 计算向量CB和向量AB的夹角
        var vectorCB =
            Vector3(poseC.tx() - poseB.tx(), poseC.ty() - poseB.ty(), poseC.tz() - poseB.tz())
        var vectorAB =
            Vector3(poseA.tx() - poseB.tx(), poseA.ty() - poseB.ty(), poseA.tz() - poseB.tz())

        // 规范化向量
        vectorCB = vectorCB.normalized()
        vectorAB = vectorAB.normalized()

        // 计算夹角的余弦值
        val dotProduct = Vector3.dot(vectorAB, vectorCB)
        val angleCos = Math.abs(dotProduct)
        // 判断夹角是否接近90度（余弦值在一个范围内）
        val thresholdCos = Math.cos(Math.toRadians(5.0)).toFloat() // 5度范围内认为接近90度
        if (angleCos >= thresholdCos) {
            Toast.makeText(this, "角度->t$angleCos", Toast.LENGTH_SHORT).show()
        }
        return angleCos >= thresholdCos
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
        val anchorInfoBean = AnchorInfoBean("", hitResult.createAnchor(), 0.0)
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
                        mHeightNodeTextView?.text = "${String.format("%.1f", mHeight * 100)}CM"
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
            mStartNode = AnchorNode(hitResult.createAnchor())
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
        var ray = mScene.camera!!.screenPointToRay(
            mScreenSize.x / 2f, mScreenSize.y / 2f
        )
        Log.d(TAG, "ray: $ray")
        val test = mSceneView.arFrame?.hitTest(((mScreenSize.x / 2f)), (mScreenSize.y / 2f))
        Log.d(TAG, "sendHit:test ${test?.size}")
        run foo@{
            test?.forEach { hit ->
                val trackable: Trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.getHitPose())) {
                    Log.d(TAG, "sendHit:plane ${hit.hitPose}")
                    if (mAnchorList.size < 3) {
                        onTap(hit, trackable)
                        return@foo
                    }
                } else if (trackable is com.google.ar.core.Point) {
                    if (mAnchorList.size == 3 && !OPEN_DEPTH) {
                        checkHeightPoint(hit) { isMatch, dy, height ->
                            if (isMatch) {
                                if (height > dy) {
                                    mBinding.skHeightControl.value = (height * 100 * 2).toInt()
                                } else {
                                    mBinding.skHeightControl.value = (dy * 100 * 2).toInt()
                                }
                            }
                        }
                    }
                } else if (trackable is DepthPoint) {
                    if (mAnchorList.size == 3 && OPEN_DEPTH) {
                        checkHeightPoint(hit) { isMatch, dy, height ->
                            if (isMatch) {
                                if (height > dy) {
                                    mBinding.skHeightControl.value = (height * 100 * 2).toInt()
                                } else {
                                    mBinding.skHeightControl.value = (dy * 100 * 2).toInt()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查高度点
     */
    private fun checkHeightPoint(
        hit: HitResult, callBack: (isMatch: Boolean, dy: Double, height: Double) -> Unit
    ) {
        //通过特征点寻找高度
        Log.d(TAG, "sendHit:point ${hit.hitPose}")
        val startPose = hit.hitPose
        val endPose = mAnchorList[1].anchor.pose
        val dx = startPose.tx() - endPose.tx()
        val dy = startPose.ty() - endPose.ty()
        val dz = startPose.tz() - endPose.tz()
        Log.d(TAG, "sendHit:dx $dx dy $dy dz $dz")
        val length = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())
        Log.d(TAG, "sendHit:length  $length")
        if (length == 0.0 || dy == 0f) {
            callBack.invoke(false, 0.0, 0.0)
            return
        }
        if ((dx <= 0.05f && dx >= -0.05f) && (dz < 0.05f && dz >= -0.05f)) {
            callBack.invoke(true, dy.toDouble(), length)
        } else {
            callBack.invoke(false, 0.0, 0.0)
        }
    }


    /**
     * 设置监听
     */
    private fun setArListener() {
        //发射射线
        mBinding.btRay.setOnClickListener {
            sendHit()
        }
        mBinding.btRay.postDelayed({
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
        val hasPlane: Boolean = mSceneView.hasTrackedPlane()
        if (hasPlane) {
            var ray = mScene.camera!!.screenPointToRay(
                mScreenSize.x / 2f, mScreenSize.y / 2f
            )
            val test = mSceneView.arFrame?.hitTest(
                ((mScreenSize.x / 2f)), (mScreenSize.y / 2f)
            )
            var rayHasPlane = false
            test?.forEach { hit ->
                val trackable: Trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.getHitPose())) {
                    if (mAnchorList.size < 3) {
                        //如果锚点小于3，就寻找平面
                        rayHasPlane = true
                    }
                } else if (trackable is com.google.ar.core.Point) {
                    //如果已经有三个锚点了，就寻找特征点
                    if (mAnchorList.size == 3 && !OPEN_DEPTH) {
                        checkHeightPoint(hit) { isMatch, dy, height ->
                            if (isMatch) {
                                rayHasPlane = true
                            }
                        }
                    }
                } else if (trackable is DepthPoint) {
                    if (mAnchorList.size == 3 && OPEN_DEPTH) {
                        checkHeightPoint(hit) { isMatch, dy, height ->
                            if (isMatch) {
                                rayHasPlane = true
                            }
                        }
                    }
                }
            }
            if (rayHasPlane) {
                mBinding.ivFpsTarget.setColorFilter(Color.parseColor("#66de7b"))
                mBinding.btRay.shapeDrawableBuilder.apply {
                    solidColor = Color.parseColor("#fcfcfc")
                    intoBackground()
                }
                mBinding.ivRayImg.setColorFilter(Color.parseColor("#8b8b8c"))
            } else {
                mBinding.ivFpsTarget.setColorFilter(Color.parseColor("#f1f3f3"))
                mBinding.btRay.shapeDrawableBuilder.apply {
                    solidColor = Color.parseColor("#8b8b8c")
                    intoBackground()
                }
                mBinding.ivRayImg.setColorFilter(Color.parseColor("#fcfcfc"))
            }
        } else {
            mBinding.ivFpsTarget.setColorFilter(Color.parseColor("#f1f3f3"))
            mBinding.btRay.shapeDrawableBuilder.apply {
                solidColor = Color.parseColor("#8b8b8c")
                intoBackground()
            }
            mBinding.ivRayImg.setColorFilter(Color.parseColor("#fcfcfc"))
        }
    }

    /**
     * 设置UI监听
     */
    private fun setUiListener() {
        mBinding.btReturn.setOnClickListener {
            backPoint()
        }
        mBinding.btSure.setOnClickListener {
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
        mBinding.skHeightControl.apply {
            setOnBoxedPointsChangeListener(object : OnValuesChangeListener {
                override fun onPointsChanged(boxedPoints: BoxedVertical?, points: Int) {
                    val upDistance = (points).toFloat() / 2f
                    mHeight = upDistance / 100.0
                    mHeightNodeTextView?.text = "${String.format("%.1f", mHeight * 100)}CM"
                    updateSizeUI()
                    mHeightAnchorNode?.localScale = Vector3(0.1f, upDistance / 10f, 0.1f)
                }

                override fun onStartTrackingTouch(boxedPoints: BoxedVertical?) {
                }

                override fun onStopTrackingTouch(boxedPoints: BoxedVertical?) {
                }
            })
        }
    }

    /**
     * 修改尺寸UI
     */
    @SuppressLint("SetTextI18n")
    private fun updateSizeUI() {
        mBinding.tvWidth.text = "宽：${String.format("%.1f", mWidth * 100)}CM"
        mBinding.tvLength.text = "长：${String.format("%.1f", mLength * 100)}CM"
        mBinding.tvHeight.text = "高：${String.format("%.1f", mHeight * 100)}CM"
        if (mAnchorList.size == 0) {
            mBinding.btReturn.visibility = View.INVISIBLE
        } else {
            mBinding.btReturn.visibility = View.VISIBLE
        }
        if (mAnchorList.size >= 3) {
            mBinding.btSure.visibility = View.VISIBLE
            mBinding.skHeightControl.visibility = View.VISIBLE
        } else {
            mBinding.btSure.visibility = View.INVISIBLE
            mBinding.skHeightControl.visibility = View.INVISIBLE
        }
        when (mAnchorList.size) {
            0 -> {
                Glide.with(this).load(R.mipmap.ic_box_step).into(mBinding.ivBoxStep)
                mBinding.tvBoxStepHint.text = "瞄准第一个底角"
            }

            1 -> {
                Glide.with(this).load(R.mipmap.ic_box_step_2).into(mBinding.ivBoxStep)
                mBinding.tvBoxStepHint.text = "瞄准第二个底角"
            }

            2 -> {
                Glide.with(this).load(R.mipmap.ic_box_step_3).into(mBinding.ivBoxStep)
                mBinding.tvBoxStepHint.text = "瞄准第三个底角"
            }

            3 -> {
                Glide.with(this).load(R.mipmap.ic_box_step_4).into(mBinding.ivBoxStep)
                mBinding.tvBoxStepHint.text = "定位箱体高度"
            }

        }
    }

}
