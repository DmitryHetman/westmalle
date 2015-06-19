package org.westmalle.wayland.x11;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.westmalle.wayland.nativ.LibEGL;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.westmalle.wayland.nativ.LibEGL.*;

@RunWith(MockitoJUnitRunner.class)
public class X11EglOutputFactoryTest {

    @Mock
    private LibEGL              libEGL;
    @InjectMocks
    private X11EglOutputFactory x11EglOutputFactory;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testCreateNoEsAPi() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("eglBindAPI failed");

        final Pointer display = mock(Pointer.class);
        final int     window  = 12345;
        when(this.libEGL.eglBindAPI(EGL_OPENGL_ES_API)).thenReturn(false);
        when(this.libEGL.eglBindAPI(EGL_OPENGL_API)).thenReturn(true);
        //when
        this.x11EglOutputFactory.create(display,
                                        window);
        //then
        verify(this.libEGL).eglBindAPI(EGL_OPENGL_ES_API);
        //runtime exception is thrown
        verifyNoMoreInteractions(this.libEGL);
    }


    @Test
    public void testCreateNoEglDisplay() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("eglGetDisplay() failed");

        final Pointer display = mock(Pointer.class);
        final int     window  = 12345;
        when(this.libEGL.eglBindAPI(EGL_OPENGL_ES_API)).thenReturn(true);
        when(this.libEGL.eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                  display,
                                                  null)).thenReturn(Pointer.createConstant(0));
        //when
        this.x11EglOutputFactory.create(display,
                                        window);
        //then
        verify(this.libEGL).eglBindAPI(EGL_OPENGL_ES_API);
        verify(this.libEGL).eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                     display,
                                                     null);
        //runtime exception is thrown
        verifyNoMoreInteractions(this.libEGL);
    }

    @Test
    public void testCreateFailedInit() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("eglInitialize() failed");

        final Pointer display    = mock(Pointer.class);
        final int     window     = 12345;
        final Pointer eglDisplay = mock(Pointer.class);

        when(this.libEGL.eglBindAPI(EGL_OPENGL_ES_API)).thenReturn(true);
        when(this.libEGL.eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                  display,
                                                  null)).thenReturn(eglDisplay);
        when(this.libEGL.eglInitialize(eglDisplay,
                                       null,
                                       null)).thenReturn(false);
        //when
        this.x11EglOutputFactory.create(display,
                                        window);
        //then
        verify(this.libEGL).eglBindAPI(EGL_OPENGL_ES_API);
        verify(this.libEGL).eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                     display,
                                                     null);
        verify(this.libEGL).eglInitialize(eglDisplay,
                                          null,
                                          null);
        //runtime exception is thrown
        verifyNoMoreInteractions(this.libEGL);
    }

    @Test
    public void testCreateFailedChooseConfig() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("eglChooseConfig() failed");

        final Pointer display            = mock(Pointer.class);
        final int     window             = 12345;
        final Pointer eglDisplay         = mock(Pointer.class);
        final Pointer num_configs        = mock(Pointer.class);
        final Pointer egl_config_attribs = mock(Pointer.class);
        final Pointer configs            = mock(Pointer.class);
        final int     configs_size       = 256 * Pointer.SIZE;

        when(this.libEGL.eglBindAPI(EGL_OPENGL_ES_API)).thenReturn(true);
        when(this.libEGL.eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                  display,
                                                  null)).thenReturn(eglDisplay);
        when(this.libEGL.eglInitialize(eglDisplay,
                                       null,
                                       null)).thenReturn(true);
        when(this.libEGL.eglChooseConfig(eglDisplay,
                                         egl_config_attribs,
                                         configs,
                                         configs_size,
                                         num_configs)).thenReturn(false);
        //when
        this.x11EglOutputFactory.create(display,
                                        window);
        //then
        verify(this.libEGL).eglBindAPI(EGL_OPENGL_ES_API);
        verify(this.libEGL).eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                     display,
                                                     null);
        verify(this.libEGL).eglInitialize(eglDisplay,
                                          null,
                                          null);
        verify(this.libEGL).eglChooseConfig(eglDisplay,
                                            egl_config_attribs,
                                            configs,
                                            configs_size,
                                            num_configs);
        //runtime exception is thrown
        verifyNoMoreInteractions(this.libEGL);
    }

    @Test
    public void testCreateNoConfigs() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("failed to find suitable EGLConfig");

        final Pointer display    = mock(Pointer.class);
        final int     window     = 12345;
        final Pointer eglDisplay = mock(Pointer.class);

        when(this.libEGL.eglBindAPI(EGL_OPENGL_ES_API)).thenReturn(true);
        when(this.libEGL.eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                  display,
                                                  null)).thenReturn(eglDisplay);
        when(this.libEGL.eglInitialize(eglDisplay,
                                       null,
                                       null)).thenReturn(true);
        when(this.libEGL.eglChooseConfig(eq(eglDisplay),
                                         any(),
                                         any(),
                                         anyInt(),
                                         any())).thenAnswer(invocation -> {
            Object arg4 = invocation.getArguments()[4];
            final Memory num_configs1 = (Memory) arg4;
            num_configs1.setInt(0,
                                0);
            return true;
        });

        //when
        this.x11EglOutputFactory.create(display,
                                        window);
        //then
        verify(this.libEGL).eglBindAPI(EGL_OPENGL_ES_API);
        verify(this.libEGL).eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                     display,
                                                     null);
        verify(this.libEGL).eglInitialize(eglDisplay,
                                          null,
                                          null);
        verify(this.libEGL).eglChooseConfig(eq(eglDisplay),
                                            any(),
                                            any(),
                                            anyInt(),
                                            any());
        //runtime exception is thrown
        verifyNoMoreInteractions(this.libEGL);
    }

    @Test
    public void testCreateFailedContextCreation() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("eglCreateContext() failed");

        final Pointer display    = mock(Pointer.class);
        final int     window     = 12345;
        final Pointer eglDisplay = mock(Pointer.class);
        final Pointer config     = new Memory(Pointer.SIZE);

        when(this.libEGL.eglBindAPI(EGL_OPENGL_ES_API)).thenReturn(true);
        when(this.libEGL.eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                  display,
                                                  null)).thenReturn(eglDisplay);
        when(this.libEGL.eglInitialize(eglDisplay,
                                       null,
                                       null)).thenReturn(true);
        when(this.libEGL.eglChooseConfig(eq(eglDisplay),
                                         any(),
                                         any(),
                                         anyInt(),
                                         any())).thenAnswer(invocation -> {
            final Object arg2 = invocation.getArguments()[2];
            final Memory configs = (Memory) arg2;
            configs.setPointer(0,
                               config);

            final Object arg4 = invocation.getArguments()[4];
            final Memory num_configs = (Memory) arg4;
            num_configs.setInt(0,
                               16);
            return true;
        });
        when(this.libEGL.eglCreateContext(eq(eglDisplay),
                                          eq(config),
                                          eq(EGL_NO_CONTEXT),
                                          any())).thenReturn(null);

        //when
        this.x11EglOutputFactory.create(display,
                                        window);
        //then
        verify(this.libEGL).eglBindAPI(EGL_OPENGL_ES_API);
        verify(this.libEGL).eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                     display,
                                                     null);
        verify(this.libEGL).eglInitialize(eglDisplay,
                                          null,
                                          null);
        verify(this.libEGL).eglChooseConfig(eq(eglDisplay),
                                            any(),
                                            any(),
                                            anyInt(),
                                            any());
        verify(this.libEGL).eglCreateContext(eq(eglDisplay),
                                             eq(config),
                                             eq(EGL_NO_CONTEXT),
                                             any());
        //runtime exception is thrown
        verifyNoMoreInteractions(this.libEGL);
    }

    @Test
    public void testCreateFailedSurfaceCreation() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("eglCreateWindowSurface() failed");

        final Pointer display    = mock(Pointer.class);
        final int     window     = 12345;
        final Pointer eglDisplay = mock(Pointer.class);
        final Pointer config     = new Memory(Pointer.SIZE);
        final Pointer context    = mock(Pointer.class);

        when(this.libEGL.eglBindAPI(EGL_OPENGL_ES_API)).thenReturn(true);
        when(this.libEGL.eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                  display,
                                                  null)).thenReturn(eglDisplay);
        when(this.libEGL.eglInitialize(eglDisplay,
                                       null,
                                       null)).thenReturn(true);
        when(this.libEGL.eglChooseConfig(eq(eglDisplay),
                                         any(),
                                         any(),
                                         anyInt(),
                                         any())).thenAnswer(invocation -> {
            final Object arg2 = invocation.getArguments()[2];
            final Memory configs = (Memory) arg2;
            configs.setPointer(0,
                               config);

            final Object arg4 = invocation.getArguments()[4];
            final Memory num_configs = (Memory) arg4;
            num_configs.setInt(0,
                               16);
            return true;
        });
        when(this.libEGL.eglCreateContext(eq(eglDisplay),
                                          eq(config),
                                          eq(EGL_NO_CONTEXT),
                                          any())).thenReturn(context);
        when(this.libEGL.eglCreatePlatformWindowSurfaceEXT(eq(eglDisplay),
                                                           eq(config),
                                                           any(),
                                                           any())).thenReturn(null);
        //when
        this.x11EglOutputFactory.create(display,
                                        window);
        //then
        verify(this.libEGL).eglBindAPI(EGL_OPENGL_ES_API);
        verify(this.libEGL).eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                     display,
                                                     null);
        verify(this.libEGL).eglInitialize(eglDisplay,
                                          null,
                                          null);
        verify(this.libEGL).eglChooseConfig(eq(eglDisplay),
                                            any(),
                                            any(),
                                            anyInt(),
                                            any());
        verify(this.libEGL).eglCreateContext(eq(eglDisplay),
                                             eq(config),
                                             eq(EGL_NO_CONTEXT),
                                             any());
        verify(this.libEGL).eglCreatePlatformWindowSurfaceEXT(eq(eglDisplay),
                                                              eq(config),
                                                              any(),
                                                              any());
        //runtime exception is thrown
        verifyNoMoreInteractions(this.libEGL);
    }

    @Test
    public void testCreateFailedMakeCurrent() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("eglMakeCurrent() failed");

        final Pointer display    = mock(Pointer.class);
        final int     window     = 12345;
        final Pointer eglDisplay = mock(Pointer.class);
        final Pointer config     = new Memory(Pointer.SIZE);
        final Pointer context    = mock(Pointer.class);
        final Pointer eglSurface = mock(Pointer.class);

        when(this.libEGL.eglBindAPI(EGL_OPENGL_ES_API)).thenReturn(true);
        when(this.libEGL.eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                  display,
                                                  null)).thenReturn(eglDisplay);
        when(this.libEGL.eglInitialize(eglDisplay,
                                       null,
                                       null)).thenReturn(true);
        when(this.libEGL.eglChooseConfig(eq(eglDisplay),
                                         any(),
                                         any(),
                                         anyInt(),
                                         any())).thenAnswer(invocation -> {
            final Object arg2 = invocation.getArguments()[2];
            final Memory configs = (Memory) arg2;
            configs.setPointer(0,
                               config);

            final Object arg4 = invocation.getArguments()[4];
            final Memory num_configs = (Memory) arg4;
            num_configs.setInt(0,
                               16);
            return true;
        });
        when(this.libEGL.eglCreateContext(eq(eglDisplay),
                                          eq(config),
                                          eq(EGL_NO_CONTEXT),
                                          any())).thenReturn(context);
        when(this.libEGL.eglCreatePlatformWindowSurfaceEXT(eq(eglDisplay),
                                                           eq(config),
                                                           any(),
                                                           any())).thenReturn(eglSurface);
        when(this.libEGL.eglMakeCurrent(eglDisplay,
                                        eglSurface,
                                        eglSurface,
                                        context)).thenReturn(false);
        //when
        this.x11EglOutputFactory.create(display,
                                        window);
        //then
        verify(this.libEGL).eglBindAPI(EGL_OPENGL_ES_API);
        verify(this.libEGL).eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                     display,
                                                     null);
        verify(this.libEGL).eglInitialize(eglDisplay,
                                          null,
                                          null);
        verify(this.libEGL).eglChooseConfig(eq(eglDisplay),
                                            any(),
                                            any(),
                                            anyInt(),
                                            any());
        verify(this.libEGL).eglCreateContext(eq(eglDisplay),
                                             eq(config),
                                             eq(EGL_NO_CONTEXT),
                                             any());
        verify(this.libEGL).eglCreatePlatformWindowSurfaceEXT(eq(eglDisplay),
                                                              eq(config),
                                                              any(),
                                                              any());
        verify(this.libEGL).eglMakeCurrent(eglDisplay,
                                           eglSurface,
                                           eglSurface,
                                           context);
        //runtime exception is thrown
        verifyNoMoreInteractions(this.libEGL);
    }

    @Test
    public void testCreate() throws Exception {
        //given
        final Pointer display    = mock(Pointer.class);
        final int     window     = 12345;
        final Pointer eglDisplay = mock(Pointer.class);
        final Pointer config     = new Memory(Pointer.SIZE);
        final Pointer context    = mock(Pointer.class);
        final Pointer eglSurface = mock(Pointer.class);

        when(this.libEGL.eglBindAPI(EGL_OPENGL_ES_API)).thenReturn(true);
        when(this.libEGL.eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                  display,
                                                  null)).thenReturn(eglDisplay);
        when(this.libEGL.eglInitialize(eglDisplay,
                                       null,
                                       null)).thenReturn(true);
        when(this.libEGL.eglChooseConfig(eq(eglDisplay),
                                         any(),
                                         any(),
                                         anyInt(),
                                         any())).thenAnswer(invocation -> {
            final Object arg2 = invocation.getArguments()[2];
            final Memory configs = (Memory) arg2;
            configs.setPointer(0,
                               config);

            final Object arg4 = invocation.getArguments()[4];
            final Memory num_configs = (Memory) arg4;
            num_configs.setInt(0,
                               16);
            return true;
        });
        when(this.libEGL.eglCreateContext(eq(eglDisplay),
                                          eq(config),
                                          eq(EGL_NO_CONTEXT),
                                          any())).thenReturn(context);
        when(this.libEGL.eglCreatePlatformWindowSurfaceEXT(eq(eglDisplay),
                                                           eq(config),
                                                           any(),
                                                           any())).thenReturn(eglSurface);
        when(this.libEGL.eglMakeCurrent(eglDisplay,
                                        eglSurface,
                                        eglSurface,
                                        context)).thenReturn(true);

        //when
        this.x11EglOutputFactory.create(display,
                                        window);
        //then
        verify(this.libEGL).eglBindAPI(EGL_OPENGL_ES_API);
        verify(this.libEGL).eglGetPlatformDisplayEXT(EGL_PLATFORM_X11_KHR,
                                                     display,
                                                     null);
        verify(this.libEGL).eglInitialize(eglDisplay,
                                          null,
                                          null);
        verify(this.libEGL).eglChooseConfig(eq(eglDisplay),
                                            any(),
                                            any(),
                                            anyInt(),
                                            any());
        verify(this.libEGL).eglCreateContext(eq(eglDisplay),
                                             eq(config),
                                             eq(EGL_NO_CONTEXT),
                                             any());
        verify(this.libEGL).eglCreatePlatformWindowSurfaceEXT(eq(eglDisplay),
                                                              eq(config),
                                                              any(),
                                                              any());
        verify(this.libEGL).eglMakeCurrent(eglDisplay,
                                           eglSurface,
                                           eglSurface,
                                           context);
    }
}