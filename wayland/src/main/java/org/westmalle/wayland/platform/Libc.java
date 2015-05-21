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
package org.westmalle.wayland.platform;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import javax.inject.Singleton;

@Singleton
public class Libc {

    static {
        Native.register(Platform.C_LIBRARY_NAME);
    }

    /* command values */
    public static final int F_DUPFD = 0;	/* duplicate file descriptor */
    public static final int F_GETFD = 1;		/* get file descriptor flags */
    public static final int F_SETFD = 2;		/* set file descriptor flags */
    public static final int F_GETFL = 3;		/* get file status flags */
    public static final int F_SETFL = 4;		/* set file status flags */

    public static final int F_GETOWN = 5;	/* get SIGIO/SIGURG proc/pgrp */
    public static final int F_SETOWN = 6;	/* set SIGIO/SIGURG proc/pgrp */

    public static final int F_GETLK  = 7;	/* get record locking information */
    public static final int F_SETLK  = 8;	/* set record locking information */
    public static final int F_SETLKW = 9;	/* F_SETLK; wait if blocked */

    /* file descriptor flags (F_GETFD, F_SETFD) */
    public static final int FD_CLOEXEC = 1;	/* close-on-exec flag */

    /* record locking flags (F_GETLK, F_SETLK, F_SETLKW) */
    public static final int F_RDLCK = 1;	/* shared or read lock */
    public static final int F_UNLCK = 2;	/* unlock */
    public static final int F_WRLCK = 3;	/* exclusive or write lock */

    public static final int F_WAIT  = 0x010;		/* Wait until lock is granted */
    public static final int F_FLOCK = 0x020; 	/* Use flock(2) semantics for lock */
    public static final int F_POSIX = 0x040; 	/* Use POSIX semantics for lock */

    public static final int O_RDONLY  = 0x0000;
    public static final int O_WRONLY  = 0x0001;
    public static final int O_RDWR    = 0x0002;
    public static final int O_ACCMODE = 0x0003;

    public static native int open(String pathname,
             int flags);

    public static native int write(int fd,
              Pointer buffer,
              int n_byte) throws LastErrorException;

    public static native int close(int fd) throws LastErrorException;

    public static native void read(int fd,
              Pointer buffer,
              int n_byte) throws LastErrorException;

    public static native int fcntl(int fd,
              int operation,
              int args) throws LastErrorException;

    public static native int pipe(int[] pipeFds) throws LastErrorException;
}
