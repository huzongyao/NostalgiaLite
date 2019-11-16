package nostalgia.framework.base;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.view.View;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import nostalgia.framework.Emulator;
import nostalgia.framework.GfxProfile;
import nostalgia.framework.utils.NLog;

@SuppressLint("ViewConstructor")
class OpenGLView extends GLSurfaceView implements EmulatorView {
    private static final String TAG = "base.OpenGLView";
    private final Renderer renderer;

    public OpenGLView(EmulatorActivity context, Emulator emulator, int paddingLeft,
                      int paddingTop, String shader) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new Renderer(context, emulator, paddingLeft, paddingTop, shader);
        renderer.setTextureSize(context.getGLTextureSize());
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public View asView() {
        return this;
    }

    public ViewPort getViewPort() {
        return renderer.getViewPort();
    }

    public void setBenchmark(Benchmark benchmark) {
        renderer.benchmark = benchmark;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (renderer.benchmark != null) {
            renderer.benchmark.reset();
        }
    }

    @Override
    public void setQuality(int quality) {
        renderer.setQuality(quality);
    }

    static class Renderer implements GLSurfaceView.Renderer {
        static final int COORDS_PER_VERTEX = 3;
        static final int COORDS_PER_TEXTURE = 2;

        private static String vertexShaderCode = "attribute vec4 a_position; "
                + "attribute vec2 a_texCoord;  								 "
                + "uniform mat4 uMVPMatrix;   								 "
                + "varying highp vec2 v_texCoord;   						 "
                + "void main()                  							 "
                + "{                            							 "
                + "   gl_Position =  uMVPMatrix  * a_position; 				 "
                + "   v_texCoord = a_texCoord;  							 "
                + "}                            							 ";

        private static String fragmentShaderCode;
        public final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;
        public final int TEXTURE_STRIDE = COORDS_PER_TEXTURE * 4;
        private final short[] drawOrder = {0, 1, 2, 0, 2, 3};
        Benchmark benchmark = null;
        private boolean hasPalette;
        private Application context;
        private int[] textureBounds;
        private int textureSize;
        private ViewPort viewPort;
        private int textureHandle;
        private int texCoordHandle;
        private int paletteHandle;
        private int positionHandle;
        private int mvpMatrixHandle;
        private int mainTextureId;
        private int paletteTextureId;
        private long startTime;
        private int program;
        private Emulator emulator;
        private float[] quadCoords;
        private float[] textureCoords;
        private float[] projMatrix = new float[16];
        private FloatBuffer vertexBuffer;
        private FloatBuffer textureBuffer;
        private ShortBuffer drawListBuffer;
        private int delayPerFrame = 40;
        private int paddingLeft;
        private int paddingTop;

        public Renderer(EmulatorActivity context, Emulator emulator, int paddingLeft,
                        int paddingTop, String shader) {

            this.emulator = emulator;
            this.hasPalette = context.hasGLPalette();
            this.context = context.getApplication();
            textureBounds = context.getTextureBounds(emulator);
            textureSize = 256;
            this.paddingLeft = paddingLeft;
            this.paddingTop = paddingTop;
            NLog.i("SHADER", "shader: " + shader);
            fragmentShaderCode = shader;
        }

        private static void checkGlError(String glOperation) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                NLog.e(TAG, glOperation + ": glError " + error);
                throw new RuntimeException(glOperation + ": glError " + error);
            }
        }

        public static int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

            if (compiled[0] == 0) {
                String log = GLES20.glGetShaderInfoLog(shader);
                throw new RuntimeException("glCompileShader failed. t: " + type + " " + log + "#");
            }

            return shader;
        }

        public void setQuality(int quality) {
            this.delayPerFrame = quality == 2 ? 17 : 40;
        }

        @Override
        public void onDrawFrame(GL10 unused) {
            if (benchmark != null) {
                benchmark.notifyFrameEnd();
            }

            long endTime = System.currentTimeMillis();
            long delay = delayPerFrame - (endTime - startTime);

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                }
            }

            if (benchmark != null) {
                benchmark.notifyFrameStart();
            }

            startTime = System.currentTimeMillis();
            render();
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            ViewPort vp = ViewUtils.loadOrComputeViewPort(context, emulator, width, height,
                    paddingLeft, paddingTop, false);
            viewPort = vp;
            Matrix.orthoM(projMatrix, 0, -vp.width / 2f, +vp.width / 2f, -vp.height / 2f,
                    +vp.height / 2f, -2f, 2f);
            int nvpy = (height - vp.y - vp.height);
            GLES20.glViewport(vp.x, nvpy, vp.width, vp.height);
            initQuadCoordinates(emulator, vp.width, vp.height);
            GLES20.glUseProgram(program);
            positionHandle = GLES20.glGetAttribLocation(program, "a_position");
            textureHandle = GLES20.glGetUniformLocation(program, "s_texture");

            if (hasPalette) {
                paletteHandle = GLES20.glGetUniformLocation(program, "s_palette");
            }

            texCoordHandle = GLES20.glGetAttribLocation(program, "a_texCoord");
            startTime = System.currentTimeMillis();
        }

        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

            if (linkStatus[0] != GLES20.GL_TRUE) {
                String log = GLES20.glGetProgramInfoLog(program);
                throw new RuntimeException("glLinkProgram failed. " + log + "#");
            }

            initTextures();
        }

        public ViewPort getViewPort() {
            return viewPort;
        }

        private void render() {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glEnableVertexAttribArray(texCoordHandle);
            checkGlError("handles");
            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                    false, VERTEX_STRIDE, vertexBuffer);
            GLES20.glVertexAttribPointer(texCoordHandle, COORDS_PER_TEXTURE, GLES20.GL_FLOAT,
                    false, TEXTURE_STRIDE, textureBuffer);
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, projMatrix, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mainTextureId);
            GLES20.glUniform1i(textureHandle, 0);

            if (hasPalette) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, paletteTextureId);
                GLES20.glUniform1i(paletteHandle, 1);
            }

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            checkGlError("uniforms");
            emulator.renderGfxGL();
            checkGlError("emu render");
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                    GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
            checkGlError("disable vertex arrays");
        }

        private void initQuadCoordinates(Emulator emulator, int width, int height) {
            int maxTexX;
            int maxTexY;

            if (textureBounds == null) {
                GfxProfile gfx = emulator.getActiveGfxProfile();
                maxTexX = gfx.originalScreenWidth;
                maxTexY = gfx.originalScreenHeight;
            } else {
                maxTexX = textureBounds[0];
                maxTexY = textureBounds[1];
            }

            int textureSize = getTextureSize();
            quadCoords = new float[]{
                    -width / 2f, -height / 2f, 0,
                    -width / 2f, height / 2f, 0,
                    width / 2f, height / 2f, 0,
                    width / 2f, -height / 2f, 0
            };
            textureCoords = new float[]{
                    0,
                    maxTexY / (float) textureSize,
                    0,
                    0,
                    maxTexX / (float) textureSize,
                    0,
                    maxTexX / (float) textureSize,
                    maxTexY / (float) textureSize,
            };
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

        private int getTextureSize() {
            return textureSize;
        }

        public void setTextureSize(int size) {
            textureSize = size;
        }

        private void initTextures() {
            int numTextures = hasPalette ? 2 : 1;
            int[] textureIds = new int[numTextures];
            int textureWidth = getTextureSize();
            int textureHeight = getTextureSize();
            int paletteSize = 256;
            GLES20.glGenTextures(numTextures, textureIds, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA, textureWidth,
                    textureHeight, 0, GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, null);
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

            if (hasPalette) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1]);
                int[] palette = new int[paletteSize];
                emulator.readPalette(palette);

                for (int i = 0; i < paletteSize; i++) {
                    int dd = palette[i];
                    int b = (dd & 0x00FF0000) >> 16;
                    int g = (dd & 0x0000FF00) >> 8;
                    int r = (dd & 0x000000FF) >> 0;
                    palette[i] = 0xff000000 | (r << 16) | (g << 8) | b;
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
                paletteTextureId = textureIds[1];
            }

            mainTextureId = textureIds[0];
            checkGlError("textures");
        }
    }

}
