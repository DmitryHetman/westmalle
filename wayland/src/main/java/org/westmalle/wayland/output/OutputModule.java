//Copyright 2015 Erik De Rijcke
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
package org.westmalle.wayland.output;

import com.google.common.util.concurrent.Service;
import com.sun.jna.Native;
import dagger.Module;
import dagger.Provides;
import org.freedesktop.wayland.server.Display;
import org.westmalle.wayland.platform.CLibrary;

import javax.inject.Singleton;

import static dagger.Provides.Type.SET;

@Module
public class OutputModule {

    @Provides
    @Singleton
    GLDrawables provideGLGlDrawables() {
        return new GLDrawables();
    }

    @Provides
    @Singleton
    CLibrary provideCLibrary() {
        return (CLibrary) Native.loadLibrary(CLibrary.JNA_LIBRARY_NAME,
                                             CLibrary.class);
    }

    @Provides
    @Singleton
    Display provideDisplay() {
        return Display.create();
    }

    @Singleton
    @Provides
    JobExecutor provideWlJobExecutor(final Display display,
                                     final CLibrary libc) {
        final int[] pipe = configure(pipe(libc),
                                     libc);
        final int pipeR  = pipe[0];
        final int pipeWR = pipe[1];

        return new JobExecutor(display,
                               pipeR,
                               pipeWR,
                               libc);
    }

    private int[] pipe(final CLibrary libc) {
        final int[] pipeFds = new int[2];
        libc.pipe(pipeFds);
        return pipeFds;
    }

    private int[] configure(final int[] pipeFds,
                            final CLibrary libc) {
        final int readFd  = pipeFds[0];
        final int writeFd = pipeFds[1];

        final int readFlags = libc.fcntl(readFd,
                                         CLibrary.F_GETFD,
                                         0);
        libc.fcntl(readFd,
                   CLibrary.F_SETFD,
                   readFlags | CLibrary.FD_CLOEXEC);

        final int writeFlags = libc.fcntl(writeFd,
                                          CLibrary.F_GETFD,
                                          0);
        libc.fcntl(writeFd,
                   CLibrary.F_SETFD,
                   writeFlags | CLibrary.FD_CLOEXEC);

        return pipeFds;
    }

    @Provides(type = SET)
    Service provideService(final ShellService shellService) {
        return shellService;
    }
}
