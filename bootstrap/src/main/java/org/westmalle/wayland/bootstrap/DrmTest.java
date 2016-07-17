package org.westmalle.wayland.bootstrap;


import org.freedesktop.jaccall.JNI;
import org.freedesktop.jaccall.Pointer;
import org.freedesktop.jaccall.Ptr;
import org.freedesktop.jaccall.Size;
import org.westmalle.wayland.core.calc.Mat4;
import org.westmalle.wayland.nativ.libEGL.EglCreatePlatformWindowSurfaceEXT;
import org.westmalle.wayland.nativ.libEGL.EglGetPlatformDisplayEXT;
import org.westmalle.wayland.nativ.libEGL.LibEGL;
import org.westmalle.wayland.nativ.libEGL.LibEGL_Symbols;
import org.westmalle.wayland.nativ.libGLESv2.LibGLESv2;
import org.westmalle.wayland.nativ.libGLESv2.LibGLESv2_Symbols;
import org.westmalle.wayland.nativ.libc.Libc;
import org.westmalle.wayland.nativ.libc.Libc_Symbols;
import org.westmalle.wayland.nativ.libdrm.DrmEventContext;
import org.westmalle.wayland.nativ.libdrm.DrmModeConnector;
import org.westmalle.wayland.nativ.libdrm.DrmModeEncoder;
import org.westmalle.wayland.nativ.libdrm.DrmModeModeInfo;
import org.westmalle.wayland.nativ.libdrm.DrmModeRes;
import org.westmalle.wayland.nativ.libdrm.Libdrm;
import org.westmalle.wayland.nativ.libdrm.Libdrm_Symbols;
import org.westmalle.wayland.nativ.libdrm.Pointerpage_flip_handler;
import org.westmalle.wayland.nativ.libdrm.Pointervblank_handler;
import org.westmalle.wayland.nativ.libgbm.Libgbm;
import org.westmalle.wayland.nativ.libgbm.Libgbm_Symbols;
import org.westmalle.wayland.nativ.libgbm.Pointerdestroy_user_data;
import org.westmalle.wayland.nativ.libudev.Libudev;
import org.westmalle.wayland.nativ.libudev.Libudev_Symbols;

import static org.freedesktop.jaccall.Pointer.malloc;
import static org.freedesktop.jaccall.Pointer.nref;
import static org.freedesktop.jaccall.Pointer.wrap;
import static org.freedesktop.jaccall.Size.sizeof;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_ALPHA_SIZE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_BACK_BUFFER;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_BLUE_SIZE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_CONTEXT_CLIENT_VERSION;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_EXTENSIONS;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_GREEN_SIZE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NONE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NO_CONTEXT;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_NO_DISPLAY;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_OPENGL_ES2_BIT;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_OPENGL_ES_API;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_PLATFORM_GBM_KHR;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_RED_SIZE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_RENDERABLE_TYPE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_RENDER_BUFFER;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_SURFACE_TYPE;
import static org.westmalle.wayland.nativ.libEGL.LibEGL.EGL_WINDOW_BIT;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_BGRA_EXT;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_BLEND;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_CLAMP_TO_EDGE;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_COLOR_BUFFER_BIT;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_COMPILE_STATUS;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_FLOAT;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_FRAGMENT_SHADER;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_INFO_LOG_LENGTH;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_LINK_STATUS;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_NEAREST;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE0;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_2D;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_MAG_FILTER;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_MIN_FILTER;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_WRAP_S;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TEXTURE_WRAP_T;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_TRIANGLES;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_UNSIGNED_BYTE;
import static org.westmalle.wayland.nativ.libGLESv2.LibGLESv2.GL_VERTEX_SHADER;
import static org.westmalle.wayland.nativ.libc.Libc.O_RDWR;
import static org.westmalle.wayland.nativ.libdrm.Libdrm.DRM_MODE_CONNECTED;
import static org.westmalle.wayland.nativ.libdrm.Libdrm.DRM_MODE_PAGE_FLIP_EVENT;
import static org.westmalle.wayland.nativ.libgbm.Libgbm.GBM_BO_USE_RENDERING;
import static org.westmalle.wayland.nativ.libgbm.Libgbm.GBM_BO_USE_SCANOUT;
import static org.westmalle.wayland.nativ.libgbm.Libgbm.GBM_FORMAT_XRGB8888;

public class DrmTest {

    private final Libudev   libudev;
    private final Libc      libc;
    private final Libdrm    libdrm;
    private final Libgbm    libgbm;
    private final LibEGL    libEGL;
    private final LibGLESv2 libGLESv2;
    private final int       drmFd;

    private int projectionArg;
    private int positionArg;
    private int textureCoordinateArg;
    private int textureArg;

    public static void main(final String[] args) throws InterruptedException {
        new DrmTest();
    }

    private DrmTest() throws InterruptedException {

        new Libudev_Symbols().link();
        this.libudev = new Libudev();
        new Libc_Symbols().link("libc.so.6");
        this.libc = new Libc();
        new Libdrm_Symbols().link();
        this.libdrm = new Libdrm();
        new Libgbm_Symbols().link();
        this.libgbm = new Libgbm();
        new LibEGL_Symbols().link();
        this.libEGL = new LibEGL();
        new LibGLESv2_Symbols().link();
        this.libGLESv2 = new LibGLESv2();

        final long udev = this.libudev.udev_new();
        if (udev == 0L) {
            throw new RuntimeException("Failed to initialize udev");
        }

        final long drmDevice = findPrimaryGpu(udev,
                                              "seat0");
        if (drmDevice == 0L) {
            throw new RuntimeException("No drm capable gpu device found.");
        }

        this.drmFd = initDrm(drmDevice);

        final long resources = this.libdrm.drmModeGetResources(this.drmFd);
        if (resources == 0L) {
            throw new RuntimeException("Getting drm resources failed.");
        }
        final DrmModeRes drmModeRes = wrap(DrmModeRes.class,
                                           resources).dref();

        final int countConnectors = drmModeRes.count_connectors();

        DrmModeConnector drmModeConnector = null;
        for (int i = 0; i < countConnectors; i++) {
            final long connector = this.libdrm.drmModeGetConnector(this.drmFd,
                                                                   drmModeRes.connectors()
                                                                             .dref(i));
            if (connector == 0L) {
                continue;
            }

            drmModeConnector = wrap(DrmModeConnector.class,
                                    connector).dref();
            if (drmModeConnector.connection() == DRM_MODE_CONNECTED) {
                break;
            }
            else {
                this.libdrm.drmModeFreeConnector(connector);
            }
        }

        if (drmModeConnector == null) {
            throw new RuntimeException("Could not find a valid connector.");
        }

        int             area = 0;
        DrmModeModeInfo mode = null;
        for (int i = 0; i < drmModeConnector.count_modes(); i++) {
            final DrmModeModeInfo currentMode = drmModeConnector.modes()
                                                                .dref(i);
            final int current_area = currentMode.hdisplay() * currentMode.vdisplay();
            if (current_area > area) {
                mode = currentMode;
                area = current_area;
            }
        }

        if (mode == null) {
            throw new RuntimeException("Could not find a valid mode.");
        }

        DrmModeEncoder drmModeEncoder = null;
        for (int j = 0; j < drmModeConnector.count_encoders(); j++) {
            final long encoder = this.libdrm.drmModeGetEncoder(this.drmFd,
                                                               drmModeConnector.encoders()
                                                                               .dref(j));
            drmModeEncoder = wrap(DrmModeEncoder.class,
                                  encoder).dref();
            if (drmModeEncoder.encoder_id() == drmModeConnector.encoder_id()) {
                break;
            }
            else {
                this.libdrm.drmModeFreeEncoder(encoder);
            }
        }

        if (drmModeEncoder == null) {
            throw new RuntimeException("Could not find a valid encoder.");
        }


        final long gbmDevice  = this.libgbm.gbm_create_device(this.drmFd);
        final long eglDisplay = createEglDisplay(gbmDevice);

        final long eglConfig = eglConfig(eglDisplay);
        final long eglContext = createEglContext(eglDisplay,
                                                 eglConfig);

        final long gbmSurface = this.libgbm.gbm_surface_create(gbmDevice,
                                                               mode.hdisplay(),
                                                               mode.vdisplay(),
                                                               GBM_FORMAT_XRGB8888,
                                                               GBM_BO_USE_SCANOUT | GBM_BO_USE_RENDERING);

        if (gbmSurface == 0) {
            throw new RuntimeException("failed to create gbm surface");
        }

        final long eglSurface = createEglSurface(eglDisplay,
                                                 eglConfig,
                                                 gbmSurface);

        this.libEGL.eglMakeCurrent(eglDisplay,
                                   eglSurface,
                                   eglSurface,
                                   eglContext);

        final String VERTEX_SHADER =
                "uniform mat4 u_projection;\n" +
                "attribute vec2 a_position;\n" +
                "attribute vec2 a_texCoord;\n" +
                "varying vec2 v_texCoord;\n" +
                "void main(){\n" +
                "    v_texCoord = a_texCoord;\n" +
                "    gl_Position = u_projection * vec4(a_position, 0.0, 1.0) ;\n" +
                "}";

        final String FRAGMENT_SHADER =
                "precision mediump float;\n" +
                "uniform sampler2D u_texture0;\n" +
                "varying vec2 v_texCoord;\n" +
                "void main(){\n" +
                "    gl_FragColor = texture2D(u_texture0, v_texCoord);\n" +
                "}";

        final int shaderProgram = createShaderProgram(VERTEX_SHADER,
                                                      FRAGMENT_SHADER);

        final int textureId = genTexture();

        this.libGLESv2.glClearColor(0.5f,
                                    0.5f,
                                    0.5f,
                                    1.0f);
        this.libGLESv2.glClear(GL_COLOR_BUFFER_BIT);
        this.libEGL.eglSwapBuffers(eglDisplay,
                                   eglSurface);

        long gbmBo = this.libgbm.gbm_surface_lock_front_buffer(gbmSurface);
        int  fbId  = getFbId(gbmBo);
        this.libdrm.drmModeSetCrtc(this.drmFd,
                                   drmModeEncoder.crtc_id(),
                                   fbId,
                                   0,
                                   0,
                                   Pointer.nref(drmModeConnector.connector_id()).address,
                                   1,
                                   Pointer.ref(mode).address);
        int i = 0;

        final Pointer<DrmEventContext> drmEventContextP = Pointer.malloc(DrmEventContext.SIZE,
                                                                         DrmEventContext.class);
        final DrmEventContext drmEventContext = drmEventContextP.dref();
        drmEventContext.version(Libdrm.DRM_EVENT_CONTEXT_VERSION);
        drmEventContext.page_flip_handler(Pointerpage_flip_handler.nref((fd, sequence, tv_sec, tv_usec, user_data) -> {
            //System.out.println("pageflip!");
        }));
        drmEventContext.vblank_handler(Pointervblank_handler.nref((fd, sequence, tv_sec, tv_usec, user_data) -> {
            //System.out.println("vblank!");
        }));

        while (true) {
            i++;
            draw(i,
                 textureId,
                 shaderProgram,
                 mode.hdisplay(),
                 mode.vdisplay());

            this.libEGL.eglSwapBuffers(eglDisplay,
                                       eglSurface);
            final long next_bo = this.libgbm.gbm_surface_lock_front_buffer(gbmSurface);
            fbId = getFbId(next_bo);

            final int ret = this.libdrm.drmModePageFlip(this.drmFd,
                                                        drmModeEncoder.crtc_id(),
                                                        fbId,
                                                        DRM_MODE_PAGE_FLIP_EVENT,
                                                        0L);
            if (ret != 0) {
                throw new RuntimeException(String.format("failed to queue page flip: %d\n",
                                                         this.libc.getErrno()));
            }

            //FIXME fugly, normally we listen for fd events & handle a pageflip callback, now we just draw at 10fps and assume the pageflip occurred.
            Thread.sleep(250);

            this.libdrm.drmHandleEvent(this.drmFd,
                                       drmEventContextP.address);

		/* release last buffer to render on again: */
            this.libgbm.gbm_surface_release_buffer(gbmSurface,
                                                   gbmBo);
            gbmBo = next_bo;
        }
    }


    private void draw(final int i,
                      final int textureId,
                      final int shaderProgram,
                      final short hdisplay,
                      final short vdisplay) {


        // Set the viewport
        this.libGLESv2.glViewport(0,
                                  0,
                                  hdisplay,
                                  vdisplay);

        this.libGLESv2.glClearColor(1.0f,
                                    1.0f,
                                    1.0f,
                                    1.0f);
        this.libGLESv2.glClear(LibGLESv2.GL_COLOR_BUFFER_BIT);

        //@formatter:off
        final float[] projection = Mat4.create(2.0f / hdisplay, 0,                0, -1,
                                               0,               2.0f / -vdisplay, 0,  1,
                                               0,               0,                1,  0,
                                               0,               0,                0,  1).toArray();
        //@formatter:on

        //upload texture
        this.libGLESv2.glBindTexture(GL_TEXTURE_2D,
                                     textureId);
        final byte[] textureBuffer = new byte[100 * 100 * 4];
        for (int i1 = 0, textureBufferLength = textureBuffer.length; i1 < textureBufferLength; i1++) {
            textureBuffer[i1] = 1;
        }

        this.libGLESv2.glTexImage2D(GL_TEXTURE_2D,
                                    0,
                                    GL_BGRA_EXT,
                                    100,
                                    100,
                                    0,
                                    GL_BGRA_EXT,
                                    GL_UNSIGNED_BYTE,
                                    Pointer.nref(textureBuffer).address);

        // Use the program object
        this.libGLESv2.glUseProgram(shaderProgram);


        //define vertex data
        final Pointer<Float> vertexData = vertexData(100,
                                                     100);

        //upload uniform vertex data
        final Pointer<Float> projectionBuffer = Pointer.nref(projection);
        this.libGLESv2.glUniformMatrix4fv(this.projectionArg,
                                          1,
                                          0,
                                          projectionBuffer.address);

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


        //set the buffer in the shader
        this.libGLESv2.glActiveTexture(GL_TEXTURE0);
        this.libGLESv2.glBindTexture(GL_TEXTURE_2D,
                                     textureId);
        this.libGLESv2.glUniform1i(this.textureArg,
                                   0);

        //enable texture blending
        this.libGLESv2.glEnable(GL_BLEND);

        this.libGLESv2.glDrawArrays(GL_TRIANGLES,
                                    0,
                                    6);
    }

    private int genTexture() {
        final Pointer<Integer> texture = Pointer.nref(0);
        this.libGLESv2.glGenTextures(1,
                                     texture.address);
        final Integer textureId = texture.dref();
        this.libGLESv2.glBindTexture(GL_TEXTURE_2D,
                                     textureId);
        this.libGLESv2.glTexParameteri(GL_TEXTURE_2D,
                                       GL_TEXTURE_WRAP_S,
                                       GL_CLAMP_TO_EDGE);
        this.libGLESv2.glTexParameteri(GL_TEXTURE_2D,
                                       GL_TEXTURE_WRAP_T,
                                       GL_CLAMP_TO_EDGE);
        this.libGLESv2.glTexParameteri(GL_TEXTURE_2D,
                                       GL_TEXTURE_MIN_FILTER,
                                       GL_NEAREST);
        this.libGLESv2.glTexParameteri(GL_TEXTURE_2D,
                                       GL_TEXTURE_MAG_FILTER,
                                       GL_NEAREST);
        this.libGLESv2.glBindTexture(GL_TEXTURE_2D,
                                     0);
        return textureId;
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

    private int getFbId(final long gbmBo) {
        final long fbIdP = this.libgbm.gbm_bo_get_user_data(gbmBo);
        if (fbIdP != 0L) {
            return Pointer.wrap(Integer.class,
                                fbIdP)
                          .dref();
        }

        final Pointer<Integer> fb = Pointer.calloc(1,
                                                   Size.sizeof((Integer) null),
                                                   Integer.class);
        final int width  = this.libgbm.gbm_bo_get_width(gbmBo);
        final int height = this.libgbm.gbm_bo_get_height(gbmBo);
        final int stride = this.libgbm.gbm_bo_get_stride(gbmBo);
        final int handle = (int) this.libgbm.gbm_bo_get_handle(gbmBo);
        final int ret = this.libdrm.drmModeAddFB(this.drmFd,
                                                 width,
                                                 height,
                                                 (byte) 24,
                                                 (byte) 32,
                                                 stride,
                                                 handle,
                                                 fb.address);
        if (ret != 0) {
            throw new RuntimeException("failed to create fb");
        }


        this.libgbm.gbm_bo_set_user_data(gbmBo,
                                         fb.address,
                                         Pointerdestroy_user_data.nref(this::destroyUserData).address);

        return fb.dref();
    }

    private void destroyUserData(@Ptr final long bo,
                                 @Ptr final long data) {
        final Pointer<Integer> fbIdP = Pointer.wrap(Integer.class,
                                                    data);
        final Integer fbId = fbIdP.dref();
        this.libdrm.drmModeRmFB(this.drmFd,
                                fbId);
        fbIdP.close();

        System.out.println("fb user data destroyed!");
    }

    private long createEglSurface(final long eglDisplay,
                                  final long config,
                                  final long gbmSurface) {
        final Pointer<Integer> eglSurfaceAttribs = Pointer.nref(EGL_RENDER_BUFFER,
                                                                EGL_BACK_BUFFER,
                                                                EGL_NONE);

        final Pointer<EglCreatePlatformWindowSurfaceEXT> eglGetPlatformDisplayEXT = Pointer.wrap(EglCreatePlatformWindowSurfaceEXT.class,
                                                                                                 this.libEGL.eglGetProcAddress(Pointer.nref("eglCreatePlatformWindowSurfaceEXT").address));
        final long eglSurface = eglGetPlatformDisplayEXT.dref()
                                                        .$(eglDisplay,
                                                           config,
                                                           gbmSurface,
                                                           eglSurfaceAttribs.address);
        if (eglSurface == 0L) {
            throw new RuntimeException("eglCreateWindowSurface() failed");
        }

        return eglSurface;
    }

    private long createEglContext(final long eglDisplay,
                                  final long config) {
        final Pointer<?> eglContextAttribs = Pointer.nref(
                //@formatter:off
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL_NONE
                //@formatter:on
                                                         );
        final long context = this.libEGL.eglCreateContext(eglDisplay,
                                                          config,
                                                          EGL_NO_CONTEXT,
                                                          eglContextAttribs.address);
        if (context == 0L) {
            throw new RuntimeException("eglCreateContext() failed");
        }
        return context;
    }

    private long eglConfig(final long eglDisplay) {
        assert (eglDisplay != EGL_NO_DISPLAY);

        if (this.libEGL.eglBindAPI(EGL_OPENGL_ES_API) == 0L) {
            throw new RuntimeException("eglBindAPI failed");
        }

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

        return configs.dref().address;
    }

    private long createEglDisplay(final long gbmDevice) {

        final Pointer<String> noDisplayExtensions = Pointer.wrap(String.class,
                                                                 this.libEGL.eglQueryString(EGL_NO_DISPLAY,
                                                                                            EGL_EXTENSIONS));
        if (noDisplayExtensions.address == 0L) {
            throw new RuntimeException("Could not query egl extensions.");
        }
        final String extensions = noDisplayExtensions.dref();

        if (!extensions.contains("EGL_MESA_platform_gbm")) {
            throw new RuntimeException("Required extension EGL_MESA_platform_gbm not available.");
        }

        final Pointer<EglGetPlatformDisplayEXT> eglGetPlatformDisplayEXT = Pointer.wrap(EglGetPlatformDisplayEXT.class,
                                                                                        this.libEGL.eglGetProcAddress(Pointer.nref("eglGetPlatformDisplayEXT").address));

        final long eglDisplay = eglGetPlatformDisplayEXT.dref()
                                                        .$(EGL_PLATFORM_GBM_KHR,
                                                           gbmDevice,
                                                           0L);
        if (eglDisplay == 0L) {
            throw new RuntimeException("eglGetDisplay() failed");
        }
        if (this.libEGL.eglInitialize(eglDisplay,
                                      0L,
                                      0L) == 0) {
            throw new RuntimeException("eglInitialize() failed");
        }

        return eglDisplay;
    }

    private int initDrm(final long device) {
        final long sysnum = this.libudev.udev_device_get_sysnum(device);
        final int  drmId;
        if (sysnum != 0) {
            drmId = Integer.parseInt(wrap(String.class,
                                          sysnum)
                                             .dref());
        }
        else {
            drmId = 0;
        }
        if (sysnum == 0 || drmId < 0) {
            throw new RuntimeException("Failed to open drm device.");
        }

        final long filename = this.libudev.udev_device_get_devnode(device);
        final int fd = this.libc.open(filename,
                                      O_RDWR);
        if (fd < 0) {
            throw new RuntimeException("Failed to open drm device.");
        }

        return fd;
    }

    private long findPrimaryGpu(final long udev,
                                final String seat) {

        final long udevEnumerate = this.libudev.udev_enumerate_new(udev);
        this.libudev.udev_enumerate_add_match_subsystem(udevEnumerate,
                                                        nref("drm").address);
        this.libudev.udev_enumerate_add_match_sysname(udevEnumerate,
                                                      nref("card[0-9]*").address);

        this.libudev.udev_enumerate_scan_devices(udevEnumerate);
        long drmDevice = 0L;

        for (long entry = this.libudev.udev_enumerate_get_list_entry(udevEnumerate);
             entry != 0L;
             entry = this.libudev.udev_list_entry_get_next(entry)) {

            final long path = this.libudev.udev_list_entry_get_name(entry);
            final long device = this.libudev.udev_device_new_from_syspath(udev,
                                                                          path);
            if (device == 0) {
                //no device, process next entry
                continue;

            }
            final String deviceSeat;
            final long seatId = this.libudev.udev_device_get_property_value(device,
                                                                            nref("ID_SEAT").address);
            if (seatId == 0) {
                //device does not have a seat, assign it a default one.
                deviceSeat = Libudev.DEFAULT_SEAT;
            }
            else {
                deviceSeat = wrap(String.class,
                                  seatId).dref();
            }
            if (!deviceSeat.equals(seat)) {
                //device has a seat, but not the one we want, process next entry
                this.libudev.udev_device_unref(device);
                continue;
            }

            final long pci = this.libudev.udev_device_get_parent_with_subsystem_devtype(device,
                                                                                        nref("pci").address,
                                                                                        0L);
            if (pci != 0) {
                final long id = this.libudev.udev_device_get_sysattr_value(pci,
                                                                           nref("boot_vga").address);
                if (id != 0L && wrap(String.class,
                                     id).dref()
                                        .equals("1")) {
                    if (drmDevice != 0L) {
                        this.libudev.udev_device_unref(drmDevice);
                    }
                    drmDevice = device;
                    break;
                }
            }

            if (drmDevice == 0L) {
                drmDevice = device;
            }
            else {
                this.libudev.udev_device_unref(device);
            }
        }

        this.libudev.udev_enumerate_unref(udevEnumerate);
        return drmDevice;
    }

    private int createShaderProgram(final String vertexShaderSource,
                                    final String fragmentShaderSource) {
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
        this.positionArg = this.libGLESv2.glGetAttribLocation(shaderProgram,
                                                              Pointer.nref("a_position").address);
        this.textureCoordinateArg = this.libGLESv2.glGetAttribLocation(shaderProgram,
                                                                       Pointer.nref("a_texCoord").address);
        this.textureArg = this.libGLESv2.glGetUniformLocation(shaderProgram,
                                                              Pointer.nref("u_texture").address);


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
}