package com.nikolaiapps.orbtrack;


import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.GLUtils.texImage2D;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.perspectiveM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class SkyboxView extends GLSurfaceView
{
    public interface OnPerspectiveChangeListener
    {
        void onChanged(float xDegrees, float yDegrees, float zoom);
    }

    private static class SkyboxRenderer implements  GLSurfaceView.Renderer
    {
        private static class ShaderProgram
        {
            //Uniform constants
            private static final String U_MATRIX = "u_Matrix";
            private static final String U_TEXTURE_UNIT = "u_TextureUnit";

            //Attribute constants
            static final String A_POSITION = "a_Position";

            private final int program;
            private final int uMatrixLocation;
            private final int uTextureUnitLocation;
            private final int aPositionLocation;

            public ShaderProgram(Context context)
            {
                program = buildProgram(readStringFromFile(context, R.raw.shader_skybox_vertex), readStringFromFile(context, R.raw.shader_skybox_fragment));
                uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
                uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT);
                aPositionLocation = glGetAttribLocation(program, A_POSITION);
            }

            private static String readStringFromFile(Context context, int fileId)
            {
                String line;
                StringBuilder fileString = new StringBuilder();
                BufferedReader file = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(fileId)));

                try
                {
                    //while able to read another line
                    while((line = file.readLine()) != null)
                    {
                        //add line
                        fileString.append(line);
                        fileString.append('\n');
                    }
                }
                catch(IOException ex)
                {
                    //do nothing
                }

                return(fileString.toString());
            }

            private static int compileShader(int type, String code)
            {
                int shader = glCreateShader(type);
                int[] status = new int[1];

                //if unable to create shader
                if(shader == 0)
                {
                    //stop
                    return(0);
                }

                //compile shader
                glShaderSource(shader, code);
                glCompileShader(shader);
                glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0);

                //if unable to compile
                if(status[0] == 0)
                {
                    //delete shader and stop
                    glDeleteProgram(shader);
                    return(0);
                }

                //return shader
                return(shader);
            }

            private static int linkProgram(int vertexShader, int fragmentShader)
            {
                int program = glCreateProgram();
                int[] status = new int[1];

                //if unable to create program
                if(program == 0)
                {
                    //stop
                    return(0);
                }

                //add shaders and program
                glAttachShader(program, vertexShader);
                glAttachShader(program, fragmentShader);
                glLinkProgram(program);
                glGetProgramiv(program, GL_LINK_STATUS, status, 0);

                //if unable to add program
                if(status[0] == 0)
                {
                    //delete program and stop
                    glDeleteProgram(program);
                    return(0);
                }

                //return program
                return(program);
            }

            private static int buildProgram(String vertexShaderCode, String fragmentShaderCode)
            {
                int vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderCode);
                int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderCode);

                return(linkProgram(vertexShader, fragmentShader));
            }

            public void useProgram()
            {
                glUseProgram(program);
            }

            public void setUniforms(float[] matrix, int texture)
            {
                glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);

                if(texture != 0)
                {
                    glActiveTexture(GL_TEXTURE0);
                    glBindTexture(GL_TEXTURE_CUBE_MAP, texture);
                    glUniform1i(uTextureUnitLocation, 0);
                }
            }

            public int getPositionAttributeLocation()
            {
                return(aPositionLocation);
            }
        }

        private static class ViewBox
        {
            private static class VertexArray
            {
                private static final int BYTES_PER_FLOAT = 4;

                private final FloatBuffer buffer;

                public VertexArray(float[] data)
                {
                    buffer = ByteBuffer.allocateDirect(data.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer().put(data);
                }

                public void setVertexAttribPointer(int offset, int location, int count, int stride)
                {
                    buffer.position(offset);
                    glVertexAttribPointer(location, count, GL_FLOAT, false, stride, buffer);
                    glEnableVertexAttribArray(location);
                    buffer.position(0);
                }
            }

            private static final int POSITION_COMPONENT_COUNT = 3;
            private final ByteBuffer indexArray;
            private final VertexArray vertexArray;

            public ViewBox()
            {
                vertexArray = new VertexArray(new float[]
                {
                    -1, 1, 1,       //0
                     1, 1, 1,       //1
                    -1, -1, 1,      //2
                     1, -1, 1,      //3
                    -1, 1, -1,      //4
                     1, 1, -1,      //5
                    -1, -1, -1,     //6
                     1, -1, -1      //7
                });

                //follow the curling order of the triangle to distinguish the front and back of the object, counterclockwise being the front
                indexArray = ByteBuffer.allocateDirect(36).put(new byte[]
                {
                    1, 3, 0,        //front
                    0, 3, 2,

                    4, 6, 5,        //back
                    5, 6, 7,

                    0, 2, 4,        //left
                    4, 2, 6,

                    5, 7, 1,        //right
                    1, 7, 3,

                    5, 1, 4,        //top
                    4, 1, 0,

                    6, 2, 7,        //bottom
                    7, 2, 3
                });
                indexArray.position(0);
            }

            public void bindData(ShaderProgram program)
            {
                vertexArray.setVertexAttribPointer(0, program.getPositionAttributeLocation(), POSITION_COMPONENT_COUNT, 0);
            }

            public void draw()
            {
                glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_BYTE, indexArray);
            }
        }

        private int areaWidth;
        private int areaHeight;
        private int targetDelayMs;
        private int skyBoxTexture;
        private long lastTimeMs;
        private float fovBase;
        private float xRotation;
        private float yRotation;
        private float zoomScale;
        private final Context currentContext;
        private ViewBox skyBox;
        private ShaderProgram skyBoxProgram;
        private OnPerspectiveChangeListener perspectiveChangeListener;
        private final float[] viewMatrix;
        private final float[] projectionMatrix;
        private final float[] viewProjectionMatrix;
        private Bitmap[] textureImages;

        public SkyboxRenderer(Context context)
        {
            currentContext = context;

            areaWidth = areaHeight = 1000;
            setTargetFps(60);
            lastTimeMs = skyBoxTexture = 0;
            fovBase = 45f;
            xRotation = yRotation = 0.0f;
            zoomScale = 1.0f;
            viewMatrix = new float[16];
            projectionMatrix = new float[16];
            viewProjectionMatrix = new float[16];
            textureImages = new Bitmap[6];
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config)
        {
            skyBoxProgram = new ShaderProgram(currentContext);
            skyBox = new ViewBox();
            loadTextures();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height)
        {
            areaWidth = width;
            areaHeight = height;
            glViewport(0, 0, areaWidth, areaHeight);

            fovBase = (height > width ? 90f : 45f);
            updatePerspective();
        }

        @Override
        public void onDrawFrame(GL10 gl)
        {
            long delayMs = (System.currentTimeMillis() - lastTimeMs);

            if(delayMs < targetDelayMs)
            {
                try
                {
                    Thread.sleep(targetDelayMs - delayMs);
                }
                catch(Exception ex)
                {
                    //do nothing
                }
            }

            glClear(GL_COLOR_BUFFER_BIT);
            setIdentityM(viewMatrix, 0);
            rotateM(viewMatrix, 0, -yRotation, 1.0f, 0.0f, 0.0f);
            rotateM(viewMatrix, 0, -xRotation, 0.0f, 1.0f, 0.0f);
            multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            skyBoxProgram.useProgram();
            skyBoxProgram.setUniforms(viewProjectionMatrix, skyBoxTexture);
            skyBox.bindData(skyBoxProgram);
            skyBox.draw();


            lastTimeMs = System.currentTimeMillis();
        }

        public void setOnPerspectiveChangeListener(OnPerspectiveChangeListener listener)
        {
            perspectiveChangeListener = listener;
        }

        private void updatePerspective()
        {
            float aspect = areaWidth / (float)areaHeight;
            boolean wide = (aspect > 1.0f);

            perspectiveM(projectionMatrix, 0, fovBase / zoomScale, aspect, 1.0f, 10.0f);

            if(perspectiveChangeListener != null)
            {
                perspectiveChangeListener.onChanged(fovBase * (wide ? aspect : 1.0f), fovBase * (wide ? 1.0f : aspect), zoomScale);
            }
        }

        public void setTargetFps(int fps)
        {
            targetDelayMs = 1000 / fps;
        }

        public float getZoom()
        {
            return(zoomScale);
        }

        public void setZoom(float scale)
        {
            zoomScale = scale;
            if(zoomScale < 1.0f)
            {
                zoomScale = 1.0f;
            }
            else if(zoomScale > 16.0f)
            {
                zoomScale = 16.0f;
            }

            updatePerspective();
        }

        public void setTextures(Bitmap leftImage, Bitmap rightImage, Bitmap bottomImage, Bitmap topImage, Bitmap frontImage, Bitmap backImage)
        {
            //update images
            textureImages = new Bitmap[]{leftImage, rightImage, bottomImage, topImage, frontImage, backImage};
        }

        private void loadTextures()
        {
            int index;
            int[] textureIds = new int[1];

            //get a texture ID
            glGenTextures(1, textureIds, 0);

            //if failed to get a texture ID
            if(textureIds[0] == 0)
            {
                //stop
                skyBoxTexture = 0;
                return;
            }

            //go through each image
            for(index = 0; index < textureImages.length; index++)
            {
                //if unable to load image
                if(textureImages[index] == null)
                {
                    //stop
                    glDeleteTextures(1, textureIds, 0);
                    skyBoxTexture = 0;
                    return;
                }
            }

            glBindTexture(GL_TEXTURE_CUBE_MAP, textureIds[0]);

            //set bilinear filtering
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            //load surfaces, left, right, bottom, top, front, back
            texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, textureImages[0], 0);
            texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, textureImages[1], 0);
            texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, textureImages[2], 0);
            texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, textureImages[3], 0);
            texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, textureImages[4], 0);
            texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, textureImages[5], 0);
            glBindTexture(GL_TEXTURE_2D, 0);

            skyBoxTexture = textureIds[0];
        }

        public void handleTouchDrag(float deltaX, float deltaY)
        {
            xRotation += (deltaX / 10.0f / zoomScale);
            yRotation += (deltaY / 10.0f / zoomScale);

            if(xRotation > 180)
            {
                xRotation -= 360;
            }
            else if(xRotation < -180)
            {
                xRotation += 360;
            }

            if(yRotation < -90)
            {
                yRotation = -90;
            }
            else if(yRotation > 90)
            {
                yRotation = 90;
            }
        }

        public float getAzimuth()
        {
            return(360 - xRotation);
        }

        public float getElevation()
        {
            return(yRotation);
        }
    }

    private boolean multiTouchPending;
    private float lastX;
    private float lastY;
    private ScaleGestureDetector scaleDetector;
    private final SkyboxRenderer renderer;

    public SkyboxView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        multiTouchPending = false;
        lastX = lastY = 0.0f;
        scaleDetector = null;
        renderer = new SkyboxRenderer(context);

        setEGLContextClientVersion(2);
        setRenderer(renderer);
    }
    public SkyboxView(Context context)
    {
        this(context, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        float currentX;
        float currentY;

        if(event != null)
        {
            if(scaleDetector != null && event.getPointerCount() > 1 && scaleDetector.onTouchEvent(event))
            {
                multiTouchPending = true;
                return(true);
            }

            currentX = event.getX();
            currentY = event.getY();

            switch(event.getAction())
            {
                case MotionEvent.ACTION_UP:
                    multiTouchPending = false;
                    break;

                case MotionEvent.ACTION_DOWN:
                    lastX = currentX;
                    lastY = currentY;
                    break;

                case MotionEvent.ACTION_MOVE:
                    final float deltaX = currentX - lastX;
                    final float deltaY = currentY - lastY;

                    lastX = currentX;
                    lastY = currentY;

                    if(!multiTouchPending)
                    {
                        //handle movement
                        queueEvent(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                renderer.handleTouchDrag(deltaX, deltaY);
                            }
                        });
                    }
                    break;
            }

            return(true);
        }
        else
        {
            performClick();
            return(super.onTouchEvent(null));
        }
    }

    @Override
    public boolean performClick()
    {
        return(super.performClick());
    }

    public void setScaleGestureDetector(ScaleGestureDetector detector)
    {
        scaleDetector = detector;
    }

    public void setOnPerspectiveChangeListener(OnPerspectiveChangeListener listener)
    {
        renderer.setOnPerspectiveChangeListener(listener);
    }

    //Sets renderer images
    private void setImages(Bitmap[] images)
    {
        //set textures
        renderer.setTextures(images[0], images[1], images[2], images[3], images[4], images[5]);
    }
    public void setImages(Bitmap leftImage, Bitmap rightImage, Bitmap bottomImage, Bitmap topImage, Bitmap frontImage, Bitmap backImage)
    {
        setImages(new Bitmap[]{leftImage, rightImage, bottomImage, topImage, frontImage, backImage});
    }
    public void setImages(int leftId, int rightId, int bottomId, int topId, int frontId, int backId)
    {
        int index;
        Resources res = getContext().getResources();
        BitmapFactory.Options imageOptions = new BitmapFactory.Options();
        int[] resourceIds = new int[]{leftId, rightId, bottomId, topId, frontId, backId};
        Bitmap[] images = new Bitmap[resourceIds.length];

        //set options
        imageOptions.inScaled = false;

        //go through each image
        for(index = 0; index < images.length; index++)
        {
            //load image
            images[index] = BitmapFactory.decodeResource(res, resourceIds[index], imageOptions);
        }

        //set images
        setImages(images);
    }

    public void setTargetFps(int fps)
    {
        renderer.setTargetFps(fps);
    }

    public float getZoom()
    {
        return(renderer.getZoom());
    }

    public void setZoom(float scale)
    {
        renderer.setZoom(scale);
    }

    public float getAzimuth()
    {
        return(renderer.getAzimuth());
    }

    public float getElevation()
    {
        return(renderer.getElevation());
    }
}
