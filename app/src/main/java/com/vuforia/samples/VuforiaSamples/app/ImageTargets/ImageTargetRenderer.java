/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VuforiaSamples.app.ImageTargets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Vuforia;
import com.vuforia.samples.SampleApplication.SampleAppRenderer;
import com.vuforia.samples.SampleApplication.SampleAppRendererControl;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.utils.CubeShaders;
import com.vuforia.samples.SampleApplication.utils.LineShaders;
import com.vuforia.samples.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.samples.SampleApplication.utils.SampleApplication3DModel;
import com.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.vuforia.samples.SampleApplication.utils.Teapot;
import com.vuforia.samples.SampleApplication.utils.Texture;
import com.vuforia.samples.VuforiaSamples.R;
import com.vuforia.samples.VuforiaSamples.app.TextRecognition.TextReco;
import com.vuforia.samples.VuforiaSamples.app.TextRecognition.TextRecoRenderer;
import com.vuforia.samples.VuforiaSamples.helper.TextPlane;


// The renderer class for the ImageTargets sample. 
public class ImageTargetRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl
{//public static Handler mainActivityHandler;
    /*
    For Displaying Text in 3D Model

     */
    private RelativeLayout mUILayout;
    private static final int MAX_NB_WORDS = 132;
    private static final float TEXTBOX_PADDING = 0.0f;

    private static final float ROIVertices[] = { -0.5f, -0.5f, 0.0f, 0.5f,
            -0.5f, 0.0f, 0.5f, 0.5f, 0.0f, -0.5f, 0.5f, 0.0f };

    private static final int NUM_QUAD_OBJECT_INDICES = 8;
    private static final short ROIIndices[] = { 0, 1, 1, 2, 2, 3, 3, 0 };

    private static final float quadVertices[] = { -0.5f, -0.5f, 0.0f, 0.5f,
            -0.5f, 0.0f, 0.5f, 0.5f, 0.0f, -0.5f, 0.5f, 0.0f, };

    private static final short quadIndices[] = { 0, 1, 1, 2, 2, 3, 3, 0 };

    private ByteBuffer mROIVerts = null;
    private ByteBuffer mROIIndices = null;/*
    private int shaderProgramID;
    private int vertexHandle;
    private int mvpMatrixHandle;*/
    private Renderer mRenderer;
    private int lineOpacityHandle;
    private int lineColorHandle;

    public float ROICenterX;
    public float ROICenterY;
    public float ROIWidth;
    public float ROIHeight;
    private int viewportPosition_x;
    private int viewportPosition_y;
    private int viewportSize_x;
    private int viewportSize_y;
    private ByteBuffer mQuadVerts;
    private ByteBuffer mQuadIndices;













    private static final String LOGTAG = "ImageTargetRenderer";
    private TextPlane mTextPlane;
    private SampleApplicationSession vuforiaAppSession;
    private ImageTargets mActivity;
    private SampleAppRenderer mSampleAppRenderer;

    private Vector<Texture> mTextures;
    
    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    
    private Teapot mTeapot;
    
    private float kBuildingScale = 0.012f;
    private SampleApplication3DModel mBuildingsModel;

    private boolean mIsActive = false;
    private boolean mModelIsLoaded = false;
    
    private static final float OBJECT_SCALE_FLOAT = 0.003f;
    
    
    public ImageTargetRenderer(ImageTargets activity, SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;
        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f , 5f);
    }
    
    
    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive) {
          mActivity.updateWordListUI();
            return;
        }
        mActivity.updateWordListUI();
        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }
    

    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();
    }


    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        initRendering();
    }
    boolean modelLoaded = false;
    private void initRendering()
    {
        if(!modelLoaded) {
            // init the vert/inde buffers
            mROIVerts = ByteBuffer.allocateDirect(4 * ROIVertices.length);
            mROIVerts.order(ByteOrder.LITTLE_ENDIAN);
            updateROIVertByteBuffer();

            mROIIndices = ByteBuffer.allocateDirect(2 * ROIIndices.length);
            mROIIndices.order(ByteOrder.LITTLE_ENDIAN);
            for (short s : ROIIndices)
                mROIIndices.putShort(s);
            mROIIndices.rewind();

            mQuadVerts = ByteBuffer.allocateDirect(4 * quadVertices.length);
            mQuadVerts.order(ByteOrder.LITTLE_ENDIAN);
            for (float f : quadVertices)
                mQuadVerts.putFloat(f);
            mQuadVerts.rewind();

            mQuadIndices = ByteBuffer.allocateDirect(2 * quadIndices.length);
            mQuadIndices.order(ByteOrder.LITTLE_ENDIAN);
            for (short s : quadIndices)
                mQuadIndices.putShort(s);
            mQuadIndices.rewind();

            mRenderer = Renderer.getInstance();
            modelLoaded = true;
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                LineShaders.LINE_VERTEX_SHADER, LineShaders.LINE_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");

        lineOpacityHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "opacity");
        lineColorHandle = GLES20.glGetUniformLocation(shaderProgramID, "color");

    }


    // Function for initializing the renderer.
   /* private void initRendering()
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);
        mTextPlane = new TextPlane();
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
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "texSampler2D");

        if(!mModelIsLoaded) {
            mTeapot = new Teapot();

            try {
                mBuildingsModel = new SampleApplication3DModel();
                mBuildingsModel.loadModel(mActivity.getResources().getAssets(),
                        "ImageTargets/Buildings.txt");
                mModelIsLoaded = true;
            } catch (IOException e) {
                Log.e(LOGTAG, "Unable to load buildings");
            }

            // Hide the Loading Dialog
            mActivity.loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }

    }*/
  /*  public void displayMessage(String text)
    {
        // We use a handler because this thread cannot
        // change the UI
        Message message = new Message();
        message.obj = text;
        mainActivityHandler.sendMessage(message);
    }*/
    public void updateConfiguration()
    {
        mSampleAppRenderer.onConfigurationChanged(mIsActive);
    }

    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            printUserData(trackable);

            //This is the place where I can do anything


           /* Matrix44F modelViewMatrix_Vuforia = Tool
                    .convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

            int textureIndex = trackable.getName().equalsIgnoreCase("keyboard") ? 0
                    : 1;
            textureIndex = trackable.getName().equalsIgnoreCase("tarmac") ? 2
                    : textureIndex;

            // deal with the modelview and projection matrices
            float[] modelViewProjection = new float[16];

            if (!mActivity.isExtendedTrackingActive()) {
                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        OBJECT_SCALE_FLOAT);
                Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
                        OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
            } else {
                Matrix.rotateM(modelViewMatrix, 0, 90.0f, 1.0f, 0, 0);
                Matrix.scaleM(modelViewMatrix, 0, kBuildingScale,
                        kBuildingScale, kBuildingScale);
            }
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

            // activate the shader program and bind the vertex/normal/tex coords
            GLES20.glUseProgram(shaderProgramID);

            if (!mActivity.isExtendedTrackingActive()) {
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mTeapot.getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mTeapot.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                // activate texture 0, bind it, and pass to shader
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);

                // pass the model view matrix to the shader
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjection, 0);

                // finally draw the teapot
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mTeapot.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mTeapot.getIndices());

                // disable the enabled arrays
                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);
            } else {
                GLES20.glDisable(GLES20.GL_CULL_FACE);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mBuildingsModel.getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mBuildingsModel.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextures.get(3).mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjection, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
                        mBuildingsModel.getNumObjectVertex());

                SampleUtils.checkGLError("Renderer DrawBuildings");
            }

            SampleUtils.checkGLError("Render Frame");
*/







            Matrix44F mvMat44f = Tool.convertPose2GLMatrix(result.getPose());
            float[] mvMat = mvMat44f.getData();
            float[] mvpMat = new float[16];
            Matrix.translateM(mvMat, 0, 0, 0, 0);
            Matrix.scaleM(mvMat, 0, 10.5f - TEXTBOX_PADDING,
                    10.5f - TEXTBOX_PADDING, 1.0f);
            Matrix.multiplyMM(mvpMat, 0, projectionMatrix, 0, mvMat, 0);

            GLES20.glUseProgram(shaderProgramID);
            GLES20.glLineWidth(3.0f);
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                    false, 0, mQuadVerts);
            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glUniform1f(lineOpacityHandle, 1.0f);
            GLES20.glUniform3f(lineColorHandle, 1.0f, 0.447f, 0.0f);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMat, 0);
            GLES20.glDrawElements(GLES20.GL_LINES, NUM_QUAD_OBJECT_INDICES,
                    GLES20.GL_UNSIGNED_SHORT, mQuadIndices);
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glLineWidth(1.0f);
            GLES20.glUseProgram(0);
        }

        // Draw the region of interest
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        drawRegionOfInterest(ROICenterX, ROICenterY, ROIWidth, ROIHeight);




    }
    private void setOrthoMatrix(float nLeft, float nRight, float nBottom,
                                float nTop, float nNear, float nFar, float[] _ROIOrthoProjMatrix)
    {
        for (int i = 0; i < 16; i++)
            _ROIOrthoProjMatrix[i] = 0.0f;

        _ROIOrthoProjMatrix[0] = 2.0f / (nRight - nLeft);
        _ROIOrthoProjMatrix[5] = 2.0f / (nTop - nBottom);
        _ROIOrthoProjMatrix[10] = 2.0f / (nNear - nFar);
        _ROIOrthoProjMatrix[12] = -(nRight + nLeft) / (nRight - nLeft);
        _ROIOrthoProjMatrix[13] = -(nTop + nBottom) / (nTop - nBottom);
        _ROIOrthoProjMatrix[14] = (nFar + nNear) / (nFar - nNear);
        _ROIOrthoProjMatrix[15] = 1.0f;

    }

    private void drawRegionOfInterest(float center_x, float center_y,
                                      float width, float height)
    {
        // assumption is that center_x, center_y, width and height are given
        // here in screen coordinates (screen pixels)
        float[] orthProj = new float[16];
        setOrthoMatrix(0.0f, (float) viewportSize_x, (float) viewportSize_y,
                0.0f, -1.0f, 1.0f, orthProj);

        // compute coordinates
        float minX = center_x - width / 2;
        float maxX = center_x + width / 2;
        float minY = center_y - height / 2;
        float maxY = center_y + height / 2;

        // Update vertex coordinates of ROI rectangle
        ROIVertices[0] = minX - viewportPosition_x;
        ROIVertices[1] = minY - viewportPosition_y;
        ROIVertices[2] = 0;

        ROIVertices[3] = maxX - viewportPosition_x;
        ROIVertices[4] = minY - viewportPosition_y;
        ROIVertices[5] = 0;

        ROIVertices[6] = maxX - viewportPosition_x;
        ROIVertices[7] = maxY - viewportPosition_y;
        ROIVertices[8] = 0;

        ROIVertices[9] = minX - viewportPosition_x;
        ROIVertices[10] = maxY - viewportPosition_y;
        ROIVertices[11] = 0;

        updateROIVertByteBuffer();

        GLES20.glUseProgram(shaderProgramID);
        GLES20.glLineWidth(3.0f);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false,
                0, mROIVerts);
        GLES20.glEnableVertexAttribArray(vertexHandle);

        GLES20.glUniform1f(lineOpacityHandle, 1.0f); // 0.35f);
        GLES20.glUniform3f(lineColorHandle, 0.0f, 1.0f, 0.0f);// R,G,B
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, orthProj, 0);

        // Then, we issue the render call
        GLES20.glDrawElements(GLES20.GL_LINES, NUM_QUAD_OBJECT_INDICES,
                GLES20.GL_UNSIGNED_SHORT, mROIIndices);

        // Disable the vertex array handle
        GLES20.glDisableVertexAttribArray(vertexHandle);

        // Restore default line width
        GLES20.glLineWidth(1.0f);

        // Unbind shader program
        GLES20.glUseProgram(0);
    }

      //  GLES20.glDisable(GLES20.GL_DEPTH_TEST);

   // }

    private void updateROIVertByteBuffer()
    {
        mROIVerts.rewind();
        for (float f : ROIVertices)
            mROIVerts.putFloat(f);
        mROIVerts.rewind();
    }
    private void printUserData(Trackable trackable)
    {
        String userData = (String) trackable.getUserData();
        Log.d(LOGTAG, "UserData:Retreived User Data	\"" + userData + "\"");
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               // Toast.makeText(mActivity, "This is new Image", Toast.LENGTH_SHORT).show();
            }
        });

    }
    
    
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
        
    }
    
}
