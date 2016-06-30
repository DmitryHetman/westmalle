//Copyright 2016 Erik De Rijcke
//
//Licensed under the Apache License,Version2.0(the"License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,software
//distributed under the License is distributed on an"AS IS"BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package org.westmalle.wayland.gles2;

import org.freedesktop.jaccall.JNI;
import org.freedesktop.jaccall.Pointer;
import org.freedesktop.wayland.server.Display;
import org.freedesktop.wayland.server.ShmBuffer;
import org.freedesktop.wayland.server.WlBufferResource;
import org.freedesktop.wayland.server.WlSurfaceResource;
import org.freedesktop.wayland.shared.WlShmFormat;
import org.westmalle.wayland.core.*;
import org.westmalle.wayland.core.calc.Mat4;
import org.westmalle.wayland.nativ.libEGL.EglBindWaylandDisplayWL;
import org.westmalle.wayland.nativ.libEGL.EglCreateImageKHR;
import org.westmalle.wayland.nativ.libEGL.EglDestroyImageKHR;
import org.westmalle.wayland.nativ.libEGL.EglQueryWaylandBufferWL;
import org.westmalle.wayland.nativ.libEGL.LibEGL;
import org.westmalle.wayland.nativ.libGLESv2.GlEGLImageTargetTexture2DOES;
import org.westmalle.wayland.nativ.libGLESv2.LibGLESv2;
import org.westmalle.wayland.protocol.WlOutput;
import org.westmalle.wayland.protocol.WlSurface;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.freedesktop.jaccall.Pointer.malloc;
import static org.freedesktop.jaccall.Pointer.wrap;
import static org.freedesktop.jaccall.Size.sizeof;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_ALPHA_SIZE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_BLUE_SIZE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_GREEN_SIZE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_HEIGHT;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NONE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NO_CONTEXT;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NO_DISPLAY;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NO_IMAGE_KHR;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_OPENGL_ES2_BIT;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_RED_SIZE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_RENDERABLE_TYPE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_SURFACE_TYPE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_TEXTURE_EXTERNAL_WL;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_TEXTURE_FORMAT;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_TEXTURE_RGB;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_TEXTURE_RGBA;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_TEXTURE_Y_UV_WL;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_TEXTURE_Y_U_V_WL;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_TEXTURE_Y_XUXV_WL;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_WAYLAND_BUFFER_WL;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_WAYLAND_PLANE_WL;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_WAYLAND_Y_INVERTED_WL;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_WIDTH;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_WINDOW_BIT;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_BGRA_EXT;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_BLEND;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_CLAMP_TO_EDGE;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_COMPILE_STATUS;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_EXTENSIONS;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_FLOAT;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_FRAGMENT_SHADER;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_INFO_LOG_LENGTH;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_LINK_STATUS;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_NEAREST;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_ONE;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_ONE_MINUS_SRC_ALPHA;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE0;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_2D;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_EXTERNAL_OES;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_MAG_FILTER;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_MIN_FILTER;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_WRAP_S;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_WRAP_T;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TRIANGLES;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_UNSIGNED_BYTE;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_VERTEX_SHADER;

@Singleton
public class Gles2Renderer implements GlRenderer {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final String VERTEX_SHADER =
            "uniform mat4 u_projection;\n" +
            "uniform mat4 u_transform;\n" +
            "attribute vec2 a_position;\n" +
            "attribute vec2 a_texCoord;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main(){\n" +
            "    v_texCoord = a_texCoord;\n" +
            "    gl_Position = u_projection * u_transform * vec4(a_position, 0.0, 1.0) ;\n" +
            "}";

    private static final String FRAGMENT_SHADER_ARGB8888 =
            "precision mediump float;\n" +
            "uniform sampler2D u_texture0;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main(){\n" +
            "    gl_FragColor = texture2D(u_texture0, v_texCoord);\n" +
            "}";

    private static final String FRAGMENT_SHADER_XRGB8888 =
            "precision mediump float;\n" +
            "uniform sampler2D u_texture0;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main(){\n" +
            "    gl_FragColor = vec4(texture2D(u_texture0, v_texCoord).bgr, 1.0);\n" +
            "}";

    private static final String FRAGMENT_SHADER_EGL_EXTERNAL =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES u_texture0;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main(){\n" +
            "   gl_FragColor = texture2D(u_texture0, v_texCoord)\n;" +
            "}";

    private static final String FRAGMENT_CONVERT_YUV =
            "  gl_FragColor.r = y + 1.59602678 * v;\n" +
            "  gl_FragColor.g = y - 0.39176229 * u - 0.81296764 * v;\n" +
            "  gl_FragColor.b = y + 2.01723214 * u;\n" +
            "  gl_FragColor.a = 1.0;\n" +
            "}";

    private static final String FRAGMENT_SHADER_EGL_Y_UV =
            "precision mediump float;\n" +
            "uniform sampler2D u_texture0;\n" +
            "uniform sampler2D u_texture1;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main() {\n" +
            "  float y = 1.16438356 * (texture2D(u_texture0, v_texCoord).x - 0.0625);\n" +
            "  float u = texture2D(u_texture1, v_texCoord).r - 0.5;\n" +
            "  float v = texture2D(u_texture1, v_texCoord).g - 0.5;\n" +
            FRAGMENT_CONVERT_YUV;

    private static final String FRAGMENT_SHADER_EGL_Y_U_V =
            "precision mediump float;\n" +
            "uniform sampler2D u_texture0;\n" +
            "uniform sampler2D u_texture1;\n" +
            "uniform sampler2D u_texture2;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main() {\n" +
            "  float y = 1.16438356 * (texture2D(u_texture0, v_texCoord).x - 0.0625);\n" +
            "  float u = texture2D(u_texture1, v_texCoord).x - 0.5;\n" +
            "  float v = texture2D(u_texture2, v_texCoord).x - 0.5;\n" +
            FRAGMENT_CONVERT_YUV;

    private static final String FRAGMENT_SHADER_EGL_Y_XUXV =
            "precision mediump float;\n" +
            "uniform sampler2D u_texture0;\n" +
            "uniform sampler2D u_texture1;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main() {\n" +
            "  float y = 1.16438356 * (texture2D(u_texture0, v_texCoord).x - 0.0625);\n" +
            "  float u = texture2D(u_texture1, v_texCoord).g - 0.5;\n" +
            "  float v = texture2D(u_texture1, v_texCoord).a - 0.5;\n" +
            FRAGMENT_CONVERT_YUV;

    @Nonnull
    private final LibEGL    libEGL;
    @Nonnull
    private final LibGLESv2 libGLESv2;
    @Nonnull
    private final Display   display;
    @Nonnull
    private final Scene     scene;

    @Nonnull
    private Optional<EglQueryWaylandBufferWL>      eglQueryWaylandBufferWL      = Optional.empty();
    @Nonnull
    private Optional<EglCreateImageKHR>            eglCreateImageKHR            = Optional.empty();
    @Nonnull
    private Optional<EglDestroyImageKHR>           eglDestroyImageKHR           = Optional.empty();
    @Nonnull
    private Optional<GlEGLImageTargetTexture2DOES> glEGLImageTargetTexture2DOES = Optional.empty();

    @Nonnull
    private final Map<WlSurfaceResource, SurfaceRenderState> surfaceRenderStates = new HashMap<>();

    @Nonnull
    private float[] projection = Mat4.IDENTITY.toArray();

    //shader programs
    //used by shm & egl
    private int argb8888ShaderProgram;
    //used by shm
    private int xrgb8888ShaderProgram;
    //used by egl
    private int y_u_vShaderProgram;
    private int y_uvShaderProgram;
    private int y_xuxvShaderProgram;
    private int externalImageShaderProgram;

    //shader args:
    //used by shm & egl
    private int projectionArg;
    private int transformArg;
    private int positionArg;
    private int textureCoordinateArg;
    private final int[] textureArgs = new int[3];

    private long    eglDisplay      = EGL_NO_DISPLAY;
    private boolean hasWlEglDisplay = false;
    private boolean init            = false;

    //TODO guarantee 1 renderer instance per platform
    @Inject
    Gles2Renderer(@Nonnull final LibEGL libEGL,
                  @Nonnull final LibGLESv2 libGLESv2,
                  @Nonnull final Display display,
                  @Nonnull final Scene scene) {
        this.libEGL = libEGL;
        this.libGLESv2 = libGLESv2;
        this.display = display;
        this.scene = scene;
    }

    @Override
    public void onDestroy(@Nonnull final WlSurfaceResource wlSurfaceResource) {
        Optional.ofNullable(this.surfaceRenderStates.remove(wlSurfaceResource))
                .ifPresent(surfaceRenderState -> {
                    surfaceRenderState.accept(new SurfaceRenderStateVisitor() {
                        @Override
                        public Optional<SurfaceRenderState> visit(final ShmSurfaceRenderState shmSurfaceRenderState) {
                            destroy(shmSurfaceRenderState);
                            return Optional.empty();
                        }

                        @Override
                        public Optional<SurfaceRenderState> visit(final EglSurfaceRenderState eglSurfaceRenderState) {
                            destroy(eglSurfaceRenderState);
                            return Optional.empty();
                        }
                    });
                });
    }

    private void destroy(final EglSurfaceRenderState eglSurfaceRenderState) {
        //delete textures & egl images
        for (final int texture : eglSurfaceRenderState.getTextures()) {
            Gles2Renderer.this.libGLESv2.glDeleteTextures(1,
                                                          Pointer.nref(texture).address);
        }

        for (final long eglImage : eglSurfaceRenderState.getEglImages()) {
            Gles2Renderer.this.eglDestroyImageKHR.ifPresent(eglDestroyImageKHR1 -> eglDestroyImageKHR1.$(Gles2Renderer.this.eglDisplay,
                                                                                                         eglImage));
        }
    }

    private void destroy(final ShmSurfaceRenderState shmSurfaceRenderState) {
        //delete texture
        Gles2Renderer.this.libGLESv2.glDeleteTextures(1,
                                                      Pointer.nref(shmSurfaceRenderState.getTexture()).address);
    }

    @Nonnull
    @Override
    public Buffer queryBuffer(@Nonnull final WlBufferResource wlBufferResource) {

        final ShmBuffer shmBuffer = ShmBuffer.get(wlBufferResource);
        if (shmBuffer != null) {
            return SmBuffer.create(shmBuffer.getWidth(),
                                   shmBuffer.getHeight(),
                                   wlBufferResource,
                                   shmBuffer);
        }

        if (this.eglQueryWaylandBufferWL.isPresent()) {
            final EglQueryWaylandBufferWL queryWlEglBuffer = this.eglQueryWaylandBufferWL.get();
            final Pointer<Integer>        textureFormatP   = Pointer.nref(0);
            final Long                    bufferPointer    = wlBufferResource.pointer;

            queryWlEglBuffer.$(this.eglDisplay,
                               bufferPointer,
                               EGL_TEXTURE_FORMAT,
                               textureFormatP.address);
            final int textureFormat = textureFormatP.dref();

            if (textureFormat != 0) {
                final Pointer<Integer> widthP  = Pointer.nref(0);
                final Pointer<Integer> heightP = Pointer.nref(0);
                queryWlEglBuffer.$(this.eglDisplay,
                                   bufferPointer,
                                   EGL_WIDTH,
                                   widthP.address);
                queryWlEglBuffer.$(this.eglDisplay,
                                   bufferPointer,
                                   EGL_HEIGHT,
                                   heightP.address);
                final int width  = widthP.dref();
                final int height = heightP.dref();

                return EglBuffer.create(width,
                                        height,
                                        wlBufferResource,
                                        textureFormat);
            }
        }

        //TODO dma buffer.

        return UnsupportedBuffer.create(wlBufferResource);
    }

    @Override
    public long eglConfig(final long eglDisplay,
                          @Nonnull final String eglExtensions) {

        final int configs_size = 256 * sizeof((Pointer<?>) null);
        final Pointer<Pointer> configs = malloc(configs_size,
                                                Pointer.class);
        final Pointer<Integer> num_configs = Pointer.nref(0);
        final Pointer<Integer> egl_config_attribs = Pointer.nref(
                //@formatter:off
                EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
	            EGL_RED_SIZE, 1,
	            EGL_GREEN_SIZE, 1,
	            EGL_BLUE_SIZE, 1,
	            EGL_ALPHA_SIZE, 0,
	            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
	            EGL_NONE
                //@formatter:on
                                                                );
        if (this.libEGL.eglChooseConfig(eglDisplay,
                                        egl_config_attribs.address,
                                        configs.address,
                                        configs_size,
                                        num_configs.address) == 0) {
            throw new RuntimeException("eglChooseConfig() failed");
        }
        if (num_configs.dref() == 0) {
            throw new RuntimeException("failed to find suitable EGLConfig");
        }

        bindEglDisplay(eglDisplay,
                       eglExtensions);
        this.eglDisplay = eglDisplay;

        return configs.dref().address;
    }

    private void bindEglDisplay(final long eglDisplay,
                                @Nonnull final String eglExtensions) {

        if (bindDisplay(eglDisplay,
                        eglExtensions)) {
            this.eglQueryWaylandBufferWL = Optional.of(wrap(EglQueryWaylandBufferWL.class,
                                                            this.libEGL.eglGetProcAddress(Pointer.nref("eglQueryWaylandBufferWL").address)).dref());

            //FIXME we need to check this gl extension before we can be 100% sure we support wayland egl.
            this.glEGLImageTargetTexture2DOES = Optional.of(wrap(GlEGLImageTargetTexture2DOES.class,
                                                                 this.libEGL.eglGetProcAddress(Pointer.nref("glEGLImageTargetTexture2DOES").address)).dref());

            if (eglExtensions.contains("EGL_KHR_image_base")) {
                this.eglCreateImageKHR = Optional.of(wrap(EglCreateImageKHR.class,
                                                          this.libEGL.eglGetProcAddress(Pointer.nref("eglCreateImageKHR").address)).dref());
                this.eglDestroyImageKHR = Optional.of(wrap(EglDestroyImageKHR.class,
                                                           this.libEGL.eglGetProcAddress(Pointer.nref("eglDestroyImageKHR").address)).dref());
                this.hasWlEglDisplay = true;
            }
            else {
                LOGGER.warning("Extension EGL_KHR_image_base not available. Required for client side egl support.");
            }
        }
    }

    @Override
    public void visit(@Nonnull final Platform platform) {
        throw new UnsupportedOperationException(String.format("Need an egl capable platform. Got %s",
                                                              platform));
    }

    @Override
    public void visit(@Nonnull final EglPlatform eglPlatform) {
        if (this.eglDisplay != EGL_NO_DISPLAY) {
            render(eglPlatform);
        }
    }

    private void render(@Nonnull final EglPlatform eglPlatform) {
        for (final EglConnector eglConnector : eglPlatform.getConnectors()) {
            eglConnector.getWlOutput()
                        .ifPresent(wlOutput -> {
                            updateRenderState(eglPlatform,
                                              eglConnector,
                                              wlOutput);
                            if (!this.init) {
                                //one time init because we need a current context
                                initRenderer();
                            }
                            //naive single pass, bottom to top overdraw rendering.
                            this.scene.getSurfacesStack()
                                      .forEach((wlSurfaceResource) -> draw(eglPlatform,
                                                                           wlSurfaceResource));
                            flushRenderState(eglPlatform,
                                             eglConnector);
                        });
        }

    }

    private void flushRenderState(final EglPlatform eglPlatform,
                                  final EglConnector eglConnector) {
        this.libEGL.eglSwapBuffers(eglPlatform.getEglDisplay(),
                                   eglConnector.getEglSurface());
        eglConnector.end();
    }

    private void updateRenderState(final EglPlatform eglPlatform,
                                   final EglConnector eglConnector,
                                   @Nonnull final WlOutput wlOutput) {
        eglConnector.begin();

        this.libEGL.eglMakeCurrent(eglPlatform.getEglDisplay(),
                                   eglConnector.getEglSurface(),
                                   eglConnector.getEglSurface(),
                                   eglPlatform.getEglContext());

        final Output     output = wlOutput.getOutput();
        final OutputMode mode   = output.getMode();

        final int width  = mode.getWidth();
        final int height = mode.getHeight();

        this.libGLESv2.glViewport(0,
                                  0,
                                  width,
                                  height);

        this.libGLESv2.glClearColor(1.0f,
                                    1.0f,
                                    1.0f,
                                    1.0f);

        this.libGLESv2.glClear(LibGLESv2.GL_COLOR_BUFFER_BIT);

        //@formatter:off
        this.projection = Mat4.create(2.0f / width, 0,              0, -1,
                                      0,            2.0f / -height, 0,  1,
                                      0,            0,              1,  0,
                                      0,            0,              0,  1).toArray();
        //@formatter:on
    }

    private void initRenderer() {
        //check for required texture glExtensions
        final String glExtensions = wrap(String.class,
                                         this.libGLESv2.glGetString(GL_EXTENSIONS)).dref();

        //init shm shaders
        LOGGER.info("GLESv2 glExtensions: " + glExtensions);
        if (!glExtensions.contains("GL_EXT_texture_format_BGRA8888")) {
            LOGGER.severe("Required extension GL_EXT_texture_format_BGRA8888 not available");
            System.exit(1);
        }
        //this shader is reused in wl egl
        this.argb8888ShaderProgram = createShaderProgram(VERTEX_SHADER,
                                                         FRAGMENT_SHADER_ARGB8888,
                                                         1);
        this.xrgb8888ShaderProgram = createShaderProgram(VERTEX_SHADER,
                                                         FRAGMENT_SHADER_XRGB8888,
                                                         1);

        //compile wl egl shaders
        if (this.hasWlEglDisplay) {
            this.y_u_vShaderProgram = createShaderProgram(VERTEX_SHADER,
                                                          FRAGMENT_SHADER_EGL_Y_U_V,
                                                          3);
            this.y_uvShaderProgram = createShaderProgram(VERTEX_SHADER,
                                                         FRAGMENT_SHADER_EGL_Y_UV,
                                                         2);
            this.y_xuxvShaderProgram = createShaderProgram(VERTEX_SHADER,
                                                           FRAGMENT_SHADER_EGL_Y_XUXV,
                                                           2);

            if (glExtensions.contains("GL_OES_EGL_image_external")) {
                this.externalImageShaderProgram = createShaderProgram(VERTEX_SHADER,
                                                                      FRAGMENT_SHADER_EGL_EXTERNAL,
                                                                      1);
            }
            else {
                LOGGER.warning("Extension GL_OES_EGL_image_external not available.");
            }
        }

        //configure texture blending
        this.libGLESv2.glBlendFunc(GL_ONE,
                                   GL_ONE_MINUS_SRC_ALPHA);
        this.init = true;
    }

    private int createShaderProgram(final String vertexShaderSource,
                                    final String fragmentShaderSource,
                                    final int nroTextures) {
        final int vertexShader = compileShader(vertexShaderSource,
                                               GL_VERTEX_SHADER);
        final int fragmentShader = compileShader(fragmentShaderSource,
                                                 GL_FRAGMENT_SHADER);

        //shader program
        final int shaderProgram = this.libGLESv2.glCreateProgram();
        this.libGLESv2.glAttachShader(shaderProgram,
                                      vertexShader);

        this.libGLESv2.glAttachShader(shaderProgram,
                                      fragmentShader);

        this.libGLESv2.glLinkProgram(shaderProgram);

        //check the link status
        final Pointer<Integer> linked = Pointer.nref(0);
        this.libGLESv2.glGetProgramiv(shaderProgram,
                                      GL_LINK_STATUS,
                                      linked.address);
        if (linked.dref() == 0) {
            final Pointer<Integer> infoLen = Pointer.nref(0);
            this.libGLESv2.glGetProgramiv(shaderProgram,
                                          GL_INFO_LOG_LENGTH,
                                          infoLen.address);
            int logSize = infoLen.dref();
            if (logSize <= 0) {
                //some drivers report incorrect log size
                logSize = 1024;
            }
            final Pointer<String> log = Pointer.nref(new String(new char[logSize]));
            this.libGLESv2.glGetProgramInfoLog(shaderProgram,
                                               logSize,
                                               0L,
                                               log.address);
            this.libGLESv2.glDeleteProgram(shaderProgram);
            System.err.println("Error compiling the vertex shader: " + log.dref());
            System.exit(1);
        }

        //find shader arguments
        this.projectionArg = this.libGLESv2.glGetUniformLocation(shaderProgram,
                                                                 Pointer.nref("u_projection").address);
        this.transformArg = this.libGLESv2.glGetUniformLocation(shaderProgram,
                                                                Pointer.nref("u_transform").address);
        this.positionArg = this.libGLESv2.glGetAttribLocation(shaderProgram,
                                                              Pointer.nref("a_position").address);
        this.textureCoordinateArg = this.libGLESv2.glGetAttribLocation(shaderProgram,
                                                                       Pointer.nref("a_texCoord").address);

        for (int i = 0; i < nroTextures; i++) {
            this.textureArgs[i] = this.libGLESv2.glGetUniformLocation(shaderProgram,
                                                                      Pointer.nref("u_texture" + i).address);
        }


        return shaderProgram;
    }

    private int compileShader(final String shaderSource,
                              final int shaderType) {
        final int                      shader  = this.libGLESv2.glCreateShader(shaderType);
        final Pointer<Pointer<String>> shaders = Pointer.nref(Pointer.nref(shaderSource));
        this.libGLESv2.glShaderSource(shader,
                                      1,
                                      shaders.address,
                                      0L);
        this.libGLESv2.glCompileShader(shader);

        checkShaderCompilation(shader);
        return shader;
    }

    private void checkShaderCompilation(final int shader) {
        final Pointer<Integer> vstatus = Pointer.nref(0);
        this.libGLESv2.glGetShaderiv(shader,
                                     GL_COMPILE_STATUS,
                                     vstatus.address);
        if (vstatus.dref() == 0) {
            //failure!
            //get log length
            final Pointer<Integer> logLength = Pointer.nref(0);
            this.libGLESv2.glGetShaderiv(shader,
                                         GL_INFO_LOG_LENGTH,
                                         logLength.address);
            //get log
            int logSize = logLength.dref();
            if (logSize == 0) {
                //some drivers report incorrect log size
                logSize = 1024;
            }
            final Pointer<String> log = Pointer.nref(new String(new char[logSize]));
            this.libGLESv2.glGetShaderInfoLog(shader,
                                              logSize,
                                              0L,
                                              log.address);
            System.err.println("Error compiling the vertex shader: " + log.dref());
            System.exit(1);
        }
    }

    private void draw(final EglPlatform eglPlatform,
                      final WlSurfaceResource wlSurfaceResource) {
        final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
        //don't bother rendering subsurfaces if the parent doesn't have a buffer.
        wlSurface.getSurface()
                 .getState()
                 .getBuffer()
                 .ifPresent(wlBufferResource -> {
                     final LinkedList<WlSurfaceResource> subsurfaces = this.scene.getSubsurfaceStack(wlSurfaceResource);
                     draw(eglPlatform,
                          wlSurfaceResource,
                          wlBufferResource);
                     subsurfaces.forEach((subsurface) -> {
                         if (subsurface != wlSurfaceResource) {
                             draw(eglPlatform,
                                  subsurface);
                         }
                     });
                 });
    }

    private void draw(final EglPlatform eglPlatform,
                      final WlSurfaceResource wlSurfaceResource,
                      final WlBufferResource wlBufferResource) {
        queryBuffer(wlBufferResource).accept(new BufferVisitor() {
            @Override
            public void visit(@Nonnull final Buffer buffer) {
                LOGGER.warning("Unsupported buffer.");
            }

            @Override
            public void visit(@Nonnull final EglBuffer eglBuffer) {
                drawEgl(eglPlatform,
                        wlSurfaceResource,
                        eglBuffer);
            }

            @Override
            public void visit(@Nonnull final SmBuffer smBuffer) {
                drawShm(wlSurfaceResource,
                        smBuffer);
            }
        });
    }

    private void drawShm(final @Nonnull WlSurfaceResource wlSurfaceResource,
                         final SmBuffer smBuffer) {

        queryShmSurfaceRenderState(wlSurfaceResource,
                                   smBuffer.getShmBuffer()).ifPresent(surfaceRenderState -> surfaceRenderState.accept(new SurfaceRenderStateVisitor() {
            @Override
            public Optional<SurfaceRenderState> visit(final ShmSurfaceRenderState shmSurfaceRenderState) {
                drawShm(wlSurfaceResource,
                        shmSurfaceRenderState);
                return null;
            }
        }));
    }

    private Optional<SurfaceRenderState> queryShmSurfaceRenderState(final WlSurfaceResource wlSurfaceResource,
                                                                    final ShmBuffer shmBuffer) {
        @Nonnull
        Optional<SurfaceRenderState> surfaceRenderState = Optional.ofNullable(this.surfaceRenderStates.get(wlSurfaceResource));

        if (surfaceRenderState.isPresent()) {
            surfaceRenderState = surfaceRenderState.get()
                                                   .accept(new SurfaceRenderStateVisitor() {
                                                       @Override
                                                       public Optional<SurfaceRenderState> visit(final ShmSurfaceRenderState shmSurfaceRenderState) {
                                                           //the surface already has an shm render state associated. update it.
                                                           return createShmSurfaceRenderState(wlSurfaceResource,
                                                                                              shmBuffer,
                                                                                              Optional.of(shmSurfaceRenderState));
                                                       }

                                                       @Override
                                                       public Optional<SurfaceRenderState> visit(final EglSurfaceRenderState eglSurfaceRenderState) {
                                                           //the surface was previously associated with an egl render state but is now using an shm render state. create it.
                                                           destroy(eglSurfaceRenderState);
                                                           //TODO we could reuse the texture id from the egl surface render state
                                                           return createShmSurfaceRenderState(wlSurfaceResource,
                                                                                              shmBuffer,
                                                                                              Optional.empty());
                                                       }
                                                   });
        }
        else {
            //the surface was not previously associated with any render state. create an shm render state.
            surfaceRenderState = createShmSurfaceRenderState(wlSurfaceResource,
                                                             shmBuffer,
                                                             Optional.empty());
        }

        if (surfaceRenderState.isPresent()) {
            this.surfaceRenderStates.put(wlSurfaceResource,
                                         surfaceRenderState.get());
        }
        else {
            onDestroy(wlSurfaceResource);
        }

        return surfaceRenderState;
    }


    private Optional<SurfaceRenderState> createShmSurfaceRenderState(final WlSurfaceResource wlSurfaceResource,
                                                                     final ShmBuffer shmBuffer,
                                                                     final Optional<ShmSurfaceRenderState> oldRenderState) {
        //new values
        final int pitch;
        final int height = shmBuffer.getHeight();
        final int target = GL_TEXTURE_2D;
        final int shaderProgram;
        final int glFormat;
        final int glPixelType;
        final int texture;

        final int shmBufferFormat = shmBuffer.getFormat();
        final int argb8888        = WlShmFormat.ARGB8888.value;
        final int xrgb8888        = WlShmFormat.XRGB8888.value;

        if (argb8888 == shmBufferFormat) {
            shaderProgram = this.argb8888ShaderProgram;
            pitch = shmBuffer.getStride() / 4;
            glFormat = GL_BGRA_EXT;
            glPixelType = GL_UNSIGNED_BYTE;
        }
        else if (xrgb8888 == shmBufferFormat) {
            shaderProgram = this.xrgb8888ShaderProgram;
            pitch = shmBuffer.getStride() / 4;
            glFormat = GL_BGRA_EXT;
            glPixelType = GL_UNSIGNED_BYTE;
        }
        else {
            LOGGER.warning(String.format("Unknown shm buffer format: %d",
                                         shmBufferFormat));
            return Optional.empty();
        }

        final ShmSurfaceRenderState newShmSurfaceRenderState;


        if (oldRenderState.isPresent()) {
            final ShmSurfaceRenderState oldShmSurfaceRenderState = oldRenderState.get();
            texture = oldShmSurfaceRenderState.getTexture();

            newShmSurfaceRenderState = ShmSurfaceRenderState.create(pitch,
                                                                    height,
                                                                    target,
                                                                    shaderProgram,
                                                                    glFormat,
                                                                    glPixelType,
                                                                    texture);

            if (pitch != oldShmSurfaceRenderState.getPitch() ||
                height != oldShmSurfaceRenderState.getHeight() ||
                glFormat != oldShmSurfaceRenderState.getGlFormat() ||
                glPixelType != oldShmSurfaceRenderState.getGlPixelType()) {
                //state needs full texture updating
                shmUpdateAll(wlSurfaceResource,
                             shmBuffer,
                             newShmSurfaceRenderState);
            }
            else {
                //partial texture update
                shmUpdateDamaged(wlSurfaceResource,
                                 shmBuffer,
                                 newShmSurfaceRenderState);
            }
        }
        else {
            //allocate new texture id & upload full texture
            texture = genTexture(target);
            newShmSurfaceRenderState = ShmSurfaceRenderState.create(pitch,
                                                                    height,
                                                                    target,
                                                                    shaderProgram,
                                                                    glFormat,
                                                                    glPixelType,
                                                                    texture);
            shmUpdateAll(wlSurfaceResource,
                         shmBuffer,
                         newShmSurfaceRenderState);
        }

        return Optional.of(newShmSurfaceRenderState);
    }

    private void shmUpdateDamaged(final WlSurfaceResource wlSurfaceResource,
                                  final ShmBuffer shmBuffer,
                                  final ShmSurfaceRenderState newShmSurfaceRenderState) {
        //TODO implement damage
        shmUpdateAll(wlSurfaceResource,
                     shmBuffer,
                     newShmSurfaceRenderState);
    }

    private void shmUpdateAll(final WlSurfaceResource wlSurfaceResource,
                              final ShmBuffer shmBuffer,
                              final ShmSurfaceRenderState newShmSurfaceRenderState) {
        this.libGLESv2.glBindTexture(newShmSurfaceRenderState.getTarget(),
                                     newShmSurfaceRenderState.getTexture());
        shmBuffer.beginAccess();
        this.libGLESv2.glTexImage2D(newShmSurfaceRenderState.getTarget(),
                                    0,
                                    newShmSurfaceRenderState.getGlFormat(),
                                    newShmSurfaceRenderState.getPitch(),
                                    newShmSurfaceRenderState.getHeight(),
                                    0,
                                    newShmSurfaceRenderState.getGlFormat(),
                                    newShmSurfaceRenderState.getGlPixelType(),
                                    JNI.unwrap(shmBuffer.getData()));
        shmBuffer.endAccess();
        this.libGLESv2.glBindTexture(newShmSurfaceRenderState.getTarget(),
                                     0);
        final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
        wlSurface.getSurface()
                 .firePaintCallbacks((int) NANOSECONDS.toMillis(System.nanoTime()));
    }

    private void drawShm(final @Nonnull WlSurfaceResource wlSurfaceResource,
                         final ShmSurfaceRenderState shmSurfaceRenderState) {
        final int shaderProgram = shmSurfaceRenderState.getShaderProgram();

        //activate & setup shader
        this.libGLESv2.glUseProgram(shaderProgram);
        setupVertexParams(wlSurfaceResource,
                          shmSurfaceRenderState.getPitch(),
                          shmSurfaceRenderState.getHeight());

        //set the buffer in the shader
        this.libGLESv2.glActiveTexture(GL_TEXTURE0);
        this.libGLESv2.glBindTexture(shmSurfaceRenderState.getTarget(),
                                     shmSurfaceRenderState.getTexture());
        this.libGLESv2.glUniform1i(this.textureArgs[0],
                                   0);

        //draw
        //enable texture blending
        this.libGLESv2.glEnable(GL_BLEND);
        this.libGLESv2.glDrawArrays(GL_TRIANGLES,
                                    0,
                                    6);

        //cleanup
        this.libGLESv2.glDisable(GL_BLEND);
        this.libGLESv2.glDisableVertexAttribArray(this.positionArg);
        this.libGLESv2.glDisableVertexAttribArray(this.textureArgs[0]);
        this.libGLESv2.glUseProgram(0);
    }

    private Optional<SurfaceRenderState> queryEglSurfaceRenderState(final EglPlatform eglPlatform,
                                                                    final WlSurfaceResource wlSurfaceResource,
                                                                    final EglBuffer eglBuffer) {
        @Nonnull
        Optional<SurfaceRenderState> surfaceRenderState = Optional.ofNullable(this.surfaceRenderStates.get(wlSurfaceResource));

        if (surfaceRenderState.isPresent()) {
            surfaceRenderState = surfaceRenderState.get()
                                                   .accept(new SurfaceRenderStateVisitor() {
                                                       @Override
                                                       public Optional<SurfaceRenderState> visit(final ShmSurfaceRenderState shmSurfaceRenderState) {
                                                           //the surface was previously associated with an shm render state but is now using an egl render state. create it.
                                                           //TODO we could reuse the texture id
                                                           destroy(shmSurfaceRenderState);
                                                           return createEglSurfaceRenderState(eglPlatform,
                                                                                              eglBuffer,
                                                                                              Optional.empty());
                                                       }

                                                       @Override
                                                       public Optional<SurfaceRenderState> visit(final EglSurfaceRenderState eglSurfaceRenderState) {
                                                           //the surface already has an egl render state associated. update it.
                                                           return createEglSurfaceRenderState(eglPlatform,
                                                                                              eglBuffer,
                                                                                              Optional.of(eglSurfaceRenderState));
                                                       }
                                                   });
        }
        else {
            //the surface was not previously associated with any render state. create an egl render state.
            surfaceRenderState = createEglSurfaceRenderState(eglPlatform,
                                                             eglBuffer,
                                                             Optional.empty());
        }

        if (surfaceRenderState.isPresent()) {
            this.surfaceRenderStates.put(wlSurfaceResource,
                                         surfaceRenderState.get());
        }
        else {
            onDestroy(wlSurfaceResource);
        }

        return surfaceRenderState;
    }

    private Optional<SurfaceRenderState> createEglSurfaceRenderState(final EglPlatform eglPlatform,
                                                                     final EglBuffer eglBuffer,
                                                                     final Optional<EglSurfaceRenderState> oldRenderState) {
        //surface egl render states:
        final int     pitch  = eglBuffer.getWidth();
        final int     height = eglBuffer.getHeight();
        final boolean yInverted;
        final int     shaderProgram;
        final int     target;
        final int[]   textures;
        final long[]  eglImages;

        //gather render states:
        final long eglDisplay = eglPlatform.getEglDisplay();
        final long buffer     = eglBuffer.getWlBufferResource().pointer;

        final EglQueryWaylandBufferWL queryWaylandBuffer = this.eglQueryWaylandBufferWL.get();

        final Pointer<Integer> yInvertedP = Pointer.nref(0);

        yInverted = queryWaylandBuffer.$(eglDisplay,
                                         buffer,
                                         EGL_WAYLAND_Y_INVERTED_WL,
                                         yInvertedP.address) == 0 || yInvertedP.dref() != 0;

        switch (eglBuffer.getTextureFormat()) {
            case EGL_TEXTURE_RGB:
            case EGL_TEXTURE_RGBA:
            default:
                textures = new int[1];
                eglImages = new long[1];
                target = GL_TEXTURE_2D;
                shaderProgram = this.argb8888ShaderProgram;
                break;
            case EGL_TEXTURE_EXTERNAL_WL:
                textures = new int[1];
                eglImages = new long[1];
                target = GL_TEXTURE_EXTERNAL_OES;
                shaderProgram = this.externalImageShaderProgram;
                break;
            case EGL_TEXTURE_Y_UV_WL:
                textures = new int[2];
                eglImages = new long[2];
                target = GL_TEXTURE_2D;
                shaderProgram = this.y_uvShaderProgram;
                break;
            case EGL_TEXTURE_Y_U_V_WL:
                textures = new int[3];
                eglImages = new long[3];
                target = GL_TEXTURE_2D;
                shaderProgram = this.y_u_vShaderProgram;
                break;
            case EGL_TEXTURE_Y_XUXV_WL:
                textures = new int[2];
                eglImages = new long[2];
                target = GL_TEXTURE_2D;
                shaderProgram = this.y_xuxvShaderProgram;
                break;
        }

        //delete old egl images
        oldRenderState.ifPresent(oldEglSurfaceRenderState -> {
            for (final long oldEglImage : oldEglSurfaceRenderState.getEglImages()) {
                this.eglDestroyImageKHR.get()
                                       .$(eglDisplay,
                                          oldEglImage);
            }
        });

        //create egl images
        final int[] attribs = new int[3];

        for (int i = 0; i < eglImages.length; i++) {
            attribs[0] = EGL_WAYLAND_PLANE_WL;
            attribs[1] = i;
            attribs[2] = EGL_NONE;

            final long eglImage = this.eglCreateImageKHR.get()
                                                        .$(eglDisplay,
                                                           EGL_NO_CONTEXT,
                                                           EGL_WAYLAND_BUFFER_WL,
                                                           buffer,
                                                           Pointer.nref(attribs).address);
            if (eglImage == EGL_NO_IMAGE_KHR) {
                return Optional.empty();
            }
            else {
                eglImages[i] = eglImage;
            }

            //make sure we have valid texture ids
            oldRenderState.ifPresent(oldEglSurfaceRenderState -> {

                final int[]   oldTextures      = oldEglSurfaceRenderState.getTextures();
                final int     deltaNewTextures = textures.length - oldTextures.length;
                final boolean needNewTextures  = deltaNewTextures > 0;

                //reuse old texture ids
                System.arraycopy(oldTextures,
                                 0,
                                 textures,
                                 0,
                                 needNewTextures ? oldTextures.length : textures.length);

                if (needNewTextures) {
                    //generate missing texture ids
                    for (int j = textures.length - 1; j >= textures.length - deltaNewTextures; j--) {
                        textures[j] = genTexture(target);
                    }
                }
                else if (deltaNewTextures < 0) {
                    //cleanup old unused texture ids
                    for (int j = oldTextures.length - 1; j >= oldTextures.length + deltaNewTextures; j--) {
                        this.libGLESv2.glDeleteTextures(1,
                                                        Pointer.nref(oldTextures[j]).address);
                    }
                }
            });

            this.libGLESv2.glActiveTexture(GL_TEXTURE0 + i);
            this.libGLESv2.glBindTexture(target,
                                         textures[i]);
            this.glEGLImageTargetTexture2DOES.get()
                                             .$(target,
                                                eglImage);
        }

        return Optional.of(EglSurfaceRenderState.create(pitch,
                                                        height,
                                                        target,
                                                        shaderProgram,
                                                        yInverted,
                                                        textures,
                                                        eglImages));
    }


    private void drawEgl(final EglPlatform eglPlatform,
                         final WlSurfaceResource wlSurfaceResource,
                         final EglBuffer eglBuffer) {
        queryEglSurfaceRenderState(eglPlatform,
                                   wlSurfaceResource,
                                   eglBuffer).ifPresent(surfaceRenderState -> surfaceRenderState.accept(new SurfaceRenderStateVisitor() {
            @Override
            public Optional<SurfaceRenderState> visit(final EglSurfaceRenderState eglSurfaceRenderState) {
                drawEgl(wlSurfaceResource,
                        eglSurfaceRenderState);
                return null;
            }
        }));
    }

    private void drawEgl(final WlSurfaceResource wlSurfaceResource,
                         final EglSurfaceRenderState eglSurfaceRenderState) {
        //TODO unify with drawShm

        final int shaderProgram = eglSurfaceRenderState.getShaderProgram();

        //activate & setup shader
        this.libGLESv2.glUseProgram(shaderProgram);
        setupVertexParams(wlSurfaceResource,
                          eglSurfaceRenderState.getPitch(),
                          eglSurfaceRenderState.getHeight());

        //set the buffer in the shader
        final int[] textures = eglSurfaceRenderState.getTextures();
        for (int i = 0, texturesLength = textures.length; i < texturesLength; i++) {
            final int texture = textures[i];
            final int target  = eglSurfaceRenderState.getTarget();

            this.libGLESv2.glActiveTexture(GL_TEXTURE0 + i);
            this.libGLESv2.glBindTexture(target,
                                         texture);
            this.libGLESv2.glTexParameteri(target,
                                           GL_TEXTURE_MIN_FILTER,
                                           GL_NEAREST);
            this.libGLESv2.glTexParameteri(target,
                                           GL_TEXTURE_MAG_FILTER,
                                           GL_NEAREST);
            this.libGLESv2.glUniform1i(this.textureArgs[i],
                                       0);
        }

        //draw
        //enable texture blending
        this.libGLESv2.glEnable(GL_BLEND);
        this.libGLESv2.glDrawArrays(GL_TRIANGLES,
                                    0,
                                    6);

        //cleanup
        this.libGLESv2.glDisable(GL_BLEND);
        this.libGLESv2.glDisableVertexAttribArray(this.positionArg);
        for (int i = 0, texturesLength = textures.length; i < texturesLength; i++) {
            this.libGLESv2.glDisableVertexAttribArray(this.textureArgs[i]);
        }
        this.libGLESv2.glUseProgram(0);

        final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
        wlSurface.getSurface()
                 .firePaintCallbacks((int) NANOSECONDS.toMillis(System.nanoTime()));
    }

    private int genTexture(final int target) {
        final Pointer<Integer> texture = Pointer.nref(0);
        this.libGLESv2.glGenTextures(1,
                                     texture.address);
        final Integer textureId = texture.dref();
        this.libGLESv2.glBindTexture(target,
                                     textureId);
        this.libGLESv2.glTexParameteri(target,
                                       GL_TEXTURE_WRAP_S,
                                       GL_CLAMP_TO_EDGE);
        this.libGLESv2.glTexParameteri(target,
                                       GL_TEXTURE_WRAP_T,
                                       GL_CLAMP_TO_EDGE);
        this.libGLESv2.glTexParameteri(target,
                                       GL_TEXTURE_MIN_FILTER,
                                       GL_NEAREST);
        this.libGLESv2.glTexParameteri(target,
                                       GL_TEXTURE_MAG_FILTER,
                                       GL_NEAREST);
        this.libGLESv2.glBindTexture(target,
                                     0);
        return textureId;
    }

    private void setupVertexParams(final @Nonnull WlSurfaceResource wlSurfaceResource,
                                   final float bufferWidth,
                                   final float bufferHeight) {
        final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
        final Surface   surface   = wlSurface.getSurface();
        final float[] transform = surface.getTransform()
                                         .toArray();

        //define vertex data
        final Pointer<Float> vertexData = vertexData(bufferWidth,
                                                     bufferHeight);

        //upload uniform vertex data
        final Pointer<Float> projectionBuffer = Pointer.nref(this.projection);
        this.libGLESv2.glUniformMatrix4fv(this.projectionArg,
                                          1,
                                          0,
                                          projectionBuffer.address);

        final Pointer<Float> transformBuffer = Pointer.nref(transform);
        this.libGLESv2.glUniformMatrix4fv(this.transformArg,
                                          1,
                                          0,
                                          transformBuffer.address);
        //set vertex data in shader
        this.libGLESv2.glEnableVertexAttribArray(this.positionArg);
        this.libGLESv2.glVertexAttribPointer(this.positionArg,
                                             2,
                                             GL_FLOAT,
                                             0,
                                             4 * Float.BYTES,
                                             vertexData.address);

        this.libGLESv2.glEnableVertexAttribArray(this.textureCoordinateArg);
        this.libGLESv2.glVertexAttribPointer(this.textureCoordinateArg,
                                             2,
                                             GL_FLOAT,
                                             0,
                                             4 * Float.BYTES,
                                             vertexData.offset(2).address);
    }

    private Pointer<Float> vertexData(final float bufferWidth,
                                      final float bufferHeight) {
        return Pointer.nref(//top left:
                            //attribute vec2 a_position
                            0f,
                            0f,
                            //attribute vec2 a_texCoord
                            0f,
                            0f,

                            //top right:
                            //attribute vec2 a_position
                            bufferWidth,
                            0f,
                            //attribute vec2 a_texCoord
                            1f,
                            0f,

                            //bottom right:
                            //vec2 a_position
                            bufferWidth,
                            bufferHeight,
                            //vec2 a_texCoord
                            1f,
                            1f,

                            //bottom right:
                            //vec2 a_position
                            bufferWidth,
                            bufferHeight,
                            //vec2 a_texCoord
                            1f,
                            1f,

                            //bottom left:
                            //vec2 a_position
                            0f,
                            bufferHeight,
                            //vec2 a_texCoord
                            0f,
                            1f,

                            //top left:
                            //attribute vec2 a_position
                            0f,
                            0f,
                            //attribute vec2 a_texCoord
                            0f,
                            0f);
    }

    private boolean bindDisplay(final long eglDisplay,
                                final String extensions) {
        if (extensions.contains("EGL_WL_bind_wayland_display")) {
            final Pointer<EglBindWaylandDisplayWL> eglBindWaylandDisplayWL = Pointer.wrap(EglBindWaylandDisplayWL.class,
                                                                                          this.libEGL.eglGetProcAddress(Pointer.nref("eglBindWaylandDisplayWL").address));
            return eglBindWaylandDisplayWL.dref()
                                          .$(eglDisplay,
                                             this.display.pointer) != 0;
        }
        else {
            LOGGER.warning("Extension EGL_WL_bind_wayland_display not available. Required for client side egl support.");
            return false;
        }
    }
}
