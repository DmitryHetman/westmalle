////Copyright 2015 Erik De Rijcke
////
////Licensed under the Apache License,Version2.0(the"License");
////you may not use this file except in compliance with the License.
////You may obtain a copy of the License at
////
////http://www.apache.org/licenses/LICENSE-2.0
////
////Unless required by applicable law or agreed to in writing,software
////distributed under the License is distributed on an"AS IS"BASIS,
////WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
////See the License for the specific language governing permissions and
////limitations under the License.
//package org.westmalle.wayland.protocol;
//
//import org.freedesktop.wayland.server.Client;
//import org.freedesktop.wayland.server.WlTouchResource;
//import org.freedesktop.wayland.server.jna.WaylandServerLibrary;
//import org.freedesktop.wayland.server.jna.WaylandServerLibraryMapping;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.powermock.api.mockito.PowerMockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import static com.google.common.truth.Truth.assertThat;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@RunWith(PowerMockRunner.class)
//@PrepareForTest(WaylandServerLibrary.class)
//public class WlTouchTest {
//
//    @Mock
//    private WaylandServerLibraryMapping waylandServerLibraryMapping;
//
//    private WlTouch wlTouch;
//
//    @Before
//    public void setUp() throws Exception {
//        PowerMockito.mockStatic(WaylandServerLibrary.class);
//        when(WaylandServerLibrary.INSTANCE()).thenReturn(this.waylandServerLibraryMapping);
//        this.wlTouch = new WlTouch();
//    }
//
//    @Test
//    public void testRelease() throws Exception {
//        //given
//        final WlTouchResource wlTouchResource = mock(WlTouchResource.class);
//        //when
//        this.wlTouch.release(wlTouchResource);
//        //then
//        verify(wlTouchResource).destroy();
//    }
//
//    @Test
//    public void testCreate() throws Exception {
//        //given
//        final Client client  = mock(Client.class);
//        final int    version = 1;
//        final int    id      = 1;
//        //when
//        final WlTouchResource wlTouchResource = this.wlTouch.create(client,
//                                                                    version,
//                                                                    id);
//        //then
//        assertThat(wlTouchResource).isNotNull();
//        assertThat(wlTouchResource.getImplementation()).isSameAs(this.wlTouch);
//    }
//}