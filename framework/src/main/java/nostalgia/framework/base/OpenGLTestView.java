package nostalgia.framework.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import nostalgia.framework.utils.NLog;

@SuppressLint("ViewConstructor")
public class OpenGLTestView extends GLSurfaceView {

    private Renderer renderer;

    public OpenGLTestView(Context context, Callback callback) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new Renderer(callback);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public interface Callback {
        void onDetected(int i);
    }

    private static class Renderer implements GLSurfaceView.Renderer {

        private static String vertexShaderCode = "attribute vec4 a_position; "
                + "attribute vec2 a_texCoord;  								 "
                + "uniform mat4 uMVPMatrix;   								 "
                + "varying lowp vec2 v_texCoord;   						     "
                + "void main()                  							 "
                + "{                            							 "
                + "   gl_Position =  uMVPMatrix  * a_position; 				 "
                + "   v_texCoord = a_texCoord;  							 "
                + "}                            							 ";

        private static String fragmentShaderCode = "precision mediump float;                                  "
                + "uniform sampler2D s_texture;                              "
                + "uniform sampler2D s_palette;                              "
                + "void main()                     							 "
                + "{                            							 "
                + " float a = texture2D(s_texture, vec2(0, 0)).a;        "
                + " float c = floor((a * 256.0) / 127.5);                    "
                + " float x = a - c * 0.001953;                               "
                + " vec2 curPt = vec2(x, 0);                                  "
                + " gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;"
                + "}                            							 ";

        private final short[] drawOrder = {0, 1, 2, 0, 2, 3};
        int[] textureIds = new int[2];
        int paletteTextureId;
        int mainTextureId;
        private Callback callback;
        private boolean detected = false;
        private int screenWidth;
        private int screenHeight;
        private int positionHandle;
        private int texCoordHandle;
        private int textureHandle;
        private int paletteHandle;
        private int mvpMatrixHandle;
        private int program;
        private float[] projMatrix = new float[16];
        private ViewPort viewPort;
        private FloatBuffer textureBuffer;
        private ByteBuffer testBuffer;
        private FloatBuffer vertexBuffer;
        private ShortBuffer drawListBuffer;
        private float[] quadCoords;
        private float[] textureCoords;

        public Renderer(Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, vertexBuffer);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, textureBuffer);
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, projMatrix, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mainTextureId);
            GLES20.glUniform1i(textureHandle, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, paletteTextureId);
            GLES20.glUniform1i(paletteHandle, 1);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, 256, 256,
                    GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, testBuffer);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                    GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
            ByteBuffer pixels = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            GLES20.glReadPixels(screenHeight / 2, screenWidth / 2, 1, 1,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels);

            if (!detected) {
                IntBuffer intBuf = pixels.asIntBuffer();
                int[] array = new int[1];
                intBuf.get(array);
                int value = intBuf.get(0) & 0x000000ff;
                NLog.i("pix", "pix: " + Integer.toHexString(value));
                detected = true;
                if (value == 0) {
                    NLog.i("pix", "on detect: 0");
                    callback.onDetected(0);
                } else if (value == 0xff) {
                    callback.onDetected(1);
                    NLog.i("pix", "on detect: 1");
                } else {
                    callback.onDetected(2);
                    NLog.i("pix", "on detect: 2");
                }
            }

            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Matrix.orthoM(projMatrix, 0, -width / 2f, +width / 2f,
                    -height / 2f, +height / 2f, -2f, 2f);
            screenWidth = width;
            screenHeight = height;
            GLES20.glViewport(0, 0, width, height);
            initQuadCoordinates(width, height);
            GLES20.glUseProgram(program);
            initQuadCoordinates(width, height);
            GLES20.glUseProgram(program);
            positionHandle = GLES20.glGetAttribLocation(program, "a_position");
            textureHandle = GLES20.glGetUniformLocation(program, "s_texture");
            paletteHandle = GLES20.glGetUniformLocation(program, "s_palette");
            texCoordHandle = GLES20.glGetAttribLocation(program, "a_texCoord");
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            int vertexShader = OpenGLView.Renderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = OpenGLView.Renderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            initTextures();
        }

        private void initQuadCoordinates(int width, int height) {
            viewPort = new ViewPort();
            viewPort.height = height;
            viewPort.width = width;
            quadCoords = new float[]{
                    -width / 2f, -height / 2f, 0,
                    -width / 2f, height / 2f, 0,
                    width / 2f, height / 2f, 0,
                    width / 2f, -height / 2f, 0
            };
            textureCoords = new float[]{
                    0, 1,
                    0, 0,
                    1, 0,
                    1, 1,
            };
            ByteBuffer bb0 = ByteBuffer.allocateDirect(256 * 256);
            bb0.order(ByteOrder.nativeOrder());
            byte[] pixels = new byte[256 * 256];
            for (int i = 0; i < 256 * 256; i++) {
                pixels[i] = (byte) 132;
            }

            testBuffer = bb0;
            testBuffer.put(pixels);
            testBuffer.position(0);
            ByteBuffer bb1 = ByteBuffer.allocateDirect(quadCoords.length * 4);
            bb1.order(ByteOrder.nativeOrder());
            vertexBuffer = bb1.asFloatBuffer();
            vertexBuffer.put(quadCoords);
            vertexBuffer.position(0);
            ByteBuffer bb2 = ByteBuffer.allocateDirect(textureCoords.length * 4);
            bb2.order(ByteOrder.nativeOrder());
            textureBuffer = bb2.asFloatBuffer();
            textureBuffer.put(textureCoords);
            textureBuffer.position(0);
            ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder);
            drawListBuffer.position(0);
        }

        private void initTextures() {
            int paletteSize = 256;
            GLES20.glGenTextures(2, textureIds, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA, 256,
                    256, 0, GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1);
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1]);
            int[] palette = new int[paletteSize];

            for (int i = 0; i < paletteSize; i++) {
                int c = i % 2 == 0 ? 0x0 : 0xff;
                palette[i] = 0xff000000 | (c << 16) | (c << 8) | c;
            }

            GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1);
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            Bitmap paletteBmp = Bitmap.createBitmap(paletteSize, paletteSize, Config.ARGB_8888);
            paletteBmp.setPixels(palette, 0, paletteSize, 0, 0, paletteSize, 1);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, paletteBmp, 0);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            mainTextureId = textureIds[0];
            paletteTextureId = textureIds[1];
        }

        public ViewPort getViewPort() {
            return viewPort;
        }
    }

}
