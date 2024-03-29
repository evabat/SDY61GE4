/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.engine.CoreSamples.app.ModelTargetsTrained;

import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;

import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.DeviceTrackableResult;
import com.vuforia.GuideView;
import com.vuforia.Image;
import com.vuforia.Matrix44F;
import com.vuforia.ModelTargetResult;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.ModelTarget;
import com.vuforia.TrackableResultList;
import com.vuforia.Vec2F;
import com.vuforia.Vec4F;
import com.vuforia.Vuforia;
import com.vuforia.engine.SampleApplication.SampleAppRenderer;
import com.vuforia.engine.SampleApplication.SampleAppRendererControl;
import com.vuforia.engine.SampleApplication.SampleApplicationSession;
import com.vuforia.engine.SampleApplication.SampleRendererBase;
import com.vuforia.engine.SampleApplication.utils.LightingShaders;
import com.vuforia.engine.SampleApplication.utils.Plane;
import com.vuforia.engine.SampleApplication.utils.SampleApplicationV3DModel;
import com.vuforia.engine.SampleApplication.utils.SampleMath;
import com.vuforia.engine.SampleApplication.utils.SampleUtils;
import com.vuforia.engine.SampleApplication.utils.Texture;
import com.vuforia.engine.SampleApplication.utils.TextureColorShaders;

import java.lang.ref.WeakReference;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


// The renderer class for the ModelTargetsTrained sample.
public class ModelTargetTrainedRenderer extends SampleRendererBase implements SampleAppRendererControl
{
    private static final String LOGTAG = "ModelTrainedRenderer";

    private ModelTargetsTrained mActivity;
    
    private int planeShaderProgramID;
    private int planeVertexHandle;
    private int planeTextureCoordHandle;
    private int planeMvpMatrixHandle;
    private int planeTexSampler2DHandle;
    private int planeColorHandle;

    private int guideViewHandle;
    private Vec2F mGuideViewScale;

    private int shaderProgramID;
    private int vertexHandle;
    private int mvpMatrixHandle;
    private int mvMatrixHandle;
    private int normalHandle;
    private int textureCoordHandle;
    private int texSampler2DHandle;
    private int normalMatrixHandle;
    private int lightPositionHandle;
    private int lightColorHandle;

    private Plane mPlane;
    private SampleApplicationV3DModel mLanderModel;
    private SampleApplicationV3DModel mKTMModel;

    private boolean mAreModelsLoaded = false;
    private volatile boolean mUpdateGuideView;

    private boolean mIsTargetCurrentlyTracked = false;

    private ModelTarget mCurrentModelTarget;

    // An object used to synchronize Guide Views updates, since it is possible the render thread
    // uses an outdated target/index while it is being updated by the Target Finder
    private final Object mGuideViewUpdate = new Object();


    public enum GuideViewStatus {
        PASSIVE,
        RECOGNIZED,
        SNAPPED
    }

    public enum GuideViewModels {
        LANDER,
        BIKE,
        UNKNOWN
    }

    ModelTargetTrainedRenderer(ModelTargetsTrained activity, SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;
        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR,
                vuforiaAppSession.getVideoMode(), false, 0.01f , 5f);

        guideViewHandle = -1;
        mUpdateGuideView = true;
    }

    
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        super.onSurfaceChanged(gl, width, height);

        // Invalidate guide view handle so it is regenerated onSurfaceChanged()
        guideViewHandle = -1;
    }


    public void setActive(boolean active)
    {
        mSampleAppRenderer.setActive(active);
    }


    // Function for initializing the renderer.
    @Override
    public void initRendering()
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);
        
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        
        planeShaderProgramID = SampleUtils.createProgramFromShaderSrc(
            TextureColorShaders.TEXTURE_COLOR_VERTEX_SHADER,
            TextureColorShaders.TEXTURE_COLOR_FRAGMENT_SHADER);

        mPlane = new Plane();

        if (planeShaderProgramID > 0)
        {
            planeVertexHandle = GLES20.glGetAttribLocation(planeShaderProgramID,
                    "vertexPosition");
            planeTextureCoordHandle = GLES20.glGetAttribLocation(planeShaderProgramID,
                    "vertexTexCoord");
            planeMvpMatrixHandle = GLES20.glGetUniformLocation(planeShaderProgramID,
                    "modelViewProjectionMatrix");
            planeTexSampler2DHandle = GLES20.glGetUniformLocation(planeShaderProgramID,
                    "texSampler2D");
            planeColorHandle = GLES20.glGetUniformLocation(planeShaderProgramID,
                    "uniformColor");

        }
        else
        {
            Log.e(LOGTAG, "Could not init plane shader");
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                LightingShaders.LIGHTING_VERTEX_SHADER,
                LightingShaders.LIGHTING_FRAGMENT_SHADER);

        if (shaderProgramID > 0)
        {
            vertexHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexPosition");
            normalHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexNormal");
            textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord");
            mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_mvpMatrix");
            mvMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_mvMatrix");
            normalMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_normalMatrix");
            lightPositionHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_lightPos");
            lightColorHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_lightColor");
            texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D");

        }
        else
        {
            Log.e(LOGTAG, "Could not init lighting shader");
        }

        if(!mAreModelsLoaded)
        {
            LoadModelTask modelTask = new LoadModelTask(this);
            modelTask.execute();
        }

        mGuideViewScale = new Vec2F(1.0f, 1.0f);
        mUpdateGuideView = true;
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
    }


    private static class LoadModelTask extends AsyncTask<Void, Integer, Boolean>
    {
        private final WeakReference<ModelTargetTrainedRenderer> mRendererRef;

        LoadModelTask(ModelTargetTrainedRenderer mtRenderer)
        {
            mRendererRef = new WeakReference<>(mtRenderer);
        }

        protected Boolean doInBackground(Void... params)
        {
            ModelTargetTrainedRenderer renderer = mRendererRef.get();
            ModelTargetsTrained activity = renderer.mActivity;

            renderer.mLanderModel = new SampleApplicationV3DModel(false);
            boolean landerLoaded = renderer.mLanderModel.loadModel(activity.getResources().getAssets(), "Lander.v3d");

            renderer.mKTMModel = new SampleApplicationV3DModel(false);
            boolean ktmLoaded = renderer.mKTMModel.loadModel(activity.getResources().getAssets(), "ModelTargetsTrained/KTM.v3d");

            renderer.mAreModelsLoaded = landerLoaded && ktmLoaded;

            return renderer.mAreModelsLoaded;
        }

        protected void onPostExecute(Boolean result)
        {
            ModelTargetsTrained activity = mRendererRef.get().mActivity;

            // Hide the Loading Dialog
            activity.showProgressIndicator(false);
            activity.showInitialUIState();
        }
    }

    public void onNewModelTarget(ModelTarget target)
    {
        // We use synchronize the current model target update to prevent
        // the case where we use an incorrect guide view in renderFrame
        synchronized (mGuideViewUpdate)
        {
            mCurrentModelTarget = target;
            mUpdateGuideView = true;
        }
    }

    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground(state);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glCullFace(GLES20.GL_BACK);

        if (state.getDeviceTrackableResult() != null)
        {
            int statusInfo = state.getDeviceTrackableResult().getStatusInfo();
            mActivity.checkForRelocalization(statusInfo);
        }

        ModelTarget currentModelTarget;
        GuideView activeGuideView;
        Image textureImage;
        boolean updateGuideViewTexture;

        // If the active guide view is not being updated then proceed
        synchronized (mGuideViewUpdate)
        {
            currentModelTarget = mCurrentModelTarget;
            updateGuideViewTexture = guideViewHandle < 0 || mUpdateGuideView;
            mUpdateGuideView = false;
        }

        // If a new guide view has to be rendered due to a target finder update, we update the guide view texture
        if (updateGuideViewTexture)
        {
            if (currentModelTarget != null && !currentModelTarget.getGuideViews().empty())
            {
                int activeGuideViewIndex = currentModelTarget.getActiveGuideViewIndex();
                activeGuideView = currentModelTarget.getGuideViews().at(activeGuideViewIndex);

                // If the active guide view image is present, we update the texture used to render the guide view
                if (activeGuideView != null && activeGuideView.getImage() != null
                        && state.getCameraCalibration() != null)
                {
                    textureImage = activeGuideView.getImage();
                    updateGuideViewTexture(textureImage, state);
                }
            }
        }

        boolean foundModelTargetResult = false;
        boolean isKTMCurrentModel = true;
        TrackableResult currentTrackableResult = null;

        TrackableResultList trackableResultList = state.getTrackableResults();

        // Determine if target is currently being tracked
        setIsTargetCurrentlyTracked(trackableResultList);

        // Iterate through trackable results and render any augmentations
        for (TrackableResult trackableResult : trackableResultList)
        {
            // We set the UI status to snapped if the target is being tracked and we use the trackable name to know which UI image to update
            if (trackableResult.isOfType(ModelTargetResult.getClassType()) && trackableResult.getStatus() == TrackableResult.STATUS.TRACKED)
            {
                foundModelTargetResult = true;

                GuideViewModels guideViewUIToUpdate = GuideViewModels.UNKNOWN;
                currentTrackableResult = trackableResult;

                String trackableName = currentTrackableResult.getTrackable().getName();
                if (trackableName.contains("MarsLander"))
                {
                    guideViewUIToUpdate = GuideViewModels.LANDER;
                    isKTMCurrentModel = false;
                }
                else if (trackableName.contains("Motorcycle"))
                {
                    guideViewUIToUpdate = GuideViewModels.BIKE;
                }

                if (guideViewUIToUpdate != GuideViewModels.UNKNOWN)
                {
                    mActivity.setStatusImage(guideViewUIToUpdate, GuideViewStatus.SNAPPED);
                }
                else
                {
                    Log.i(LOGTAG, "No matching guide view for current trackable result");
                }
            }
        }

        // If no model is tracked, we render the guide view
        if (!foundModelTargetResult && currentModelTarget != null && guideViewHandle >= 0)
        {
            Matrix44F modelMatrix = SampleMath.Matrix44FIdentity();

            Vec4F color = new Vec4F(1.0f, 1.0f, 1.0f, 1.0f);

            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_CULL_FACE);

            float orthoProjMatrix[] = new float[16];
            Matrix.orthoM(orthoProjMatrix, 0, -0.5f, 0.5f, -0.5f, 0.5f, 0, 1);
            Matrix44F orthoProjMatrix44 = new Matrix44F();
            orthoProjMatrix44.setData(orthoProjMatrix);

            renderPlaneTextured(orthoProjMatrix44, modelMatrix, mGuideViewScale, color, guideViewHandle);

            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            SampleUtils.checkGLError("Render Frame, no model target tracked");
        }
        else if (foundModelTargetResult)
        {
            int modelTextureIndex = isKTMCurrentModel ? 1 : 0;

            SampleApplicationV3DModel currentModel = isKTMCurrentModel ? mKTMModel : mLanderModel;

            // Set the device pose matrix as identity
            Matrix44F devicePoseMatrix = SampleMath.Matrix44FIdentity();
            Matrix44F modelMatrix;
            // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
            if (state.getDeviceTrackableResult() != null
                    && state.getDeviceTrackableResult().getStatus() != TrackableResult.STATUS.NO_POSE)
            {
                Matrix44F tempDevicePoseMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose());

                // We transpose here because Matrix44FInverse returns a transposed matrix
                devicePoseMatrix = SampleMath.Matrix44FTranspose(SampleMath.Matrix44FInverse(tempDevicePoseMatrix));
            }

            modelMatrix = Tool.convertPose2GLMatrix(currentTrackableResult.getPose());

            // deal with the modelview and projection matrices
            float[] modelViewMatrix = new float[16];
            float[] modelViewProjection = new float[16];

            // Combine device pose (view matrix) with model matrix
            Matrix.multiplyMM(modelViewMatrix, 0, devicePoseMatrix.getData(), 0, modelMatrix.getData(), 0);
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

            // activate the shader program and bind the vertex/normal/tex coords
            GLES20.glUseProgram(shaderProgramID);

            GLES20.glDisable(GLES20.GL_CULL_FACE);

            // Render MT augmentation
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                    false, 0, currentModel.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, currentModel.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, currentModel.getTexCoords());

            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                    mTextures.get(modelTextureIndex).mTextureID[0]);

            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                    modelViewProjection, 0);
            GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, modelViewMatrix, 0);

            float[] inverseMatrix = new float[16];
            Matrix.invertM(inverseMatrix, 0, modelViewMatrix, 0);

            float normalMatrix[] = new float[16];
            Matrix.transposeM(normalMatrix, 0, inverseMatrix, 0);

            GLES20.glUniformMatrix4fv(normalMatrixHandle, 1, false, normalMatrix, 0);

            GLES20.glUniform4f(lightPositionHandle, 0.2f, -1.0f, 0.5f, -1.0f);
            GLES20.glUniform4f(lightColorHandle, 0.5f, 0.5f, 0.5f, 1.0f);

            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
                    currentModel.getNumObjectVertex());

            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

            SampleUtils.checkGLError("Render Frame");

        }

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }

    private void renderPlaneTextured(Matrix44F projectionMatrix, Matrix44F modelViewMatrix, Vec2F scale, Vec4F colour, int textureHandle)
    {
        float modelViewProjectionMatrix[] = projectionMatrix.getData();
        float scaledModelMatrixArray[] = modelViewMatrix.getData();

        Matrix.scaleM(scaledModelMatrixArray, 0, scale.getData()[0], scale.getData()[1], 1.0f);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix.getData(), 0, scaledModelMatrixArray, 0);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        GLES20.glEnableVertexAttribArray(planeVertexHandle);
        GLES20.glVertexAttribPointer(planeVertexHandle, 3, GLES20.GL_FLOAT, false, 0, mPlane.getVertices());

        GLES20.glEnableVertexAttribArray(planeTextureCoordHandle);
        GLES20.glVertexAttribPointer(planeTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mPlane.getTexCoords());

        GLES20.glUseProgram(planeShaderProgramID);
        GLES20.glUniformMatrix4fv(planeMvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0);
        GLES20.glUniform4f(planeColorHandle, colour.getData()[0], colour.getData()[1], colour.getData()[2], colour.getData()[3]);
        GLES20.glUniform1i(planeTexSampler2DHandle, 0);

        // Draw
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, Plane.NUM_PLANE_INDEX, GLES20.GL_UNSIGNED_SHORT, mPlane.getIndices());

        // disable input data structures

        GLES20.glDisableVertexAttribArray(planeTextureCoordHandle);
        GLES20.glDisableVertexAttribArray(planeVertexHandle);
        GLES20.glUseProgram(0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private void updateGuideViewTexture(Image textureImage, State state)
    {
        if (textureImage == null)
        {
            Log.e(LOGTAG, "Guide view image null");
            return;
        }

        if (guideViewHandle > 0)
        {
            SampleUtils.substituteTextureImage(guideViewHandle, textureImage);
        }
        else
        {
            guideViewHandle = SampleUtils.createTexture(textureImage);
        }

        float guideViewAspectRatio = (float)textureImage.getWidth() / textureImage.getHeight();
        Point size = new Point();
        mActivity.getWindowManager().getDefaultDisplay().getSize(size);

        float cameraAspectRatio = (float)size.x / size.y;

        // doing this calculation in world space, at an assumed camera near plane distance of 0.01f;
        // this is also what the Unity rendering code does
        float planeDistance = 0.01f;
        float fieldOfView = state.getCameraCalibration().getFieldOfViewRads().getData()[1];
        float nearPlaneHeight = (float)(2.0f * planeDistance * Math.tan(fieldOfView * 0.5f));
        float nearPlaneWidth = nearPlaneHeight * cameraAspectRatio;

        float planeWidth;
        float planeHeight;
        
        if(guideViewAspectRatio >= 1.0f && cameraAspectRatio >= 1.0f) // guideview landscape, camera landscape
        {
            // scale so that the long side of the camera (width)
            // is the same length as guideview width
            planeWidth = nearPlaneWidth;
            planeHeight = planeWidth / guideViewAspectRatio;
        }
        
        else if(guideViewAspectRatio < 1.0f && cameraAspectRatio < 1.0f) // guideview portrait, camera portrait
        {
            // scale so that the long side of the camera (height)
            // is the same length as guideview height
            planeHeight = nearPlaneHeight;
            planeWidth = planeHeight * guideViewAspectRatio;
        }
        else if (cameraAspectRatio < 1.0f) // guideview landscape, camera portrait
        {
            // scale so that the long side of the camera (height)
            // is the same length as guideview width
            planeWidth = nearPlaneHeight;
            planeHeight = planeWidth / guideViewAspectRatio;
        }
        else // guideview portrait, camera landscape
        {
            // scale so that the long side of the camera (width)
            // is the same length as guideview height
            planeHeight = nearPlaneWidth;
            planeWidth = planeHeight * guideViewAspectRatio;
        }

        // normalize world space plane sizes into view space again
        mGuideViewScale = new Vec2F(planeWidth / nearPlaneWidth, -planeHeight / nearPlaneHeight);
    }

    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }

    boolean areModelsLoaded()
    {
        return mAreModelsLoaded;
    }


    private void setIsTargetCurrentlyTracked(TrackableResultList trackableResultList)
    {
        for(TrackableResult result : trackableResultList)
        {
            // Check the tracking status for result types
            // other than DeviceTrackableResult. ie: ModelTargetResult
            if (!result.isOfType(DeviceTrackableResult.getClassType()))
            {
                int currentStatus = result.getStatus();
                int currentStatusInfo = result.getStatusInfo();

                // The target is currently being tracked if the status is TRACKED|NORMAL
                if (currentStatus == TrackableResult.STATUS.TRACKED
                        || currentStatusInfo == TrackableResult.STATUS_INFO.NORMAL)
                {
                    mIsTargetCurrentlyTracked = true;
                    return;
                }
            }
        }

        mIsTargetCurrentlyTracked = false;
    }


    boolean isTargetCurrentlyTracked()
    {
        return mIsTargetCurrentlyTracked;
    }
}
